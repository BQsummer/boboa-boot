# 实施计划：用户消息LLM请求任务队列

**分支**：`004-message-llm-queue` | **日期**：2025-10-22 | **规范文档**：[spec.md](./spec.md)
**输入**：来自 `/specs/004-message-llm-queue/spec.md` 的功能规范文档

**备注**：此模板由 `/speckit.plan` 命令填充。执行流程请参见 `.specify/templates/commands/plan.md`。

## 摘要

本功能实现用户向AI角色发送消息时，自动创建机器人任务来处理LLM请求的完整流程。核心需求包括：
1. 用户发送消息时同时创建message记录和robot_task记录（SEND_MESSAGE类型）
2. 任务写入数据库后自动加载到RobotTaskScheduler的内存DelayQueue
3. 任务到期时调度执行，RobotTaskExecutor的executeSendMessage方法调用LLM推理服务
4. LLM响应作为AI回复消息写入message表
5. 完整的失败重试机制和执行日志记录

技术方案：扩展现有MessageService的sendMessage方法，添加创建robot_task的逻辑；实现RobotTaskExecutor的executeSendMessage方法调用UnifiedInferenceController的chat接口；使用@Transactional确保数据一致性。

## 技术背景

**语言/版本**：Java 17  
**主要依赖**：Spring Boot 3.5.5, MyBatis Plus 3.5.14, Spring Security 6.5.3  
**存储**：MySQL (通过datasourceInit.sql管理)  
**数据库管理**：所有 SQL 必须写入 src/main/resources/datasourceInit.sql，**禁止**使用 migration 目录  
**测试**：JUnit 5 + RestAssured 5.5.6  
**目标平台**：Linux 服务器（Spring Boot应用）
**项目类型**：单一Web后端项目  
**性能目标**：
- 消息发送到任务创建延迟 < 50ms
- LLM任务执行吞吐量 ≥ 10 请求/秒（单实例）
- 90%的消息在5秒内收到AI回复

**约束条件**：
- 任务创建必须在message插入的同一事务中（@Transactional）
- 不能修改robot_task表结构（已由前序功能定义）
- 必须使用现有的RobotTaskScheduler和RobotTaskExecutor框架
- action_type固定为SEND_MESSAGE，不能扩展

**规模/范围**：
- 预期用户量：1万日活用户
- 预期消息量：每秒10-50条消息
- 内存队列容量：10000个任务（可配置）
- 单实例SEND_MESSAGE并发：10（可配置）

## 基本原则检查

*关卡：必须在阶段 0 调研前通过。在阶段 1 设计后需重新检查。*

### I. 中文优先的文档规范
- ✅ 所有文档使用中文
- ✅ 用户故事和需求使用中文
- ✅ 代码注释使用中文（业务逻辑）

### II. 测试驱动开发 (TDD)
- ✅ 承诺先编写测试
- ✅ 计划包含单元测试和集成测试
- ✅ 每个测试使用中文@DisplayName

### III. 事务一致性优先
- ✅ Message和RobotTask创建将在同一@Transactional事务中
- ✅ 任务状态变更使用乐观锁（version字段）
- ✅ 已规划事务回滚测试场景

### IV. 安全第一
- ✅ 使用现有的MessageController端点权限验证
- ✅ 从SecurityContext获取用户ID
- ✅ 不涉及新的敏感操作

### V. 最小化修改原则
- ✅ 仅修改MessageService.sendMessage（添加任务创建）
- ✅ 仅实现RobotTaskExecutor.executeSendMessage（当前为TODO）
- ✅ 不修改现有robot_task表结构
- ✅ 不重构无关代码

### VI. 数据库管理规范
- ✅ 无需新增表结构（message和robot_task已存在）
- ✅ 如需修改，将写入datasourceInit.sql
- ✅ 不会创建migration目录下的SQL文件

**阶段 0 检查结果**：✅ 通过，无违规项

## 项目结构

### 文档（本功能）

```
specs/004-message-llm-queue/
├── spec.md              # 功能规范文档
├── plan.md              # 本文件（/speckit.plan 命令的输出）
├── research.md          # 阶段 0 输出（/speckit.plan 命令）
├── data-model.md        # 阶段 1 输出（/speckit.plan 命令）
├── quickstart.md        # 阶段 1 输出（/speckit.plan 命令）
├── contracts/           # 阶段 1 输出（/speckit.plan 命令）
│   └── message-task-api.md
├── checklists/
│   └── requirements.md  # 需求检查清单（已完成）
└── tasks.md             # 阶段 2 输出（/speckit.tasks 命令 - 不由 /speckit.plan 创建）
```

### 源代码（仓库根目录）

本功能修改现有的单一Web后端项目结构：

```
src/main/java/com/bqsummer/
├── service/
│   ├── im/
│   │   └── MessageService.java          # 【修改】添加创建robot_task逻辑
│   └── robot/
│       ├── RobotTaskExecutor.java       # 【修改】实现executeSendMessage方法
│       ├── RobotTaskScheduler.java      # 【不变】已有队列管理
│       └── RobotTaskWrapper.java        # 【不变】任务包装类
├── mapper/
│   ├── robot/
│   │   └── RobotTaskMapper.java         # 【不变】已有Mapper
│   └── MessageRepository.java           # 【不变】已有Repository
├── common/dto/
│   ├── im/
│   │   └── Message.java                 # 【不变】消息实体
│   └── robot/
│       ├── RobotTask.java               # 【不变】任务实体
│       └── RobotTaskExecutionLog.java   # 【不变】日志实体
├── model/
│   ├── dto/
│   │   ├── InferenceRequest.java        # 【不变】LLM请求DTO
│   │   └── InferenceResponse.java       # 【不变】LLM响应DTO
│   └── controller/
│       └── UnifiedInferenceController.java  # 【不变】LLM推理接口
└── configuration/
    └── RobotTaskConfiguration.java      # 【不变】任务配置类

src/test/java/com/bqsummer/
├── service/
│   ├── im/
│   │   └── MessageServiceTest.java      # 【新增】消息服务测试
│   └── robot/
│       └── RobotTaskExecutorTest.java   # 【新增】任务执行器测试
└── integration/
    └── MessageLlmQueueIntegrationTest.java  # 【新增】端到端集成测试

src/main/resources/
└── datasourceInit.sql                   # 【不变】数据库脚本（无需修改）
```

