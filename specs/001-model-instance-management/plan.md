# 实施计划：模型实例管理

**分支**：`001-model-instance-management` | **日期**：2025-10-21 | **规范文档**：[spec.md](./spec.md)  
**输入**：来自 `/specs/001-model-instance-management/spec.md` 的功能规范文档

**备注**：此计划由 `/speckit.plan` 命令生成，使用 Spring AI 框架实现统一的 AI 模型管理和调用能力。

## 摘要

本功能实现一个统一的 AI 模型实例管理系统，支持：
1. 手动注册多种 AI 模型（ChatGPT、Qwen、Yi、Gemini 等）
2. 管理模型元信息（版本、API端点、上下文长度、参数量等）
3. 提供统一的 RESTful API 接口屏蔽底层模型差异
4. 基于策略的智能路由（标签匹配、负载均衡、优先级路由）
5. 模型健康监控和自动故障转移

**技术方案**：使用 Spring AI 框架作为统一抽象层，利用其 ChatClient 和 ModelOptionsProvider 机制统一不同模型提供商的接口，通过自定义路由策略实现智能请求分配。

## 技术背景

**语言/版本**：Java 17  
**主要依赖**：
- Spring Boot 3.5.5
- Spring AI 1.0.0-M5（需新增）
- Spring Security 6.5.3
- MyBatis Plus 3.5.14
- MySQL + Druid
- Quartz（用于健康检查调度）

**存储**：MySQL（持久化模型配置、路由策略、健康状态、请求日志）  
**缓存**：使用现有数据库，暂不引入 Redis（保持最小修改原则）  
**测试**：JUnit 5 + RestAssured 5.5.6（遵循 TDD 原则）  
**目标平台**：Spring Boot Web 应用（RESTful API）  
**项目类型**：单一 Spring Boot 项目（符合现有项目结构）  
**性能目标**：
- 路由决策：< 100ms
- 接口响应：< 500ms（不含模型响应时间）
- 支持管理：≥ 50 个模型实例
- 成功率：≥ 95%

**约束条件**：
- 必须使用 MyBatis Plus 进行数据访问
- 必须使用 Spring Security 进行权限控制
- 必须遵循项目宪章的 TDD 原则
- API密钥必须加密存储
- 多表操作必须使用 @Transactional

**规模/范围**：
- 4 个新数据表（Model, RoutingStrategy, ModelHealthStatus, RequestLog）
- 约 8-10 个 REST 端点
- 预计新增代码：2000-3000 行（含测试）

## 基本原则检查

*关卡：必须在阶段 0 调研前通过。在阶段 1 设计后需重新检查。*

### 必须遵守的原则

✅ **中文优先文档**：所有文档、注释、用户故事使用中文  
✅ **TDD 开发**：先编写测试，再实现功能  
✅ **事务一致性**：多表操作使用 @Transactional  
✅ **安全第一**：API密钥加密存储，端点权限控制  
✅ **最小化修改**：仅添加新功能，不修改现有代码  

### 潜在违规项说明

| 违规项 | 必要性说明 | 为何拒绝更简单的替代方案 |
|--------|-----------|-------------------------|
| 新增 Spring AI 依赖 | Spring AI 提供统一的模型抽象层，支持多种提供商，避免为每个模型编写独立适配器 | 手动实现适配器需要维护大量样板代码，且难以应对模型提供商的 API 变更 |
| 4 个新数据表 | 模型管理需要持久化配置、策略、健康状态和日志 | 使用单表或嵌入JSON会导致查询性能差、事务复杂度高，不符合规范化设计 |
| Quartz 健康检查任务 | 需要定期检查模型可用性，项目已有 Quartz 依赖 | 使用 @Scheduled 可能在集群环境下重复执行，Quartz 提供分布式调度支持 |

## 项目结构

### 文档（本功能）

```
specs/001-model-instance-management/
├── plan.md              # 本文件（实施计划）
├── research.md          # 阶段 0：技术调研结果
├── data-model.md        # 阶段 1：数据模型设计
├── quickstart.md        # 阶段 1：快速开始指南
├── contracts/           # 阶段 1：API 契约定义
│   ├── model-api.yaml   # 模型管理 API
│   ├── routing-api.yaml # 路由策略 API
│   └── inference-api.yaml # 统一推理 API
└── tasks.md             # 阶段 2：任务清单（由 /speckit.tasks 生成）
```

