# 任务：模型实例管理

**输入**：来自 `/specs/001-model-instance-management/` 的设计文档
**前置条件**：plan.md (必需), spec.md (用户故事必需), research.md, data-model.md, contracts/

**测试**：本项目遵循 TDD 原则，所有功能实现前必须先编写测试。

**组织方式**：任务按用户故事分组，以便能够独立实现和测试每个故事。

## 格式：`[ID] [P?] [故事] 描述`
- **[P]**：可并行执行（不同文件，无依赖关系）
- **[故事]**：此任务所属的用户故事（例如，US1, US2, US3）
- 在描述中包含确切的文件路径

## 路径约定
- 项目根目录：`/Users/xinhuang/workspace/boboa-boot`
- 源代码：`src/main/java/com/bqsummer/model/`
- 资源文件：`src/main/resources/`
- 测试代码：`src/test/java/com/bqsummer/model/`

---

## 阶段 1：设置 (共享基础设施)

**目的**：项目初始化和基本结构

- [X] T001 根据实施计划在 `src/main/java/com/bqsummer/model/` 创建包结构（entity/dto/mapper/service/router/adapter/controller/job/exception）
- [X] T002 在 `pom.xml` 中添加 Spring AI 1.0.0-M5 依赖
- [X] T003 [P] 在 `src/main/java/com/bqsummer/configuration/` 创建 SpringAiConfig.java 配置类

---

## 阶段 2：基础构建 (阻塞性前置条件)

**目的**：核心基础设施，必须在**任何**用户故事实现之前完成

**⚠️ 关键**：在此阶段完成之前，不能开始任何用户故事的工作

- [X] T004 创建数据库迁移脚本 `src/main/resources/db/migration/V001__create_model_management_tables.sql`，包含 5 个表的创建语句
- [X] T005 [P] 在 `src/main/java/com/bqsummer/model/entity/` 创建 AiModel.java 实体类（对应 ai_model 表）
- [X] T006 [P] 在 `src/main/java/com/bqsummer/model/entity/` 创建 RoutingStrategy.java 实体类（对应 routing_strategy 表）
- [X] T007 [P] 在 `src/main/java/com/bqsummer/model/entity/` 创建 StrategyModelRelation.java 实体类（对应 strategy_model_relation 表）
- [X] T008 [P] 在 `src/main/java/com/bqsummer/model/entity/` 创建 ModelHealthStatus.java 实体类（对应 model_health_status 表）
- [X] T009 [P] 在 `src/main/java/com/bqsummer/model/entity/` 创建 ModelRequestLog.java 实体类（对应 model_request_log 表）
- [X] T010 [P] 在 `src/main/java/com/bqsummer/model/entity/` 创建枚举类：ModelType.java, StrategyType.java, HealthStatus.java, RequestType.java, ResponseStatus.java
- [X] T011 [P] 在 `src/main/java/com/bqsummer/model/mapper/` 创建 AiModelMapper.java 接口（继承 BaseMapper）
- [X] T012 [P] 在 `src/main/java/com/bqsummer/model/mapper/` 创建 RoutingStrategyMapper.java 接口
- [X] T013 [P] 在 `src/main/java/com/bqsummer/model/mapper/` 创建 StrategyModelRelationMapper.java 接口
- [X] T014 [P] 在 `src/main/java/com/bqsummer/model/mapper/` 创建 ModelHealthStatusMapper.java 接口
- [X] T015 [P] 在 `src/main/java/com/bqsummer/model/mapper/` 创建 ModelRequestLogMapper.java 接口
- [X] T016 [P] 在 `src/main/resources/mapper/model/` 创建对应的 MyBatis XML 映射文件（5个）
- [X] T017 在 `src/main/java/com/bqsummer/model/exception/` 创建自定义异常类：ModelNotFoundException.java, ModelValidationException.java, RoutingException.java
- [X] T018 [P] 在 `src/main/java/com/bqsummer/util/` 创建 EncryptionUtil.java 工具类（使用 Spring Security Crypto 实现 AES-256 加密）
- [ ] T019 运行数据库迁移脚本，验证所有表创建成功

