# Robot Task API 契约文档

**功能**: 机器人延迟调度系统  
**日期**: 2025年10月17日  
**版本**: v1.0  
**基础路径**: `/api/v1/robot/tasks`

## API 概览

本 API 提供机器人任务的创建、查询和监控功能。主要用于：
1. 创建延迟执行的机器人任务
2. 查询任务执行状态
3. 监控系统运行指标（管理员）

**认证要求**: 所有接口需要 JWT 认证（`Authorization: Bearer <token>`）

---

## 1. 创建机器人任务

### 请求

**端点**: `POST /api/v1/robot/tasks`

**权限**: `USER` 或 `ADMIN`

**请求体**:

```json
{
  "robot_id": 5,
  "action_type": "SEND_MESSAGE",
  "action_payload": {
    "conversation_id": 789,
    "message": "您好！有什么可以帮助您的吗？"
  },
  "delay_seconds": 5,
  "max_retry_count": 3
}
```

**字段说明**:

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `robot_id` | Long | 是 | 机器人ID（必须是用户有权访问的机器人） |
| `action_type` | String | 是 | 行为类型：`SEND_MESSAGE`, `SEND_VOICE`, `SEND_NOTIFICATION` |
| `action_payload` | Object | 是 | 任务载荷（JSON对象，根据 action_type 不同而不同） |
| `delay_seconds` | Integer | 否 | 延迟秒数，0表示立即执行，默认0 |
| `max_retry_count` | Integer | 否 | 最大重试次数，默认3 |

**action_payload 示例**:

- **SEND_MESSAGE**:
  ```json
  {
    "conversation_id": 789,
    "message": "文本内容"
  }
  ```

- **SEND_VOICE**:
  ```json
  {
    "conversation_id": 789,
    "voice_asset_id": 123
  }
  ```

- **SEND_NOTIFICATION**:
  ```json
  {
    "title": "通知标题",
    "message": "通知内容"
  }
  ```

### 响应

**成功响应** (HTTP 201):

```json
{
  "code": 200,
  "message": "任务创建成功",
  "data": {
    "task_id": 1001,
    "scheduled_at": "2025-10-17T10:30:05Z",
    "status": "PENDING"
  }
}
```

**错误响应**:

- **400 Bad Request** - 参数验证失败
  ```json
  {
    "code": 400,
    "message": "参数验证失败",
    "errors": [
      "robot_id 不能为空",
      "action_type 必须是 SEND_MESSAGE, SEND_VOICE, SEND_NOTIFICATION 之一"
    ]
  }
  ```

- **403 Forbidden** - 无权访问指定机器人
  ```json
  {
    "code": 403,
    "message": "无权操作该机器人"
  }
  ```

- **404 Not Found** - 机器人不存在
  ```json
  {
    "code": 404,
    "message": "机器人不存在"
  }
  ```

---

## 2. 查询任务详情

### 请求

**端点**: `GET /api/v1/robot/tasks/{taskId}`

**权限**: `USER` 或 `ADMIN` (只能查询自己创建的任务)

**路径参数**:
- `taskId` (Long): 任务ID

### 响应

**成功响应** (HTTP 200):

```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "task_id": 1001,
    "user_id": 100,
    "robot_id": 5,
    "task_type": "SHORT_DELAY",
    "action_type": "SEND_MESSAGE",
    "action_payload": {
      "conversation_id": 789,
      "message": "您好！"
    },
    "scheduled_at": "2025-10-17T10:30:05Z",
    "status": "DONE",
    "retry_count": 0,
    "max_retry_count": 3,
    "started_at": "2025-10-17T10:30:05Z",
    "completed_at": "2025-10-17T10:30:06Z",
    "error_message": null,
    "created_time": "2025-10-17T10:30:00Z"
  }
}
```

**错误响应**:

- **403 Forbidden** - 无权查询该任务
- **404 Not Found** - 任务不存在

---

## 3. 查询用户的任务列表

### 请求

**端点**: `GET /api/v1/robot/tasks`

**权限**: `USER` 或 `ADMIN`

**查询参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `status` | String | 否 | 过滤状态：`PENDING`, `RUNNING`, `DONE`, `FAILED` |
| `robot_id` | Long | 否 | 过滤机器人ID |
| `page` | Integer | 否 | 页码，默认1 |
| `size` | Integer | 否 | 每页大小，默认20，最大100 |

**示例**: `GET /api/v1/robot/tasks?status=PENDING&page=1&size=20`

### 响应

**成功响应** (HTTP 200):

```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "total": 150,
    "page": 1,
    "size": 20,
    "items": [
      {
        "task_id": 1001,
        "robot_id": 5,
        "action_type": "SEND_MESSAGE",
        "scheduled_at": "2025-10-17T10:30:05Z",
        "status": "PENDING",
        "created_time": "2025-10-17T10:30:00Z"
      },
      {
        "task_id": 1002,
        "robot_id": 5,
        "action_type": "SEND_VOICE",
        "scheduled_at": "2025-10-18T10:30:00Z",
        "status": "PENDING",
        "created_time": "2025-10-17T10:30:00Z"
      }
    ]
  }
}
```

---

## 4. 取消任务 (可选功能)

### 请求

**端点**: `DELETE /api/v1/robot/tasks/{taskId}`

**权限**: `USER` 或 `ADMIN` (只能取消自己创建的任务)

**路径参数**:
- `taskId` (Long): 任务ID

**注意**: 只能取消状态为 `PENDING` 的任务

### 响应

**成功响应** (HTTP 200):

```json
{
  "code": 200,
  "message": "任务已取消"
}
```

**错误响应**:

- **400 Bad Request** - 任务已开始执行，无法取消
  ```json
  {
    "code": 400,
    "message": "任务已开始执行，无法取消"
  }
  ```

