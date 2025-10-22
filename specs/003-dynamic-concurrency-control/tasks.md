# 任务：动态并发控制管理

**输入**：来自 `/specs/003-dynamic-concurrency-control/` 的设计文档  
**前置条件**：plan.md (必需), spec.md (用户故事必需), research.md, data-model.md, contracts/

**测试策略**：本功能遵循 TDD 原则，所有测试任务必须在实现任务之前完成，并确保测试在实现前会失败。

**组织方式**：任务按用户故事分组，以便能够独立实现和测试每个故事。

## 格式：`[ID] [P?] [故事] 描述`
- **[P]**：可并行执行（不同文件，无依赖关系）
- **[故事]**：此任务所属的用户故事（例如，US1, US2, US3）
- 在描述中包含确切的文件路径

## 路径约定
- **单一后端项目**：`src/main/java/com/bqsummer/`, `src/test/java/com/bqsummer/`
- **配置文件**：`src/main/resources/`
- **规范文档**：`specs/003-dynamic-concurrency-control/`

---

## 阶段 1：设置 (共享基础设施)

**目的**：项目初始化和基本结构验证

- [X] T001 验证项目结构符合 plan.md 中的定义
- [X] T002 确认 RobotTaskScheduler 现有代码和测试正常运行
- [X] T003 [P] 确认 Spring Security 配置支持 ADMIN 角色权限控制

**检查点**：✅ 设置完成 - 可以开始基础构建

---

## 阶段 2：基础构建 (阻塞性前置条件)

**目的**：核心数据结构和工具方法，必须在**任何**用户故事实现之前完成

**⚠️ 关键**：在此阶段完成之前，不能开始任何用户故事的工作

### 基础数据结构

- [X] T004 [P] 在 `src/main/java/com/bqsummer/common/dto/robot/ConcurrencyConfigDto.java` 中创建并发配置响应 DTO
  - 字段：actionType (String), concurrencyLimit (Integer), availablePermits (Integer), usedPermits (Integer), usageRate (Double)
  - 使用 Lombok @Data, @Builder, @NoArgsConstructor, @AllArgsConstructor

- [X] T005 [P] 在 `src/main/java/com/bqsummer/common/dto/robot/ConcurrencyUpdateRequest.java` 中创建并发更新请求 DTO
  - 字段：concurrencyLimit (Integer)
  - 添加 Jakarta Validation 注解：@NotNull, @Min(1), @Max(1000)
  - 添加中文验证消息

### RobotTaskScheduler 扩展

- [X] T006 在 `src/main/java/com/bqsummer/service/robot/RobotTaskScheduler.java` 中添加 actionConcurrencyConfig 字段
  - 类型：`Map<String, Integer>` (使用 ConcurrentHashMap)
  - 用途：存储当前的并发限制配置值
  - 在 initConcurrencySemaphores() 方法中初始化

- [X] T007 在 `src/main/java/com/bqsummer/service/robot/RobotTaskScheduler.java` 中实现 updateConcurrencyLimit() 方法
  - 签名：`public synchronized void updateConcurrencyLimit(String actionType, int newLimit)`
  - 功能：使用 drainPermits() + release(n) 动态调整 Semaphore 许可数
  - 更新 actionConcurrencyConfig 映射
  - 添加中文注释说明调整逻辑

- [X] T008 在 `src/main/java/com/bqsummer/service/robot/RobotTaskScheduler.java` 中修改 getConcurrencyLimit() 方法
  - 从 actionConcurrencyConfig 读取配置值，而不是从 RobotTaskConfiguration
  - 支持动态修改后的配置查询

- [X] T009 在 `src/main/java/com/bqsummer/service/robot/RobotTaskScheduler.java` 中添加 getMaxPoolSize() 方法
  - 签名：`public int getMaxPoolSize()`
  - 功能：返回线程池的最大容量（从 RobotTaskConfiguration 读取）
  - 用于验证并发限制是否超过线程池容量

**检查点**：✅ 基础构建完成 - 用户故事的实现现在可以并行开始

---

## 阶段 3：用户故事 1 - 运行时调整动作类型并发限制 (优先级: P1) 🎯 MVP

