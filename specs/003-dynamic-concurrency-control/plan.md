# 实施计划：动态并发控制管理

**分支**：`003-dynamic-concurrency-control` | **日期**：2025-10-22 | **规范文档**：[spec.md](./spec.md)
**输入**：来自 `/specs/003-dynamic-concurrency-control/spec.md` 的功能规范文档

**备注**：此模板由 `/speckit.plan` 命令填充。执行流程请参见 `.specify/templates/commands/plan.md`。

## 摘要

允许系统管理员通过 RESTful API 接口在运行时动态调整 RobotTaskScheduler 中不同动作类型（SEND_MESSAGE、SEND_VOICE、SEND_NOTIFICATION）的并发限制，无需重启服务。主要功能包括：

1. **运行时调整并发限制** (P1)：提供 API 接口修改指定动作类型的并发限制值，立即生效
2. **查询并发配置状态** (P2)：查询所有动作类型的并发限制配置和实时使用情况
3. **配置修改审计** (P3)：记录并发限制修改操作日志，支持审计追溯

技术方案：在 RobotTaskScheduler 中添加动态修改 Semaphore 的方法，通过新增 AdminController 管理接口暴露功能，确保线程安全。

## 技术背景

**语言/版本**：Java 17  
**主要依赖**：Spring Boot 3.5.5, Spring Security 6.5.3, MyBatis Plus 3.5.14  
**存储**：不适用（此功能不涉及数据库表修改，仅内存操作）  
**数据库管理**：不适用  
**测试**：JUnit 5 + RestAssured 5.5.6 (集成测试) + Mockito (单元测试)  
**目标平台**：Linux 服务器  
**项目类型**：单一后端项目（Spring Boot Web 应用）  
**性能目标**：接口响应时间 < 1秒，支持高负载场景（1000+ 并发任务）下的配置修改  
**约束条件**：
- 接口响应时间必须在 1 秒以内
- 并发限制修改必须线程安全
- 不能中断正在执行的任务
- 修改操作必须是原子性的
**规模/范围**：
- 3 个动作类型的并发控制管理
- 2 个管理接口（查询 + 修改）
- 内存修改，不持久化（重启后恢复默认配置）

## 基本原则检查

*关卡：必须在阶段 0 调研前通过。在阶段 1 设计后需重新检查。*

### I. 中文优先的文档规范
- ✅ 所有文档使用中文编写
- ✅ 代码注释使用中文描述业务逻辑
- ✅ 测试用例使用中文 `@DisplayName`

### II. 测试驱动开发 (TDD)
- ✅ 先编写失败的集成测试（RestAssured 测试 API 端点）
- ✅ 先编写失败的单元测试（测试 RobotTaskScheduler 动态修改方法）
- ✅ 实现功能代码使测试通过
- ✅ 验证并发安全性和边界场景

### III. 事务一致性优先
- ✅ 不适用（此功能不涉及多表数据库操作，仅内存操作）

### IV. 安全第一
- ✅ 所有管理接口必须使用 `@PreAuthorize("hasRole('ADMIN')")` 进行权限控制
- ✅ 验证输入参数（并发限制必须为正整数）
- ✅ 记录敏感操作日志（包含操作人、操作时间、修改内容）

### V. 最小化修改原则
- ✅ 仅修改 RobotTaskScheduler 添加动态修改方法
- ✅ 新增 RobotTaskManagementController（不修改现有 Controller）
- ✅ 不修改现有的并发控制逻辑，仅扩展配置能力
- ✅ 不改变现有测试，仅新增本功能相关测试

### VI. 数据库管理规范
- ✅ 不适用（此功能不涉及数据库表修改）

**关卡评估**：✅ 通过 - 所有原则均已遵守，无需额外说明

## 项目结构

### 文档（本功能）

```
specs/003-dynamic-concurrency-control/
├── plan.md              # 本文件（/speckit.plan 命令的输出）
├── research.md          # 阶段 0 输出（/speckit.plan 命令）
├── data-model.md        # 阶段 1 输出（/speckit.plan 命令）
├── quickstart.md        # 阶段 1 输出（/speckit.plan 命令）
├── contracts/           # 阶段 1 输出（/speckit.plan 命令）
│   └── concurrency-management-api.md
└── tasks.md             # 阶段 2 输出（/speckit.tasks 命令 - 不由 /speckit.plan 创建）
```

### 源代码（仓库根目录）

```
src/
├── main/java/com/bqsummer/
│   ├── controller/
│   │   └── RobotTaskManagementController.java  # 新增：并发控制管理接口
│   ├── service/robot/
│   │   └── RobotTaskScheduler.java              # 修改：添加动态修改并发限制的方法
│   └── common/dto/robot/
│       ├── ConcurrencyConfigDto.java            # 新增：并发配置 DTO
│       └── ConcurrencyUpdateRequest.java        # 新增：并发更新请求 DTO
│
└── test/java/com/bqsummer/
    ├── integration/
    │   └── RobotTaskManagementIntegrationTest.java  # 新增：API 集成测试
    └── service/robot/
        └── RobotTaskSchedulerDynamicConfigTest.java # 新增：动态配置单元测试
```

**结构决策**：采用单一后端项目结构，所有代码位于 `src/main/java/com/bqsummer/` 下。遵循现有项目的分层架构（Controller → Service），新增必要的 DTO 类支持 API 交互。测试代码按类型分为集成测试和单元测试。

## 复杂度追踪

*仅当"基本原则检查"中存在必须说明理由的违规项时填写*

不适用 - 本功能符合所有基本原则，无违规项需要说明。