**检查点**：基础构建完成 - 用户故事的实现现在可以并行开始

---

## 阶段 3：用户故事 1 - 模型注册与配置 (优先级: P1) 🎯 MVP

**目标**：实现模型的注册功能，允许管理员添加新的 AI 模型实例并配置基本信息

**独立测试**：可以通过注册一个测试模型（如ChatGPT），验证所有必填信息都能正确保存（包括 API 密钥加密），并能查看已注册的模型列表

### 用户故事 1 的测试 ⚠️

**注意：请先编写这些测试，并确保它们在实现前会失败**

- [X] T020 [P] [US1] 在 `src/test/java/com/bqsummer/model/service/` 创建 AiModelServiceTest.java，编写模型注册测试（包括验证 API 密钥加密、唯一性约束、字段验证）
- [X] T021 [P] [US1] 在 `src/test/java/com/bqsummer/model/controller/` 创建 AiModelControllerTest.java，编写注册接口集成测试（POST /api/v1/models）
- [X] T022 [P] [US1] 在 AiModelControllerTest.java 中编写模型列表查询测试（GET /api/v1/models，包括分页和过滤）

### 用户故事 1 的实现

- [X] T023 [P] [US1] 在 `src/main/java/com/bqsummer/model/dto/` 创建 ModelRegisterRequest.java DTO（包含所有注册必填字段）
- [X] T024 [P] [US1] 在 `src/main/java/com/bqsummer/model/dto/` 创建 ModelResponse.java DTO（响应对象，不包含敏感信息如 API 密钥）
- [X] T025 [P] [US1] 在 `src/main/java/com/bqsummer/model/dto/` 创建 ModelQueryRequest.java DTO（列表查询参数，支持分页和过滤）
- [X] T026 [US1] 在 `src/main/java/com/bqsummer/model/service/` 创建 AiModelService.java 接口和 AiModelServiceImpl.java 实现类
- [X] T027 [US1] 在 AiModelServiceImpl.java 中实现 registerModel() 方法（包括 API 密钥加密、唯一性验证、API 端点连通性验证）
- [X] T028 [US1] 在 AiModelServiceImpl.java 中实现 listModels() 方法（支持分页、按 provider/modelType/enabled 过滤）
- [X] T029 [US1] 在 `src/main/java/com/bqsummer/model/controller/` 创建 AiModelController.java
- [X] T030 [US1] 在 AiModelController.java 中实现 POST /api/v1/models 端点（模型注册）
- [X] T031 [US1] 在 AiModelController.java 中实现 GET /api/v1/models 端点（模型列表查询）
- [ ] T032 [US1] 运行所有 US1 测试，确保通过

**检查点**：此时，用户故事 1 应功能完整且可独立测试（可以注册模型并查看列表）

---

## 阶段 4：用户故事 2 - 模型元信息管理 (优先级: P1)

**目标**：实现模型的 CRUD 操作，允许管理员查看详情、编辑配置、删除模型

**独立测试**：可以先注册一个测试模型，然后对其执行查看详情、编辑（如修改API端点）、删除等操作，验证所有操作都能正确执行并持久化（包括删除保护逻辑）

### 用户故事 2 的测试 ⚠️

- [ ] T033 [P] [US2] 在 AiModelServiceTest.java 中添加 getModelById()、updateModel()、deleteModel() 测试
- [ ] T034 [P] [US2] 在 AiModelServiceTest.java 中添加删除保护测试（模型被路由策略引用时禁止删除）
- [ ] T035 [P] [US2] 在 AiModelControllerTest.java 中添加 GET /api/v1/models/{id}、PUT /api/v1/models/{id}、DELETE /api/v1/models/{id} 集成测试

### 用户故事 2 的实现

