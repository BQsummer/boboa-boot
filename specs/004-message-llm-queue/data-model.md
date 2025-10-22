# 数据模型：用户消息LLM请求任务队列

**功能**: 004-message-llm-queue  
**日期**: 2025-10-22  
**阶段**: Phase 1 - 设计

## 概述

本文档定义"用户消息LLM请求任务队列"功能涉及的数据实体、关系和状态转换。本功能主要复用现有实体，不新增表结构。

## 核心实体

### 1. Message（消息）

**用途**：存储用户与AI角色之间的对话消息

**已有字段**：
| 字段 | 类型 | 说明 | 约束 |
|------|------|------|------|
| id | BIGINT | 主键 | AUTO_INCREMENT |
| sender_id | BIGINT | 发送者用户ID | NOT NULL |
| receiver_id | BIGINT | 接收者用户ID | NOT NULL |
| type | VARCHAR(32) | 消息类型 | NOT NULL |
| content | VARCHAR(2048) | 消息内容 | NOT NULL |
| status | VARCHAR(20) | 消息状态 | - |
| is_deleted | TINYINT | 是否删除 | DEFAULT 0 |
| created_at | DATETIME | 创建时间 | - |
| updated_at | DATETIME | 更新时间 | - |

**本功能使用**：
- 用户发送消息时创建一条记录（sender_id=用户ID, receiver_id=AI用户ID）
- AI回复时创建另一条记录（sender_id=AI用户ID, receiver_id=用户ID）

**验证规则**：
- sender_id和receiver_id必须存在于用户表
- content不能为空
- type通常为"text"

### 2. RobotTask（机器人任务）

**用途**：存储延迟执行的机器人任务

**已有字段**：
| 字段 | 类型 | 说明 | 约束 |
|------|------|------|------|
| id | BIGINT | 主键 | AUTO_INCREMENT |
| user_id | BIGINT | 用户ID（发送者） | NOT NULL |
| robot_id | BIGINT | 机器人ID（AI用户） | NULL |
| task_type | VARCHAR(50) | 任务类型 | NOT NULL |
| action_type | VARCHAR(50) | 动作类型 | NOT NULL |
| action_payload | TEXT | 任务载荷（JSON） | NOT NULL |
| scheduled_at | DATETIME | 计划执行时间 | NOT NULL |
| status | VARCHAR(20) | 状态 | DEFAULT 'PENDING' |
| version | INT | 乐观锁版本 | DEFAULT 0 |
| retry_count | INT | 当前重试次数 | DEFAULT 0 |
| max_retry_count | INT | 最大重试次数 | DEFAULT 3 |
| started_at | DATETIME | 开始执行时间 | NULL |
| completed_at | DATETIME | 完成时间 | NULL |
| heartbeat_at | DATETIME | 最后心跳时间 | NULL |
| error_message | TEXT | 错误信息 | NULL |
| created_time | DATETIME | 创建时间 | DEFAULT CURRENT_TIMESTAMP |
| updated_time | DATETIME | 更新时间 | ON UPDATE CURRENT_TIMESTAMP |

**本功能使用**：
- user_id: 发送消息的用户ID
- robot_id: 接收消息的AI用户ID
- task_type: "SHORT_DELAY"（3秒延迟）
- action_type: "SEND_MESSAGE"（固定值）
- action_payload: JSON格式，见下文
- scheduled_at: 当前时间 + 3秒

**验证规则**：
- action_type必须为"SEND_MESSAGE"
- action_payload必须是合法的JSON
- scheduled_at不能早于created_time
- user_id和robot_id必须存在

### 3. RobotTaskExecutionLog（任务执行日志）

**用途**：记录任务执行的详细信息

**已有字段**：
| 字段 | 类型 | 说明 | 约束 |
|------|------|------|------|
| id | BIGINT | 主键 | AUTO_INCREMENT |
| task_id | BIGINT | 任务ID | NOT NULL |
| execution_attempt | INT | 执行尝试次数 | NOT NULL |
| status | VARCHAR(20) | 执行结果 | NOT NULL |
| started_at | DATETIME | 开始时间 | NOT NULL |
| completed_at | DATETIME | 完成时间 | NOT NULL |
| execution_duration_ms | BIGINT | 执行耗时（毫秒） | NOT NULL |
| delay_from_scheduled_ms | BIGINT | 延迟时间（毫秒） | NOT NULL |
| error_message | TEXT | 错误信息 | NULL |
| instance_id | VARCHAR(100) | 实例标识 | NOT NULL |
| created_time | DATETIME | 创建时间 | DEFAULT CURRENT_TIMESTAMP |

**本功能使用**：
- 由RobotTaskExecutor框架自动创建
- 记录LLM请求的执行时间和结果
- 本功能无需直接操作此表

## 辅助实体

### 4. InferenceRequest（LLM推理请求）

**用途**：封装对LLM服务的推理请求

**非数据库实体**：仅为Java DTO对象