---

## 实施阶段总结

### 阶段 0：大纲与研究 ✅

**状态**：已完成

**输出**：`research.md`

**关键决策**：
1. ✅ Semaphore 动态调整方案：使用 `drainPermits()` + `release(n)` 组合
2. ✅ 并发配置存储：使用 `ConcurrentHashMap<String, Integer>` 内存存储
3. ✅ 权限控制：使用 `@PreAuthorize("hasRole('ADMIN')")`
4. ✅ 参数验证：使用 Jakarta Validation
5. ✅ 操作日志：使用 SLF4J/Logback 记录
6. ✅ 错误处理：使用 `SnorlaxClientException`

**技术风险**：低 - 所有技术方案已验证可行，无阻塞性问题

---

### 阶段 1：设计与合约 ✅

**状态**：已完成

**输出**：
- ✅ `data-model.md`：定义 DTO 类和内部数据结构
- ✅ `contracts/concurrency-management-api.md`：定义 RESTful API 契约
- ✅ `quickstart.md`：提供快速开始指南和使用示例
- ✅ 更新 `.github/copilot-instructions.md`：添加本功能的技术上下文

**设计要点**：
- **DTO 类**：
  - `ConcurrencyConfigDto`：查询接口响应（包含配置和实时状态）
  - `ConcurrencyUpdateRequest`：修改接口请求（包含验证规则）
- **API 接口**：
  - `GET /api/v1/admin/robot-task/concurrency/config`：查询所有配置
  - `PUT /api/v1/admin/robot-task/concurrency/config/{actionType}`：修改指定配置
- **数据安全**：
  - 读操作：`ConcurrentHashMap` 支持并发读，无需加锁
  - 写操作：`synchronized` 方法确保线程安全
  - 权限控制：`@PreAuthorize("hasRole('ADMIN')")`

**基本原则检查（重新验证）**：
- ✅ 中文优先文档规范：所有文档和注释使用中文
- ✅ 测试驱动开发：已规划集成测试和单元测试（阶段 2 实现）
- ✅ 事务一致性优先：不适用（无数据库操作）
- ✅ 安全第一：ADMIN 角色权限控制 + 参数验证
- ✅ 最小化修改原则：仅扩展 RobotTaskScheduler，新增 Controller 和 DTO
- ✅ 数据库管理规范：不适用（无数据库表修改）

**关卡评估**：✅ 通过 - 设计完整，符合所有原则，无需调整

---

### 阶段 2：任务规划（待执行）

**状态**：待 `/speckit.tasks` 命令生成

**预期输出**：`tasks.md`

**预期任务概览**：
1. 修改 `RobotTaskScheduler`：
   - 添加 `actionConcurrencyConfig` 字段
   - 添加 `updateConcurrencyLimit()` 方法
   - 修改 `getConcurrencyLimit()` 从配置映射读取
2. 创建 DTO 类：
   - `ConcurrencyConfigDto`
   - `ConcurrencyUpdateRequest`
3. 创建 `RobotTaskManagementController`：
   - 实现查询接口
   - 实现修改接口
4. 编写测试：
   - 集成测试：`RobotTaskManagementIntegrationTest`
   - 单元测试：`RobotTaskSchedulerDynamicConfigTest`

---

## 下一步行动

**当前状态**：阶段 1 已完成，准备进入阶段 2（任务规划）

**执行命令**：
```bash
/speckit.tasks
```

此命令将基于 plan.md 和 contracts 生成详细的任务清单，包括：
- 任务优先级排序
- 每个任务的具体实现步骤
- 测试验收标准
- 任务依赖关系

---

## 附录

### 相关文档索引

- **功能规格**：[spec.md](./spec.md)
- **研究文档**：[research.md](./research.md)
- **数据模型**：[data-model.md](./data-model.md)
- **API 契约**：[contracts/concurrency-management-api.md](./contracts/concurrency-management-api.md)
- **快速开始**：[quickstart.md](./quickstart.md)

### 技术栈总结

| 组件 | 技术选型 | 版本 | 用途 |
|------|----------|------|------|
| 语言 | Java | 17 | 核心开发语言 |
| 框架 | Spring Boot | 3.5.5 | Web 应用框架 |
| 安全 | Spring Security | 6.5.3 | 认证和授权 |
| 测试 | JUnit 5 + RestAssured | 5.5.6 | 单元测试 + 集成测试 |
| 并发控制 | java.util.concurrent.Semaphore | JDK 17 | 并发限制实现 |
| 日志 | SLF4J + Logback | - | 操作日志记录 |
| 验证 | Jakarta Validation | - | 请求参数验证 |

---

**计划完成日期**：2025-10-22  
**计划作者**：GitHub Copilot  
**审核状态**：待审核

## 复杂度追踪

*仅当"基本原则检查"中存在必须说明理由的违规项时填写*

不适用 - 本功能符合所有基本原则，无违规项需要说明。

### 源代码（仓库根目录）
<!--
  【需要操作】：请将下方的占位符目录树替换为该功能的具体布局。
  删除未使用的选项，并用真实路径（例如 apps/admin, packages/something）展开所选结构。
  最终交付的计划中不得包含“选项”标签。
-->

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

*仅当“基本原则检查”中存在必须说明理由的违规项时填写*

| 违规项 | 必要性说明 | 为何拒绝更简单的替代方案 |
|-----------|------------|-------------------------------------|
| [例如，第 4 个项目] | [当前需求] | [为何 3 个项目不足够] |
| [例如，仓库模式] | [具体问题] | [为何直接访问数据库不足够] |