# API契约：AI角色与用户账户管理

**功能**：002-bind-aichar-user-creation  
**日期**：2025-10-21  
**版本**：v1.0

## 概述

本功能复用现有AI角色管理API，通过在响应中添加`associatedUserId`字段来暴露用户账户关联信息。不新增API端点。

## 基础信息

**Base URL**: `/api/v1/ai/characters`  
**认证方式**: JWT Bearer Token  
**Content-Type**: `application/json`  
**字符编码**: UTF-8

---

## API端点

### 1. 创建AI角色（增强）

**端点**: `POST /api/v1/ai/characters`

**描述**: 创建新的AI角色，自动创建关联的用户账户

**请求头**:
```
Authorization: Bearer <token>
Content-Type: application/json
```

**请求体**:
```json
{
  "name": "小助手",
  "imageUrl": "https://example.com/avatar.jpg",
  "author": "系统",
  "visibility": "PUBLIC",
  "status": 1
}
```

**字段说明**:
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | String | 是 | AI角色名称，长度1-100 |
| imageUrl | String | 否 | 头像URL，长度≤500 |
| author | String | 否 | 作者信息 |
| visibility | String | 是 | 可见性：PUBLIC或PRIVATE |
| status | Integer | 否 | 状态：1-启用（默认），0-禁用 |

**响应**（200 OK - 增强）:
```json
{
  "id": 123,
  "associatedUserId": 456
}
```

**响应字段**:
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | AI角色ID |
| **associatedUserId** | **Long** | **关联的用户账户ID（新增）** |

**错误响应**:

- **400 Bad Request** - 请求参数错误
```json
{
  "error": "visibility 仅支持 PUBLIC/PRIVATE"
}
```

- **401 Unauthorized** - 未授权或token无效
```json
{
  "error": "未授权"
}
```

- **500 Internal Server Error** - 服务器内部错误
```json
{
  "error": "创建AI角色失败"
}
```

**业务逻辑**:
1. 验证用户身份（JWT token）
2. 验证请求参数（name必填，visibility合法）
3. **开启事务**
4. 插入`ai_characters`记录
5. **自动创建`users`记录**:
   - `username` = `ai_character_{characterId}`
   - `email` = `ai_character_{characterId}@ai.internal`
   - `nickName` = `request.name`
   - `avatar` = `request.imageUrl`
   - `userType` = `"AI"`
   - `password` = BCrypt(UUID.randomUUID())
6. 更新`ai_characters.associated_user_id`
7. **提交事务**
8. 返回AI角色ID和用户账户ID

---

### 2. 查询AI角色详情（增强）

**端点**: `GET /api/v1/ai/characters/{id}`

**描述**: 查询AI角色详细信息，包含关联用户ID

**路径参数**:
| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | AI角色ID |

**请求头**:
```
Authorization: Bearer <token>
```

**响应**（200 OK - 增强）:
```json
{
  "id": 123,
  "name": "小助手",
  "imageUrl": "https://example.com/avatar.jpg",
  "author": "系统",
  "createdByUserId": 100,
  "visibility": "PUBLIC",
  "status": 1,
  "isDeleted": 0,
  "createdTime": "2025-10-21T10:00:00",
  "updatedTime": "2025-10-21T10:00:00",
  "associatedUserId": 456
}
```

**响应字段**（新增部分）:
| 字段 | 类型 | 说明 |
|------|------|------|
| **associatedUserId** | **Long** | **关联的用户账户ID（新增字段）** |

**错误响应**:

- **403 Forbidden** - 无权访问（私有角色且非创建者）
```json
{
  "statusCode": 403
}
```

- **404 Not Found** - AI角色不存在或已删除
```json
{
  "statusCode": 404
}
```

**业务逻辑**:
1. 查询AI角色
2. 验证访问权限（PUBLIC或创建者）
3. **返回包含`associatedUserId`的完整信息**

---

### 3. 列出AI角色（增强）

**端点**: `GET /api/v1/ai/characters`

**描述**: 列出当前用户可见的AI角色（PUBLIC或自己创建）

**请求头**:
```
Authorization: Bearer <token>
```

**响应**（200 OK - 增强）:
```json
[
  {
    "id": 123,
    "name": "小助手",
    "imageUrl": "https://example.com/avatar.jpg",
    "visibility": "PUBLIC",
    "createdByUserId": 100,
    "associatedUserId": 456,
    "createdTime": "2025-10-21T10:00:00"
  },
  {
    "id": 124,
    "name": "客服机器人",
    "imageUrl": "https://example.com/bot.jpg",
    "visibility": "PRIVATE",
    "createdByUserId": 100,
    "associatedUserId": 457,
    "createdTime": "2025-10-21T11:00:00"
  }
]
```

**业务逻辑**:
1. 查询当前用户可见的AI角色（visibility=PUBLIC或created_by_user_id=当前用户）
2. **返回列表，每项包含`associatedUserId`**

---

### 4. 更新AI角色（增强）

**端点**: `PUT /api/v1/ai/characters/{id}`

**描述**: 更新AI角色信息，自动同步到关联用户账户

**路径参数**:
| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | AI角色ID |

**请求头**:
```
Authorization: Bearer <token>
Content-Type: application/json
```

**请求体**:
```json
{
  "name": "新名称",
  "imageUrl": "https://example.com/new-avatar.jpg",
  "visibility": "PRIVATE"
}
```

**字段说明**（所有字段可选）:
| 字段 | 类型 | 说明 |
|------|------|------|
| name | String | AI角色名称 |
| imageUrl | String | 头像URL |
| visibility | String | 可见性 |
| author | String | 作者信息 |
| status | Integer | 状态 |

