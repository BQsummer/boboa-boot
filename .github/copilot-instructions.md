# boboa-boot Development Guidelines

Auto-generated from all feature plans. Last updated: 2025-10-22

## Active Technologies
- (002-bind-aichar-user-creation) - AI角色与用户账户自动绑定
- (002-robot-task-queue-metrics)

## Project Structure
```
src/
├── main/java/com/bqsummer/
│   ├── common/dto/
│   │   ├── auth/User.java (userType字段：REAL/AI)
│   │   └── character/AiCharacter.java (associatedUserId字段)
│   ├── service/
│   │   ├── AiCharacterService.java (@Transactional 用户账户自动创建/同步/删除)
│   │   └── auth/AuthService.java (AI用户登录限制)
│   ├── mapper/
│   │   ├── AiCharacterMapper.java (associatedUserId映射)
│   │   ├── UserMapper.java (userType映射，部分字段更新)
│   │   └── FriendMapper.java (userType映射)
│   └── constant/UserType.java (用户类型枚举)
└── main/resources/
    └── datasourceInit.sql (所有SQL脚本必须写在此文件，禁止使用db/migration/)
tests/
├── service/AiCharacterServiceTest.java (US1-3单元测试)
└── integration/AiCharacterUserIntegrationTest.java (US1-4集成测试)
```

## Commands
# AI角色相关
- 创建AI角色会自动创建关联用户账户（userType=AI）
- 更新AI角色name/imageUrl会自动同步User的nickName/avatar
- 删除AI角色会同步软删除关联用户账户
- AI用户禁止登录（在AuthService.login中检查）

## Code Style
- 中文注释优先
- 遵循TDD原则
- 使用@Transactional确保事务一致性
- 安全第一：AI用户不能登录，注册默认为REAL用户
- **数据库管理**：所有SQL必须写在 `src/main/resources/datasourceInit.sql`，禁止在 `db/migration/` 下创建文件

## Recent Changes
- 2025-10-22: 更新项目宪章至 v1.1.0
  - 新增原则VI：数据库管理规范 (禁止使用migration目录)
  - 更新模板文件以反映数据库管理要求
- 002-bind-aichar-user-creation: Added (2025-10-21)
  - AiCharacter.associatedUserId字段
  - User.userType字段(REAL/AI)
  - AiCharacterService事务性创建/更新/删除用户账户
  - AuthService AI用户登录限制
  - UserMapper/AiCharacterMapper/FriendMapper userType映射
- 002-robot-task-queue-metrics: Added

<!-- MANUAL ADDITIONS START -->
<!-- MANUAL ADDITIONS END -->
