# API契约：用户消息LLM请求任务队列

**功能**: 004-message-llm-queue  
**日期**: 2025-10-22  
**版本**: v1.0

## 概述

本功能主要扩展现有的消息发送API，不新增公开的HTTP接口。内部接口变更包括：
1. MessageService.sendMessage - 添加RobotTask创建逻辑
2. RobotTaskExecutor.executeSendMessage - 实现LLM请求逻辑

## HTTP API（无变更）

### 1. 发送消息

**接口**: 现有接口，行为扩展

```
POST /api/v1/messages
```

**请求头**:
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

**请求体**:
```json
{
  "receiverId": 2001,
  "type": "text",
  "content": "你好，今天天气怎么样？"
}
```

**请求参数说明**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| receiverId | Long | 是 | 接收者用户ID（可以是AI用户） |
| type | String | 是 | 消息类型（通常为"text"） |
| content | String | 是 | 消息内容 |

**响应**:
```json
{
  "code": 0,
  "message": "发送成功",
  "data": null
}
```

**行为变更**:
- **原有行为**: 创建Message记录，更新Conversation，通知接收方
- **新增行为**: 如果receiverId是AI用户，额外创建RobotTask并加载到内存队列

**错误响应**:
```json
{
  "code": -1,
  "message": "队列已满，请稍后重试",
  "data": null
}
```

**说明**:
- 当内存队列已满时，任务创建失败但Message已保存
- 后续可通过RobotTaskLoaderJob自动加载

## 内部服务接口

### 1. MessageService.sendMessage

**签名**:
```java
public void sendMessage(@Valid SendMessageRequest request)
```

**扩展行为**:
1. 检查receiverId是否为AI用户（userType='AI'）
2. 如果是，创建RobotTask记录
3. 调用robotTaskScheduler.loadTask()加载到内存队列
4. 如果loadTask返回false，记录警告日志

**实现伪代码**:
```java
@Transactional(rollbackFor = Exception.class)
public void sendMessage(@Valid SendMessageRequest request) {
    // 原有逻辑：创建Message
    Long uid = getCurrentUserId();
    Message msg = createAndSaveMessage(uid, request);
    
    // 原有逻辑：更新Conversation
    updateConversations(uid, request.getReceiverId(), msg.getId());
    
    // 原有逻辑：通知接收方
    notifyUser(request.getReceiverId());
    
    // 新增逻辑：如果是AI用户，创建任务
    if (isAiUser(request.getReceiverId())) {
        RobotTask task = createRobotTask(msg, request.getReceiverId());
        robotTaskMapper.insert(task);
        
        // 尝试加载到内存队列
        boolean loaded = robotTaskScheduler.loadTask(task);
        if (!loaded) {
            log.warn("任务加载失败，队列已满: taskId={}", task.getId());
        }
    }
}

private boolean isAiUser(Long userId) {
    User user = userMapper.selectById(userId);
    return user != null && "AI".equals(user.getUserType());
}

private RobotTask createRobotTask(Message msg, Long robotId) {
    // 构建action_payload
    Map<String, Object> payload = new HashMap<>();
    payload.put("messageId", msg.getId());
    payload.put("senderId", msg.getSenderId());
    payload.put("receiverId", msg.getReceiverId());
    payload.put("content", msg.getContent());
    payload.put("modelId", config.getDefaultModelId());
    
    RobotTask task = RobotTask.builder()
        .userId(msg.getSenderId())
        .robotId(robotId)
        .taskType("SHORT_DELAY")
        .actionType("SEND_MESSAGE")
        .actionPayload(JSON.toJSONString(payload))
        .scheduledAt(LocalDateTime.now().plusSeconds(config.getMessageDelaySeconds()))
        .status("PENDING")
        .maxRetryCount(config.getMaxRetryCount())
        .build();
    
    return task;
}
```

### 2. RobotTaskExecutor.executeSendMessage

**签名**:
```java
private void executeSendMessage(RobotTask task)
```

**实现逻辑**:
1. 解析action_payload JSON
2. 构建InferenceRequest
3. 调用inferenceService.chat()
4. 将响应内容保存为新Message
5. 异常情况下抛出，由框架处理重试

