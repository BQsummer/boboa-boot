# 功能规格说明书：任务抢占机制从乐观锁改为声明式领取

**功能分支**: `006-replace-optimistic-lock-claim`  
**创建日期**: 2025-10-24  
**状态**: 草稿  
**输入**: 用户描述: "乐观锁基于version在抢占和完成之间要求version不变。LLM调用耗时很长时，任何其他并发写（heartbeat、审计、手动更新等）会导致version改变，持续冲突导致反复抢占失败。解决思路：不要在长事务/长操作期间依赖version比对。改为'声明式领取（claim）'或分布式锁：在DB行上原子UPDATE把任务标记为 RUNNING 并写入 locked_by（或 owner），只用 status 判断（UPDATE ... WHERE status='PENDING'），只有一次会成功（affected=1）。完成或失败时，要求 locked_by 等于当前实例 ID 再去更新/转状态，避免别的实例误操作。并移除version字段"

## 用户场景与测试 *（必填）*

### 用户故事 1 - 任务领取不受长时操作影响 (优先级: P1)

当多个机器人实例同时从任务队列中领取任务执行时，系统应使用声明式领取机制（基于实例ID所有权），而不是乐观锁版本号。这样即使任务执行过程中（如LLM推理）耗时很长，其他并发写操作（如心跳更新、审计日志、手动状态修改）也不会导致任务完成时的状态更新失败，确保任务能够正常执行完毕并更新为完成状态。

**设定此优先级的理由**: 这是核心缺陷修复，直接影响任务执行的可靠性。当前乐观锁机制在LLM长时调用场景下，导致任务反复抢占失败，严重影响系统稳定性和用户体验。

**独立测试**: 可通过创建多个待执行任务，模拟多实例并发抢占，并在任务执行期间模拟心跳更新等并发写操作，验证任务是否能够成功完成而不会因版本冲突失败。

**验收场景**:

1. **假如** 存在一个PENDING状态的任务，**当** 实例A使用声明式领取机制（设置locked_by为实例A的ID）将其标记为RUNNING时，**那么** 该任务应被成功领取，其他实例尝试领取该任务时应失败（affected=0）
2. **假如** 实例A已领取任务并正在执行（耗时30秒的LLM调用），**当** 心跳更新任务的heartbeat_at字段时，**那么** 任务执行完成后仍能成功更新状态为DONE，不会因为数据变更而失败
3. **假如** 实例A已领取任务并正在执行，**当** 管理员手动更新任务的error_message字段时，**那么** 实例A完成任务后更新状态为DONE时应成功（基于locked_by验证而不是version）
4. **假如** 任务被实例A领取（locked_by=实例A），**当** 实例B尝试更新该任务状态为DONE或FAILED时，**那么** 更新应失败（因为locked_by不匹配），防止误操作

---

### 用户故事 2 - 移除版本字段及相关逻辑 (优先级: P1)

系统应从robot_task表中移除version字段，同时删除RobotTask实体类中的@Version注解及version属性，并更新所有依赖乐观锁version比对的SQL语句和代码逻辑，改为基于locked_by字段的所有权验证，确保代码库中不再存在任何版本号相关的遗留逻辑。

**设定此优先级的理由**: 彻底移除旧机制是保证新机制可靠运行的前提，避免遗留代码导致混淆或潜在bug。

**独立测试**: 可通过代码审查和集成测试验证所有版本号相关代码已被移除，所有任务状态更新操作均使用locked_by验证。

**验收场景**:

1. **假如** 数据库表robot_task存在version列，**当** 执行数据库迁移脚本时，**那么** version列应被成功删除
2. **假如** RobotTask实体类中存在version字段和@Version注解，**当** 执行代码重构时，**那么** 这些内容应被完全移除
3. **假如** RobotTaskExecutor中存在依赖version字段的SQL WHERE条件（如eq("version", task.getVersion())），**当** 执行代码重构时，**那么** 这些条件应被替换为基于locked_by的验证
4. **假如** 执行全量单元测试和集成测试，**当** 测试完成时，**那么** 所有测试应通过，不应出现version相关的错误或警告

---

### 用户故事 3 - 任务失败重试保持所有权 (优先级: P2)

当任务执行失败需要重试时，系统应将任务状态从RUNNING改回PENDING，同时清空locked_by字段（释放所有权），让其他实例或同一实例在下次调度时可以重新领取该任务，避免任务被永久锁定在失败的实例上。

**设定此优先级的理由**: 确保失败任务的重试机制正常工作，提升系统的容错能力和任务执行成功率。

**独立测试**: 可通过模拟任务执行失败场景，验证任务状态被正确重置为PENDING且locked_by被清空，后续能被重新领取执行。

**验收场景**:

