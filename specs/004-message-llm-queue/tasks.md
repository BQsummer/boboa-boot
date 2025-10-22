# 任务：用户消息LLM请求任务队列

**输入**：来自 `/specs/004-message-llm-queue/` 的设计文档  
**前置条件**：plan.md (必需), spec.md (用户故事必需), research.md, data-model.md, contracts/

**测试**：本功能遵循TDD原则，所有任务包含测试编写和实现。

**组织方式**：任务按用户故事分组，以便能够独立实现和测试每个故事。

## 格式：`[ID] [P?] [故事] 描述`
- **[P]**：可并行执行（不同文件，无依赖关系）
- **[故事]**：此任务所属的用户故事（例如，US1, US2, US3）
- 在描述中包含确切的文件路径

## 路径约定
本项目为单一Spring Boot Web应用：
- 源代码：`src/main/java/com/bqsummer/`
- 测试代码：`src/test/java/com/bqsummer/`
- 资源文件：`src/main/resources/`

---

## 阶段 1：设置 (共享基础设施)

**目的**：项目初始化和配置准备

- [X] T001 在 `src/main/resources/application.properties` 中添加LLM任务配置参数（robot.task.default-model-id, robot.task.message-delay-seconds）
- [X] T002 [P] 检查并确认 `src/main/resources/datasourceInit.sql` 中message和robot_task表结构完整（无需修改）

**检查点**：配置文件就绪，数据库表结构确认

---

## 阶段 2：基础构建 (阻塞性前置条件)

**目的**：核心工具类和配置类，必须在**任何**用户故事实现之前完成

**⚠️ 关键**：在此阶段完成之前，不能开始任何用户故事的工作

- [X] T003 在 `src/main/java/com/bqsummer/constant/ActionType.java` 中确认SEND_MESSAGE常量存在
- [X] T004 在 `src/main/java/com/bqsummer/common/dto/robot/SendMessagePayload.java` 中创建SEND_MESSAGE任务载荷POJO（包含messageId, senderId, receiverId, content, modelId字段）
- [X] T005 在 `src/main/java/com/bqsummer/util/JsonUtil.java` 中添加JSON序列化/反序列化工具方法（如不存在）

**检查点**：基础构建完成 - 用户故事的实现现在可以开始

---

## 阶段 3：用户故事 1 - 用户发送消息创建LLM任务 (优先级: P1) 🎯 MVP

**目标**：实现用户向AI角色发送消息时，自动创建robot_task并加载到内存队列，任务到期后调用LLM生成回复

**独立测试**：通过POST /api/v1/messages发送消息给AI用户，验证robot_task表有新记录，3秒后AI用户自动回复

### 用户故事 1 的测试 ⚠️ TDD优先

**注意：请先编写这些测试，并确保它们在实现前会失败**

- [X] T006 [P] [US1] 在 `src/test/java/com/bqsummer/service/im/MessageServiceTest.java` 中编写单元测试：测试发送消息给AI用户时创建RobotTask
- [X] T007 [P] [US1] 在 `src/test/java/com/bqsummer/service/im/MessageServiceTest.java` 中编写单元测试：测试发送消息给普通用户时不创建RobotTask
- [X] T008 [P] [US1] 在 `src/test/java/com/bqsummer/service/im/MessageServiceTest.java` 中编写单元测试：测试RobotTask的action_payload格式正确
- [X] T009 [P] [US1] 在 `src/test/java/com/bqsummer/service/robot/RobotTaskExecutorTest.java` 中编写单元测试：测试executeSendMessage方法解析action_payload
- [X] T010 [P] [US1] 在 `src/test/java/com/bqsummer/service/robot/RobotTaskExecutorTest.java` 中编写单元测试：测试executeSendMessage调用InferenceService
- [X] T011 [P] [US1] 在 `src/test/java/com/bqsummer/service/robot/RobotTaskExecutorTest.java` 中编写单元测试：测试executeSendMessage创建AI回复消息
- [X] T012 [P] [US1] 在 `src/test/java/com/bqsummer/integration/MessageLlmQueueIntegrationTest.java` 中编写集成测试：测试端到端流程（发送消息→创建任务→LLM请求→AI回复）