**结构决策**：采用现有的Spring Boot单体应用结构,遵循项目既定的分层架构（Controller → Service → Mapper）。本功能主要扩展Service层逻辑，不引入新的架构组件。

## 阶段 1 关卡检查

*关卡：设计产物完成后（research.md, data-model.md, contracts/, quickstart.md），重新验证基本原则*

### I. 中文优先的文档规范
- ✅ 所有设计文档使用中文
- ✅ API文档、数据模型文档使用中文
- ✅ Quickstart示例代码注释使用中文

### II. 测试驱动开发 (TDD)
- ✅ Quickstart包含完整的单元测试和集成测试示例
- ✅ 定义了清晰的测试场景（正常流程、失败重试、事务回滚）
- ✅ 所有测试使用@DisplayName中文注解

### III. 事务一致性优先
- ✅ 数据模型明确定义@Transactional边界
- ✅ 设计包含乐观锁版本控制（version字段）
- ✅ 异常处理策略明确（事务回滚条件）

### IV. 安全第一
- ✅ API契约明确权限验证要求（@PreAuthorize）
- ✅ 从SecurityContext获取当前用户ID
- ✅ 不暴露敏感信息（LLM内部调用不经过HTTP）

### V. 最小化修改原则
- ✅ 设计确认仅修改2个文件（MessageService, RobotTaskExecutor）
- ✅ 不修改数据库表结构
- ✅ 复用现有RobotTaskScheduler和InferenceService
- ✅ 不引入新的外部依赖

### VI. 数据库管理规范
- ✅ 设计确认无需修改datasourceInit.sql
- ✅ 复用现有message和robot_task表
- ✅ 未创建任何migration目录下的SQL文件

**阶段 1 检查结果**：✅ 全部通过，设计完全符合项目宪章

## 复杂度追踪

*仅当"基本原则检查"中存在必须说明理由的违规项时填写*

**本功能无违规项**，完全符合项目宪章的所有原则。

---

## 阶段 1 交付产物

本计划已完成阶段 0（调研）和阶段 1（设计）的所有产物生成：

### 阶段 0 产物
- ✅ **research.md** - 技术调研文档
  - 5个关键技术决策（action payload格式、模型关联、任务创建时机、调度延迟、执行实现）
  - 替代方案分析
  - 风险评估与缓解措施

### 阶段 1 产物
- ✅ **data-model.md** - 数据模型设计
  - 实体定义（Message, RobotTask, RobotTaskExecutionLog）
  - JSON payload格式规范
  - 状态机转换图
  - 数据流程图
  
- ✅ **contracts/message-task-api.md** - API契约文档
  - HTTP API规范（POST /api/v1/messages）
  - 内部服务接口（MessageService, RobotTaskExecutor）
  - 错误码定义
  - 性能指标
  
- ✅ **quickstart.md** - 快速开始指南
  - 5分钟快速演示
  - 核心实现代码示例
  - 单元测试和集成测试示例
  - 调试技巧和常见问题

### 代理上下文更新
- ✅ 执行 `.specify/scripts/bash/update-agent-context.sh copilot`
- ✅ 更新 `.github/copilot-instructions.md` 成功

---

## 下一步行动

本计划文档已完成阶段 0 和阶段 1 的所有任务。根据 `/speckit.plan` 命令的工作流程，**命令在阶段 1 完成后结束**。

**生成的产物汇总**：
- 实施计划：`/Users/xinhuang/workspace/boboa-boot/specs/004-message-llm-queue/plan.md`
- 技术调研：`/Users/xinhuang/workspace/boboa-boot/specs/004-message-llm-queue/research.md`
- 数据模型：`/Users/xinhuang/workspace/boboa-boot/specs/004-message-llm-queue/data-model.md`
- API契约：`/Users/xinhuang/workspace/boboa-boot/specs/004-message-llm-queue/contracts/message-task-api.md`
- 快速开始：`/Users/xinhuang/workspace/boboa-boot/specs/004-message-llm-queue/quickstart.md`

**功能分支**：`004-message-llm-queue`

**下一阶段**：执行 `/speckit.tasks` 命令生成任务清单（tasks.md），将功能分解为可执行的开发任务。

```
# [若不使用请删除] 选项 1：单一项目（默认）
src/
├── models/
├── services/
├── cli/
└── lib/

tests/
├── contract/
├── integration/
└── unit/

# [若不使用请删除] 选项 2：Web 应用（当检测到“前端”+“后端”时）
backend/
├── src/
│   ├── models/
│   ├── services/
│   └── api/
└── tests/

frontend/
├── src/
│   ├── components/
│   ├── pages/
│   └── services/
└── tests/

# [若不使用请删除] 选项 3：移动端 + API（当检测到“iOS/Android”时）
api/
└── [同上方的 backend 结构]

ios/ 或 android/
└── [平台特定结构：功能模块、UI 流程、平台测试]
```

**结构决策**：[记录所选的结构，并引用上方捕获的实际目录]

## 复杂度追踪

*仅当"基本原则检查"中存在必须说明理由的违规项时填写*

**本功能无违规项**，完全符合项目宪章的所有原则。