1. **假如** 实例A领取了任务（locked_by=实例A）并开始执行，**当** 任务执行失败（如LLM调用超时）时，**那么** 任务状态应更新为PENDING，locked_by字段应被清空，scheduled_at应设置为未来重试时间
2. **假如** 任务执行失败被重置为PENDING且locked_by已清空，**当** 下次任务加载器运行时，**那么** 该任务应能被其他实例或同一实例重新领取并执行
3. **假如** 任务达到最大重试次数，**当** 最后一次执行失败时，**那么** 任务状态应更新为FAILED，locked_by字段应清空（可选，因为已失败）

---

### 边缘场景

- 当实例A在执行任务过程中崩溃（未能更新状态为DONE或FAILED），locked_by仍然指向实例A，该任务会如何处理？（应通过超时检测机制重新加载并清空locked_by）
- 当两个实例几乎同时尝试领取同一任务时，数据库如何保证只有一个实例成功？（通过UPDATE WHERE status='PENDING'的原子性保证）
- 当实例ID格式发生变化或重复时，locked_by字段的验证逻辑是否仍然可靠？（应确保实例ID唯一性）
- 系统如何处理locked_by字段长度限制？（应根据实际实例ID格式设置合理的字段长度）

## 需求 *（必填）*

### 功能性需求

- **FR-001**: 系统**必须**在robot_task表中新增locked_by字段（varchar类型，长度建议255），用于存储领取任务的实例ID
- **FR-002**: 系统**必须**在RobotTask实体类中新增lockedBy属性（String类型）
- **FR-003**: 系统**必须**移除robot_task表中的version字段
- **FR-004**: 系统**必须**移除RobotTask实体类中的version属性和@Version注解
- **FR-005**: RobotTaskExecutor的tryAcquireTask方法**必须**修改为使用声明式领取：执行UPDATE robot_task SET status='RUNNING', locked_by=?, started_at=?, heartbeat_at=? WHERE id=? AND status='PENDING'，并验证affected rows是否等于1
- **FR-006**: tryAcquireTask方法**必须**移除所有与version字段相关的WHERE条件和SET语句
- **FR-007**: RobotTaskExecutor的updateTaskStatusToDone方法**必须**修改为：UPDATE robot_task SET status='DONE', completed_at=? WHERE id=? AND locked_by=?，确保只有拥有所有权的实例能更新
- **FR-008**: updateTaskStatusToDone方法**必须**移除所有与version字段相关的WHERE条件和SET语句
- **FR-009**: RobotTaskExecutor的handleTaskFailure方法**必须**在任务需要重试时清空locked_by字段（SET locked_by=NULL），并移除version相关逻辑
- **FR-010**: handleTaskFailure方法**必须**在任务达到最大重试次数标记为FAILED时，清空locked_by字段（可选，但建议实现）
- **FR-011**: 所有涉及robot_task表更新的SQL语句**必须**移除version字段的WHERE条件和SET操作
- **FR-012**: 系统**必须**使用当前Java实例的唯一标识（如IP地址+进程ID或UUID）作为locked_by的值
- **FR-013**: 系统**必须**在datasourceInit.sql文件中添加数据库迁移脚本：ALTER TABLE robot_task ADD COLUMN locked_by VARCHAR(255) DEFAULT NULL 和 ALTER TABLE robot_task DROP COLUMN version
- **FR-014**: 系统**必须**更新所有相关的单元测试和集成测试，移除version相关的断言和验证逻辑
- **FR-015**: 系统**必须**记录详细日志，当任务领取失败（affected=0）或状态更新因locked_by不匹配失败时，记录相关信息以便排查

### 关键实体 *（如果功能涉及数据，请包含此项）*

- **RobotTask（机器人任务）**: 代表一个待执行的机器人任务，关键属性包括：
  - id: 任务唯一标识
  - status: 任务状态（PENDING, RUNNING, DONE, FAILED, TIMEOUT）
  - locked_by: 领取该任务的实例ID（新增），用于验证所有权
  - scheduled_at: 预定执行时间
  - started_at: 实际开始执行时间
  - completed_at: 完成时间
  - heartbeat_at: 最后心跳时间
  - retry_count: 已重试次数
  - ~~version: 乐观锁版本号（移除）~~

## 成功标准 *（必填）*

### 可衡量的成果

- **SC-001**: 在模拟LLM长时调用（30秒以上）的场景下，任务执行完成的成功率应达到100%（相比当前乐观锁机制的频繁冲突失败）
- **SC-002**: 在多实例（至少3个）并发抢占任务的压力测试中，不应出现任务被重复执行的情况（通过声明式领取保证互斥）
- **SC-003**: 当任务执行过程中有并发写操作（如心跳更新、手动状态修改）时，任务状态更新的成功率应达到100%
- **SC-004**: 所有现有的单元测试和集成测试在代码重构后应继续通过，不应引入新的测试失败
- **SC-005**: 系统运行日志中不应出现version字段相关的错误或警告信息
- **SC-006**: 数据库robot_task表的schema变更应在系统启动时自动执行完成，无需手动干预
- **SC-007**: 任务失败重试后应能被重新领取并成功执行，重试成功率应与原机制一致或更高