### 用户故事 1 的实现

- [X] T013 [US1] 在 `src/main/java/com/bqsummer/service/im/MessageService.java` 中添加isAiUser()私有方法（查询User表判断userType）
- [X] T014 [US1] 在 `src/main/java/com/bqsummer/service/im/MessageService.java` 中添加createRobotTask()私有方法（构建RobotTask对象和action_payload JSON）
- [X] T015 [US1] 在 `src/main/java/com/bqsummer/service/im/MessageService.java` 中修改sendMessage()方法：添加AI用户判断和RobotTask创建逻辑（在@Transactional事务内）
- [X] T016 [US1] 在 `src/main/java/com/bqsummer/service/im/MessageService.java` 中添加队列加载失败的警告日志记录
- [X] T017 [US1] 在 `src/main/java/com/bqsummer/service/robot/RobotTaskExecutor.java` 中实现executeSendMessage()方法：解析action_payload JSON
- [X] T018 [US1] 在 `src/main/java/com/bqsummer/service/robot/RobotTaskExecutor.java` 中的executeSendMessage()方法：调用InferenceService.chat()请求LLM
- [X] T019 [US1] 在 `src/main/java/com/bqsummer/service/robot/RobotTaskExecutor.java` 中的executeSendMessage()方法：创建AI回复Message记录
- [X] T020 [US1] 在 `src/main/java/com/bqsummer/service/robot/RobotTaskExecutor.java` 中的executeSendMessage()方法：更新Conversation表
- [X] T021 [US1] 在 `src/main/java/com/bqsummer/service/robot/RobotTaskExecutor.java` 中的executeSendMessage()方法：添加INFO级别日志记录（任务开始、LLM调用、回复创建、任务完成）
- [X] T022 [US1] 运行T006-T012的所有测试，确保全部通过

**检查点**：此时，用户故事 1 应功能完整且可独立测试 - 用户可发送消息给AI，AI自动回复

---

## 阶段 4：用户故事 2 - 任务执行失败自动重试 (优先级: P2)

**目标**：当LLM请求失败时，系统自动重试，确保消息不丢失

**独立测试**：模拟LLM服务故障，验证任务状态变更为FAILED、retry_count增加、任务重新调度

### 用户故事 2 的测试 ⚠️ TDD优先

- [X] T023 [P] [US2] 在 `src/test/java/com/bqsummer/service/robot/RobotTaskExecutorTest.java` 中编写单元测试：测试LLM调用超时时任务重试
- [X] T024 [P] [US2] 在 `src/test/java/com/bqsummer/service/robot/RobotTaskExecutorTest.java` 中编写单元测试：测试LLM返回错误时任务重试
- [X] T025 [P] [US2] 在 `src/test/java/com/bqsummer/service/robot/RobotTaskExecutorTest.java` 中编写单元测试：测试达到最大重试次数后任务不再重试
- [X] T026 [P] [US2] 在 `src/test/java/com/bqsummer/service/robot/RobotTaskExecutorTest.java` 中编写单元测试：测试重试时retry_count正确增加
- [X] T027 [P] [US2] 在 `src/test/java/com/bqsummer/integration/MessageLlmQueueIntegrationTest.java` 中编写集成测试：测试LLM失败重试后成功的完整流程

### 用户故事 2 的实现

- [X] T028 [US2] 在 `src/main/java/com/bqsummer/service/robot/RobotTaskExecutor.java` 中的executeSendMessage()方法：添加try-catch捕获所有LLM调用异常
- [X] T029 [US2] 在 `src/main/java/com/bqsummer/service/robot/RobotTaskExecutor.java` 中的executeSendMessage()方法：捕获异常后更新任务状态为FAILED并记录error_message
- [X] T030 [US2] 在 `src/main/java/com/bqsummer/service/robot/RobotTaskExecutor.java` 中的executeSendMessage()方法：判断retry_count是否达到max_retry_count
- [X] T031 [US2] 在 `src/main/java/com/bqsummer/service/robot/RobotTaskExecutor.java` 中的executeSendMessage()方法：未达到最大重试次数时重新调度任务（增加retry_count，更新scheduled_at）
- [X] T032 [US2] 在 `src/main/java/com/bqsummer/service/robot/RobotTaskExecutor.java` 中的executeSendMessage()方法：添加WARN级别日志记录失败和重试信息
- [X] T033 [US2] 运行T023-T027的所有测试，确保全部通过

