# 数据模型：AI角色与用户账户自动绑定

**功能**：002-bind-aichar-user-creation  
**日期**：2025-10-21  
**基于**：[spec.md](./spec.md) 和 [research.md](./research.md)

## 实体概览

本功能涉及2个核心实体和它们之间的一对一关联关系：

1. **AiCharacter**（AI角色）- 修改
2. **User**（用户）- 使用现有

## 实体详细设计

### 1. AiCharacter（AI角色）

**表名**：`ai_characters`

**用途**：存储AI虚拟角色的基本信息和配置

**字段说明**：

| 字段名 | 类型 | 约束 | 说明 | 变更 |
|--------|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 | 现有 |
| name | VARCHAR(100) | NOT NULL | 角色名称 | 现有 |
| image_url | VARCHAR(500) | NULL | 角色头像URL | 现有 |
| author | VARCHAR(100) | NULL | 作者信息 | 现有 |
| created_by_user_id | BIGINT | NOT NULL | 创建者用户ID | 现有 |
| visibility | VARCHAR(20) | NOT NULL | 可见性：PUBLIC/PRIVATE | 现有 |
| status | INT | NOT NULL | 状态：1-启用，0-禁用 | 现有 |
| is_deleted | INT | NOT NULL, DEFAULT 0 | 软删除标记 | 现有 |
| created_time | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 | 现有 |
| updated_time | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新时间 | 现有 |
| **associated_user_id** | **BIGINT** | **NULL** | **关联的用户账户ID** | **新增** |

**新增字段说明**：
- `associated_user_id`：
  - 指向`users`表的外键（逻辑外键，不强制约束）
  - 允许NULL（兼容已有数据）
  - 创建AI角色时自动填充
  - 用于快速查询AI角色对应的用户账户

**索引**：
- PRIMARY KEY: `id`
- INDEX: `created_by_user_id`（现有）
- **INDEX: `associated_user_id`（新增，优化反向查询）**

**数据迁移SQL**：
```sql
ALTER TABLE ai_characters 
ADD COLUMN associated_user_id BIGINT NULL 
COMMENT '关联的用户账户ID'
AFTER updated_time;

CREATE INDEX idx_associated_user_id ON ai_characters(associated_user_id);
```

---

### 2. User（用户）

**表名**：`users`

**用途**：存储所有用户账户信息（包括真实用户和AI用户）

**字段说明**（仅列出相关字段）：

| 字段名 | 类型 | 约束 | 说明 | 使用方式 |
|--------|------|------|------|----------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 | 现有 |
| username | VARCHAR(50) | NOT NULL, UNIQUE | 用户名 | **生成格式：ai_character_{id}** |
| nick_name | VARCHAR(100) | NULL | 昵称 | **同步自AiCharacter.name** |
| password | VARCHAR(100) | NOT NULL | BCrypt加密密码 | **生成随机密码** |
| email | VARCHAR(100) | NULL | 邮箱 | **生成格式：ai_character_{id}@ai.internal** |
| avatar | VARCHAR(500) | NULL | 头像URL | **同步自AiCharacter.image_url** |
| phone | VARCHAR(20) | NULL | 手机号 | NULL（AI用户无需） |
| status | INT | NOT NULL, DEFAULT 1 | 状态：1-启用 | 设置为1 |
| created_time | DATETIME | NOT NULL | 创建时间 | 自动生成 |
| updated_time | DATETIME | NOT NULL | 更新时间 | 自动更新 |
| is_deleted | INT | NOT NULL, DEFAULT 0 | 软删除标记 | **同步删除状态** |
| last_login_time | DATETIME | NULL | 最后登录时间 | NULL（AI用户不登录） |
| **user_type** | **VARCHAR(20)** | **NULL** | **用户类型** | **设置为"AI"** |

**userType字段**：
- **AI用户**：`"AI"`
- **真实用户**：`"REAL"` 或 NULL（兼容旧数据）
- 用于区分用户类型，控制登录权限，UI展示等

**注意事项**：
- `username`必须唯一，由数据库唯一索引保证
- `password`虽然AI用户不使用，但必须设置（满足NOT NULL约束）
- AI用户不应具备登录能力，在认证层拦截

---

## 实体关系

### AiCharacter ↔ User（一对一）

```
AiCharacter (1) ---- (1) User
    ↓                    ↓
associated_user_id   ←   id
```

**关系说明**：
- **基数**：一对一（每个AI角色对应一个用户账户）
- **方向**：双向关联
  - AI角色 → 用户：通过`associated_user_id`
  - 用户 → AI角色：通过查询`ai_characters`表（`associated_user_id = user.id`）
- **级联行为**：
  - 创建：创建AI角色时自动创建用户
  - 更新：更新AI角色名称/头像时同步更新用户
  - 删除：软删除AI角色时同步软删除用户
- **约束**：不使用数据库外键约束，保持灵活性

