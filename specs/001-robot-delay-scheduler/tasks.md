# Tasks: 机器人延迟调度系统 (Robot Delay Scheduler System)

**Input**: Design documents from `/specs/001-robot-delay-scheduler/`
**Prerequisites**: plan.md, spec.md, data-model.md, contracts/robot-task-api.md, research.md, quickstart.md

**Tests**: 包含测试任务（遵循 TDD 原则）

**Organization**: 任务按用户故事分组，使每个故事可以独立实现和测试

## Format: `[ID] [P?] [Story] Description`
- **[P]**: 可并行执行（不同文件，无依赖）
- **[Story]**: 任务所属用户故事（例如 US1, US2, US3）
- 包含确切的文件路径

## Path Conventions
项目结构基于现有 Spring Boot 单体应用：
- Java 源码: `src/main/java/com/bqsummer/`
- 资源文件: `src/main/resources/`
- 测试代码: `src/test/java/com/bqsummer/`

---

## Phase 1: Setup (共享基础设施)

**目的**: 项目初始化和基础结构

- [X] T001 [P] 创建数据库迁移脚本 src/main/resources/db/migration/V1__create_robot_task_tables.sql
- [X] T002 [P] 创建 RobotTask 实体类 src/main/java/com/bqsummer/common/dto/robot/RobotTask.java
- [X] T003 [P] 创建 RobotTaskExecutionLog 实体类 src/main/java/com/bqsummer/common/dto/robot/RobotTaskExecutionLog.java
- [X] T004 [P] 创建 TaskType 枚举 src/main/java/com/bqsummer/common/dto/robot/TaskType.java
- [X] T005 [P] 创建 TaskStatus 枚举 src/main/java/com/bqsummer/common/dto/robot/TaskStatus.java
- [X] T006 [P] 创建 ActionType 枚举 src/main/java/com/bqsummer/common/dto/robot/ActionType.java
- [X] T007 [P] 创建 RobotTaskMapper 接口 src/main/java/com/bqsummer/mapper/robot/RobotTaskMapper.java
- [X] T008 [P] 创建 RobotTaskExecutionLogMapper 接口 src/main/java/com/bqsummer/mapper/robot/RobotTaskExecutionLogMapper.java
- [X] T009 [P] 配置 MyBatis Plus 乐观锁插件（如未启用）在 src/main/java/com/bqsummer/configuration/MybatisPlusConfiguration.java

---

## Phase 2: Foundational (阻塞性前置条件)

**目的**: 核心基础设施，必须在任何用户故事实现前完成

**⚠️ 关键**: 在此阶段完成前，不能开始用户故事工作

- [X] T010 创建 RobotTaskConfiguration 配置类 src/main/java/com/bqsummer/configuration/RobotTaskConfiguration.java
- [X] T011 [P] 创建任务包装类 RobotTaskWrapper (实现 Delayed 接口) src/main/java/com/bqsummer/service/robot/RobotTaskWrapper.java
- [X] T012 [P] 创建 RobotTaskScheduler 核心调度器（包含 DelayQueue） src/main/java/com/bqsummer/service/robot/RobotTaskScheduler.java
- [X] T013 创建 RobotTaskExecutor 任务执行器 src/main/java/com/bqsummer/service/robot/RobotTaskExecutor.java
- [X] T014 创建 RobotTaskLoaderJob Quartz 定时任务 src/main/java/com/bqsummer/job/RobotTaskLoaderJob.java
- [X] T015 [P] 创建 application.yml 中的任务调度配置项
- [X] T016 [P] 配置 Quartz 属性 src/main/resources/quartz.properties （如需要）
- [X] T017 执行数据库迁移脚本，创建 robot_task 和 robot_task_execution_log 表

**检查点**: 基础设施就绪 - 用户故事实现现在可以并行开始

---

## Phase 3: User Story 1 - 立即回复 (Priority: P1) 🎯 MVP

**目标**: 用户发送消息后，机器人立即自动回复

**独立测试**: 发送单条消息给机器人，验证在2秒内收到自动回复

### 测试 for User Story 1 (TDD)

**注意: 先编写这些测试，确保它们失败后再实现**

