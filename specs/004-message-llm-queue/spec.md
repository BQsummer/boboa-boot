# 功能规格说明书：用户消息LLM请求任务队列

**功能分支**: `004-message-llm-queue`  
**创建日期**: 2025-10-22  
**状态**: 草稿  
**输入**: 用户描述: "用户发送一条信息, 这时写入rabottask表, 并写入内存队列, SEND_MESSAGE类型, 这个任务是请求llm"

## 用户场景与测试 *（必填）*

### 用户故事 1 - 用户发送消息创建LLM任务 (优先级: P1)

当用户向AI角色发送一条消息时，系统需要自动创建一个机器人任务来处理LLM请求。任务会被持久化到数据库，同时加载到内存队列中等待执行。

**设定此优先级的理由**: 这是核心功能，用户消息必须被可靠接收和处理。没有此功能，AI聊天功能无法运行。

**独立测试**: 可通过发送消息API测试完整流程 - 消息发送后，验证robot_task表有新记录，且任务已进入内存队列等待处理。

**验收场景**:

1.  **假如** 用户已登录且存在一个AI角色，**当** 用户向该AI角色发送一条文本消息时，**那么** 系统在message表中创建一条消息记录，同时在robot_task表中创建一条SEND_MESSAGE类型的任务
2.  **假如** 用户消息任务已创建，**当** 任务写入数据库成功后，**那么** 该任务自动加载到RobotTaskScheduler的内存队列中，状态为PENDING
3.  **假如** 内存队列已包含该任务，**当** scheduled_at时间到达时，**那么** 任务被调度执行，调用LLM服务生成回复

---

### 用户故事 2 - 任务执行失败自动重试 (优先级: P2)

当LLM请求因网络或服务异常失败时，系统应自动重试，确保用户消息不会丢失。

**设定此优先级的理由**: 提高系统可靠性，避免用户体验因临时故障中断。

**独立测试**: 可通过模拟LLM服务故障，验证任务状态变更和重试逻辑。

**验收场景**:

1.  **假如** 一个SEND_MESSAGE任务正在执行，**当** LLM服务调用超时或返回错误时，**那么** 任务状态更新为FAILED，retry_count增加，且任务被重新调度执行（如未达到max_retry_count）
2.  **假如** 任务已达到最大重试次数，**当** 再次失败时，**那么** 任务状态标记为FAILED，不再重试，并记录错误信息

---

### 用户故事 3 - 任务执行日志追踪 (优先级: P2)

系统需要记录每次LLM请求的执行情况，包括耗时、token使用、成功/失败状态，用于监控和问题排查。

**设定此优先级的理由**: 支持系统运维和性能优化，帮助快速定位问题。

**独立测试**: 执行任务后，验证robot_task_execution_log表有相应日志记录。

**验收场景**:

1.  **假如** 一个SEND_MESSAGE任务开始执行，**当** 任务完成（无论成功或失败）时，**那么** 在robot_task_execution_log表中创建一条执行日志，包含execution_attempt、status、execution_duration_ms、error_message等字段
2.  **假如** 任务执行成功，**当** 查询执行日志时，**那么** 日志中包含完整的性能指标（响应时间、延迟时间等）

---

### 边缘场景

- 当用户在短时间内发送大量消息时，系统如何避免内存队列溢出？
- 当数据库写入失败但任务已进入内存队列时，如何保证数据一致性？
- 当系统重启时，内存队列中未完成的任务如何恢复？
- 当LLM服务长时间不可用时，任务队列积压如何处理？
- 当scheduled_at时间设置在过去时，任务如何处理？

## 需求 *（必填）*

### 功能性需求