**响应**（200 OK）:
```json
{
  "statusCode": 200
}
```

**错误响应**:

- **403 Forbidden** - 无权修改（非创建者）
```json
{
  "statusCode": 403,
  "error": "只有创建者可以修改"
}
```

- **404 Not Found** - AI角色不存在
```json
{
  "statusCode": 404
}
```

**业务逻辑**:
1. 验证创建者权限
2. **开启事务**
3. 更新`ai_characters`记录
4. **如果`name`字段更新，同步更新`users.nick_name`**
5. **如果`imageUrl`字段更新，同步更新`users.avatar`**
6. **提交事务**
7. 返回成功响应

**同步规则**:
- `name` 变化 → 更新 `User.nickName`
- `imageUrl` 变化 → 更新 `User.avatar`
- 只同步实际变化的字段

---

### 5. 删除AI角色（增强）

**端点**: `DELETE /api/v1/ai/characters/{id}`

**描述**: 软删除AI角色，同时软删除关联用户账户

**路径参数**:
| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | AI角色ID |

**请求头**:
```
Authorization: Bearer <token>
```

**响应**（200 OK）:
```json
{
  "statusCode": 200
}
```

**错误响应**:

- **403 Forbidden** - 无权删除（非创建者）
```json
{
  "statusCode": 403,
  "error": "只有创建者可以删除"
}
```

- **404 Not Found** - AI角色不存在或已删除
```json
{
  "statusCode": 404
}
```

**业务逻辑**:
1. 验证创建者权限
2. **开启事务**
3. 软删除`ai_characters`（设置`is_deleted=1`）
4. **软删除关联的`users`记录（设置`is_deleted=1`）**
5. **提交事务**
6. 返回成功响应

---

## 用户搜索API（现有，行为说明）

### 搜索用户

**端点**: `GET /api/v1/users/search?keyword={keyword}`

**描述**: 搜索用户（包括真实用户和AI用户）

**响应**（200 OK）:
```json
{
  "data": [
    {
      "id": 456,
      "username": "ai_character_123",
      "nickName": "小助手",
      "avatar": "https://example.com/avatar.jpg",
      "userType": "AI"
    },
    {
      "id": 100,
      "username": "real_user",
      "nickName": "真实用户",
      "avatar": "https://example.com/user.jpg",
      "userType": "REAL"
    }
  ]
}
```

**行为变更**:
- **AI用户现在会出现在搜索结果中**（当对应AI角色visibility=PUBLIC时）
- **响应中包含`userType`字段，用于UI区分**
- AI用户的`username`格式为`ai_character_{id}`

---

## 数据契约

### AiCharacter对象（增强）

```typescript
interface AiCharacter {
  id: number;
  name: string;
  imageUrl?: string;
  author?: string;
  createdByUserId: number;
  visibility: "PUBLIC" | "PRIVATE";
  status: number;
  isDeleted: number;
  createdTime: string;  // ISO 8601格式
  updatedTime: string;  // ISO 8601格式
  associatedUserId?: number;  // 新增字段
}
```

### User对象（AI用户特征）

```typescript
interface User {
  id: number;
  username: string;      // 格式：ai_character_{id}
  nickName: string;      // 同步自AiCharacter.name
  email: string;         // 格式：ai_character_{id}@ai.internal
  avatar?: string;       // 同步自AiCharacter.imageUrl
  userType: "AI" | "REAL";  // AI用户标识
  status: number;
  isDeleted: number;
  createdTime: string;
  updatedTime: string;
}
```

---

## 错误码

| 状态码 | 说明 | 场景 |
|--------|------|------|
| 200 | 成功 | 操作成功 |
| 400 | 请求参数错误 | 参数验证失败 |
| 401 | 未授权 | 未登录或token无效 |
| 403 | 禁止访问 | 无权限操作资源 |
| 404 | 资源不存在 | AI角色不存在或已删除 |
| 500 | 服务器错误 | 系统内部错误，事务回滚 |

---

## 事务保证

所有涉及多表操作的API都使用事务保证原子性：

| API | 事务范围 |
|-----|----------|
| POST /characters | 创建AI角色 + 创建用户 + 更新关联ID |
| PUT /characters/{id} | 更新AI角色 + 同步更新用户 |
| DELETE /characters/{id} | 软删除AI角色 + 软删除用户 |

**失败行为**: 任何步骤失败，整个事务回滚，不会留下孤儿数据。

---

## 安全要求

1. **认证**: 所有端点需要JWT Bearer Token
2. **授权**: 
   - 创建：任何认证用户
   - 查询：PUBLIC可见或创建者
   - 更新/删除：仅创建者
3. **AI用户登录限制**: 
   - AI用户（userType="AI"）不允许通过密码登录
   - 在`CustomUserDetailsService`层面拦截
4. **防止手动创建AI用户**:
   - 用户注册接口禁止设置userType为"AI"
   - 只能通过AI角色创建流程自动生成

---

## 版本历史

| 版本 | 日期 | 变更说明 |
|------|------|----------|
| v1.0 | 2025-10-21 | 初始版本，添加associatedUserId字段和自动绑定逻辑 |

---

## 总结

本API契约完全复用现有端点，通过在响应中添加`associatedUserId`字段来暴露用户账户关联信息。主要变更：

1. ✅ **向后兼容**：不破坏现有API结构
2. ✅ **最小化变更**：不新增端点，只增强响应
3. ✅ **事务保证**：所有多表操作使用事务
4. ✅ **安全加固**：禁止AI用户登录，防止手动创建
5. ✅ **清晰的错误处理**：明确的错误码和消息

API契约已完成，可以进入实施阶段。