- [ ] T018 [P] [US1] 编写集成测试：创建立即执行任务 src/test/java/com/bqsummer/controller/RobotTaskControllerTest.java (测试方法: testCreateImmediateTask)
- [ ] T019 [P] [US1] 编写单元测试：RobotTaskService 创建任务逻辑 src/test/java/com/bqsummer/service/robot/RobotTaskServiceTest.java (测试方法: testCreateTaskWithZeroDelay)
- [ ] T020 [P] [US1] 编写单元测试：立即任务加载到内存队列 src/test/java/com/bqsummer/service/robot/RobotTaskSchedulerTest.java (测试方法: testLoadImmediateTaskToQueue)

### 实现 for User Story 1

- [ ] T021 [US1] 实现 RobotTaskService.createTask() 方法 src/main/java/com/bqsummer/service/robot/RobotTaskService.java
- [ ] T022 [US1] 实现任务类型判断逻辑（delay_seconds = 0 → IMMEDIATE）在 RobotTaskService 中
- [ ] T023 [US1] 实现立即任务加载逻辑：创建后直接加入 DelayQueue（在 RobotTaskService 中）
- [ ] T024 [US1] 实现 RobotTaskExecutor.execute() 方法：执行 SEND_MESSAGE 操作
- [ ] T025 [US1] 实现乐观锁更新：PENDING → RUNNING 状态转换（在 RobotTaskExecutor 中）
- [ ] T026 [US1] 实现任务完成逻辑：RUNNING → DONE 状态转换（在 RobotTaskExecutor 中）
- [ ] T027 [US1] 实现执行日志记录：RobotTaskExecutionLog 插入（在 RobotTaskExecutor 中）
- [ ] T028 [US1] 创建 CreateTaskRequest DTO src/main/java/com/bqsummer/common/vo/req/robot/CreateTaskRequest.java
- [ ] T029 [US1] 创建 TaskResponse DTO src/main/java/com/bqsummer/common/vo/resp/robot/TaskResponse.java
- [ ] T030 [US1] 实现 RobotTaskController.createTask() 端点 src/main/java/com/bqsummer/controller/RobotTaskController.java
- [ ] T031 [US1] 添加请求参数验证（@Validated, @NotNull 等）在 CreateTaskRequest 中
- [ ] T032 [US1] 添加用户权限验证：仅能创建自己的任务（在 RobotTaskController 中）
- [ ] T033 [US1] 添加日志记录：任务创建和执行日志（使用 @Slf4j）

**检查点**: 此时，User Story 1 应该完全功能化且可独立测试 - 运行测试验证

---

## Phase 4: User Story 4 - 任务可靠性和恢复 (Priority: P1)

**目标**: 确保任务在应用重启和错误情况下可靠执行

**独立测试**: 调度任务，强制重启应用，验证所有任务恰好执行一次

### 测试 for User Story 4 (TDD)

- [ ] T034 [P] [US4] 编写集成测试：应用重启后任务恢复 src/test/java/com/bqsummer/service/robot/RobotTaskRecoveryTest.java
- [ ] T035 [P] [US4] 编写单元测试：任务失败重试逻辑 src/test/java/com/bqsummer/service/robot/RobotTaskExecutorTest.java (测试方法: testTaskRetryOnFailure)
- [ ] T036 [P] [US4] 编写单元测试：超时检测逻辑 src/test/java/com/bqsummer/service/robot/RobotTaskTimeoutTest.java
- [ ] T037 [P] [US4] 编写单元测试：历史数据清理 src/test/java/com/bqsummer/service/robot/RobotTaskCleanupTest.java

### 实现 for User Story 4

