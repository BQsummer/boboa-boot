# API 契约：动态并发控制管理

**功能**：003-dynamic-concurrency-control  
**创建日期**：2025-10-22  
**状态**：已完成  
**基础路径**：`/api/v1/admin/robot-task`

---

## 接口概览

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/concurrency/config` | 查询所有动作类型的并发配置 | ADMIN |
| PUT | `/concurrency/config/{actionType}` | 修改指定动作类型的并发限制 | ADMIN |

---

## 1. 查询并发配置

### 基本信息

**端点**：`GET /api/v1/admin/robot-task/concurrency/config`  
**说明**：查询所有动作类型的并发限制配置和实时使用情况  
**权限**：ADMIN角色  
**认证**：JWT Bearer Token

### 请求

**HTTP 方法**：GET

**请求头**：
```http
Authorization: Bearer <JWT_TOKEN>
```

**路径参数**：无

**查询参数**：无

**请求体**：无

### 响应

**成功响应（200 OK）**：

**Content-Type**：`application/json`

**响应体结构**：
```json
[
  {
    "actionType": "SEND_MESSAGE",
    "concurrencyLimit": 15,
    "availablePermits": 10,
    "usedPermits": 5,
    "usageRate": 0.33
  },
  {
    "actionType": "SEND_VOICE",
    "concurrencyLimit": 5,
    "availablePermits": 2,
    "usedPermits": 3,
    "usageRate": 0.60
  },
  {
    "actionType": "SEND_NOTIFICATION",
    "concurrencyLimit": 10,
    "availablePermits": 10,
    "usedPermits": 0,
    "usageRate": 0.00
  }
]
```

**字段说明**：

| 字段 | 类型 | 说明 | 示例 |
|------|------|------|------|
| `actionType` | String | 动作类型 | `"SEND_MESSAGE"` |
| `concurrencyLimit` | Integer | 并发限制上限 | `15` |
| `availablePermits` | Integer | 当前可用的并发槽位数 | `10` |
| `usedPermits` | Integer | 当前使用中的并发槽位数（计算值：limit - available） | `5` |
| `usageRate` | Double | 并发使用率（0.0 ~ 1.0） | `0.33` |

**错误响应（403 Forbidden）**：

```json
{
  "timestamp": "2025-10-22T10:30:00",
  "status": 403,
  "error": "Forbidden",
  "message": "Access Denied",
  "path": "/api/v1/admin/robot-task/concurrency/config"
}
```

**说明**：当前用户不具备 ADMIN 角色

**错误响应（401 Unauthorized）**：

```json
{
  "timestamp": "2025-10-22T10:30:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource",
  "path": "/api/v1/admin/robot-task/concurrency/config"
}
```

**说明**：未提供有效的 JWT Token

### 示例

**cURL 请求**：
```bash
curl -X GET \
  'http://localhost:8080/api/v1/admin/robot-task/concurrency/config' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...'
```

**响应示例**：
```json
[
  {
    "actionType": "SEND_MESSAGE",
    "concurrencyLimit": 10,
    "availablePermits": 7,
    "usedPermits": 3,
    "usageRate": 0.30
  },
  {
    "actionType": "SEND_VOICE",
    "concurrencyLimit": 5,
    "availablePermits": 5,
    "usedPermits": 0,
    "usageRate": 0.00
  },
  {
    "actionType": "SEND_NOTIFICATION",
    "concurrencyLimit": 10,
    "availablePermits": 8,
    "usedPermits": 2,
    "usageRate": 0.20
  }
]
```

---

## 2. 修改并发限制

### 基本信息

**端点**：`PUT /api/v1/admin/robot-task/concurrency/config/{actionType}`  
**说明**：修改指定动作类型的并发限制值，立即生效  
**权限**：ADMIN角色  
**认证**：JWT Bearer Token

### 请求

**HTTP 方法**：PUT

**请求头**：
```http
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

**路径参数**：

| 参数 | 类型 | 必填 | 说明 | 示例 |
|------|------|------|------|------|
| `actionType` | String | 是 | 动作类型（SEND_MESSAGE, SEND_VOICE, SEND_NOTIFICATION） | `SEND_MESSAGE` |

**请求体**：

```json
{
  "concurrencyLimit": 20
}
```

**字段说明**：

| 字段 | 类型 | 必填 | 验证规则 | 说明 | 示例 |
|------|------|------|----------|------|------|
| `concurrencyLimit` | Integer | 是 | 最小值1, 最大值1000 | 新的并发限制值 | `20` |

### 响应

**成功响应（200 OK）**：

**Content-Type**：`application/json`

**响应体**：
```json
{
  "success": true,
  "message": "并发限制修改成功"
}
```