**检查点**：此时，用户故事 1 **和** 2 都应可独立工作 - 系统具备容错能力

---

## 阶段 5：用户故事 3 - 任务执行日志追踪 (优先级: P2)

**目标**：记录每次LLM请求的执行情况，包括耗时、成功/失败状态，用于监控和排查

**独立测试**：执行任务后，验证robot_task_execution_log表有相应日志记录，包含execution_attempt、status、execution_duration_ms等字段

### 用户故事 3 的测试 ⚠️ TDD优先

- [X] T034 [P] [US3] 在 `src/test/java/com/bqsummer/service/robot/RobotTaskExecutorTest.java` 中编写单元测试：测试任务成功执行时创建执行日志
- [X] T035 [P] [US3] 在 `src/test/java/com/bqsummer/service/robot/RobotTaskExecutorTest.java` 中编写单元测试：测试任务失败时创建执行日志并记录错误信息
- [X] T036 [P] [US3] 在 `src/test/java/com/bqsummer/service/robot/RobotTaskExecutorTest.java` 中编写单元测试：测试执行日志包含正确的execution_duration_ms
- [X] T037 [P] [US3] 在 `src/test/java/com/bqsummer/service/robot/RobotTaskExecutorTest.java` 中编写单元测试：测试执行日志包含正确的delay_from_scheduled_ms
- [X] T038 [P] [US3] 在 `src/test/java/com/bqsummer/integration/MessageLlmQueueIntegrationTest.java` 中编写集成测试：验证端到端流程中执行日志的完整性

### 用户故事 3 的实现

**注意**：RobotTaskExecutor框架已自动创建执行日志，本故事主要验证日志记录的完整性

- [X] T039 [US3] 在 `src/main/java/com/bqsummer/service/robot/RobotTaskExecutor.java` 中确认executeSendMessage()正确抛出异常以触发框架日志记录
- [X] T040 [US3] 在 `src/main/java/com/bqsummer/service/robot/RobotTaskExecutor.java` 中验证任务执行时间计算准确（started_at和completed_at）
- [X] T041 [US3] 在 `src/test/java/com/bqsummer/integration/MessageLlmQueueIntegrationTest.java` 中添加查询robot_task_execution_log表的验证逻辑
- [X] T042 [US3] 运行T034-T038的所有测试，确保全部通过

**检查点**：所有用户故事现在都应可独立运行 - 系统具备完整的监控能力

---

## 阶段 6：完善与横切关注点

**目的**：影响多个用户故事的改进和文档更新

- [X] T043 [P] 在 `src/main/java/com/bqsummer/service/im/MessageService.java` 中添加事务回滚测试场景（数据库异常时Message和RobotTask都回滚）
- [X] T044 [P] 在 `src/main/java/com/bqsummer/service/robot/RobotTaskExecutor.java` 中添加性能日志（LLM响应时间、任务等待时间）
- [X] T045 在 `src/test/java/com/bqsummer/integration/MessageLlmQueueIntegrationTest.java` 中添加边缘场景测试：队列已满时的处理
- [X] T046 在 `src/test/java/com/bqsummer/integration/MessageLlmQueueIntegrationTest.java` 中添加边缘场景测试：scheduled_at在过去时的处理
- [X] T047 在 `src/test/java/com/bqsummer/integration/MessageLlmQueueIntegrationTest.java` 中添加性能测试：验证消息发送到任务创建延迟<50ms
- [X] T048 代码审查和重构：确保所有代码符合项目规范（中文注释、异常处理、日志级别）
- [X] T049 更新 `specs/004-message-llm-queue/quickstart.md` 文档：补充实际实现的代码片段
- [X] T050 运行完整的测试套件，确保所有测试通过且覆盖率≥80%
- [X] T051 在测试环境执行端到端验证：按照 `quickstart.md` 的5分钟演示步骤完整验证

