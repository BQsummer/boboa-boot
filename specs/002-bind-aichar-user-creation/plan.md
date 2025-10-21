# 实施计划：AI角色与用户账户自动绑定

**分支**：`002-bind-aichar-user-creation` | **日期**：2025-10-21 | **规范文档**：[spec.md](./spec.md)
**输入**：来自 `/specs/002-bind-aichar-user-creation/spec.md` 的功能规范文档

**备注**：此模板由 `/speckit.plan` 命令填充。

## 摘要

本功能实现AI角色创建时自动创建并绑定users表记录的完整生命周期管理。主要需求包括：

1. **创建绑定** (P1)：创建AI角色时自动创建User记录，设置userType为"AI"，生成唯一用户名和邮箱，在一个事务中完成
2. **数据同步** (P1)：更新AI角色名称/头像时同步更新关联User的nickName/avatar字段
3. **删除处理** (P2)：删除AI角色时同时软删除关联User账户
4. **社交集成** (P2)：AI用户可被搜索、添加好友，UI上正确显示AI用户标识

**技术方案**：在现有`AiCharacterService`中添加事务性方法，使用`@Transactional`确保AI角色和User的原子性创建/更新/删除。利用MyBatis Plus的事务管理和Spring的声明式事务。

## 技术背景

**语言/版本**：Java 17  
**主要依赖**：
- Spring Boot 3.5.5
- Spring Security 6.5.3
- MyBatis Plus 3.5.14
- MySQL + Druid连接池
- Lombok（代码简化）
- BCrypt（密码加密）

**存储**：MySQL（已有ai_characters和users表）  
**测试**：JUnit 5 + RestAssured 5.5.6（遵循TDD原则）  
**目标平台**：Spring Boot Web应用（RESTful API）  
**项目类型**：单一Spring Boot项目（后端API服务）  

**性能目标**：
- 创建AI角色+用户：< 1秒
- 更新同步：< 1秒
- 查询响应：< 200ms
- 支持：≥ 10000个AI角色并发存在

**约束条件**：
- **必须**使用`@Transactional`确保AI角色和User的原子性操作（宪章要求）
- **必须**遵循TDD原则：先写测试再实现（宪章要求）
- **必须**使用现有`UserMapper`和`AiCharacterMapper`，不新增Mapper
- **必须**在`AiCharacterService`中实现，符合现有分层架构
- **必须**对AI用户生成随机密码但禁止登录（安全要求）
- **必须**防止通过普通注册接口创建AI用户（安全要求）

**规模/范围**：
- 修改文件：≤ 5个（最小化修改原则）
- 新增测试：≥ 10个测试用例
- 数据库变更：1个ALTER语句（添加`associated_user_id`列）
- 不新增Controller端点（使用现有AI角色CRUD接口）

## 基本原则检查

*关卡：必须在阶段 0 调研前通过。在阶段 1 设计后需重新检查。*

### 通过项

- ✅ **中文优先**：所有文档和注释使用中文，代码使用英文
- ✅ **TDD**：先写测试用例，再实现功能（遵循Red-Green-Refactor循环）
- ✅ **事务一致性**：所有多表操作使用`@Transactional`注解
- ✅ **安全第一**：密码BCrypt加密，防止AI用户登录，验证权限
- ✅ **最小化修改**：只修改AI角色相关代码，不重构无关代码
- ✅ **现有技术栈**：不引入新依赖，使用MyBatis Plus和Spring Boot现有功能

### 需要特别注意

- ⚠️ **事务边界**：确保`createCharacter`、`updateCharacter`、`deleteCharacter`三个方法都正确使用`@Transactional`
- ⚠️ **测试覆盖**：必须包含事务回滚场景测试，验证失败时不会留下孤儿数据
- ⚠️ **密码处理**：AI用户密码必须BCrypt加密，但不允许通过密码登录

## 项目结构

### 文档（本功能）

```
specs/002-bind-aichar-user-creation/
├── spec.md              # 功能规格说明
├── plan.md              # 本文件（实施计划）
├── research.md          # 阶段 0 输出（技术调研）
├── data-model.md        # 阶段 1 输出（数据模型）
├── quickstart.md        # 阶段 1 输出（快速开始）
├── contracts/           # 阶段 1 输出（API契约）
│   └── ai-character-api.md
├── checklists/          # 质量检查清单
│   └── requirements.md
└── tasks.md             # 阶段 2 输出（任务清单 - 由/speckit.tasks创建）
```

### 源代码（仓库根目录）

```
src/main/java/com/bqsummer/
├── common/
│   └── dto/
│       ├── auth/
│       │   └── User.java                    # [修改] 添加userType字段验证
│       └── character/
│           └── AiCharacter.java             # [修改] 添加associatedUserId字段
├── mapper/
│   ├── UserMapper.java                      # [使用] 现有Mapper
│   └── AiCharacterMapper.java               # [修改] 添加更新关联用户ID的方法
├── service/
│   └── AiCharacterService.java              # [修改] 核心修改：添加User创建/更新/删除逻辑
├── controller/
│   └── AiCharacterController.java           # [不修改] 使用现有端点
└── framework/
    └── security/
        └── CustomUserDetailsService.java    # [修改] 禁止AI用户登录

src/main/resources/
└── db/
    └── migration/
        └── V002__add_associated_user_id_to_ai_characters.sql  # [新增] 数据库迁移

src/test/java/com/bqsummer/
├── service/
│   └── AiCharacterServiceTest.java          # [修改] 添加事务测试用例
└── integration/
    └── AiCharacterUserIntegrationTest.java  # [使用] 现有集成测试
```

**结构决策**：
- 采用现有分层架构：Controller → Service → Mapper
- 核心逻辑在`AiCharacterService`中实现，符合单一职责原则
- 利用Spring的`@Transactional`管理事务边界
- 复用现有`UserMapper`和`AiCharacterMapper`，符合最小化修改原则
- 数据库迁移使用标准命名：`V002__`（按功能编号递增）

## 复杂度追踪

本功能无宪章违规项，所有实现符合基本原则：
- 使用现有依赖，不新增第三方库
- 事务管理使用Spring标准注解
- 遵循现有代码规范和分层架构
- 测试驱动开发，先写测试