- **FR-001**: 系统**必须**在用户发送消息时，同时创建message记录和robot_task记录
- **FR-002**: 系统**必须**为每个用户消息创建一条SEND_MESSAGE类型的robot_task任务
- **FR-003**: robot_task记录**必须**包含以下字段：user_id（发送者）、robot_id（接收者AI角色）、task_type（任务类型）、action_type（固定为SEND_MESSAGE）、action_payload（包含消息内容的JSON）、scheduled_at（计划执行时间）
- **FR-004**: action_payload **必须**是JSON格式，至少包含message_id（消息ID）、sender_id、receiver_id、content等字段
- **FR-005**: 任务创建成功后，系统**必须**自动将任务加载到RobotTaskScheduler的内存DelayQueue中
- **FR-006**: 任务的scheduled_at时间**必须**根据业务逻辑设置，默认为立即执行（当前时间）或短延迟（如3秒后）
- **FR-007**: 系统**必须**在任务到期时，从内存队列取出任务并调用RobotTaskExecutor执行
- **FR-008**: RobotTaskExecutor **必须**在executeSendMessage方法中实现LLM请求逻辑
- **FR-009**: 执行LLM请求时，系统**必须**解析action_payload，提取消息内容，调用UnifiedInferenceController的chat接口
- **FR-010**: LLM响应内容**必须**作为AI角色的回复消息，写入message表
- **FR-011**: 任务执行完成后，系统**必须**更新robot_task状态为DONE，并记录completed_at时间
- **FR-012**: 任务执行失败时，系统**必须**更新状态为FAILED，记录error_message，并在未达到max_retry_count时重新调度
- **FR-013**: 每次任务执行**必须**在robot_task_execution_log表中创建日志记录
- **FR-014**: 内存队列容量**必须**受到配置限制（maxQueueSize），防止OOM
- **FR-015**: 当队列容量达到上限时，新任务加载**必须**被拒绝或延迟处理

### 关键实体

- **Message（消息）**: 代表用户与AI角色之间的对话消息，包含发送者、接收者、内容、类型、状态等属性
- **RobotTask（机器人任务）**: 代表需要延迟执行的任务，包含任务类型、动作类型、载荷、计划执行时间、状态、重试信息等属性
- **RobotTaskExecutionLog（任务执行日志）**: 记录任务执行的详细信息，包含执行尝试次数、状态、耗时、延迟、错误信息等属性
- **InferenceRequest（推理请求）**: 代表对LLM的推理请求，包含模型ID、提示词、生成参数等属性
- **InferenceResponse（推理响应）**: 代表LLM的推理结果，包含生成内容、token使用、耗时、成功状态等属性

## 成功标准 *（必填）*

### 可衡量的成果

- **SC-001**: 用户发送消息后，90%以上的情况下在5秒内收到AI回复（包括LLM处理时间）
- **SC-002**: 任务创建到数据库写入的成功率达到99.9%以上
- **SC-003**: 内存队列加载任务的延迟不超过100毫秒
- **SC-004**: 在正常负载下（每秒10个消息），内存队列大小不超过配置容量的80%
- **SC-005**: 任务执行失败后，重试机制能在90%的情况下成功恢复（假设LLM服务可用）
- **SC-006**: 每次任务执行都有完整的日志记录，日志记录成功率达到100%
- **SC-007**: 系统重启后，未完成任务能在5分钟内通过RobotTaskLoaderJob重新加载到内存队列
- **SC-008**: 支持单实例下SEND_MESSAGE任务并发执行数不少于10（根据配置）

## 假设与约束

### 假设

- AI角色与用户账户已通过feature 002绑定，robot_id对应的是AI角色关联的user_id
- LLM推理服务（UnifiedInferenceController）已实现并可用
- message表已存在并包含必要字段（sender_id, receiver_id, content, type等）
- robot_task表和相关基础设施（RobotTaskScheduler, RobotTaskExecutor）已在前序功能中实现
- 用户消息的scheduled_at默认为立即执行或短延迟（如3秒），不涉及长延迟场景
- action_payload的JSON格式由发送消息的Service层构建，本功能假设已提供正确格式

### 约束

- 必须遵循项目宪章中的数据库管理规范：所有SQL必须写在datasourceInit.sql
- 必须使用@Transactional确保message和robot_task的创建是原子操作
- 必须遵循TDD原则，先编写测试用例再实现功能
- 不能修改robot_task表结构（已由前序功能定义）
- action_type固定为SEND_MESSAGE，不能随意扩展
- 内存队列容量限制必须通过配置文件控制，不能硬编码

### 依赖

- 依赖feature 002：AI角色与用户账户绑定功能
- 依赖feature 001：LLM模型实例管理和推理服务
- 依赖已实现的RobotTaskScheduler和RobotTaskExecutor基础设施
- 依赖Message实体和MessageMapper已存在
- 依赖application.properties中robot.task相关配置

## 范围界定

### 包含在本功能内