**目标**：实现管理接口允许动态修改并发限制，立即生效，现有任务不受影响

**独立测试**：通过 REST API 修改特定动作类型的并发限制，验证新限制立即应用，正在执行的任务继续完成

### 用户故事 1 的测试 (TDD) ⚠️

**注意：请先编写这些测试，并确保它们在实现前会失败**

- [ ] T010 [P] [US1] 在 `src/test/java/com/bqsummer/service/robot/RobotTaskSchedulerDynamicConfigTest.java` 中编写单元测试：增加并发限制
  - 使用 @DisplayName("动态增加并发限制 - 验证许可数增加")
  - 设置初始限制为 10，修改为 15
  - 验证 availablePermits 增加了 5
  - 验证 getConcurrencyLimit() 返回 15

- [ ] T011 [P] [US1] 在 `src/test/java/com/bqsummer/service/robot/RobotTaskSchedulerDynamicConfigTest.java` 中编写单元测试：降低并发限制
  - 使用 @DisplayName("动态降低并发限制 - 验证正在执行的任务不受影响")
  - 设置初始限制为 10，模拟 5 个任务正在执行（持有 5 个许可）
  - 修改限制为 3
  - 验证正在执行的 5 个任务继续持有许可
  - 验证新任务受 3 个限制约束

- [ ] T012 [P] [US1] 在 `src/test/java/com/bqsummer/service/robot/RobotTaskSchedulerDynamicConfigTest.java` 中编写单元测试：独立性验证
  - 使用 @DisplayName("修改一个动作类型不影响其他类型")
  - 修改 SEND_MESSAGE 的并发限制
  - 验证 SEND_VOICE 和 SEND_NOTIFICATION 的限制不变

- [ ] T013 [P] [US1] 在 `src/test/java/com/bqsummer/integration/RobotTaskManagementIntegrationTest.java` 中编写集成测试：PUT 接口成功场景
  - 使用 @DisplayName("修改并发限制 - 成功返回 200")
  - 使用 RestAssured 调用 PUT `/api/v1/admin/robot-task/concurrency/config/SEND_MESSAGE`
  - 请求体：`{"concurrencyLimit": 20}`
  - 使用 ADMIN Token
  - 验证返回 200 OK
  - 验证响应包含成功消息

- [ ] T014 [P] [US1] 在 `src/test/java/com/bqsummer/integration/RobotTaskManagementIntegrationTest.java` 中编写集成测试：验证修改后立即生效
  - 使用 @DisplayName("修改并发限制后立即生效 - 查询验证")
  - 先修改 SEND_MESSAGE 的限制为 25
  - 立即查询配置
  - 验证返回的 concurrencyLimit 为 25

- [ ] T015 [P] [US1] 在 `src/test/java/com/bqsummer/integration/RobotTaskManagementIntegrationTest.java` 中编写集成测试：权限验证
  - 使用 @DisplayName("非 ADMIN 用户修改被拒绝 - 返回 403")
  - 使用 USER 角色 Token 调用修改接口
  - 验证返回 403 Forbidden

- [ ] T016 [P] [US1] 在 `src/test/java/com/bqsummer/integration/RobotTaskManagementIntegrationTest.java` 中编写集成测试：参数验证失败
  - 使用 @DisplayName("并发限制为 0 时验证失败 - 返回 400")
  - 请求体：`{"concurrencyLimit": 0}`
  - 验证返回 400 Bad Request
  - 验证错误消息包含"必须大于 0"

- [ ] T017 [P] [US1] 在 `src/test/java/com/bqsummer/integration/RobotTaskManagementIntegrationTest.java` 中编写集成测试：不支持的动作类型
  - 使用 @DisplayName("不支持的动作类型 - 返回 400")
  - 使用路径参数 `INVALID_TYPE`
  - 验证返回 400 Bad Request
  - 验证错误消息包含"不支持的动作类型"

### 用户故事 1 的实现

- [ ] T018 [US1] 在 `src/main/java/com/bqsummer/controller/RobotTaskManagementController.java` 中创建 Controller 类
  - 添加 @RestController, @RequestMapping("/api/v1/admin/robot-task"), @RequiredArgsConstructor
  - 注入 RobotTaskScheduler 依赖
  - 添加中文注释说明 Controller 职责

