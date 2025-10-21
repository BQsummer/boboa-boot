# 技术调研报告：AI角色与用户账户自动绑定

**功能**：002-bind-aichar-user-creation  
**日期**：2025-10-21  
**状态**：已完成

## 调研目标

解决实施计划中的所有"待明确"项，确定技术实现方案，确保符合项目宪章要求。

## 现有代码分析

### 1. AiCharacter实体现状

**已存在字段**：
- id, name, imageUrl, author, createdByUserId, visibility, status, isDeleted, createdTime, updatedTime

**需要添加**：
- `associatedUserId` (Long)：关联的用户账户ID

**决策**：在`AiCharacter.java`中添加`associatedUserId`字段，同时在数据库表`ai_characters`中添加对应列。

### 2. User实体现状

**已存在字段**：
- id, username, nickName, password, email, avatar, phone, status, createdTime, updatedTime, isDeleted, lastLoginTime, roles, userType

**userType字段分析**：
- 当前类型：`String`
- 现有值：尚未明确定义枚举
- 需要设置：`"AI"`表示AI用户，`"REAL"`表示真实用户

**决策**：使用`userType`字段区分用户类型，值为`"AI"`或`"REAL"`。不需要修改表结构。

### 3. 事务管理策略

**现有实践分析**（参考001-model-instance-management）：
- Service层方法使用`@Transactional`注解
- Spring Boot自动配置DataSource事务管理器
- MyBatis Plus继承事务上下文

**决策**：
- 在`AiCharacterService.createCharacter()`添加`@Transactional`注解
- 在`AiCharacterService.updateCharacter()`添加`@Transactional`注解
- 在`AiCharacterService.deleteCharacter()`添加`@Transactional`注解
- 使用默认传播行为`REQUIRED`

**理由**：遵循项目现有实践，符合宪章"事务一致性优先"原则。

### 4. 密码生成与安全

**现有密码加密方案**：
- 使用BCrypt加密（已有依赖：`org.mindrot.jbcrypt:0.4`）
- `CustomUserDetailsService`加载用户时验证密码

**AI用户密码策略**：
- 生成随机密码：使用`UUID.randomUUID().toString()`
- BCrypt加密存储：使用`BCrypt.hashpw(password, BCrypt.gensalt())`
- 禁止登录：在`CustomUserDetailsService`中检查`userType`，拒绝AI用户登录

**决策**：
```java
// 生成随机密码
String randomPassword = UUID.randomUUID().toString();
String encryptedPassword = BCrypt.hashpw(randomPassword, BCrypt.gensalt());

// 在CustomUserDetailsService中添加检查
if ("AI".equals(user.getUserType())) {
    throw new DisabledException("AI用户不允许登录");
}
```

**理由**：符合宪章"安全第一"原则，AI用户不应具备登录能力。

### 5. 用户名和邮箱生成规则

**唯一性保证**：
- 用户名：`ai_character_{characterId}`
- 邮箱：`ai_character_{characterId}@ai.internal`
- characterId使用数据库自增ID，确保唯一性

**时序问题**：
- 必须先插入`AiCharacter`获取自增ID
- 然后使用该ID生成用户名和邮箱
- 再插入`User`
- 最后更新`AiCharacter`的`associatedUserId`

**决策**：创建流程
```
1. 插入AiCharacter → 获取characterId
2. 生成用户名和邮箱（使用characterId）
3. 插入User → 获取userId
4. 更新AiCharacter.associatedUserId = userId
5. 事务提交
```

**理由**：利用数据库自增ID确保唯一性，无需额外的唯一性检查逻辑。

### 6. 数据同步策略

**同步字段映射**：
- `AiCharacter.name` → `User.nickName`
- `AiCharacter.imageUrl` → `User.avatar`

**同步时机**：
- 创建时：自动同步
- 更新时：仅当name或imageUrl字段更新时同步
- 删除时：同步软删除

**决策**：在`updateCharacter()`中检测字段变化
```java
if (req.getName() != null && !req.getName().equals(existingCharacter.getName())) {
    // 同步更新User.nickName
}
if (req.getImageUrl() != null && !req.getImageUrl().equals(existingCharacter.getImageUrl())) {
    // 同步更新User.avatar
}
```