- [ ] T038 [US4] 实现应用启动时任务恢复：加载未来10分钟内的 PENDING 任务（在 RobotTaskScheduler 的 @PostConstruct 方法中）
- [ ] T039 [US4] 实现失败任务重试逻辑：捕获异常，增加 retry_count，状态回退 PENDING（在 RobotTaskExecutor 中）
- [ ] T040 [US4] 实现指数退避计算：next_execute_at = NOW() + (retry_count ^ 2) * 1分钟（在 RobotTaskExecutor 中）
- [ ] T041 [US4] 实现最大重试限制：retry_count >= max_retry_count → FAILED（在 RobotTaskExecutor 中）
- [ ] T042 [US4] 实现心跳更新：执行过程中每分钟更新 heartbeat_at（在 RobotTaskExecutor 中）
- [ ] T043 [US4] 创建 RobotTaskTimeoutRecoveryJob Quartz 定时任务 src/main/java/com/bqsummer/job/RobotTaskTimeoutRecoveryJob.java
- [ ] T044 [US4] 实现超时检测：status = RUNNING AND heartbeat_at < NOW() - 5分钟（在 RobotTaskTimeoutRecoveryJob 中）
- [ ] T045 [US4] 实现超时恢复：RUNNING → PENDING 状态重置（在 RobotTaskTimeoutRecoveryJob 中）
- [ ] T046 [US4] 创建 RobotTaskCleanupJob Quartz 定时任务 src/main/java/com/bqsummer/job/RobotTaskCleanupJob.java
- [ ] T047 [US4] 实现历史数据清理：删除 DONE (30天) 和 FAILED (90天) 任务（在 RobotTaskCleanupJob 中）
- [ ] T048 [US4] 添加 @Transactional 注解确保状态更新的原子性（在所有 Service 方法上）
- [ ] T049 [US4] 实现错误日志记录：error_message 字段存储异常堆栈（在 RobotTaskExecutor 中）

**检查点**: User Story 1 和 4 现在都应该独立工作

---

## Phase 5: User Story 2 - 短延迟回复 (Priority: P2)

**目标**: 支持秒级到分钟级的延迟任务，精确到±1秒

**独立测试**: 配置机器人在10秒后发送跟进消息，验证精确时间和送达

### 测试 for User Story 2 (TDD)

- [ ] T050 [P] [US2] 编写集成测试：创建5秒延迟任务 src/test/java/com/bqsummer/controller/RobotTaskControllerTest.java (测试方法: testCreateShortDelayTask)
- [ ] T051 [P] [US2] 编写单元测试：短延迟任务类型判断 src/test/java/com/bqsummer/service/robot/RobotTaskServiceTest.java (测试方法: testShortDelayTaskType)
- [ ] T052 [P] [US2] 编写单元测试：定时加载器加载短延迟任务 src/test/java/com/bqsummer/service/robot/RobotTaskLoaderTest.java

### 实现 for User Story 2

- [ ] T053 [US2] 实现短延迟任务类型判断：0 < delay_seconds <= 600 → SHORT_DELAY（在 RobotTaskService 中）
- [ ] T054 [US2] 实现 RobotTaskLoaderJob.execute() 方法：每30秒扫描数据库 src/main/java/com/bqsummer/job/RobotTaskLoaderJob.java
- [ ] T055 [US2] 实现查询逻辑：SELECT * FROM robot_task WHERE status = 'PENDING' AND scheduled_at BETWEEN NOW() AND NOW() + 10分钟（在 RobotTaskLoaderJob 中）
- [ ] T056 [US2] 实现任务加载到 DelayQueue：批量加载，防止重复（在 RobotTaskScheduler 中）
- [ ] T057 [US2] 实现 DelayQueue 消费线程：从队列中取出到期任务并执行（在 RobotTaskScheduler 中）
- [ ] T058 [US2] 实现多实例防重：乐观锁更新失败时跳过（在 RobotTaskExecutor 中已实现，验证即可）
- [ ] T059 [US2] 添加执行延迟计算：delay_from_scheduled_ms = 实际执行时间 - scheduled_at（在 RobotTaskExecutor 中）
- [ ] T060 [US2] 配置 Quartz cron 表达式：RobotTaskLoaderJob 每30秒执行一次（在 @JobInfo 注解中）

**检查点**: User Story 1, 2, 4 现在都应该独立工作

---

## Phase 6: User Story 3 - 长延迟调度 (Priority: P2)

**目标**: 支持小时到月级别的延迟任务，精确到±5分钟

**独立测试**: 调度24小时后的机器人动作，验证正确时间执行

### 测试 for User Story 3 (TDD)

- [ ] T061 [P] [US3] 编写集成测试：创建24小时延迟任务 src/test/java/com/bqsummer/controller/RobotTaskControllerTest.java (测试方法: testCreateLongDelayTask)
- [ ] T062 [P] [US3] 编写单元测试：长延迟任务类型判断 src/test/java/com/bqsummer/service/robot/RobotTaskServiceTest.java (测试方法: testLongDelayTaskType)
- [ ] T063 [P] [US3] 编写单元测试：长延迟任务延迟加载 src/test/java/com/bqsummer/service/robot/RobotTaskLoaderTest.java (测试方法: testLongDelayTaskLoading)

