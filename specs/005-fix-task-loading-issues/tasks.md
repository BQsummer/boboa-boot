# 任务：机器人任务加载修复

**输入**：来自 `/specs/005-fix-task-loading-issues/` 的设计文档
**前置条件**：plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, quickstart.md ✅

**测试要求**：本功能遵循TDD原则，所有测试任务必须在实现前完成

**组织方式**：任务按用户故事分组，以便能够独立实现和测试每个故事

## 格式：`[ID] [P?] [故事] 描述`
- **[P]**：可并行执行（不同文件，无依赖关系）
- **[故事]**：此任务所属的用户故事（例如，US1, US2, US3）
- 在描述中包含确切的文件路径

## 路径约定
- 项目根目录：`/Users/xinhuang/workspace/boboa-boot`
- 源代码：`src/main/java/com/bqsummer/`
- 测试代码：`src/test/java/com/bqsummer/`
- 配置文件：`src/main/resources/`

---

## 阶段 1：设置 (共享基础设施)

**目的**：项目初始化和环境准备

- [ ] T001 切换到功能分支 `005-fix-task-loading-issues`（已完成）
- [ ] T002 验证开发环境：Java 17、Maven、MySQL可用
- [ ] T003 阅读设计文档：spec.md、plan.md、research.md、data-model.md、quickstart.md

**检查点**：环境准备完成 ✅

---

## 阶段 2：基础构建 (阻塞性前置条件)

**目的**：核心基础设施，必须在**任何**用户故事实现之前完成

**⚠️ 关键**：在此阶段完成之前，不能开始任何用户故事的工作

- [ ] T004 验证数据库索引存在：`idx_status_scheduled (status, scheduled_at)` 在 `robot_task` 表
- [ ] T005 验证数据库表结构包含必要字段：`status`, `scheduled_at`, `updated_time`, `version` 在 `robot_task` 表
- [ ] T006 确认配置类 `RobotTaskConfiguration` 包含 `timeoutThresholdMinutes` 配置项（默认5分钟）

**检查点**：基础设施验证完成 - 用户故事的实现现在可以开始

---

## 阶段 3：用户故事 1 - 加载过期未执行任务 (优先级: P1) 🎯 MVP

**目标**：修复过期PENDING任务不被执行的缺陷，确保所有待执行任务最终都能被处理

**独立测试**：创建过期PENDING任务记录，运行RobotTaskLoaderJob，验证任务被成功加载到内存队列

**验收标准**：
- 过期PENDING任务（scheduledAt <= 当前时间）在30秒内被检测并加载
- 过期任务优先于未来任务执行
- 加载成功率100%

### 用户故事 1 的测试 (TDD - 先写测试) ⚠️

**注意：请先编写这些测试，并确保它们在实现前会失败**

- [ ] T007 [P] [US1] 创建测试类 `src/test/java/com/bqsummer/job/RobotTaskLoaderJobTest.java`
- [ ] T008 [US1] 编写测试：`shouldLoadOverduePendingTasks()` - 验证过期PENDING任务被加载
- [ ] T009 [US1] 编写测试：`shouldPrioritizeOverdueTasksOverFutureTasks()` - 验证过期任务优先于未来任务
- [ ] T010 [US1] 编写测试：`shouldHandleEmptyOverdueTasksList()` - 验证无过期任务时正常返回
- [ ] T011 [US1] 运行测试，确认全部失败（RED阶段）

### 用户故事 1 的实现

- [ ] T012 [US1] 在 `src/main/java/com/bqsummer/job/RobotTaskLoaderJob.java` 中添加私有方法 `queryOverduePendingTasks(int limit)`
  - 查询条件：`status = 'PENDING' AND scheduled_at <= NOW()`
  - 排序：`ORDER BY scheduled_at ASC, id ASC`
  - 限制：`LIMIT {limit}`
- [ ] T013 [US1] 修改 `execute()` 方法，在查询未来任务前先查询过期任务
  - 记录日志：`log.info("加载过期PENDING任务: 查询={}, 加载={}", ...)`