- 用户发送消息时创建robot_task任务的Service层逻辑
- 将任务写入robot_task表并加载到内存队列的完整流程
- RobotTaskExecutor中executeSendMessage方法的LLM请求实现
- 解析action_payload并调用LLM推理服务
- 将LLM响应作为AI回复消息写入message表
- 任务执行日志记录到robot_task_execution_log
- 任务失败重试逻辑（已有框架，需验证在LLM场景下的正确性）
- 单元测试和集成测试覆盖上述流程

### 不包含在本功能内

- message表的创建和基础CRUD（假设已存在）
- robot_task表的创建（已在前序功能完成）
- RobotTaskScheduler和RobotTaskExecutor的核心框架（已实现，仅扩展executeSendMessage）
- LLM模型管理和推理服务实现（已在feature 001完成）
- AI角色管理和用户绑定（已在feature 002完成）
- WebSocket推送实时消息给前端（可能在后续功能实现）
- 消息队列持久化到外部消息系统（如Kafka、RabbitMQ）
- 任务优先级调度（当前按scheduled_at时间顺序）
- 任务取消或暂停功能

## 技术无关的实现指引

### 核心流程描述

1. **消息接收阶段**：
   - 用户通过API发送消息
   - 服务层验证用户权限和AI角色存在性
   - 在一个事务中同时创建message记录和robot_task记录
   - robot_task的action_payload包含完整消息信息（JSON格式）

2. **任务入队阶段**：
   - robot_task创建成功后，触发任务加载逻辑
   - 检查内存队列容量是否允许加载
   - 将任务包装为RobotTaskWrapper并加入DelayQueue
   - 队列按scheduled_at时间自动排序

3. **任务调度阶段**：
   - 调度器线程持续从DelayQueue取出到期任务
   - 使用乐观锁（version字段）抢占任务
   - 获取SEND_MESSAGE类型的并发槽位（Semaphore）
   - 将任务提交给RobotTaskExecutor异步执行

4. **LLM请求阶段**：
   - 解析action_payload JSON，提取消息内容
   - 根据robot_id确定AI角色和关联的LLM模型（可能需要配置）
   - 构建InferenceRequest对象（prompt为用户消息内容）
   - 调用UnifiedInferenceController的chat接口
   - 等待LLM响应或超时

5. **回复生成阶段**：
   - 接收InferenceResponse，提取生成的内容
   - 创建新的message记录：sender_id为robot_id，receiver_id为原消息的sender_id，content为LLM生成内容
   - 更新robot_task状态为DONE，设置completed_at
   - 记录执行日志到robot_task_execution_log

6. **异常处理阶段**：
   - 捕获所有异常（网络超时、LLM错误、数据库错误等）
   - 更新robot_task状态为FAILED，记录error_message
   - 增加retry_count，如未达到max_retry_count则重新调度
   - 记录失败日志到robot_task_execution_log

### 数据一致性保证

- message和robot_task的创建必须在同一事务中
- 任务状态变更（PENDING → RUNNING → DONE/FAILED）必须使用乐观锁
- 任务执行日志记录失败不应影响任务状态更新
- 内存队列与数据库状态应通过RobotTaskLoaderJob定期同步

### 性能考虑

- 内存队列大小应根据系统资源配置（建议不超过10000）
- SEND_MESSAGE并发数应根据LLM服务能力配置（建议10-50）
- LLM请求超时时间应合理设置（建议30秒）
- 任务执行日志应批量写入以减少数据库压力（如使用批处理或异步写入）

### 监控指标

- 每分钟创建的SEND_MESSAGE任务数
- 内存队列当前大小和使用率
- SEND_MESSAGE任务平均执行时间
- LLM请求成功率和失败率
- 任务重试次数分布
- 任务从创建到完成的端到端延迟

## 验收标准总结

本功能在以下条件下视为验收通过：

1. 所有P1用户故事的验收场景通过测试
2. 单元测试覆盖率达到80%以上，集成测试覆盖核心流程
3. 在正常负载下，成功标准SC-001到SC-008全部达成
4. 边缘场景有明确的处理逻辑（队列溢出、数据库故障、LLM不可用等）
5. 代码审查通过，遵循项目代码规范和TDD原则
6. 功能文档和API文档更新完整
7. 在测试环境验证端到端流程：用户发送消息 → 创建任务 → 调用LLM → 接收回复