**完成标记**：功能已全部实现且通过验证，可进入生产部署流程

---

## 依赖关系与执行顺序

### 阶段依赖关系

```
阶段 1 (设置)
    ↓
阶段 2 (基础构建) ⚠️ 阻塞所有用户故事
    ↓
阶段 3 (US1 - P1) ← MVP核心
    ↓
阶段 4 (US2 - P2) ← 可与US3并行
    ↓
阶段 5 (US3 - P2) ← 可与US2并行
    ↓
阶段 6 (完善)
```

- **设置 (阶段 1)**：无依赖 - 可立即开始
- **基础构建 (阶段 2)**：依赖于设置阶段的完成 - **阻塞**所有用户故事
- **用户故事 (阶段 3-5)**：全部依赖于基础构建阶段的完成
  - US1 (P1) 是MVP，必须首先完成
  - US2 (P2) 和 US3 (P2) 可在US1完成后并行开发
- **完善 (阶段 6)**：依赖于所有用户故事都已完成

### 用户故事依赖关系

- **用户故事 1 (P1)**：可在基础构建 (阶段 2) 后开始 - 不依赖其他故事 - **MVP必需**
- **用户故事 2 (P2)**：依赖US1的executeSendMessage实现 - 扩展异常处理逻辑
- **用户故事 3 (P2)**：依赖US1的执行流程 - 验证日志记录功能

### 每个用户故事内部（TDD流程）

1. **测试优先**：先编写所有测试（T006-T012, T023-T027, T034-T038）
2. **运行测试**：确保测试失败（红灯）
3. **最小实现**：编写最少代码使测试通过（绿灯）
4. **重构优化**：改进代码质量
5. **验证完成**：所有测试通过

### 并行机会

#### 阶段 1 (设置)
```bash
# 可并行执行
T001 & T002
```

#### 阶段 2 (基础构建)
```bash
# 可并行执行（不同文件）
T003 & T004 & T005
```

#### 阶段 3 (US1) - 测试阶段
```bash
# 所有测试可并行编写
T006 & T007 & T008 & T009 & T010 & T011 & T012
```

#### 阶段 3 (US1) - 实现阶段
```bash
# 按依赖顺序执行
T013 → T014 → T015 → T016  # MessageService修改（串行）
T017 → T018 → T019 → T020 → T021  # RobotTaskExecutor实现（串行）
T022  # 验证测试
```

#### 阶段 4 和 5 (US2 & US3)
```bash
# 两个用户故事可并行开发（如果团队能力允许）
阶段4 (T023-T033) 并行于 阶段5 (T034-T042)
```

#### 阶段 6 (完善)
```bash
# 独立任务可并行
T043 & T044 & T049
# 集成测试串行
T045 → T046 → T047
# 最后验证
T048 → T050 → T051
```

---

## 并行示例：用户故事 1

### 测试阶段（所有测试一起编写）
```bash
# 同时编写所有测试文件
git checkout -b feature/004-us1-tests

# 并行编写
vim src/test/java/com/bqsummer/service/im/MessageServiceTest.java
vim src/test/java/com/bqsummer/service/robot/RobotTaskExecutorTest.java  
vim src/test/java/com/bqsummer/integration/MessageLlmQueueIntegrationTest.java

# 运行测试（预期失败）
mvn test -Dtest=MessageServiceTest
mvn test -Dtest=RobotTaskExecutorTest
mvn test -Dtest=MessageLlmQueueIntegrationTest
```

