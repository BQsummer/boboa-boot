# 实施计划：Beetl 模板引擎集成与 Prompt 模板管理

**分支**：`001-add-beetl-prompt-template` | **日期**：2025-11-27 | **规范文档**：[spec.md](./spec.md)
**输入**：来自 `/specs/001-add-beetl-prompt-template/spec.md` 的功能规范文档

**备注**：此模板由 `/speckit.plan` 命令填充。执行流程请参见 `.specify/templates/commands/plan.md`。

## 摘要

本功能实现 Beetl 模板引擎的集成，并基于 `prompt_template` 表提供完整的 CRUD 接口。主要包括：

1. **Beetl 模板引擎集成**：添加 Beetl 依赖，提供模板渲染服务
2. **Prompt 模板管理**：创建、查询、更新、删除模板
3. **版本管理**：自动版本号递增，最新版本标记
4. **模板预览**：支持传入参数进行渲染预览

## 技术背景

**语言/版本**：Java 17  
**主要依赖**：Spring Boot 3.5.5, MyBatis Plus 3.5.14, Beetl 3.x  
**存储**：MySQL (datasourceInit.sql)，表 `prompt_template` 已存在  
**数据库管理**：所有 SQL 写入 src/main/resources/datasourceInit.sql，禁止使用 migration 目录  
**测试**：JUnit 5 + RestAssured 5.5.6  
**目标平台**：Linux 服务器  
**项目类型**：单一项目（Spring Boot 后端服务）  
**规模/范围**：内部管理系统，约 1 万用户

## 基本原则检查

*关卡：必须在阶段 0 调研前通过。在阶段 1 设计后需重新检查。*

| 原则 | 状态 | 说明 |
|------|------|------|
| 中文优先的文档规范 | ✅ 通过 | 所有文档和注释使用中文 |
| 测试驱动开发 (TDD) | ✅ 待执行 | 将先编写测试用例再实现功能 |
| 事务一致性优先 | ✅ 通过 | 版本号更新涉及多表操作，需使用 @Transactional |
| 安全第一 | ✅ 通过 | 接口使用 @PreAuthorize 控制权限 |
| 最小化修改原则 | ✅ 通过 | 仅添加新功能，不修改现有代码 |
| 数据库管理规范 | ✅ 通过 | prompt_template 表已在 datasourceInit.sql 中定义 |

## 项目结构

### 文档（本功能）

```
specs/001-add-beetl-prompt-template/
├── plan.md              # 本文件（/speckit.plan 命令的输出）
├── research.md          # 阶段 0 输出（Beetl 调研）
├── data-model.md        # 阶段 1 输出（数据模型设计）
├── quickstart.md        # 阶段 1 输出（快速开始指南）
├── contracts/           # 阶段 1 输出（API 契约）
│   └── prompt-template-api.yaml
└── tasks.md             # 阶段 2 输出（/speckit.tasks 命令）
```

### 源代码（仓库根目录）

```
src/main/java/com/bqsummer/
├── common/
│   ├── dto/
│   │   └── PromptTemplateDTO.java          # 数据传输对象
│   └── vo/
│       ├── req/
│       │   └── prompt/
│       │       ├── PromptTemplateCreateRequest.java
│       │       ├── PromptTemplateUpdateRequest.java
│       │       ├── PromptTemplateQueryRequest.java
│       │       └── PromptTemplateRenderRequest.java
│       └── resp/
│           └── prompt/
│               └── PromptTemplateResponse.java
├── configuration/
│   └── BeetlConfiguration.java             # Beetl 配置类
├── controller/
│   └── PromptTemplateController.java       # 模板管理控制器
├── mapper/
│   └── PromptTemplateMapper.java           # MyBatis Mapper
├── repository/
│   └── PromptTemplateEntity.java           # 实体类
└── service/
    └── prompt/
        ├── PromptTemplateService.java      # 模板服务接口
        ├── PromptTemplateServiceImpl.java  # 模板服务实现
        ├── BeetlTemplateService.java       # Beetl 渲染服务接口
        └── BeetlTemplateServiceImpl.java   # Beetl 渲染服务实现

src/test/java/com/bqsummer/
├── integration/
│   └── PromptTemplateControllerIntegrationTest.java
└── service/
    └── prompt/
        ├── PromptTemplateServiceTest.java
        └── BeetlTemplateServiceTest.java
```

**结构决策**：采用现有项目的分层架构（Controller → Service → Mapper），遵循项目既有的代码组织方式。

## 复杂度追踪

*无违规项，无需填写*