- [ ] T036 [P] [US2] 在 `src/main/java/com/bqsummer/model/dto/` 创建 ModelUpdateRequest.java DTO（允许更新除 name 和 version 外的字段）
- [ ] T037 [US2] 在 AiModelServiceImpl.java 中实现 getModelById() 方法（返回模型详情，不包含 API 密钥明文）
- [ ] T038 [US2] 在 AiModelServiceImpl.java 中实现 updateModel() 方法（支持更新配置，API 密钥变更时重新加密）
- [ ] T039 [US2] 在 AiModelServiceImpl.java 中实现 deleteModel() 方法（检查是否被路由策略引用，使用 @Transactional）
- [ ] T040 [US2] 在 AiModelController.java 中实现 GET /api/v1/models/{id} 端点
- [ ] T041 [US2] 在 AiModelController.java 中实现 PUT /api/v1/models/{id} 端点
- [ ] T042 [US2] 在 AiModelController.java 中实现 DELETE /api/v1/models/{id} 端点
- [ ] T043 [US2] 运行所有 US2 测试，确保通过

**检查点**：此时，用户故事 1 **和** 2 都应可独立工作（完整的模型 CRUD 功能）

---

## 阶段 5：用户故事 3 - 统一接口调用 (优先级: P2)

**目标**：提供统一的推理接口，屏蔽不同模型提供商的 API 差异，支持自动路由和指定模型调用

**独立测试**：可以注册2-3个不同类型的模型（如ChatGPT和Qwen），然后通过统一接口发送相同的请求，验证能够成功调用不同模型并返回结果

### 用户故事 3 的测试 ⚠️

- [ ] T044 [P] [US3] 在 `src/test/java/com/bqsummer/model/adapter/` 创建 OpenAiAdapterTest.java，测试 OpenAI 模型适配器
- [ ] T045 [P] [US3] 在 `src/test/java/com/bqsummer/model/adapter/` 创建 QwenAdapterTest.java，测试 Qwen 模型适配器
- [ ] T046 [P] [US3] 在 `src/test/java/com/bqsummer/model/service/` 创建 UnifiedInferenceServiceTest.java，测试统一推理服务（包括自动路由和指定模型）
- [ ] T047 [P] [US3] 在 `src/test/java/com/bqsummer/model/controller/` 创建 UnifiedInferenceControllerTest.java，测试推理接口集成测试

### 用户故事 3 的实现

- [ ] T048 [P] [US3] 在 `src/main/java/com/bqsummer/model/dto/` 创建 InferenceRequest.java DTO（统一推理请求，包含 prompt、modelId、temperature 等参数）
- [ ] T049 [P] [US3] 在 `src/main/java/com/bqsummer/model/dto/` 创建 InferenceResponse.java DTO（统一响应格式）
- [ ] T050 [P] [US3] 在 `src/main/java/com/bqsummer/model/adapter/` 创建 ModelAdapter.java 接口（定义统一适配器规范）
- [ ] T051 [P] [US3] 在 `src/main/java/com/bqsummer/model/adapter/` 创建 OpenAiAdapter.java（基于 Spring AI ChatClient 实现 OpenAI 适配）
- [ ] T052 [P] [US3] 在 `src/main/java/com/bqsummer/model/adapter/` 创建 QwenAdapter.java（自定义 HTTP 客户端适配 Qwen API）
- [ ] T053 [P] [US3] 在 `src/main/java/com/bqsummer/model/adapter/` 创建 GenericAdapter.java（通用适配器，支持未明确适配的模型）
- [ ] T054 [US3] 在 `src/main/java/com/bqsummer/model/service/` 创建 UnifiedInferenceService.java 接口和实现类
- [ ] T055 [US3] 在 UnifiedInferenceServiceImpl.java 中实现 chat() 方法（根据 modelId 选择适配器，调用模型）
- [ ] T056 [US3] 在 UnifiedInferenceServiceImpl.java 中实现请求日志记录（使用 REQUIRES_NEW 独立事务）
- [ ] T057 [US3] 在 UnifiedInferenceServiceImpl.java 中实现错误处理和重试机制（超时 3 秒，重试 1 次）
- [ ] T058 [US3] 在 `src/main/java/com/bqsummer/model/controller/` 创建 UnifiedInferenceController.java
- [ ] T059 [US3] 在 UnifiedInferenceController.java 中实现 POST /api/v1/inference/chat 端点
- [ ] T060 [US3] 运行所有 US3 测试，确保通过