**实现伪代码**:
```java
private void executeSendMessage(RobotTask task) {
    try {
        // 1. 解析payload
        JSONObject payload = JSON.parseObject(task.getActionPayload());
        Long messageId = payload.getLong("messageId");
        Long senderId = payload.getLong("senderId");
        Long receiverId = payload.getLong("receiverId");
        String content = payload.getString("content");
        Long modelId = payload.getLong("modelId");
        
        log.info("执行LLM任务: taskId={}, messageId={}, modelId={}", 
                 task.getId(), messageId, modelId);
        
        // 2. 构建LLM请求
        InferenceRequest request = new InferenceRequest();
        request.setModelId(modelId);
        request.setPrompt(content);
        request.setTemperature(0.7);
        request.setMaxTokens(500);
        
        // 3. 调用LLM服务
        InferenceResponse response = inferenceService.chat(request);
        
        if (!response.getSuccess()) {
            throw new RuntimeException("LLM推理失败: " + response.getErrorMessage());
        }
        
        log.info("LLM响应成功: taskId={}, tokens={}, time={}ms", 
                 task.getId(), response.getTotalTokens(), response.getResponseTimeMs());
        
        // 4. 保存AI回复消息
        Message aiReply = new Message();
        aiReply.setSenderId(receiverId);  // AI用户
        aiReply.setReceiverId(senderId);  // 原发送者
        aiReply.setType("text");
        aiReply.setContent(response.getContent());
        aiReply.setStatus("sent");
        aiReply.setIsDeleted(0);
        aiReply.setCreatedAt(LocalDateTime.now());
        aiReply.setUpdatedAt(LocalDateTime.now());
        
        messageRepository.save(aiReply);
        
        // 5. 更新会话（可选）
        try {
            conversationMapper.upsertSender(receiverId, senderId, aiReply.getId(), aiReply.getCreatedAt());
            conversationMapper.upsertReceiver(senderId, receiverId, aiReply.getId(), aiReply.getCreatedAt());
        } catch (Exception e) {
            log.warn("更新会话失败: {}", e.getMessage());
        }
        
        // 6. 通知用户有新消息
        messageService.notifyUser(senderId);
        
        log.info("SEND_MESSAGE任务完成: taskId={}, aiReplyId={}", task.getId(), aiReply.getId());
        
    } catch (Exception e) {
        log.error("SEND_MESSAGE任务执行失败: taskId={}, error={}", task.getId(), e.getMessage(), e);
        throw e; // 抛出异常，由框架处理重试
    }
}
```

## 数据契约

### Action Payload JSON

**格式**:
```json
{
  "messageId": 123,
  "senderId": 1001,
  "receiverId": 2001,
  "content": "用户消息内容",
  "modelId": 1
}
```

**字段定义**:
```typescript
interface ActionPayload {
  messageId: number;    // 原始消息ID
  senderId: number;     // 发送者用户ID
  receiverId: number;   // 接收者AI用户ID
  content: string;      // 用户消息内容（作为LLM的prompt）
  modelId: number;      // LLM模型ID
}
```

**验证规则**:
- 所有字段必填
- messageId必须存在于message表
- senderId和receiverId必须存在于user表
- content不能为空
- modelId必须存在于ai_model表且enabled=true

### InferenceRequest（内部）

```java
public class InferenceRequest {
    @NotNull
    private Long modelId;
    
    @NotBlank
    private String prompt;
    
    private Double temperature;  // 可选，默认0.7
    private Integer maxTokens;   // 可选，默认500
    private Double topP;         // 可选
    private Double frequencyPenalty;  // 可选
    private Double presencePenalty;   // 可选
}
```

### InferenceResponse（内部）

```java
public class InferenceResponse {
    private String content;           // 生成的回复内容
    private Long modelId;
    private String modelName;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private Integer responseTimeMs;
    private String requestId;
    private Boolean success;
    private String errorMessage;
}
```

## 依赖服务

### 1. UnifiedInferenceController/InferenceService

**作用**: 提供LLM推理能力

**接口**: 
```java
InferenceResponse chat(InferenceRequest request)
```

**依赖性**: 强依赖，失败时任务自动重试

### 2. RobotTaskScheduler

**作用**: 管理内存任务队列

**接口**:
```java
boolean loadTask(RobotTask task)
```

**返回值**:
- `true`: 任务成功加载到队列
- `false`: 队列已满或任务已存在

### 3. UserMapper

**作用**: 查询用户类型

**新增查询**:
```java
User selectById(Long userId)
```