### 源代码（仓库根目录）

```
src/main/java/com/bqsummer/
├── model/                          # 新增：模型管理模块
│   ├── entity/                     # 实体类
│   │   ├── AiModel.java            # 模型实例实体
│   │   ├── RoutingStrategy.java    # 路由策略实体
│   │   ├── ModelHealthStatus.java  # 健康状态实体
│   │   └── ModelRequestLog.java    # 请求日志实体
│   ├── dto/                        # 数据传输对象
│   │   ├── ModelRegisterRequest.java
│   │   ├── ModelUpdateRequest.java
│   │   ├── ModelResponse.java
│   │   ├── StrategyCreateRequest.java
│   │   └── InferenceRequest.java
│   ├── mapper/                     # MyBatis Mapper
│   │   ├── AiModelMapper.java
│   │   ├── RoutingStrategyMapper.java
│   │   ├── ModelHealthStatusMapper.java
│   │   └── ModelRequestLogMapper.java
│   ├── service/                    # 业务服务层
│   │   ├── AiModelService.java     # 模型 CRUD 服务
│   │   ├── RoutingStrategyService.java  # 路由策略服务
│   │   ├── ModelHealthService.java      # 健康监控服务
│   │   ├── UnifiedInferenceService.java # 统一推理服务
│   │   └── impl/                   # 服务实现
│   │       ├── AiModelServiceImpl.java
│   │       ├── RoutingStrategyServiceImpl.java
│   │       ├── ModelHealthServiceImpl.java
│   │       └── UnifiedInferenceServiceImpl.java
│   ├── router/                     # 路由策略实现
│   │   ├── ModelRouter.java        # 路由器接口
│   │   ├── TagBasedRouter.java     # 标签匹配路由
│   │   ├── LoadBalancingRouter.java # 负载均衡路由
│   │   └── PriorityRouter.java     # 优先级路由
│   ├── adapter/                    # 模型适配器（基于 Spring AI）
│   │   ├── ModelAdapter.java       # 适配器接口
│   │   ├── OpenAiAdapter.java      # OpenAI/ChatGPT 适配器
│   │   ├── QwenAdapter.java        # Qwen 适配器
│   │   └── GenericAdapter.java     # 通用适配器
│   ├── controller/                 # REST 控制器
│   │   ├── AiModelController.java  # 模型管理接口
│   │   ├── RoutingStrategyController.java  # 路由策略接口
│   │   └── UnifiedInferenceController.java # 统一推理接口
│   ├── job/                        # 定时任务
│   │   └── ModelHealthCheckJob.java # 健康检查任务
│   └── exception/                  # 异常类
│       ├── ModelNotFoundException.java
│       ├── ModelValidationException.java
│       └── RoutingException.java
├── configuration/
│   └── SpringAiConfig.java         # Spring AI 配置

src/main/resources/
├── mapper/model/                   # MyBatis XML
│   ├── AiModelMapper.xml
│   ├── RoutingStrategyMapper.xml
│   ├── ModelHealthStatusMapper.xml
│   └── ModelRequestLogMapper.xml
└── db/migration/                   # 数据库迁移脚本
    └── V001__create_model_management_tables.sql

src/test/java/com/bqsummer/model/
├── controller/                     # 控制器测试（集成测试）
│   ├── AiModelControllerTest.java
│   ├── RoutingStrategyControllerTest.java
│   └── UnifiedInferenceControllerTest.java
├── service/                        # 服务层测试（单元测试）
│   ├── AiModelServiceTest.java
│   ├── RoutingStrategyServiceTest.java
│   ├── ModelHealthServiceTest.java
│   └── UnifiedInferenceServiceTest.java
└── router/                         # 路由器测试（单元测试）
    ├── TagBasedRouterTest.java
    ├── LoadBalancingRouterTest.java
    └── PriorityRouterTest.java
```