- [ ] T014 [US1] 实现加载逻辑：调用 `robotTaskScheduler.loadTasks(overdueTasks)`
- [ ] T015 [US1] 添加详细日志记录：查询数量、加载数量、跳过原因
- [ ] T016 [US1] 运行测试，确认全部通过（GREEN阶段）
- [ ] T017 [US1] 代码审查和重构（REFACTOR阶段）：提取常量、优化可读性

**检查点**：此时，用户故事 1 应功能完整且可独立测试

---

## 阶段 4：用户故事 2 - 检测并恢复超时RUNNING任务 (优先级: P1)

**目标**：修复长时间RUNNING任务无法被检测和恢复的缺陷，防止任务永久卡死

**独立测试**：创建超时RUNNING任务记录（updated_time超过阈值），运行RobotTaskLoaderJob，验证任务被重置为PENDING并重新加载

**验收标准**：
- 超时RUNNING任务（updated_time距当前超过5分钟）在30秒内被检测
- 任务状态成功重置为PENDING（使用乐观锁）
- 重置成功率100%（并发冲突时允许失败）

### 用户故事 2 的测试 (TDD - 先写测试) ⚠️

- [ ] T018 [US2] 编写测试：`shouldDetectTimeoutRunningTasks()` - 验证超时RUNNING任务被检测
- [ ] T019 [US2] 编写测试：`shouldResetTimeoutTasksToPending()` - 验证任务状态被重置为PENDING
- [ ] T020 [US2] 编写测试：`shouldHandleOptimisticLockConflict()` - 验证乐观锁冲突时的处理
- [ ] T021 [US2] 编写测试：`shouldLogTimeoutDuration()` - 验证超时时长被正确计算和记录
- [ ] T022 [US2] 运行测试，确认全部失败（RED阶段）

### 用户故事 2 的实现

- [ ] T023 [US2] 在 `src/main/java/com/bqsummer/job/RobotTaskLoaderJob.java` 中添加私有方法 `queryTimeoutRunningTasks(int limit)`
  - 查询条件：`status = 'RUNNING' AND updated_time <= NOW() - INTERVAL {timeoutThresholdMinutes} MINUTE`
  - 排序：`ORDER BY updated_time ASC, id ASC`
  - 限制：`LIMIT {limit}`
- [ ] T024 [US2] 在 `RobotTaskLoaderJob.java` 中添加私有方法 `resetTimeoutTasksToPending(List<RobotTask> tasks)`
  - 遍历任务列表，设置 `status = 'PENDING'`
  - 设置 `errorMessage = "检测到超时（超过X分钟），重置状态并重新调度"`
  - 调用 `robotTaskMapper.updateById(task)`（自动使用乐观锁）
  - 判断返回值，成功则添加到结果列表，失败则记录警告日志
- [ ] T025 [US2] 在 `RobotTaskLoaderJob.java` 中添加私有方法 `calculateTimeoutDuration(RobotTask task)`
  - 计算 `Duration.between(task.getUpdatedTime(), LocalDateTime.now()).toMinutes()`
- [ ] T026 [US2] 修改 `execute()` 方法，在加载过期任务后、加载未来任务前，查询和重置超时任务
  - 查询超时任务
  - 重置状态为PENDING
  - 加载成功重置的任务
  - 记录日志：`log.info("加载超时RUNNING任务: 查询={}, 重置成功={}, 加载={}", ...)`
- [ ] T027 [US2] 添加详细日志记录：超时时长、重置成功/失败原因
- [ ] T028 [US2] 运行测试，确认全部通过（GREEN阶段）
- [ ] T029 [US2] 代码审查和重构（REFACTOR阶段）

**检查点**：此时，用户故事 1 **和** 2 都应可独立工作

---

## 阶段 5：用户故事 3 - 任务加载优先级排序 (优先级: P2)

**目标**：确保最紧急的任务优先处理，避免新任务挤占补偿任务

**独立测试**：创建混合类型任务记录，监控任务执行顺序，验证符合优先级规则

**验收标准**：
- 加载顺序：过期PENDING > 超时RUNNING > 未来PENDING
- 队列容量不足时，优先分配给高优先级任务
- 日志记录完整，可追溯加载顺序

### 用户故事 3 的测试 (TDD - 先写测试) ⚠️