**依赖字段**: `user.user_type` (VARCHAR) - 需要确认字段存在

## 错误码定义

| 错误码 | 说明 | HTTP状态码 | 处理方式 |
|--------|------|-----------|----------|
| 0 | 成功 | 200 | - |
| -1 | 通用错误 | 200 | 查看message字段 |
| 401 | 未授权 | 401 | 重新登录 |
| 403 | 权限不足 | 403 | 联系管理员 |

**注意**: 本功能的错误主要通过日志记录，不直接返回给用户。任务失败会触发重试机制。

## 性能指标

### 请求性能

| 指标 | 目标值 | 说明 |
|------|--------|------|
| /api/v1/messages P95延迟 | < 100ms | 包括任务创建 |
| 任务创建开销 | < 10ms | 相对于原有流程的增量 |

### 任务执行性能

| 指标 | 目标值 | 说明 |
|------|--------|------|
| LLM任务P95延迟 | < 5秒 | 从scheduled_at到AI回复创建 |
| LLM推理时间 | < 3秒 | 取决于LLM服务 |
| 任务执行吞吐量 | ≥ 10/秒 | 单实例SEND_MESSAGE并发数 |

## 兼容性

### 向后兼容

- ✅ 现有的消息发送功能完全兼容
- ✅ 非AI用户接收消息时无任何变化
- ✅ Message表结构无变化
- ✅ RobotTask表结构无变化

### 配置兼容

**新增配置项**（有默认值）:
```properties
robot.task.default-model-id=1
robot.task.message-delay-seconds=3
```

**已有配置项**（复用）:
```properties
robot.task.max-queue-size=10000
robot.task.concurrency-send-message=10
robot.task.max-retry-count=3
```

## 测试契约

### 单元测试契约

**MessageServiceTest**:
- `testSendMessageToAiUser_ShouldCreateTask()` - 验证AI用户接收消息时创建任务
- `testSendMessageToRealUser_ShouldNotCreateTask()` - 验证真实用户接收消息时不创建任务
- `testSendMessageWithQueueFull_ShouldLogWarning()` - 验证队列满时的处理

**RobotTaskExecutorTest**:
- `testExecuteSendMessage_Success()` - 验证正常执行流程
- `testExecuteSendMessage_LlmFailure()` - 验证LLM失败时抛出异常
- `testExecuteSendMessage_InvalidPayload()` - 验证非法payload的处理

### 集成测试契约

**MessageLlmQueueIntegrationTest**:
- `testEndToEnd_UserSendsMessage_AiReplies()` - 验证完整流程
- `testEndToEnd_TaskRetry_OnLlmFailure()` - 验证重试机制
- `testEndToEnd_QueuePersistence_OnRestart()` - 验证任务持久化

## 监控和告警

### 监控指标

- `robot_task.created.count{action_type=SEND_MESSAGE}` - 创建的任务数
- `robot_task.execution.duration{action_type=SEND_MESSAGE}` - 执行耗时
- `robot_task.execution.success{action_type=SEND_MESSAGE}` - 成功率
- `robot_task.queue.size` - 队列大小
- `robot_task.queue.usage` - 队列使用率

### 告警规则

- 队列使用率 > 80%：警告
- 任务失败率 > 10%：警告
- 任务平均延迟 > 10秒：警告
- LLM服务失败率 > 20%：严重

## 安全考虑

### 权限验证

- ✅ 复用现有的/api/v1/messages端点权限（@PreAuthorize）
- ✅ 从SecurityContext获取用户ID，防止伪造
- ✅ AI用户禁止登录（已在feature 002实现）

### 数据安全

- ✅ action_payload不包含敏感信息（密码、token等）
- ✅ LLM请求内容限制在用户消息范围内
- ✅ 使用事务保证数据一致性

### 限流

- ✅ 已有IP限流（IpRateLimitFilter）
- ✅ 队列容量限制（maxQueueSize）
- ✅ 并发限制（concurrencySendMessage）

## 文档变更

### 需要更新的文档

1. **API文档**: 无需更新（行为透明扩展）
2. **运维文档**: 新增配置项说明
3. **监控文档**: 新增SEND_MESSAGE任务指标

### 需要沟通的变更

- 给AI用户发送消息时会有3秒延迟
- 需要配置默认的LLM模型ID
- 需要关注队列容量和并发配置