**结构决策**：
- 采用分层架构：Controller → Service → Mapper
- 使用包 `com.bqsummer.model` 避免与现有模块冲突
- 遵循项目现有命名规范和目录结构
- 路由器作为独立组件，支持策略模式扩展
- 适配器层封装 Spring AI 调用，便于未来扩展更多模型

## 复杂度追踪

| 违规项 | 必要性说明 | 为何拒绝更简单的替代方案 |
|--------|-----------|-------------------------|
| Spring AI 依赖 | 提供统一的 ChatClient 抽象，支持 OpenAI、Azure、Anthropic 等多个提供商，自动处理请求/响应格式差异 | 手动编写 HTTP 客户端需要处理不同的认证方式、请求格式、响应解析、错误处理，维护成本高且容易出错 |
| 4 个数据表 | 满足第三范式设计，实体间关系清晰（模型 1:N 日志，策略 M:N 模型），便于查询和维护 | 使用 JSON 列存储会导致：1) 无法建立外键约束 2) 查询性能差 3) 数据一致性难保证 4) 违反数据库设计规范 |
| Quartz 定时任务 | 健康检查需要在集群环境下避免重复执行，Quartz 提供分布式锁机制，项目已有依赖无需新增 | @Scheduled 在多实例部署时会重复执行健康检查，造成资源浪费和数据不一致 |
| 路由策略抽象层 | 支持多种路由算法（标签、轮询、优先级），策略模式便于扩展新算法，符合开闭原则 | 硬编码路由逻辑会导致：1) 难以扩展新策略 2) 测试困难 3) 违反单一职责原则 |

---

## 阶段 0：大纲与研究

需要研究的技术点：
1. ✅ Spring AI 架构和最佳实践
2. ✅ Spring AI 支持的模型提供商及配置方式
3. ✅ 模型 API 密钥的安全加密存储方案
4. ✅ 负载均衡算法实现（轮询、最少连接）
5. ✅ Quartz 集群健康检查任务配置

待明确项：无（用户已明确使用 Spring AI 实现）

**输出**：research.md（包含上述技术点的研究结果和决策）

## 阶段 1：设计与合约

### 数据模型设计

从功能规范中提取的关键实体：
1. **AiModel（模型实例）**：模型配置和元数据
2. **RoutingStrategy（路由策略）**：请求分配规则
3. **ModelHealthStatus（健康状态）**：实时可用性信息
4. **ModelRequestLog（请求日志）**：调用历史记录

### API 契约

根据用户故事生成的端点：

**模型管理 API**（对应用户故事 1、2）：
- POST /api/v1/models - 注册新模型
- GET /api/v1/models - 查询模型列表
- GET /api/v1/models/{id} - 查询模型详情
- PUT /api/v1/models/{id} - 更新模型配置
- DELETE /api/v1/models/{id} - 删除模型

**路由策略 API**（对应用户故事 4）：
- POST /api/v1/strategies - 创建路由策略
- GET /api/v1/strategies - 查询策略列表
- PUT /api/v1/strategies/{id} - 更新策略
- DELETE /api/v1/strategies/{id} - 删除策略

**统一推理 API**（对应用户故事 3）：
- POST /api/v1/inference/chat - 聊天推理
- POST /api/v1/inference/embed - 向量嵌入
- POST /api/v1/inference/rerank - 重排序

**健康监控 API**（对应用户故事 5）：
- GET /api/v1/models/health - 查询所有模型健康状态
- GET /api/v1/models/{id}/health/history - 查询模型历史可用性

**输出文件**：
- data-model.md：详细的实体设计和表结构
- contracts/model-api.yaml：OpenAPI 规范
- contracts/routing-api.yaml：OpenAPI 规范
- contracts/inference-api.yaml：OpenAPI 规范
- quickstart.md：开发者快速上手指南

### 代理上下文更新

运行脚本更新 Copilot 上下文：
```bash
.specify/scripts/bash/update-agent-context.sh copilot
```

## 阶段 2：任务规划

*（由 `/speckit.tasks` 命令生成，不在本计划中展开）*

预期任务分类：
1. 数据库设计与迁移（1-2 个任务）
2. 实体和 Mapper 开发（4-6 个任务）
3. 服务层开发（8-12 个任务，包含测试）
4. 路由器开发（3-4 个任务）
5. 控制器开发（6-9 个任务，包含集成测试）
6. 健康检查任务开发（1-2 个任务）
7. 文档和配置（2-3 个任务）