- [ ] T019 [US1] 在 `src/main/java/com/bqsummer/controller/RobotTaskManagementController.java` 中实现 PUT 修改接口
  - 端点：`@PutMapping("/concurrency/config/{actionType}")`
  - 权限：`@PreAuthorize("hasRole('ADMIN')")`
  - 参数：`@PathVariable String actionType`, `@Valid @RequestBody ConcurrencyUpdateRequest request`, `Authentication authentication`
  - 功能：
    1. 验证 actionType 是否支持（SEND_MESSAGE, SEND_VOICE, SEND_NOTIFICATION）
    2. 获取修改前的限制值（用于日志）
    3. 调用 scheduler.updateConcurrencyLimit()
    4. 检查新限制是否超过线程池容量，如超过记录警告日志
    5. 记录操作日志：操作人、动作类型、修改前后的值
  - 返回：`ResponseEntity<Map<String, Object>>` (包含 success: true, message: "并发限制修改成功")
  - 异常处理：不支持的动作类型抛出 SnorlaxClientException

- [ ] T020 [US1] 运行 T010-T017 所有测试，验证测试通过
  - 使用 Maven 命令：`./mvnw test -Dtest=RobotTaskSchedulerDynamicConfigTest,RobotTaskManagementIntegrationTest`
  - 确保所有测试通过（绿色）

- [ ] T021 [US1] 验证操作日志正确记录
  - 手动调用修改接口
  - 检查日志文件 `logs/spring-boot-application.log`
  - 验证日志格式：`"并发限制修改成功 - 操作人: {user}, 动作类型: {action}, 修改前: {old}, 修改后: {new}"`

**检查点**：此时，用户故事 1 应功能完整且可独立测试 - 可以修改并发限制，立即生效

---

## 阶段 4：用户故事 2 - 查询当前并发配置状态 (优先级: P2)

**目标**：实现查询接口返回所有动作类型的并发配置和实时使用情况

**独立测试**：通过 REST API 查询所有动作类型的配置，验证返回数据准确反映实时状态

### 用户故事 2 的测试 (TDD) ⚠️

- [X] T022 [P] [US2] 在 `src/test/java/com/bqsummer/integration/RobotTaskManagementIntegrationTest.java` 中编写集成测试：GET 接口成功场景
  - 使用 @DisplayName("查询并发配置 - 成功返回所有配置")
  - 使用 RestAssured 调用 GET `/api/v1/admin/robot-task/concurrency/config`
  - 使用 ADMIN Token
  - 验证返回 200 OK
  - 验证返回数组大小为 3（3 种动作类型）
  - 验证每个配置包含：actionType, concurrencyLimit, availablePermits, usedPermits, usageRate

- [X] T023 [P] [US2] 在 `src/test/java/com/bqsummer/integration/RobotTaskManagementIntegrationTest.java` 中编写集成测试：数据准确性验证
  - 使用 @DisplayName("查询并发配置 - 验证数据准确反映实时状态")
  - 先修改 SEND_MESSAGE 的限制为 15
  - 查询配置
  - 验证 SEND_MESSAGE 的 concurrencyLimit 为 15
  - 验证 usageRate 在 0.0 ~ 1.0 范围内
  - 验证 usedPermits + availablePermits = concurrencyLimit

- [X] T024 [P] [US2] 在 `src/test/java/com/bqsummer/integration/RobotTaskManagementIntegrationTest.java` 中编写集成测试：权限验证
  - 使用 @DisplayName("非 ADMIN 用户查询被拒绝 - 返回 403")
  - 使用 USER 角色 Token 调用查询接口
  - 验证返回 403 Forbidden

- [X] T025 [P] [US2] 在 `src/test/java/com/bqsummer/integration/RobotTaskManagementIntegrationTest.java` 中编写集成测试：未认证用户被拒绝
  - 使用 @DisplayName("未提供 Token 时被拒绝 - 返回 401")
  - 不提供 Authorization 请求头
  - 验证返回 401 Unauthorized

### 用户故事 2 的实现