- [ ] T030 [US3] 编写测试：`shouldLoadTasksByPriority()` - 验证加载顺序符合优先级
- [ ] T031 [US3] 编写测试：`shouldAllocateQueueCapacityByPriority()` - 验证容量分配优先级
- [ ] T032 [US3] 编写测试：`shouldLoadAllTypesWhenCapacitySufficient()` - 验证容量充足时所有类型都被加载
- [ ] T033 [US3] 运行测试，确认全部失败（RED阶段）

### 用户故事 3 的实现

- [ ] T034 [US3] 修改 `execute()` 方法，实现分批加载逻辑
  - 初始化 `remaining = maxLoadSize`
  - 加载过期PENDING任务后，更新 `remaining -= loaded1`
  - 检查 `if (remaining > 0)` 再加载超时RUNNING任务，更新 `remaining -= loaded2`
  - 检查 `if (remaining > 0)` 再加载未来PENDING任务
- [ ] T035 [US3] 优化日志输出，记录每种类型的查询和加载统计
  - 调试级别：`log.debug("开始加载任务: 时间窗口={}分钟, 超时阈值={}分钟, 最大加载数={}", ...)`
  - 信息级别：`log.info("任务加载完成: 总加载={}, 当前队列大小={}", ...)`
- [ ] T036 [US3] 运行测试，确认全部通过（GREEN阶段）
- [ ] T037 [US3] 代码审查和重构（REFACTOR阶段）

**检查点**：所有用户故事现在都应可独立运行

---

## 阶段 6：完善与横切关注点

**目的**：影响多个用户故事的改进和最终验证

- [ ] T038 [P] 运行完整测试套件：`./mvnw test -Dtest=RobotTaskLoaderJobTest`
- [ ] T039 编写集成测试（可选）：准备测试数据，启动应用，观察日志输出
- [ ] T040 [P] 代码格式化：运行 `./mvnw spotless:apply`
- [ ] T041 验证日志输出符合规范：中文注释、统计信息完整
- [ ] T042 性能测试（可选）：创建大量测试数据（10万条），验证查询性能 <2秒
- [ ] T043 代码审查：确认符合项目宪章所有原则
- [ ] T044 更新 `specs/005-fix-task-loading-issues/` 文档，标记已完成
- [ ] T045 提交代码：`git commit -m "feat: 修复机器人任务加载缺陷"`

---

## 依赖关系与执行顺序

### 阶段依赖关系

```
阶段 1 (设置)
    ↓
阶段 2 (基础构建) ← 阻塞所有用户故事
    ↓
阶段 3 (US1: 过期任务) ← MVP，最高优先级
    ↓
阶段 4 (US2: 超时任务) ← 依赖US1的查询模式
    ↓
阶段 5 (US3: 优先级排序) ← 依赖US1和US2的实现
    ↓
阶段 6 (完善)
```

### 用户故事依赖关系

- **用户故事 1 (P1)**：可在阶段2后开始 - 不依赖其他故事 ✅ MVP核心
- **用户故事 2 (P1)**：可在阶段2后开始 - 建议在US1后实施（复用查询模式）
- **用户故事 3 (P2)**：依赖US1和US2完成 - 整合优先级逻辑

### 每个用户故事内部

1. **测试优先（TDD原则）**：
   - 编写所有测试用例（T007-T011, T018-T022, T030-T033）
   - 运行测试，确认失败（RED）
   
2. **实现功能**：
   - 按任务顺序实现（T012-T017, T023-T029, T034-T037）
   - 运行测试，确认通过（GREEN）
   
3. **重构优化（REFACTOR）**：
   - 代码审查
   - 提取常量
   - 优化可读性

### 并行机会

**阶段内并行**：
- 阶段1：T001-T003 可并行执行
- 阶段2：T004-T006 可并行验证
- 测试编写：同一故事内的测试可并行编写（T008-T010, T018-T021, T030-T032）

**跨阶段并行（不推荐）**：
- US1、US2、US3 **理论上**可并行开发，但建议串行以保证质量
- 原因：三个故事都修改同一个文件 `RobotTaskLoaderJob.java`

**推荐执行策略**：
- **单人开发**：严格按阶段顺序串行执行（3+4+5）
- **多人开发**：阶段3完成后，可安排一人做阶段4，另一人准备阶段5的测试