**查询示例**：

```java
// 从AI角色查询用户
User user = userMapper.findById(character.getAssociatedUserId());

// 从用户查询AI角色
AiCharacter character = aiCharacterMapper.findByAssociatedUserId(userId);
```

---

## 数据流转

### 创建流程

```
1. 用户提交创建AI角色请求
   ↓
2. Service开启事务
   ↓
3. 插入AiCharacter（without associated_user_id）
   ↓ 获取characterId
4. 生成用户数据：
   - username = "ai_character_{characterId}"
   - email = "ai_character_{characterId}@ai.internal"
   - nickName = character.name
   - avatar = character.imageUrl
   - userType = "AI"
   - password = BCrypt(UUID.randomUUID())
   ↓
5. 插入User
   ↓ 获取userId
6. 更新AiCharacter.associated_user_id = userId
   ↓
7. 事务提交
   ↓
8. 返回结果（包含characterId和userId）
```

### 更新流程

```
1. 用户提交更新AI角色请求
   ↓
2. Service开启事务
   ↓
3. 查询现有AiCharacter
   ↓
4. 更新AiCharacter字段
   ↓
5. 检测name或imageUrl是否变化
   ↓ 如果变化
6. 更新User对应字段（nickName或avatar）
   ↓
7. 事务提交
```

### 删除流程

```
1. 用户提交删除AI角色请求
   ↓
2. Service开启事务
   ↓
3. 软删除AiCharacter（is_deleted = 1）
   ↓
4. 获取associated_user_id
   ↓
5. 软删除User（is_deleted = 1）
   ↓
6. 事务提交
```

---

## 数据验证规则

### 创建时验证

| 字段 | 验证规则 |
|------|----------|
| AiCharacter.name | 必填，长度1-100 |
| AiCharacter.imageUrl | 可选，长度≤500 |
| AiCharacter.visibility | 必填，枚举：PUBLIC/PRIVATE |
| User.username | 自动生成，唯一性由数据库保证 |
| User.email | 自动生成 |
| User.password | 自动生成，BCrypt加密 |

### 更新时验证

| 字段 | 验证规则 |
|------|----------|
| AiCharacter.name | 可选，如提供则长度1-100 |
| AiCharacter.imageUrl | 可选，长度≤500 |
| 权限检查 | 仅创建者可更新 |

### 删除时验证

| 项目 | 验证规则 |
|------|----------|
| 权限检查 | 仅创建者可删除 |
| 存在性检查 | AI角色必须存在且未删除 |

---

## 状态转换

### AiCharacter状态

```
[创建] → is_deleted=0, status=1
   ↓
[更新] → is_deleted=0, status可变
   ↓
[软删除] → is_deleted=1
```

### User状态（AI用户）

```
[自动创建] → is_deleted=0, status=1, user_type="AI"
   ↓
[同步更新] → is_deleted=0
   ↓
[同步删除] → is_deleted=1
```

**状态同步规则**：
- User的删除状态必须跟随AiCharacter
- User的status始终为1（启用）
- User的user_type始终为"AI"，不可更改

---

## 性能考虑

### 索引策略

- `ai_characters.associated_user_id`：支持从AI角色快速查询用户
- `users.username`：唯一索引，保证用户名唯一性，支持快速查找
- `users.user_type`：（建议）支持按类型筛选用户

### 查询优化

- 避免N+1查询：批量查询时使用JOIN或IN查询
- 使用MyBatis Plus的BaseMapper提供的批量方法
- 利用Druid连接池提高并发性能

### 事务优化

- 最小化事务范围：只包含必要的数据库操作
- 避免在事务中执行外部API调用
- 使用合适的事务隔离级别（默认READ_COMMITTED）

---

## 数据完整性

### 一致性保证

1. **原子性**：使用`@Transactional`确保AI角色和用户的创建/更新/删除要么全部成功，要么全部失败
2. **唯一性**：username的唯一索引防止重复
3. **引用完整性**：虽然不使用外键约束，但通过应用层逻辑保证关联一致性

### 孤儿数据预防

- 创建失败时事务回滚，不会留下孤儿数据
- 删除使用软删除，保留历史记录
- 定期检查脚本：验证`associated_user_id`的有效性

### 数据迁移兼容性

- `associated_user_id`允许NULL，兼容已有AI角色
- 已有AI角色可通过后续脚本补充创建用户账户
- 查询时容错处理NULL值

---

## 总结

本数据模型设计遵循以下原则：
1. ✅ **最小化修改**：只新增一个字段，复用现有表结构
2. ✅ **向后兼容**：新字段允许NULL，不影响已有数据
3. ✅ **事务一致性**：所有操作在事务中完成，保证数据一致性
4. ✅ **性能优化**：合理使用索引，支持高效查询
5. ✅ **可维护性**：清晰的关系模型，易于理解和扩展

数据模型已完成，可以进入API契约设计阶段。