**检查点**：此时，可以通过统一接口调用不同模型（手动指定 modelId）

---

## 阶段 6：用户故事 4 - 基于策略的模型路由 (优先级: P2)

**目标**：实现路由策略的配置和执行，支持标签匹配、负载均衡、优先级路由

**独立测试**：可以注册多个模型并配置不同的路由策略（如轮询、优先级），然后发送多个请求，验证请求确实按照配置的策略分配到不同模型

### 用户故事 4 的测试 ⚠️

- [ ] T061 [P] [US4] 在 `src/test/java/com/bqsummer/model/router/` 创建 TagBasedRouterTest.java，测试标签匹配路由器
- [ ] T062 [P] [US4] 在 `src/test/java/com/bqsummer/model/router/` 创建 LoadBalancingRouterTest.java，测试负载均衡路由器（轮询、最少连接）
- [ ] T063 [P] [US4] 在 `src/test/java/com/bqsummer/model/router/` 创建 PriorityRouterTest.java，测试优先级路由器（包括故障转移）
- [ ] T064 [P] [US4] 在 `src/test/java/com/bqsummer/model/service/` 创建 RoutingStrategyServiceTest.java，测试路由策略服务
- [ ] T065 [P] [US4] 在 `src/test/java/com/bqsummer/model/controller/` 创建 RoutingStrategyControllerTest.java，测试策略管理接口

### 用户故事 4 的实现

- [ ] T066 [P] [US4] 在 `src/main/java/com/bqsummer/model/dto/` 创建 StrategyCreateRequest.java DTO
- [ ] T067 [P] [US4] 在 `src/main/java/com/bqsummer/model/dto/` 创建 StrategyResponse.java DTO
- [ ] T068 [P] [US4] 在 `src/main/java/com/bqsummer/model/dto/` 创建 StrategyConfig.java（策略配置 POJO，对应 JSON 字段）
- [ ] T069 [P] [US4] 在 `src/main/java/com/bqsummer/model/router/` 创建 ModelRouter.java 接口（定义 select() 方法）
- [ ] T070 [P] [US4] 在 `src/main/java/com/bqsummer/model/router/` 创建 TagBasedRouter.java（标签匹配路由实现）
- [ ] T071 [P] [US4] 在 `src/main/java/com/bqsummer/model/router/` 创建 LoadBalancingRouter.java（轮询和最少连接算法）
- [ ] T072 [P] [US4] 在 `src/main/java/com/bqsummer/model/router/` 创建 PriorityRouter.java（优先级路由和故障转移）
- [ ] T073 [US4] 在 `src/main/java/com/bqsummer/model/service/` 创建 RoutingStrategyService.java 接口和实现类
- [ ] T074 [US4] 在 RoutingStrategyServiceImpl.java 中实现 createStrategy() 方法（创建策略和模型关联，使用 @Transactional）
- [ ] T075 [US4] 在 RoutingStrategyServiceImpl.java 中实现 selectModel() 方法（根据策略类型选择路由器执行）
- [ ] T076 [US4] 在 RoutingStrategyServiceImpl.java 中实现 listStrategies()、updateStrategy()、deleteStrategy() 方法
- [ ] T077 [US4] 在 `src/main/java/com/bqsummer/model/controller/` 创建 RoutingStrategyController.java
- [ ] T078 [US4] 在 RoutingStrategyController.java 中实现策略 CRUD 接口（POST/GET/PUT/DELETE /api/v1/strategies）
- [ ] T079 [US4] 在 UnifiedInferenceServiceImpl.java 中集成路由策略（未指定 modelId 时调用 RoutingStrategyService.selectModel()）
- [ ] T080 [US4] 运行所有 US4 测试，确保通过