**字段**：
| 字段 | 类型 | 说明 | 必填 |
|------|------|------|------|
| modelId | Long | 模型ID | 是 |
| prompt | String | 提示词（用户消息） | 是 |
| temperature | Double | 温度参数 | 否 |
| maxTokens | Integer | 最大token数 | 否 |
| topP | Double | Top-P采样 | 否 |

**本功能使用**：
- 在executeSendMessage中构建此对象
- modelId从配置文件或AI角色获取
- prompt为用户消息内容

### 5. InferenceResponse（LLM推理响应）

**用途**：封装LLM服务的推理结果

**非数据库实体**：仅为Java DTO对象

**字段**：
| 字段 | 类型 | 说明 |
|------|------|------|
| content | String | 生成的回复内容 |
| modelId | Long | 使用的模型ID |
| modelName | String | 模型名称 |
| promptTokens | Integer | 输入token数 |
| completionTokens | Integer | 输出token数 |
| totalTokens | Integer | 总token数 |
| responseTimeMs | Integer | 响应时间（毫秒） |
| requestId | String | 请求ID |
| success | Boolean | 是否成功 |
| errorMessage | String | 错误信息 |

**本功能使用**：
- 从inferenceService.chat()获取此对象
- 提取content字段作为AI回复内容
- 保存到新的Message记录

## Action Payload JSON结构

### 格式定义

```json
{
  "messageId": 123,
  "senderId": 1001,
  "receiverId": 2001,
  "content": "用户消息内容",
  "modelId": 1
}
```

### 字段说明

| 字段 | 类型 | 说明 | 示例 |
|------|------|------|------|
| messageId | Long | 原始消息ID | 123 |
| senderId | Long | 发送者用户ID | 1001 |
| receiverId | Long | 接收者AI用户ID | 2001 |
| content | String | 用户消息内容 | "你好，今天天气怎么样？" |
| modelId | Long | LLM模型ID | 1 |

### 构建示例（Java）

```java
Map<String, Object> payload = new HashMap<>();
payload.put("messageId", message.getId());
payload.put("senderId", message.getSenderId());
payload.put("receiverId", message.getReceiverId());
payload.put("content", message.getContent());
payload.put("modelId", defaultModelId);

String json = JSON.toJSONString(payload);
```

### 解析示例（Java）

```java
JSONObject payload = JSON.parseObject(task.getActionPayload());
Long messageId = payload.getLong("messageId");
Long senderId = payload.getLong("senderId");
Long receiverId = payload.getLong("receiverId");
String content = payload.getString("content");
Long modelId = payload.getLong("modelId");
```

## 实体关系图

```
┌─────────────┐         ┌─────────────┐
│   Message   │         │  RobotTask  │
│  (用户消息)  │────────▶│ (LLM任务)    │
│             │ 触发创建  │             │
│ sender_id   │         │ user_id     │
│ receiver_id │         │ robot_id    │
│ content     │         │ action_     │
└─────────────┘         │ _payload    │
                        └──────┬──────┘
                               │
                               │ 执行
                               ▼
                        ┌─────────────┐
                        │  Inference  │
                        │   Request   │
                        │  (LLM请求)   │
                        │ modelId     │
                        │ prompt      │
                        └──────┬──────┘
                               │
                               │ 调用
                               ▼
                        ┌─────────────┐
                        │ LLM服务     │
                        │ (推理)      │
                        └──────┬──────┘
                               │
                               │ 返回
                               ▼
                        ┌─────────────┐
                        │  Inference  │
                        │   Response  │
                        │  (LLM响应)   │
                        │ content     │
                        └──────┬──────┘
                               │
                               │ 保存为
                               ▼
                        ┌─────────────┐
                        │   Message   │
                        │  (AI回复)    │
                        │ sender_id   │
                        │  =robot_id  │
                        └─────────────┘
```

## 状态转换

### RobotTask状态机

```
创建    │
       ▼
   PENDING ───加载──▶ [内存队列]
       │                │
       │                │ 到期
       │                ▼
       └──────────▶ RUNNING
                        │
                ┌───────┴───────┐
                │               │
            执行成功        执行失败
                │               │
                ▼               ▼
              DONE          FAILED
                              │
                              │ retry_count < max_retry_count
                              │
                              ▼
                          重新调度 ──▶ PENDING
```

**状态说明**：
- **PENDING**: 任务已创建，等待加载到内存队列
- **RUNNING**: 任务正在执行（被RobotTaskExecutor抢占）
- **DONE**: 任务执行成功完成
- **FAILED**: 任务执行失败且不再重试

### Message状态（简化）

本功能中Message状态较简单：
- 创建时status设置为"sent"
- 本功能不处理消息的其他状态（如"delivered", "read"等）

## 数据流

### 完整数据流