- **403 Forbidden** - 无权取消该任务
- **404 Not Found** - 任务不存在

---

## 5. 查询任务执行日志 (管理员)

### 请求

**端点**: `GET /api/v1/robot/tasks/{taskId}/logs`

**权限**: `ADMIN`

**路径参数**:
- `taskId` (Long): 任务ID

### 响应

**成功响应** (HTTP 200):

```json
{
  "code": 200,
  "message": "查询成功",
  "data": [
    {
      "log_id": 5001,
      "task_id": 1001,
      "execution_attempt": 1,
      "status": "SUCCESS",
      "started_at": "2025-10-17T10:30:05Z",
      "completed_at": "2025-10-17T10:30:06Z",
      "execution_duration_ms": 1200,
      "delay_from_scheduled_ms": 500,
      "error_message": null,
      "instance_id": "pod-abc123"
    }
  ]
}
```

---

## 6. 获取系统监控指标 (管理员)

### 请求

**端点**: `GET /api/v1/robot/tasks/metrics`

**权限**: `ADMIN`

### 响应

**成功响应** (HTTP 200):

```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "queue_size": 1250,
    "pending_count": 5430,
    "running_count": 12,
    "success_rate_1h": 0.987,
    "avg_delay_ms_1h": 350,
    "retry_distribution": {
      "0": 9500,
      "1": 450,
      "2": 30,
      "3": 20
    }
  }
}
```

**字段说明**:

| 字段 | 说明 |
|------|------|
| `queue_size` | 内存队列中的任务数 |
| `pending_count` | 数据库中 PENDING 状态的任务数 |
| `running_count` | 数据库中 RUNNING 状态的任务数 |
| `success_rate_1h` | 最近1小时的成功率 (0-1) |
| `avg_delay_ms_1h` | 最近1小时的平均执行延迟（毫秒） |
| `retry_distribution` | 重试次数分布（次数 -> 任务数） |

---

## 7. 重试失败任务 (管理员)

### 请求

**端点**: `POST /api/v1/robot/tasks/{taskId}/retry`

**权限**: `ADMIN`

**路径参数**:
- `taskId` (Long): 任务ID

**请求体** (可选):

```json
{
  "reset_retry_count": false
}
```

**字段说明**:
- `reset_retry_count` (Boolean): 是否重置重试计数，默认 false

### 响应

**成功响应** (HTTP 200):

```json
{
  "code": 200,
  "message": "任务已重新调度",
  "data": {
    "task_id": 1001,
    "new_scheduled_at": "2025-10-17T10:35:00Z",
    "status": "PENDING"
  }
}
```

**错误响应**:

- **400 Bad Request** - 任务状态不允许重试
  ```json
  {
    "code": 400,
    "message": "只能重试状态为 FAILED 或 TIMEOUT 的任务"
  }
  ```

---

## 数据模型

### CreateTaskRequest

```java
@Data
public class CreateTaskRequest {
    @NotNull(message = "robot_id 不能为空")
    private Long robotId;
    
    @NotBlank(message = "action_type 不能为空")
    private String actionType;  // SEND_MESSAGE, SEND_VOICE, SEND_NOTIFICATION
    
    @NotNull(message = "action_payload 不能为空")
    private Map<String, Object> actionPayload;
    
    @Min(value = 0, message = "delay_seconds 不能为负数")
    private Integer delaySeconds = 0;
    
    @Min(value = 0, message = "max_retry_count 不能为负数")
    @Max(value = 10, message = "max_retry_count 不能超过10")
    private Integer maxRetryCount = 3;
}
```

### TaskResponse

```java
@Data
public class TaskResponse {
    private Long taskId;
    private Long userId;
    private Long robotId;
    private String taskType;
    private String actionType;
    private Map<String, Object> actionPayload;
    private LocalDateTime scheduledAt;
    private String status;
    private Integer retryCount;
    private Integer maxRetryCount;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String errorMessage;
    private LocalDateTime createdTime;
}
```

---

## 错误码规范

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未认证 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 409 | 资源冲突（如任务已存在） |
| 500 | 服务器内部错误 |

---

## 业务规则

1. **任务归属**: 每个任务必须关联到创建它的用户，用户只能查询和操作自己的任务
2. **机器人权限**: 用户只能使用自己创建或有权访问的机器人创建任务
3. **延迟范围**: 
   - `delay_seconds = 0`: 立即执行（IMMEDIATE）
   - `0 < delay_seconds <= 600`: 短延迟（SHORT_DELAY）
   - `delay_seconds > 600`: 长延迟（LONG_DELAY）
4. **取消限制**: 只能取消状态为 `PENDING` 的任务
5. **重试限制**: 重试次数不能超过 `max_retry_count`
6. **分页限制**: 每页最大返回100条记录

---

## 使用示例

### 创建立即执行的消息任务

```bash
curl -X POST https://api.example.com/api/v1/robot/tasks \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "robot_id": 5,
    "action_type": "SEND_MESSAGE",
    "action_payload": {
      "conversation_id": 789,
      "message": "您好！"
    },
    "delay_seconds": 0
  }'
```

### 创建5秒后发送的语音任务

```bash
curl -X POST https://api.example.com/api/v1/robot/tasks \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "robot_id": 5,
    "action_type": "SEND_VOICE",
    "action_payload": {
      "conversation_id": 789,
      "voice_asset_id": 123
    },
    "delay_seconds": 5
  }'
```

### 查询待执行任务

```bash
curl -X GET "https://api.example.com/api/v1/robot/tasks?status=PENDING&page=1&size=20" \
  -H "Authorization: Bearer <token>"
```

---

**API 契约完成日期**: 2025年10月17日  
**下一步**: 创建快速开始文档 (`quickstart.md`)