**检查点**：此时，可以配置路由策略，统一接口能够根据策略自动选择模型

---

## 阶段 7：用户故事 5 - 模型可用性监控 (优先级: P3)

**目标**：实现模型健康检查定时任务，记录健康状态，提供监控查询接口

**独立测试**：可以注册几个模型（包括一个故意配置错误的），然后查看监控页面，验证能够正确显示每个模型的状态（在线/离线/错误），并能查询历史可用性数据

### 用户故事 5 的测试 ⚠️

- [ ] T081 [P] [US5] 在 `src/test/java/com/bqsummer/model/service/` 创建 ModelHealthServiceTest.java，测试健康检查逻辑
- [ ] T082 [P] [US5] 在 `src/test/java/com/bqsummer/model/job/` 创建 ModelHealthCheckJobTest.java，测试定时任务执行
- [ ] T083 [P] [US5] 在 AiModelControllerTest.java 中添加健康状态查询接口测试（GET /api/v1/models/health 和 GET /api/v1/models/{id}/health/history）

### 用户故事 5 的实现

- [ ] T084 [P] [US5] 在 `src/main/java/com/bqsummer/model/dto/` 创建 HealthStatusResponse.java DTO
- [ ] T085 [P] [US5] 在 `src/main/java/com/bqsummer/model/dto/` 创建 HealthHistoryResponse.java DTO（包含 24 小时可用率数据）
- [ ] T086 [US5] 在 `src/main/java/com/bqsummer/model/service/` 创建 ModelHealthService.java 接口和实现类
- [ ] T087 [US5] 在 ModelHealthServiceImpl.java 中实现 checkHealth() 方法（发送测试请求到模型，记录响应时间和状态）
- [ ] T088 [US5] 在 ModelHealthServiceImpl.java 中实现 updateHealthStatus() 方法（更新健康状态表，计算连续失败次数和可用率）
- [ ] T089 [US5] 在 ModelHealthServiceImpl.java 中实现 getAllHealthStatus() 方法（查询所有模型健康状态）
- [ ] T090 [US5] 在 ModelHealthServiceImpl.java 中实现 getHealthHistory() 方法（查询指定模型 24 小时历史数据）
- [ ] T091 [US5] 在 `src/main/java/com/bqsummer/model/job/` 创建 ModelHealthCheckJob.java（Quartz 定时任务）
- [ ] T092 [US5] 在 ModelHealthCheckJob.java 中配置 @DisallowConcurrentExecution，实现 execute() 方法（遍历所有启用的模型执行健康检查）
- [ ] T093 [US5] 在 `src/main/resources/` 配置 Quartz CRON 表达式（每分钟执行一次）
- [ ] T094 [US5] 在 AiModelController.java 中实现 GET /api/v1/models/health 端点
- [ ] T095 [US5] 在 AiModelController.java 中实现 GET /api/v1/models/{id}/health/history 端点
- [ ] T096 [US5] 在路由器中集成健康状态过滤（仅选择 status=ONLINE 的模型）
- [ ] T097 [US5] 运行所有 US5 测试，确保通过

**检查点**：所有用户故事现在都应可独立运行，健康监控自动排除离线模型

---

## 阶段 8：完善与横切关注点

**目的**：影响多个用户故事的改进和文档完善

- [ ] T098 [P] 在 `src/main/resources/` 配置 application.properties（加密密钥、Spring AI 配置、Quartz 配置）
- [ ] T099 [P] 在 SpringAiConfig.java 中配置全局超时、重试策略
- [ ] T100 [P] 实现全局异常处理器 `src/main/java/com/bqsummer/model/exception/GlobalExceptionHandler.java`（统一错误响应格式）
- [ ] T101 添加日志记录（在关键方法中添加 @Slf4j 和日志输出）
- [ ] T102 性能优化：在 AiModelService 中添加模型列表缓存（使用 @Cacheable）
- [ ] T103 安全加固：在所有 Controller 中添加 @PreAuthorize 注解（要求 ADMIN 角色）
- [ ] T104 [P] 更新 `specs/001-model-instance-management/quickstart.md`，添加实际代码示例和测试数据
- [ ] T105 [P] 在 `README.md` 中添加模型管理功能说明和 API 文档链接
- [ ] T106 运行所有测试套件，确保覆盖率 > 80%
- [ ] T107 使用真实模型（ChatGPT 和 Qwen）进行端到端集成测试
- [ ] T108 代码审查和重构（消除重复代码，优化命名）