### 实现阶段（按依赖顺序）
```bash
# 实现MessageService扩展
git checkout -b feature/004-us1-messageservice
# T013 → T014 → T015 → T016

# 实现RobotTaskExecutor
git checkout -b feature/004-us1-executor
# T017 → T018 → T019 → T020 → T021

# 合并并验证
git checkout 004-message-llm-queue
git merge feature/004-us1-messageservice
git merge feature/004-us1-executor
mvn test  # T022: 所有测试应通过
```

---

## 实现策略

### MVP优先（最小可行产品）

**MVP范围**：仅包含用户故事 1 (P1)
- 用户发送消息→创建任务→LLM请求→AI回复

**MVP验收标准**：
- 用户可向AI角色发送消息并在5秒内收到回复
- 任务创建和执行成功率≥99%
- 所有US1测试通过

**MVP后增量**：
- 第一次增量：添加US2（失败重试）
- 第二次增量：添加US3（执行日志验证）

### TDD工作流

每个任务严格遵循TDD循环：

1. **红灯**：编写失败的测试
   - 明确验收标准
   - 定义接口契约
   
2. **绿灯**：最小实现
   - 仅编写通过测试所需的代码
   - 不过度设计
   
3. **重构**：改进代码
   - 消除重复
   - 优化结构
   - 添加中文注释
   
4. **验证**：确保测试仍然通过
   - 运行完整测试套件
   - 检查覆盖率

### 代码审查检查清单

每个任务完成后，确认：

- ✅ 所有测试使用 `@DisplayName("中文描述")`
- ✅ 业务逻辑使用中文注释
- ✅ 使用 `@Transactional` 确保事务一致性
- ✅ 异常处理完整（捕获、记录、重试）
- ✅ 日志级别正确（INFO/WARN/ERROR）
- ✅ 无硬编码值（使用配置文件）
- ✅ 遵循最小化修改原则
- ✅ 无SQL文件在migration目录

---

## 任务统计

- **总任务数**：51
- **设置阶段**：2个任务
- **基础阶段**：3个任务
- **US1 (P1)**：17个任务（7个测试 + 10个实现）
- **US2 (P2)**：11个任务（5个测试 + 6个实现）
- **US3 (P2)**：9个任务（5个测试 + 4个实现）
- **完善阶段**：9个任务

### 并行机会统计

- **阶段1**：2个任务可并行
- **阶段2**：3个任务可并行
- **US1测试**：7个测试可并行编写
- **US2测试**：5个测试可并行编写
- **US3测试**：5个测试可并行编写
- **US2和US3**：两个故事可整体并行开发
- **完善阶段**：部分任务可并行（3-4个）

**理想情况下**（3-4人团队）：
- 阶段1-2：1天
- US1：2-3天（测试1天，实现2天）
- US2和US3并行：2天
- 完善：1-2天
- **总计**：6-8个工作日

**单人开发**：
- 阶段1-2：1天
- US1：3-4天
- US2：2天
- US3：1-2天
- 完善：1-2天
- **总计**：8-11个工作日

---

## MVP交付范围

**MVP定义**：完成阶段1-3（设置 + 基础 + US1）

**交付清单**：
- ✅ 配置文件就绪（application.properties）
- ✅ MessageService支持AI用户消息自动创建任务
- ✅ RobotTaskExecutor.executeSendMessage实现LLM调用
- ✅ 端到端流程可运行：用户发消息→AI自动回复
- ✅ 所有US1测试通过（单元测试+集成测试）
- ✅ 代码审查通过
- ✅ 快速开始文档可用

**MVP验收**：
1. 按照quickstart.md执行5分钟演示
2. 所有测试通过且覆盖率≥80%
3. 在测试环境成功运行3次完整流程
4. 性能满足：消息发送到AI回复<5秒

---

## 格式验证

✅ 所有任务遵循清单格式：`- [ ] [ID] [P?] [Story?] 描述`  
✅ 每个任务包含确切的文件路径  
✅ 测试任务标记为[P]且在实现前  
✅ 用户故事任务标记为[US1]/[US2]/[US3]  
✅ 任务ID按执行顺序递增（T001-T051）  
✅ 所有依赖关系清晰标注