## 关键技术决策

### 1. Spring AI 集成方式

**决策**：使用 Spring AI 的 ChatClient 作为统一抽象层

**理由**：
- 提供统一的 API 接口（Prompt、ChatResponse）
- 内置支持 OpenAI、Azure OpenAI、Anthropic、Ollama 等
- 自动处理流式响应、函数调用等高级特性
- 与 Spring Boot 无缝集成，符合项目技术栈

**实现要点**：
- 为每个模型提供商创建独立的 ChatClient Bean
- 使用动态配置（从数据库加载 API Key 和端点）
- 实现统一的 ModelAdapter 接口屏蔽差异

### 2. API 密钥加密方案

**决策**：使用 Spring Security Crypto + AES-256 加密

**理由**：
- 项目已依赖 Spring Security，无需新增依赖
- AES-256 是行业标准，满足安全要求
- 支持密钥轮换和中央密钥管理

**实现要点**：
- 加密密钥存储在 application.properties（生产环境使用环境变量）
- 在 Service 层进行加密/解密，Mapper 层只处理密文
- 密钥从不输出到日志或响应中

### 3. 路由策略设计

**决策**：策略模式 + 责任链模式

**理由**：
- 每种路由算法封装为独立策略类
- 支持策略组合（先按标签过滤，再负载均衡）
- 易于扩展新算法，符合开闭原则

**实现要点**：
- ModelRouter 接口定义 select(List<AiModel>, InferenceRequest) 方法
- 每个路由器维护自身状态（如负载计数器）
- RoutingStrategyService 根据策略配置选择路由器

### 4. 健康检查机制

**决策**：Quartz 定时任务 + 简单 HTTP 健康探测

**理由**：
- Quartz 支持集群环境下的任务分布式调度
- HTTP HEAD 请求快速检测端点可用性
- 失败后指数退避，避免频繁探测

**实现要点**：
- 每分钟执行一次健康检查
- 检测逻辑：发送测试 prompt 到模型，验证响应格式
- 连续失败 3 次标记为离线，成功 1 次恢复在线
- 健康状态变更时触发事件通知路由器更新可用模型池

### 5. 事务边界设计

**决策**：Service 层方法级事务

**理由**：
- 模型注册：单表操作，无需事务
- 策略创建：策略 + 关联表操作，需要事务
- 模型删除：检查引用 + 删除操作，需要事务
- 推理调用：日志记录独立事务，避免影响推理性能

**实现要点**：
- 删除模型：@Transactional(rollbackFor = Exception.class)
- 日志记录：@Transactional(propagation = REQUIRES_NEW)

## 风险与缓解

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| Spring AI 版本不稳定（M5 里程碑版本） | 可能存在 bug 或 API 变更 | 1) 充分测试核心功能 2) 准备降级方案（手动 HTTP 调用） 3) 关注官方发布计划 |
| 模型 API 调用延迟高 | 影响用户体验 | 1) 设置合理超时（3秒）2) 实施请求重试 3) 启用故障转移 4) 考虑缓存常见请求 |
| 加密密钥泄露 | 安全风险 | 1) 密钥存储在环境变量 2) 实施访问审计 3) 定期轮换密钥 4) 使用 Vault 等密钥管理服务 |
| 并发路由决策冲突 | 负载统计不准确 | 1) 使用原子操作更新计数器 2) 定期重置统计数据 3) 接受最终一致性 |
| 健康检查影响模型配额 | 可能消耗 API 额度 | 1) 使用轻量级探测（简单 prompt）2) 失败后指数退避 3) 支持配置检查频率 |

## 下一步行动

1. 执行 `/speckit.plan` 命令完成阶段 0 和阶段 1
2. 生成 research.md、data-model.md、contracts/* 文件
3. 更新 Copilot 上下文
4. 执行 `/speckit.tasks` 命令生成详细任务清单
5. 执行 `/speckit.implement` 开始 TDD 开发

---

**计划状态**：✅ 完成（待执行阶段 0-1）  
**预计工作量**：8-10 人天  
**责任人**：待分配  
**审核人**：待分配