**理由**：只同步实际变化的字段，符合最小化修改原则。

### 7. 测试策略

**现有测试实践**（参考AiCharacterUserIntegrationTest）：
- 已有集成测试验证AI角色创建和用户账户关联
- 使用RestAssured进行端到端测试
- 使用`@Transactional` + `@Rollback`确保测试隔离

**需要补充的测试场景**：
1. 事务回滚测试：模拟User创建失败，验证AiCharacter也回滚
2. 并发创建测试：验证用户名和邮箱唯一性
3. 部分字段更新测试：验证只同步更新的字段
4. 删除级联测试：验证软删除同步
5. AI用户登录限制测试：验证禁止登录

**决策**：
- 单元测试：`AiCharacterServiceTest` - 测试Service层逻辑
- 集成测试：`AiCharacterUserIntegrationTest` - 测试完整流程
- 使用`DbAssertions`验证数据库状态

**理由**：遵循TDD原则，确保测试覆盖所有关键场景。

## 技术选型总结

| 技术点 | 选择 | 理由 |
|--------|------|------|
| 事务管理 | Spring `@Transactional` | 项目标准实践，声明式事务 |
| 密码加密 | BCrypt | 现有依赖，安全可靠 |
| 密码生成 | UUID | 简单高效，满足随机性要求 |
| 唯一性 | 数据库自增ID | 天然唯一，无需额外逻辑 |
| 用户类型 | String "AI"/"REAL" | 利用现有字段，无需新增列 |
| 登录控制 | CustomUserDetailsService检查 | 集中控制，符合现有架构 |
| 测试 | JUnit 5 + RestAssured | 项目标准，TDD实践 |

## 风险与缓解

### 风险1：事务性能

**问题**：在一个事务中执行4次数据库操作（插入AiCharacter，插入User，更新AiCharacter，查询验证）可能影响性能。

**缓解措施**：
- 使用MyBatis Plus的批量操作优化
- 监控事务执行时间，确保< 1秒
- 使用数据库连接池（Druid）复用连接

### 风险2：用户名冲突

**问题**：虽然使用自增ID，但理论上存在并发创建导致冲突的极端情况。

**缓解措施**：
- 在users表的username列添加唯一索引（如果尚未添加）
- 捕获`DuplicateKeyException`，重试或添加UUID后缀

### 风险3：已有AI角色的迁移

**问题**：现有AI角色没有`associatedUserId`，需要数据迁移。

**缓解措施**：
- 数据库迁移脚本支持`NULL`值（新增列默认NULL）
- 后续通过脚本或手动为已有AI角色创建关联用户
- 或在查询时容错处理NULL值

## 实施建议

### 阶段1：数据库迁移（优先）
1. 添加`ai_characters.associated_user_id`列
2. 添加外键约束（可选，建议先不加以提高灵活性）
3. 添加`users.username`唯一索引（如果没有）

### 阶段2：核心功能实现
1. 修改`AiCharacterService.createCharacter()` - 添加User创建逻辑
2. 修改`AiCharacterService.updateCharacter()` - 添加User同步逻辑
3. 修改`AiCharacterService.deleteCharacter()` - 添加User软删除逻辑

### 阶段3：安全加固
1. 修改`CustomUserDetailsService` - 禁止AI用户登录
2. 修改用户注册接口 - 禁止手动创建AI用户

### 阶段4：测试覆盖
1. 单元测试 - Service层
2. 集成测试 - 端到端流程
3. 事务测试 - 回滚场景

## 参考资料

- 项目宪章：`.specify/memory/constitution.md`
- 现有实现：`src/main/java/com/bqsummer/service/AiCharacterService.java`
- 现有测试：`src/test/java/com/bqsummer/integration/AiCharacterUserIntegrationTest.java`
- Spring事务文档：https://docs.spring.io/spring-framework/reference/data-access/transaction.html
- MyBatis Plus文档：https://baomidou.com/

## 结论

所有技术细节已明确，无待明确项。实施方案成熟可行，符合项目宪章所有要求。可以进入阶段1（设计与合约）。