### 实现 for User Story 3

- [ ] T064 [US3] 实现长延迟任务类型判断：delay_seconds > 600 → LONG_DELAY（在 RobotTaskService 中）
- [ ] T065 [US3] 验证定时加载器正确处理长延迟任务：仅当进入10分钟窗口时加载（在 RobotTaskLoaderJob 中，已实现）
- [ ] T066 [US3] 实现 SEND_VOICE 行为类型执行逻辑（在 RobotTaskExecutor 中）
- [ ] T067 [US3] 实现 SEND_NOTIFICATION 行为类型执行逻辑（在 RobotTaskExecutor 中）
- [ ] T068 [US3] 添加任务载荷验证：根据 action_type 验证 action_payload 结构（在 CreateTaskRequest 或 Service 中）
- [ ] T069 [US3] 实现多种 action_type 的分发逻辑（在 RobotTaskExecutor 中）

**检查点**: User Story 1, 2, 3, 4 现在都应该独立工作

---

## Phase 7: User Story 5 - 系统监控 (Priority: P3)

**目标**: 提供系统健康和性能指标

**独立测试**: 访问监控端点，验证准确反映任务队列和执行状态

### 测试 for User Story 5 (TDD)

- [ ] T070 [P] [US5] 编写集成测试：获取系统监控指标 src/test/java/com/bqsummer/controller/RobotTaskControllerTest.java (测试方法: testGetMetrics)
- [ ] T071 [P] [US5] 编写单元测试：监控服务指标计算 src/test/java/com/bqsummer/service/robot/RobotTaskMonitorTest.java

### 实现 for User Story 5

- [ ] T072 [P] [US5] 创建 RobotTaskMonitor 监控服务 src/main/java/com/bqsummer/service/robot/RobotTaskMonitor.java
- [ ] T073 [US5] 实现 getQueueSize() 方法：返回 DelayQueue 当前大小（在 RobotTaskMonitor 中）
- [ ] T074 [US5] 实现 getPendingCount() 方法：查询数据库 PENDING 任务数（在 RobotTaskMonitor 中）
- [ ] T075 [US5] 实现 getRunningCount() 方法：查询数据库 RUNNING 任务数（在 RobotTaskMonitor 中）
- [ ] T076 [US5] 实现 getSuccessRate() 方法：最近1小时成功率统计（在 RobotTaskMonitor 中）
- [ ] T077 [US5] 实现 getAverageDelay() 方法：最近1小时平均执行延迟（在 RobotTaskMonitor 中）
- [ ] T078 [US5] 实现 getRetryDistribution() 方法：重试次数分布统计（在 RobotTaskMonitor 中）
- [ ] T079 [US5] 创建 MetricsResponse DTO src/main/java/com/bqsummer/common/vo/resp/robot/MetricsResponse.java
- [ ] T080 [US5] 实现 RobotTaskController.getMetrics() 端点（管理员权限）src/main/java/com/bqsummer/controller/RobotTaskController.java
- [ ] T081 [US5] 配置 Spring Actuator 自定义指标（可选）在 RobotTaskMonitor 中
- [ ] T082 [US5] 注册 Micrometer Gauge 指标：queue_size, pending_count 等（在 RobotTaskMonitor 中）

**检查点**: 所有用户故事现在都应该独立功能化

---

## Phase 8: 附加查询和管理功能

**目的**: 完善 API 功能，支持任务查询和管理

### 测试 (TDD)

- [ ] T083 [P] 编写集成测试：查询任务详情 src/test/java/com/bqsummer/controller/RobotTaskControllerTest.java (测试方法: testGetTaskById)
- [ ] T084 [P] 编写集成测试：查询任务列表（分页）src/test/java/com/bqsummer/controller/RobotTaskControllerTest.java (测试方法: testListTasks)
- [ ] T085 [P] 编写集成测试：取消任务 src/test/java/com/bqsummer/controller/RobotTaskControllerTest.java (测试方法: testCancelTask)

### 实现