```
1. 用户发送消息
   ↓
2. MessageService.sendMessage
   ├─ 创建Message (sender_id=用户, receiver_id=AI)
   ├─ 更新Conversation
   ├─ 创建RobotTask (user_id=用户, robot_id=AI, action_type=SEND_MESSAGE)
   └─ 加载任务到内存队列 (RobotTaskScheduler.loadTask)
   ↓
3. 任务调度 (scheduled_at到期)
   ↓
4. RobotTaskExecutor.executeSendMessage
   ├─ 解析action_payload
   ├─ 构建InferenceRequest
   ├─ 调用inferenceService.chat
   ├─ 获取InferenceResponse
   ├─ 创建新Message (sender_id=AI, receiver_id=用户, content=LLM回复)
   └─ [框架自动] 更新RobotTask状态为DONE
   ↓
5. [框架自动] 记录RobotTaskExecutionLog
```

### 异常处理流

```
LLM调用失败
   ↓
捕获异常
   ↓
[框架自动] 更新RobotTask
   ├─ status = FAILED
   ├─ error_message = 异常信息
   └─ retry_count += 1
   ↓
判断 retry_count < max_retry_count？
   │
   ├─ 是 → 重新调度 (scheduled_at = now + 延迟)
   │
   └─ 否 → 任务最终失败，不再重试
```

## 索引和性能

本功能复用现有表，已有索引：

**robot_task表**：
- `idx_status_scheduled (status, scheduled_at)` - 用于查询待执行任务
- `idx_user_id (user_id)` - 用于用户任务查询
- `idx_robot_id (robot_id)` - 用于机器人任务查询

**message表**：
- 假设已有sender_id和receiver_id的索引（需确认）

**性能考虑**：
- action_payload为TEXT类型，避免过大的JSON（当前设计约200字节，可接受）
- 使用乐观锁避免并发冲突
- 内存队列容量限制防止OOM

## 数据一致性保证

### 事务边界

**事务1：消息和任务创建**
- 范围：MessageService.sendMessage
- 包含操作：
  1. INSERT message
  2. UPDATE conversation (发送方)
  3. UPDATE conversation (接收方)
  4. INSERT robot_task
- 失败处理：整个事务回滚，用户需要重新发送

**事务2：任务执行**
- 范围：RobotTaskExecutor.execute
- 包含操作：
  1. UPDATE robot_task (PENDING → RUNNING, 乐观锁)
  2. 执行业务逻辑 (executeSendMessage)
  3. UPDATE robot_task (RUNNING → DONE/FAILED)
  4. INSERT robot_task_execution_log
- 失败处理：任务标记为FAILED，触发重试机制

### 乐观锁保证

使用MyBatis Plus的@Version注解：
- 任务抢占时version+1
- 并发抢占时只有一个实例成功
- 失败的实例自动放弃任务

### 幂等性

- RobotTask通过乐观锁保证只被执行一次
- Message通过business key（sender_id + receiver_id + timestamp）避免重复
- executeSendMessage内部无需额外的幂等性检查

## 配置依赖

### robot.task配置

```properties
# 任务调度配置
robot.task.max-queue-size=10000
robot.task.concurrency-send-message=10
robot.task.max-retry-count=3

# LLM配置（新增）
robot.task.default-model-id=1
robot.task.message-delay-seconds=3
```

### 配置说明

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| max-queue-size | Integer | 10000 | 内存队列容量上限 |
| concurrency-send-message | Integer | 10 | SEND_MESSAGE并发数 |
| max-retry-count | Integer | 3 | 最大重试次数 |
| default-model-id | Long | 1 | 默认LLM模型ID |
| message-delay-seconds | Integer | 3 | 消息任务延迟秒数 |

## 监控指标

### 数据库指标

- robot_task表的PENDING/RUNNING/DONE/FAILED任务数
- 平均任务执行时间（通过robot_task_execution_log计算）
- 任务失败率（FAILED / DONE+FAILED）
- 任务重试率（retry_count > 0的任务占比）

### 业务指标

- 每分钟创建的SEND_MESSAGE任务数
- 用户消息到AI回复的平均延迟
- LLM服务调用成功率
- 队列积压任务数（loadedTaskIds.size）

## 数据清理策略

### 历史数据清理

- 建议定期清理已完成的任务（status=DONE且completed_at > 30天）
- 建议保留失败任务用于问题排查（status=FAILED，保留90天）
- 执行日志可根据需要清理（建议保留30-90天）

### 清理SQL示例

```sql
-- 清理30天前的已完成任务
DELETE FROM robot_task 
WHERE status = 'DONE' 
  AND completed_at < DATE_SUB(NOW(), INTERVAL 30 DAY);

-- 清理90天前的失败任务
DELETE FROM robot_task 
WHERE status = 'FAILED' 
  AND completed_at < DATE_SUB(NOW(), INTERVAL 90 DAY);

-- 清理执行日志
DELETE FROM robot_task_execution_log 
WHERE created_time < DATE_SUB(NOW(), INTERVAL 30 DAY);
```

注意：清理策略不在本功能范围内，仅作为运维参考。