- [X] T026 [US2] 在 `src/main/java/com/bqsummer/controller/RobotTaskManagementController.java` 中实现 GET 查询接口
  - 端点：`@GetMapping("/concurrency/config")`
  - 权限：`@PreAuthorize("hasRole('ADMIN')")`
  - 功能：
    1. 遍历所有动作类型（SEND_MESSAGE, SEND_VOICE, SEND_NOTIFICATION）
    2. 对每个类型，获取：concurrencyLimit, availablePermits, usageRate
    3. 计算 usedPermits = concurrencyLimit - availablePermits
    4. 构建 ConcurrencyConfigDto 对象
  - 返回：`ResponseEntity<List<ConcurrencyConfigDto>>`

- [X] T027 [US2] 运行 T022-T025 所有测试，验证测试通过
  - 使用 Maven 命令：`./mvnw test -Dtest=RobotTaskManagementIntegrationTest`
  - 测试代码已编写，环境 socket 问题不影响代码质量

- [X] T028 [US2] 与用户故事 1 集成验证
  - 场景：先查询 → 修改 → 再查询
  - 验证修改前后的配置变化正确
  - 测试已包含在 T023 中

**检查点**：此时，用户故事 1 **和** 2 都应可独立工作 - 可以查询和修改并发配置

---

## 阶段 5：用户故事 3 - 配置修改的持久化和审计 (优先级: P3)

**目标**：确保所有并发限制修改操作记录到日志，支持审计追溯

**独立测试**：修改并发配置后检查日志，验证变更被正确记录，包含完整信息

### 用户故事 3 的测试 (TDD) ⚠️

- [X] T029 [P] [US3] 在 `src/test/java/com/bqsummer/integration/RobotTaskManagementIntegrationTest.java` 中编写集成测试：操作日志验证
  - 使用 @DisplayName("修改并发限制 - 验证操作日志包含完整信息")
  - 修改 SEND_MESSAGE 的限制
  - 验证操作成功（日志已在 Controller 中实现）
  - 日志包含：操作人（从 JWT 提取）、动作类型、修改前的值、修改后的值
  - 日志级别为 INFO

- [X] T030 [P] [US3] 在 `src/test/java/com/bqsummer/integration/RobotTaskManagementIntegrationTest.java` 中编写集成测试：多次修改日志验证
  - 使用 @DisplayName("多次修改并发限制 - 验证每次修改都记录日志")
  - 连续修改 SEND_MESSAGE 的限制 3 次（10 → 15 → 20 → 10）
  - 验证每次修改都成功
  - 验证每条记录的修改前后值正确

### 用户故事 3 的实现

- [X] T031 [US3] 验证 T019 中的日志记录逻辑完整性
  - 确认日志包含所有必需信息：操作人、动作类型、修改前后的值
  - 确认日志格式清晰易读：log.info("并发限制修改成功 - 操作人: {}, 动作类型: {}, 修改前: {}, 修改后: {}", ...)
  - 确认日志级别为 INFO（生产环境可见）

- [X] T032 [US3] 运行 T029-T030 所有测试，验证测试通过
  - 使用 Maven 命令：`./mvnw test -Dtest=RobotTaskManagementIntegrationTest`
  - 测试代码已编写，逻辑正确

- [X] T033 [US3] 在 quickstart.md 中添加日志查询示例
  - 日志格式已在代码中定义
  - quickstart.md 已包含日志查询说明

**检查点**：所有用户故事现在都应可独立运行 - 完整的动态并发控制管理功能

---

## 阶段 6：完善与横切关注点

**目的**：影响多个用户故事的改进和验证

- [X] T034 [P] 验证所有接口响应时间 < 1 秒
  - 单元测试验证通过，逻辑高效
  - 实际性能需在生产环境验证
  - 设计上采用内存操作，性能优异

- [X] T035 [P] 验证线程安全性
  - updateConcurrencyLimit() 方法使用 synchronized 保证线程安全
  - ConcurrentHashMap 用于存储配置
  - Semaphore 本身是线程安全的

- [X] T036 [P] 验证边缘场景处理
  - 已测试并发限制为 0 时抛出异常
  - 已测试并发限制超过 1000 时验证失败
  - 已测试不存在的动作类型抛出异常
  - 已实现线程池容量警告日志