---

## 并行示例：用户故事 1

```bash
# 1. 先并行编写所有测试（TDD）
# 终端1：编写测试 shouldLoadOverduePendingTasks
# 终端2：编写测试 shouldPrioritizeOverdueTasksOverFutureTasks
# 终端3：编写测试 shouldHandleEmptyOverdueTasksList

# 2. 运行测试确认失败
./mvnw test -Dtest=RobotTaskLoaderJobTest

# 3. 串行实现功能（因为修改同一文件）
# T012: 添加 queryOverduePendingTasks()
# T013: 修改 execute() 方法
# T014: 实现加载逻辑
# T015: 添加日志

# 4. 运行测试确认通过
./mvnw test -Dtest=RobotTaskLoaderJobTest

# 5. 重构（可选并行）
# 终端1：提取常量
# 终端2：优化日志格式
```

---

## 实施策略

### MVP 范围（最小可行产品）

**仅实现用户故事 1（阶段 3）**：
- 修复过期任务不执行的核心缺陷
- 独立可测试、可部署
- 交付核心价值：确保所有任务最终被执行

**验收标准**：
```bash
# 创建过期任务
INSERT INTO robot_task (user_id, robot_id, task_type, action_type, action_payload, scheduled_at, status)
VALUES (1, 1, 'SHORT_DELAY', 'SEND_MESSAGE', '{}', NOW() - INTERVAL 30 MINUTE, 'PENDING');

# 等待30秒（任务加载周期）
# 查看日志，应该看到：
# INFO - 加载过期PENDING任务: 查询=1, 加载=1

# 验证任务已从队列中取出执行
```

### 增量交付

1. **第一次发布**：MVP（US1）- 1-2小时开发
2. **第二次发布**：MVP + US2 - 再增加2-3小时
3. **第三次发布**：完整功能（US1+US2+US3）- 再增加1-2小时

### 预计工作量

| 阶段 | 任务数 | 预计时间 | 说明 |
|------|--------|----------|------|
| 阶段1: 设置 | 3 | 15分钟 | 环境准备和文档阅读 |
| 阶段2: 基础 | 3 | 15分钟 | 验证数据库和配置 |
| 阶段3: US1 | 11 | 1-2小时 | MVP核心功能 |
| 阶段4: US2 | 12 | 2-3小时 | 超时检测和重置 |
| 阶段5: US3 | 8 | 1-2小时 | 优先级整合 |
| 阶段6: 完善 | 8 | 1小时 | 测试和文档 |
| **总计** | **45** | **6-9小时** | 单人完整实施 |

### 风险提示

⚠️ **高风险任务**：
- T024: 状态重置逻辑 - 需要正确处理乐观锁
- T034: 优先级排序 - 需要正确管理队列容量

⚠️ **测试关键**：
- 所有测试必须先于实现（TDD原则）
- 测试必须覆盖边界情况（空列表、乐观锁冲突、容量不足）

---

## 快速开始

1. **阅读设计文档**（15分钟）：
   ```bash
   cat specs/005-fix-task-loading-issues/spec.md
   cat specs/005-fix-task-loading-issues/quickstart.md
   ```

2. **启动MVP开发**（从US1开始）：
   ```bash
   # 创建测试类
   mkdir -p src/test/java/com/bqsummer/job
   touch src/test/java/com/bqsummer/job/RobotTaskLoaderJobTest.java
   
   # 开始TDD循环
   # RED -> GREEN -> REFACTOR
   ```

3. **参考实现示例**：
   查看 `quickstart.md` 中的完整代码示例

---

## 总结

- **任务总数**：45个任务
- **用户故事数**：3个（US1-P1, US2-P1, US3-P2）
- **并行机会**：测试编写阶段、验证任务阶段
- **MVP范围**：仅US1（11个任务，1-2小时）
- **完整功能**：US1+US2+US3（31个核心任务，4-7小时）
- **独立测试**：每个用户故事都可独立测试和部署
- **TDD原则**：所有用户故事都遵循测试优先原则

**下一步**：从阶段1的T001开始执行，或使用 `/speckit.implement` 命令开始自动化实施。