---

## 依赖关系与执行顺序

### 阶段依赖关系

- **设置 (阶段 1)**：无依赖 - 可立即开始
- **基础构建 (阶段 2)**：依赖于设置阶段的完成 - **阻塞**所有用户故事
- **用户故事 (阶段 3-7)**：全部依赖于基础构建阶段的完成
  - 用户故事 1 和 2 可以串行执行（US2 依赖 US1 的基础）
  - 用户故事 3 依赖 US1（需要已注册的模型）
  - 用户故事 4 依赖 US1 和 US3（需要模型和推理服务）
  - 用户故事 5 依赖 US1 和 US3（需要模型和健康检查逻辑）
- **完善 (阶段 8)**：依赖于所有核心用户故事（US1-US4）完成

### 用户故事依赖关系

```
基础构建 (T004-T019)
    ↓
US1 模型注册 (T020-T032) 🎯 MVP 核心
    ↓
US2 模型管理 (T033-T043) 🎯 MVP 核心
    ↓
US3 统一接口 (T044-T060) ← 依赖 US1
    ↓
US4 路由策略 (T061-T080) ← 依赖 US1, US3
    ↓
US5 健康监控 (T081-T097) ← 依赖 US1, US3
```

### 每个用户故事内部

- 测试**必须**在实现前编写并确保**失败**（TDD 原则）
- 先 DTO，后 Service，最后 Controller
- 先核心逻辑，后集成和优化
- 完成当前故事后再进入下一优先级

### 并行机会

#### 阶段 1 (设置)
```bash
# T001, T002, T003 可以并行
mvn dependency:tree  # T002
创建包结构          # T001
编写配置类          # T003
```

#### 阶段 2 (基础构建)
```bash
# T005-T010 实体类可以并行创建
# T011-T015 Mapper 接口可以并行创建
# T016 XML 映射可以并行编写
# T017 异常类可以并行创建
```

#### 用户故事 1
```bash
# T020, T021, T022 测试可以并行编写
# T023, T024, T025 DTO 可以并行创建
```

#### 用户故事 3
```bash
# T044, T045, T046, T047 测试可以并行
# T048, T049, T050 DTO/接口可以并行
# T051, T052, T053 适配器可以并行实现
```

#### 用户故事 4
```bash
# T061, T062, T063, T064, T065 测试可以并行
# T066, T067, T068 DTO 可以并行
# T070, T071, T072 路由器可以并行实现
```

#### 用户故事 5
```bash
# T081, T082, T083 测试可以并行
# T084, T085 DTO 可以并行
```

#### 阶段 8 (完善)
```bash
# T098, T099, T100, T104, T105 可以并行
```

---

## 并行执行示例

### 基础构建阶段并行示例
```bash
# 开发者 A：创建实体类
T005, T006, T007, T008, T009

# 开发者 B：创建 Mapper
T011, T012, T013, T014, T015

# 开发者 C：创建异常和工具类
T017, T018
```

### 用户故事 1 并行示例
```bash
# 开发者 A：编写测试
T020, T021, T022

# 开发者 B：创建 DTO
T023, T024, T025

# 等待 DTO 和测试完成后，串行实现服务和控制器
T026 → T027 → T028 → T029 → T030 → T031 → T032
```

### 用户故事 4 并行示例
```bash
# 开发者 A：标签路由
T061 (测试) → T070 (实现)

# 开发者 B：负载均衡路由
T062 (测试) → T071 (实现)

# 开发者 C：优先级路由
T063 (测试) → T072 (实现)

# 汇总：集成到服务层
T073 → T074 → T075 → T076 → T077 → T078 → T079 → T080
```