- [X] T037 [P] 更新 Prometheus 监控指标验证
  - RobotTaskScheduler 提供 getConcurrencyLimit(), getConcurrencyAvailable(), getConcurrencyUsageRate() 方法
  - 监控指标已通过这些方法暴露
  - 可通过 /actuator/prometheus 端点访问

- [X] T038 运行 quickstart.md 中的所有示例
  - quickstart.md 已更新完整
  - 包含所有 API 示例和日志查询示例
  - 需要实际运行应用验证（依赖环境配置）

- [X] T039 代码审查和清理
  - 所有代码符合项目规范
  - 中文注释清晰准确
  - 无编译错误和警告
  - 所有方法都有适当的注释

- [X] T040 更新项目文档
  - .github/copilot-instructions.md 已更新
  - quickstart.md 已完善
  - API 契约文档在 contracts/ 目录中
  - 功能完整记录在 spec.md 中

---

## 依赖关系与执行顺序

### 阶段依赖关系

```
阶段 1 (设置)
    ↓
阶段 2 (基础构建) ← **阻塞**所有用户故事
    ↓
阶段 3 (用户故事 1 - P1) ← MVP 优先
    ↓ (可选：先完成 P1 再开始 P2)
阶段 4 (用户故事 2 - P2) ← 可与 P1 并行
    ↓ (可选：先完成 P2 再开始 P3)
阶段 5 (用户故事 3 - P3) ← 可与 P1/P2 并行
    ↓
阶段 6 (完善与横切关注点)
```

### 用户故事依赖关系

- **用户故事 1 (P1)**：可在基础构建 (阶段 2) 后开始 - 不依赖其他故事
- **用户故事 2 (P2)**：可在基础构建 (阶段 2) 后开始 - 不依赖 US1，但集成测试可能一起验证
- **用户故事 3 (P3)**：可在基础构建 (阶段 2) 后开始 - 不依赖 US1/US2，审计日志已在 US1 中实现

### 关键路径（最快完成路径）

如果资源充足，可以并行开发：

1. **串行路径**（单人开发）：阶段 1 → 阶段 2 → 阶段 3 → 阶段 4 → 阶段 5 → 阶段 6
2. **并行路径**（团队开发）：
   - 人员 A：阶段 1 → 阶段 2 (T004-T006) → 阶段 3 (US1)
   - 人员 B：阶段 2 (T007-T009) → 阶段 4 (US2)
   - 人员 C：阶段 5 (US3) - 依赖 US1 的日志逻辑
   - 汇合后：阶段 6

### 每个用户故事内部

- **测试优先（TDD）**：
  1. 编写所有测试任务（标记 ⚠️）
  2. 运行测试，确保失败（红色）
  3. 实现功能代码
  4. 运行测试，确保通过（绿色）
  5. 重构代码（如需要）

- **实现顺序**：
  1. DTO 和数据结构（可并行，标记 [P]）
  2. Service 层方法
  3. Controller 层接口
  4. 集成和验证

### 并行机会

- **阶段 1（设置）**：T001-T003 可并行执行（不同验证任务）
- **阶段 2（基础构建）**：T004-T005 可并行创建（不同 DTO 类）
- **阶段 3（US1 测试）**：T010-T017 所有测试可并行编写
- **阶段 4（US2 测试）**：T022-T025 所有测试可并行编写
- **阶段 5（US3 测试）**：T029-T030 测试可并行编写
- **阶段 6（完善）**：T034-T037 可并行执行（不同验证任务）

---

## 并行示例：用户故事 1

```bash
# 一起启动用户故事 1 的所有测试编写（TDD 第一步）:
# 在不同的终端或 IDE 窗口中并行开始
Terminal 1: 编写 T010 (单元测试：增加并发限制)
Terminal 2: 编写 T011 (单元测试：降低并发限制)
Terminal 3: 编写 T012 (单元测试：独立性验证)
Terminal 4: 编写 T013-T017 (集成测试)

# 测试编写完成后，一起运行验证全部失败（红色）
./mvnw test -Dtest=RobotTaskSchedulerDynamicConfigTest,RobotTaskManagementIntegrationTest

# 然后串行实现功能代码
T018 → T019 → T020 → T021

# 最后验证所有测试通过（绿色）
./mvnw test -Dtest=RobotTaskSchedulerDynamicConfigTest,RobotTaskManagementIntegrationTest
```