- [ ] T086 [P] 实现 RobotTaskService.getTaskById() 方法 src/main/java/com/bqsummer/service/robot/RobotTaskService.java
- [ ] T087 [P] 实现 RobotTaskService.listTasks() 方法（支持分页和过滤）src/main/java/com/bqsummer/service/robot/RobotTaskService.java
- [ ] T088 [P] 实现 RobotTaskService.cancelTask() 方法（仅 PENDING 状态）src/main/java/com/bqsummer/service/robot/RobotTaskService.java
- [ ] T089 [P] 实现 RobotTaskService.getTaskLogs() 方法（管理员）src/main/java/com/bqsummer/service/robot/RobotTaskService.java
- [ ] T090 [P] 实现 RobotTaskService.retryTask() 方法（管理员）src/main/java/com/bqsummer/service/robot/RobotTaskService.java
- [ ] T091 实现 RobotTaskController.getTask() 端点 src/main/java/com/bqsummer/controller/RobotTaskController.java
- [ ] T092 实现 RobotTaskController.listTasks() 端点 src/main/java/com/bqsummer/controller/RobotTaskController.java
- [ ] T093 实现 RobotTaskController.cancelTask() 端点 src/main/java/com/bqsummer/controller/RobotTaskController.java
- [ ] T094 实现 RobotTaskController.getTaskLogs() 端点（管理员）src/main/java/com/bqsummer/controller/RobotTaskController.java
- [ ] T095 实现 RobotTaskController.retryTask() 端点（管理员）src/main/java/com/bqsummer/controller/RobotTaskController.java
- [ ] T096 添加权限验证：用户仅能查询和操作自己的任务（在 Controller 或 Service 中）
- [ ] T097 创建 TaskListRequest DTO（分页参数）src/main/java/com/bqsummer/common/vo/req/robot/TaskListRequest.java
- [ ] T098 创建 TaskListResponse DTO（分页结果）src/main/java/com/bqsummer/common/vo/resp/robot/TaskListResponse.java

---

## Phase 9: Polish & 横切关注点

**目的**: 跨用户故事的改进

- [ ] T099 [P] 添加全局异常处理：SnorlaxClientException 处理任务相关错误 src/main/java/com/bqsummer/framework/exception/GlobalExceptionHandler.java
- [ ] T100 [P] 完善日志记录：所有关键操作添加 @Slf4j 日志
- [ ] T101 [P] 添加 API 文档注释：Swagger/OpenAPI 注解（可选）
- [ ] T102 [P] 代码格式化和清理：遵循项目代码规范
- [ ] T103 [P] 性能优化：数据库查询优化，索引验证
- [ ] T104 [P] 安全加固：验证所有用户输入，防止注入攻击
- [ ] T105 [P] 配置文件完善：application.yml 添加所有配置项说明
- [ ] T106 运行 quickstart.md 验证：按快速开始指南验证功能
- [ ] T107 [P] 编写额外单元测试（如需要）：提高代码覆盖率
- [ ] T108 [P] 集成测试覆盖：端到端场景测试
- [ ] T109 更新 README.md：添加机器人调度系统使用说明
- [ ] T110 [P] 创建监控 Dashboard 配置（可选）：Grafana 配置文件

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: 无依赖 - 可立即开始
- **Foundational (Phase 2)**: 依赖 Setup 完成 - **阻塞**所有用户故事
- **User Stories (Phase 3-7)**: 全部依赖 Foundational 阶段完成
  - 然后可以并行进行（如果有足够人员）
  - 或按优先级顺序进行（P1 → P2 → P3）
- **附加功能 (Phase 8)**: 可在任何用户故事完成后开始
- **Polish (Phase 9)**: 依赖所有期望的用户故事完成

### User Story Dependencies

- **User Story 1 (P1) - 立即回复**: Foundational 完成后可开始 - 无其他依赖
- **User Story 4 (P1) - 可靠性恢复**: Foundational 完成后可开始 - 扩展 US1 功能
- **User Story 2 (P2) - 短延迟**: 依赖 Foundational - 可与 US1/US4 并行
- **User Story 3 (P2) - 长延迟**: 依赖 Foundational - 可与其他故事并行
- **User Story 5 (P3) - 监控**: 依赖 Foundational - 可在任何时候添加

### Within Each User Story

- 测试必须先编写并失败，然后再实现
- 实体类在 Service 之前
- Service 在 Controller 之前
- 核心实现在集成之前
- 完成一个故事再进入下一个优先级

### Parallel Opportunities