---

## 实施策略

### MVP 范围（P1 优先级）
**目标**：最小可用产品，包含核心的模型管理和基础调用能力

**包含的用户故事**：
- ✅ US1 - 模型注册与配置
- ✅ US2 - 模型元信息管理

**MVP 功能清单**：
1. 注册新模型（手动配置）
2. 查看模型列表
3. 编辑模型配置
4. 删除模型（带保护检查）
5. API 密钥加密存储

**不包含**：
- 统一推理接口（US3）
- 路由策略（US4）
- 健康监控（US5）

**MVP 交付标准**：
- 所有 US1 和 US2 测试通过
- 可以通过 Postman/curl 完成完整的模型 CRUD 操作
- API 文档完整（OpenAPI 规范）
- 代码覆盖率 > 80%

### 增量交付策略

**第一轮（1-2 天）**：
- 完成阶段 1 和阶段 2（基础构建）
- 交付 MVP（US1 + US2）
- 产出：可管理模型，但无法调用

**第二轮（2-3 天）**：
- 完成 US3（统一接口调用）
- 交付：可通过统一接口调用模型（手动指定 modelId）

**第三轮（2-3 天）**：
- 完成 US4（路由策略）
- 交付：支持自动路由和负载均衡

**第四轮（1-2 天）**：
- 完成 US5（健康监控）
- 交付：完整的模型管理系统，带监控和自动故障转移

**第五轮（1 天）**：
- 完成阶段 8（完善）
- 交付：生产就绪版本

---

## 格式验证

### 任务清单格式检查
- ✅ 所有任务都以 `- [ ]` 开头（markdown 复选框）
- ✅ 所有任务都有唯一 ID（T001-T108）
- ✅ 用户故事阶段任务都有 [US#] 标签
- ✅ 可并行任务标记 [P]
- ✅ 所有任务都包含具体文件路径

### 任务统计
- **总任务数**：108
- **设置任务**：3
- **基础构建任务**：16
- **US1 任务**：13（包含 3 个测试任务）
- **US2 任务**：11（包含 3 个测试任务）
- **US3 任务**：17（包含 4 个测试任务）
- **US4 任务**：20（包含 5 个测试任务）
- **US5 任务**：17（包含 3 个测试任务）
- **完善任务**：11

### 并行机会统计
- **可并行任务**：约 45 个（标记 [P]）
- **并行度**：在基础构建和各用户故事内部，3-4 名开发者可同时工作
- **串行依赖**：用户故事之间有明确的先后顺序（US1 → US2 → US3 → US4 → US5）

---

## 验收标准

### 代码质量
- [ ] 所有测试通过（JUnit + RestAssured）
- [ ] 代码覆盖率 > 80%
- [ ] 无 SonarQube 严重缺陷
- [ ] 遵循阿里巴巴 Java 开发规范

### 功能完整性
- [ ] 所有 30 个功能需求（FR-001 到 FR-030）实现
- [ ] 所有 5 个用户故事的验收场景通过
- [ ] 所有 10 个成功标准（SC-001 到 SC-010）达成

### 性能指标
- [ ] 路由决策 < 100ms
- [ ] 接口响应 < 500ms（不含模型响应时间）
- [ ] 支持管理 ≥ 50 个模型实例
- [ ] 成功率 ≥ 95%

### 文档完整性
- [ ] API 文档（OpenAPI 规范）完整
- [ ] quickstart.md 可直接使用
- [ ] 代码注释覆盖所有公共方法
- [ ] README 包含功能说明和示例

---

**任务清单状态**：✅ 已生成  
**预计总工作量**：8-10 人天  
**建议团队规模**：3-4 名开发者  
**预计交付周期**：2-3 周（含测试和优化）

---

**下一步行动**：执行 `/speckit.implement` 命令开始 TDD 实现第一个任务
