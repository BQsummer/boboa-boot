# 实施计划：虚拟人物月度计划表管理

**分支**：`001-character-monthly-plan-crud` | **日期**：2025-11-28 | **规范文档**：[spec.md](./spec.md)  
**输入**：来自 `/specs/001-character-monthly-plan-crud/spec.md` 的功能规范文档

## 摘要

本功能为虚拟人物（AiCharacter）添加月度计划管理能力，支持完整的CRUD操作：
- 为虚拟人物创建月度活动计划，支持固定日期（如每月5号）和相对日期（如每月第二个周一）两种日期规则
- 查询虚拟人物的月度计划列表和单条计划详情
- 更新和删除（软删除）月度计划
- 权限控制：只有虚拟人物的创建者才能管理其计划

技术方案采用与现有 AiCharacter 模块一致的架构模式，使用 MyBatis Plus 进行数据访问，RESTful API 设计。

## 技术背景

**语言/版本**：Java 17  
**主要依赖**：Spring Boot 3.5.5, Spring Security 6.5.3, MyBatis Plus 3.5.14  
**存储**：MySQL (datasourceInit.sql)  
**数据库管理**：所有 SQL 写入 `src/main/resources/datasourceInit.sql`，禁止使用 migration 目录  
**测试**：JUnit 5 + RestAssured 5.5.6  
**目标平台**：Linux 服务器  
**项目类型**：单一项目（Spring Boot Web 应用）  
**规模/范围**：中小规模，扩展现有 AiCharacter 模块

## 基本原则检查

*关卡：必须在阶段 0 调研前通过。在阶段 1 设计后需重新检查。*

| 原则 | 状态 | 说明 |
|------|------|------|
| 中文优先文档 | ✅ 通过 | 所有文档使用中文，代码变量遵循Java命名规范 |
| 测试驱动开发 | ✅ 通过 | 计划先编写集成测试和单元测试 |
| 事务一致性 | ✅ 通过 | 涉及多表操作的方法将使用 @Transactional |
| 安全第一 | ✅ 通过 | API 需要 JWT 认证，验证资源所有权 |
| 最小化修改 | ✅ 通过 | 仅添加新模块，不修改现有代码 |
| 数据库管理规范 | ✅ 通过 | SQL 将写入 datasourceInit.sql |

## 项目结构

### 文档（本功能）

```
specs/001-character-monthly-plan-crud/
├── plan.md              # 本文件（/speckit.plan 命令的输出）
├── spec.md              # 功能规范文档
├── research.md          # 阶段 0 输出（/speckit.plan 命令）
├── data-model.md        # 阶段 1 输出（/speckit.plan 命令）
├── quickstart.md        # 阶段 1 输出（/speckit.plan 命令）
├── contracts/           # 阶段 1 输出（/speckit.plan 命令）
│   └── monthly-plan-api.yaml
└── tasks.md             # 阶段 2 输出（/speckit.tasks 命令）
```

### 源代码（仓库根目录）

```
src/
├── main/
│   ├── java/com/bqsummer/
│   │   ├── common/
│   │   │   ├── dto/
│   │   │   │   └── character/
│   │   │   │       └── MonthlyPlan.java          # 新增：月度计划实体
│   │   │   └── vo/
│   │   │       └── req/
│   │   │           └── character/
│   │   │               ├── CreateMonthlyPlanReq.java   # 新增：创建请求
│   │   │               └── UpdateMonthlyPlanReq.java   # 新增：更新请求
│   │   ├── controller/
│   │   │   └── MonthlyPlanController.java        # 新增：月度计划控制器
│   │   ├── mapper/
│   │   │   └── MonthlyPlanMapper.java            # 新增：月度计划 Mapper
│   │   └── service/
│   │       └── MonthlyPlanService.java           # 新增：月度计划服务
│   └── resources/
│       └── datasourceInit.sql                    # 修改：追加 monthly_plans 表
└── test/
    └── java/com/bqsummer/
        ├── integration/
        │   └── MonthlyPlanIntegrationTest.java   # 新增：集成测试
        └── service/
            └── MonthlyPlanServiceTest.java       # 新增：服务单元测试
```

**结构决策**：采用现有项目的单一模块结构，与 AiCharacter 模块保持一致的代码组织方式。

## 复杂度追踪

*无违规项，所有设计符合基本原则*

## 阶段 1 设计后检查（重新评估）

| 原则 | 状态 | 验证结果 |
|------|------|----------|
| 中文优先文档 | ✅ 通过 | 所有规范文档使用中文，API合约使用英文技术术语 |
| 测试驱动开发 | ✅ 通过 | 计划包含集成测试和单元测试，遵循TDD流程 |
| 事务一致性 | ✅ 通过 | Service 层方法将添加 @Transactional |
| 安全第一 | ✅ 通过 | API 设计包含 JWT 认证和权限校验 |
| 最小化修改 | ✅ 通过 | 仅添加新模块，不修改现有代码（除 datasourceInit.sql） |
| 数据库管理规范 | ✅ 通过 | SQL DDL 将追加到 datasourceInit.sql |

## 生成的产物

| 文件 | 描述 | 状态 |
|------|------|------|
| `plan.md` | 实施计划 | ✅ 已生成 |
| `research.md` | 技术研究 | ✅ 已生成 |
| `data-model.md` | 数据模型 | ✅ 已生成 |
| `quickstart.md` | 快速开始 | ✅ 已生成 |
| `contracts/monthly-plan-api.yaml` | OpenAPI 合约 | ✅ 已生成 |