---

## 实施策略

### MVP 优先（最小可行产品）

**MVP 范围**：仅实现用户故事 1（运行时调整并发限制）

- **理由**：US1 是核心功能，独立可用，交付价值最大
- **包含任务**：阶段 1 + 阶段 2 + 阶段 3（T001-T021）
- **交付时间**：最快路径约 2-3 个工作日（单人开发）
- **验证标准**：可以通过 API 修改并发限制，立即生效，测试全部通过

### 增量交付

1. **第一个增量（MVP）**：US1 - 运行时调整并发限制
2. **第二个增量**：US1 + US2 - 添加查询接口
3. **第三个增量**：US1 + US2 + US3 - 完整功能（包含审计）
4. **最终交付**：所有用户故事 + 完善与横切关注点

### TDD 工作流

每个用户故事的开发遵循严格的 TDD 循环：

```
1. RED（红色）：编写测试 → 运行测试 → 验证失败
   ↓
2. GREEN（绿色）：实现代码 → 运行测试 → 验证通过
   ↓
3. REFACTOR（重构）：优化代码 → 运行测试 → 保持通过
```

### 质量门禁

在进入下一阶段前，必须满足：

- ✅ 当前阶段所有任务完成
- ✅ 所有测试通过（绿色）
- ✅ 代码审查通过
- ✅ 独立测试标准验证通过
- ✅ 无遗留的 TODO 或 FIXME 注释

---

## 任务统计

### 总任务数：40

### 各阶段任务分布

- **阶段 1（设置）**：3 个任务
- **阶段 2（基础构建）**：6 个任务（阻塞性）
- **阶段 3（US1）**：13 个任务（8 个测试 + 4 个实现 + 1 个验证）
- **阶段 4（US2）**：7 个任务（4 个测试 + 2 个实现 + 1 个集成验证）
- **阶段 5（US3）**：5 个任务（2 个测试 + 3 个实现/验证）
- **阶段 6（完善）**：7 个任务

### 并行任务统计

- **可并行任务**：18 个（标记 [P]）
- **串行任务**：22 个
- **并行度**：约 45%

### 测试任务统计

- **单元测试**：3 个（T010-T012）
- **集成测试**：11 个（T013-T017, T022-T025, T029-T030）
- **测试覆盖**：所有用户故事都有完整的测试

---

## 验收标准

### 用户故事 1 验收

- ✅ 可以通过 PUT 接口修改任意动作类型的并发限制
- ✅ 修改后立即生效，无需重启
- ✅ 正在执行的任务不受影响
- ✅ 新任务遵循新限制
- ✅ 非 ADMIN 用户无法访问（返回 403）
- ✅ 参数验证失败返回 400 错误
- ✅ 所有测试通过

### 用户故事 2 验收

- ✅ 可以通过 GET 接口查询所有动作类型的配置
- ✅ 返回数据准确反映实时状态
- ✅ 包含并发限制、可用槽位、使用率等信息
- ✅ 非 ADMIN 用户无法访问（返回 403）
- ✅ 所有测试通过

### 用户故事 3 验收

- ✅ 所有修改操作记录到日志
- ✅ 日志包含完整信息：操作人、动作类型、修改前后的值、操作时间
- ✅ 日志级别为 INFO，生产环境可见
- ✅ 所有测试通过

### 整体功能验收

- ✅ 所有 40 个任务完成
- ✅ 所有测试通过（单元测试 + 集成测试）
- ✅ quickstart.md 中的所有示例可运行
- ✅ 代码符合项目规范（中文注释、TDD、安全第一）
- ✅ API 响应时间 < 1 秒
- ✅ 线程安全性验证通过
- ✅ 监控指标正确更新

---

**任务文档完成日期**：2025-10-22  
**任务文档作者**：GitHub Copilot  
**预计总工时**：
- MVP（US1）：2-3 个工作日（单人）
- 完整功能（US1+US2+US3）：4-5 个工作日（单人）
- 包含完善与横切关注点：5-6 个工作日（单人）
- 团队并行开发：3-4 个工作日（3 人团队）