- Setup 阶段所有 [P] 任务可并行
- Foundational 阶段所有 [P] 任务可并行（在 Phase 2 内）
- Foundational 完成后，所有用户故事可并行开始（如团队容量允许）
- 每个用户故事内的所有 [P] 测试可并行
- 每个用户故事内的 [P] 实体类可并行
- 不同用户故事可由不同团队成员并行工作

---

## Parallel Example: User Story 1

```bash
# 同时启动 User Story 1 的所有测试：
Task: "编写集成测试：创建立即执行任务 src/test/java/com/bqsummer/controller/RobotTaskControllerTest.java"
Task: "编写单元测试：RobotTaskService 创建任务逻辑 src/test/java/com/bqsummer/service/robot/RobotTaskServiceTest.java"
Task: "编写单元测试：立即任务加载到内存队列 src/test/java/com/bqsummer/service/robot/RobotTaskSchedulerTest.java"

# 测试失败后，同时启动 DTO 创建：
Task: "创建 CreateTaskRequest DTO src/main/java/com/bqsummer/common/vo/req/robot/CreateTaskRequest.java"
Task: "创建 TaskResponse DTO src/main/java/com/bqsummer/common/vo/resp/robot/TaskResponse.java"
```

---

## Implementation Strategy

### MVP First (仅 User Story 1 和 4)

1. 完成 Phase 1: Setup
2. 完成 Phase 2: Foundational（**关键** - 阻塞所有故事）
3. 完成 Phase 3: User Story 1（立即回复）
4. 完成 Phase 4: User Story 4（可靠性）
5. **停止并验证**: 独立测试 US1 和 US4
6. 准备好后部署/演示

### Incremental Delivery

1. 完成 Setup + Foundational → 基础就绪
2. 添加 User Story 1 → 独立测试 → 部署/演示（MVP!）
3. 添加 User Story 4 → 独立测试 → 部署/演示
4. 添加 User Story 2 → 独立测试 → 部署/演示
5. 添加 User Story 3 → 独立测试 → 部署/演示
6. 添加 User Story 5 → 独立测试 → 部署/演示
7. 每个故事增加价值而不破坏之前的故事

### Parallel Team Strategy

多个开发人员：

1. 团队一起完成 Setup + Foundational
2. Foundational 完成后：
   - 开发者 A: User Story 1 + 4
   - 开发者 B: User Story 2
   - 开发者 C: User Story 3
   - 开发者 D: User Story 5
3. 故事独立完成并集成

---

## Task Summary

**总任务数**: 110 个任务

**按阶段分类**:
- Phase 1 (Setup): 9 个任务
- Phase 2 (Foundational): 8 个任务
- Phase 3 (US1 - 立即回复): 16 个任务
- Phase 4 (US4 - 可靠性): 16 个任务
- Phase 5 (US2 - 短延迟): 11 个任务
- Phase 6 (US3 - 长延迟): 9 个任务
- Phase 7 (US5 - 监控): 13 个任务
- Phase 8 (附加功能): 16 个任务
- Phase 9 (Polish): 12 个任务

**并行机会**: 约 60% 任务标记为 [P]，可在其阶段内并行执行

**独立测试准则**:
- US1: 创建立即任务并验证2秒内执行
- US2: 创建5秒延迟任务并验证±1秒精度
- US3: 创建24小时任务并验证正确时间执行
- US4: 重启应用验证任务恢复和重试
- US5: 访问监控端点验证指标准确性

**建议 MVP 范围**: User Story 1 + User Story 4（立即回复 + 可靠性）

**预计工作量**: 
- MVP (US1 + US4): 约 40 个任务，3-4 个工作日
- 完整功能: 110 个任务，约 1-1.5 周

---

## Notes

- [P] 任务 = 不同文件，无依赖
- [Story] 标签将任务映射到特定用户故事以便追踪
- 每个用户故事应该独立完成和测试
- 在实现前验证测试失败
- 每个任务或逻辑组后提交
- 在任何检查点停止以独立验证故事
- 避免：模糊任务、相同文件冲突、破坏独立性的跨故事依赖

**TDD 工作流**:
1. 编写测试（红色）
2. 实现最小代码使测试通过（绿色）
3. 重构改进代码（重构）
4. 提交并继续下一个任务

**中文注释规范**:
- 所有测试方法使用 `@DisplayName("中文描述")`
- 业务逻辑注释使用中文
- 技术细节注释可使用中英文混合