**错误响应（400 Bad Request - 参数验证失败）**：

```json
{
  "timestamp": "2025-10-22T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "并发限制必须大于 0",
  "path": "/api/v1/admin/robot-task/concurrency/config/SEND_MESSAGE"
}
```

**可能的验证错误消息**：
- `"并发限制不能为空"`
- `"并发限制必须大于 0"`
- `"并发限制不能超过 1000"`

**错误响应（400 Bad Request - 不支持的动作类型）**：

```json
{
  "timestamp": "2025-10-22T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "不支持的动作类型: INVALID_TYPE",
  "path": "/api/v1/admin/robot-task/concurrency/config/INVALID_TYPE"
}
```

**说明**：提供的动作类型不在支持列表中（SEND_MESSAGE, SEND_VOICE, SEND_NOTIFICATION）

**错误响应（403 Forbidden）**：

```json
{
  "timestamp": "2025-10-22T10:30:00",
  "status": 403,
  "error": "Forbidden",
  "message": "Access Denied",
  "path": "/api/v1/admin/robot-task/concurrency/config/SEND_MESSAGE"
}
```

**说明**：当前用户不具备 ADMIN 角色

**错误响应（401 Unauthorized）**：

```json
{
  "timestamp": "2025-10-22T10:30:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource",
  "path": "/api/v1/admin/robot-task/concurrency/config/SEND_MESSAGE"
}
```

**说明**：未提供有效的 JWT Token

### 示例

**cURL 请求**：
```bash
curl -X PUT \
  'http://localhost:8080/api/v1/admin/robot-task/concurrency/config/SEND_MESSAGE' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...' \
  -H 'Content-Type: application/json' \
  -d '{
    "concurrencyLimit": 20
  }'
```

**成功响应示例**：
```json
{
  "success": true,
  "message": "并发限制修改成功"
}
```

**日志记录示例**：
```
2025-10-22 10:30:00 INFO  [RobotTaskManagementController] 并发限制修改成功 - 操作人: admin@example.com, 动作类型: SEND_MESSAGE, 修改前: 10, 修改后: 20
```

---

## 业务规则

### 1. 动作类型约束
- 仅支持 3 种动作类型：`SEND_MESSAGE`, `SEND_VOICE`, `SEND_NOTIFICATION`
- 提供其他动作类型将返回 400 错误

### 2. 并发限制约束
- 并发限制必须为正整数（最小值 1）
- 并发限制最大值为 1000（防止配置过大）
- 违反约束将返回 400 错误，并提示具体原因

### 3. 权限约束
- 所有接口必须具备 ADMIN 角色
- 普通用户（USER 角色）无法访问，返回 403 错误
- 未登录用户返回 401 错误

### 4. 并发修改行为
- 增加并发限制：立即生效，新任务可使用增加的槽位
- 降低并发限制：立即生效，但正在执行的任务不受影响（继续持有槽位直到完成）
- 新任务从修改后立即遵循新限制

### 5. 操作审计
- 所有修改操作记录到应用日志（INFO 级别）
- 日志包含：操作时间、操作人、动作类型、修改前后的值
- 日志格式：`"并发限制修改成功 - 操作人: {user}, 动作类型: {action}, 修改前: {old}, 修改后: {new}"`

### 6. 监控指标
- 并发配置修改后，Prometheus 监控指标自动更新
- 相关指标：
  - `robot_task_concurrency_limit{action_type="SEND_MESSAGE"}`: 并发限制值
  - `robot_task_concurrency_available{action_type="SEND_MESSAGE"}`: 可用槽位数
  - `robot_task_concurrency_usage_rate{action_type="SEND_MESSAGE"}`: 使用率

### 7. 线程池容量警告
- 如果新设置的并发限制超过线程池容量，记录警告日志
- 警告内容：`"并发限制 (X) 超过线程池容量 (Y), 可能导致部分槽位无法使用"`
- 不阻止修改操作，由管理员自行判断

---

## 状态码总结

| 状态码 | 说明 | 场景 |
|--------|------|------|
| 200 OK | 操作成功 | 查询成功或修改成功 |
| 400 Bad Request | 请求参数错误 | 并发限制验证失败或不支持的动作类型 |
| 401 Unauthorized | 未认证 | 未提供 JWT Token 或 Token 无效 |
| 403 Forbidden | 权限不足 | 当前用户不是 ADMIN 角色 |
| 500 Internal Server Error | 服务器内部错误 | 系统异常（应记录错误日志并通知管理员） |

---

## 安全性考虑

### 1. 认证
- 使用 JWT Bearer Token 进行身份认证
- Token 必须在 `Authorization` 请求头中提供
- Token 格式：`Bearer <token>`

### 2. 授权
- 使用 Spring Security `@PreAuthorize("hasRole('ADMIN')")` 进行方法级权限控制
- 仅 ADMIN 角色可访问所有接口
- USER 角色或未认证用户无法访问

### 3. 输入验证
- 使用 Jakarta Validation 进行请求参数验证
- 验证规则在 DTO 类中定义（`@NotNull`, `@Min`, `@Max`）
- 验证失败自动返回 400 错误

### 4. 操作审计
- 所有修改操作记录到日志
- 日志包含操作人信息（从 JWT 提取）
- 便于追溯和审计

---

## 性能考虑

### 1. 响应时间
- 查询接口：< 100ms（内存读取）
- 修改接口：< 200ms（包含日志记录和配置更新）
- 不涉及数据库操作，性能优秀

### 2. 并发性能
- 查询接口支持高并发（`ConcurrentHashMap` 无锁读）
- 修改接口使用 `synchronized` 加锁（串行化修改）
- 修改频率低，锁竞争不严重

### 3. 系统影响
- 并发限制修改不影响正在执行的任务
- 不需要重启服务
- 对系统吞吐量的影响取决于新设置的并发限制值

---

## 集成测试示例

### 场景1：查询并发配置

```java
@Test
@DisplayName("查询并发配置 - 成功返回所有动作类型的配置")
void getConcurrencyConfig_ShouldReturnAllConfigs() {
    given()
        .header("Authorization", "Bearer " + adminToken)
    .when()
        .get("/api/v1/admin/robot-task/concurrency/config")
    .then()
        .statusCode(200)
        .body("size()", equalTo(3))
        .body("[0].actionType", notNullValue())
        .body("[0].concurrencyLimit", greaterThan(0))
        .body("[0].usageRate", greaterThanOrEqualTo(0.0f))
        .body("[0].usageRate", lessThanOrEqualTo(1.0f));
}
```

### 场景2：修改并发限制（成功）

```java
@Test
@DisplayName("修改并发限制 - 成功修改并立即生效")
void updateConcurrencyLimit_ShouldUpdateSuccessfully() {
    String requestBody = "{ \"concurrencyLimit\": 20 }";
    
    given()
        .header("Authorization", "Bearer " + adminToken)
        .contentType("application/json")
        .body(requestBody)
    .when()
        .put("/api/v1/admin/robot-task/concurrency/config/SEND_MESSAGE")
    .then()
        .statusCode(200)
        .body("success", equalTo(true))
        .body("message", equalTo("并发限制修改成功"));
    
    // 验证修改后的配置
    given()
        .header("Authorization", "Bearer " + adminToken)
    .when()
        .get("/api/v1/admin/robot-task/concurrency/config")
    .then()
        .body("find { it.actionType == 'SEND_MESSAGE' }.concurrencyLimit", equalTo(20));
}
```

### 场景3：修改并发限制（权限不足）

```java
@Test
@DisplayName("修改并发限制 - 非ADMIN用户被拒绝")
void updateConcurrencyLimit_ShouldRejectNonAdmin() {
    String requestBody = "{ \"concurrencyLimit\": 20 }";
    
    given()
        .header("Authorization", "Bearer " + userToken)  // USER角色Token
        .contentType("application/json")
        .body(requestBody)
    .when()
        .put("/api/v1/admin/robot-task/concurrency/config/SEND_MESSAGE")
    .then()
        .statusCode(403);
}
```

### 场景4：修改并发限制（参数验证失败）

```java
@Test
@DisplayName("修改并发限制 - 并发限制为0时验证失败")
void updateConcurrencyLimit_ShouldRejectZeroLimit() {
    String requestBody = "{ \"concurrencyLimit\": 0 }";
    
    given()
        .header("Authorization", "Bearer " + adminToken)
        .contentType("application/json")
        .body(requestBody)
    .when()
        .put("/api/v1/admin/robot-task/concurrency/config/SEND_MESSAGE")
    .then()
        .statusCode(400)
        .body("message", containsString("并发限制必须大于 0"));
}
```

---

## 总结

本 API 契约定义了 2 个管理接口：
1. **查询接口**：获取所有动作类型的并发配置和实时状态
2. **修改接口**：动态修改指定动作类型的并发限制

关键特性：
- ✅ RESTful 风格设计
- ✅ 基于角色的权限控制（ADMIN 角色）
- ✅ JWT Token 认证
- ✅ 请求参数验证（Jakarta Validation）
- ✅ 统一错误响应格式
- ✅ 操作审计日志
- ✅ 符合项目现有 API 规范

这些接口简洁、安全、易用，满足规范中定义的所有功能需求。
