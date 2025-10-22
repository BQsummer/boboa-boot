# 调研文档：用户消息LLM请求任务队列

**功能**: 004-message-llm-queue  
**日期**: 2025-10-22  
**阶段**: Phase 0 - 调研与分析

## 调研目的

本调研旨在明确实现"用户消息LLM请求任务队列"功能的技术细节，包括：
1. 如何在MessageService中创建RobotTask任务
2. 如何实现RobotTaskExecutor.executeSendMessage调用LLM服务
3. action_payload的JSON格式设计
4. AI角色与用户的关联关系
5. LLM推理服务的调用方式

## 已知信息（来自现有代码）

### 1. 现有MessageService结构

当前`MessageService.sendMessage`方法的实现：
- 从SecurityContext获取当前用户ID（发送者）
- 创建Message对象并保存到数据库
- 更新Conversation表（发送方和接收方）
- 调用notifyUser通知接收方

**关键发现**：
- 已有完整的消息创建流程
- 使用@Transactional注解（在调用方或需要添加）
- 接收者ID通过request.getReceiverId()传入

### 2. RobotTask实体结构

```java
@TableName("robot_task")
public class RobotTask {
    private Long id;
    private Long userId;              // 发送者ID
    private Long robotId;             // 接收者AI角色ID
    private String taskType;          // IMMEDIATE, SHORT_DELAY, LONG_DELAY
    private String actionType;        // SEND_MESSAGE, SEND_VOICE, SEND_NOTIFICATION
    private String actionPayload;     // JSON格式载荷
    private LocalDateTime scheduledAt;
    private String status;            // PENDING, RUNNING, DONE, FAILED
    private Integer version;          // 乐观锁
    private Integer retryCount;
    private Integer maxRetryCount;
    // ... 其他字段
}
```

**关键发现**：
- userId应为发送消息的用户ID
- robotId应为接收消息的AI角色ID（关联的user_id）
- taskType应设置为"IMMEDIATE"或"SHORT_DELAY"
- actionType固定为"SEND_MESSAGE"
- scheduledAt可设置为当前时间或延迟几秒

### 3. RobotTaskScheduler加载机制

```java
public boolean loadTask(RobotTask task) {
    if (loadedTaskIds.contains(task.getId())) {
        return false; // 已加载
    }
    if (taskQueue.size() >= maxQueueSize) {
        return false; // 队列已满
    }
    taskQueue.offer(new RobotTaskWrapper(task));
    loadedTaskIds.add(task.getId());
    return true;
}
```

**关键发现**：
- 需要在任务创建后调用robotTaskScheduler.loadTask(task)
- 需要注入RobotTaskScheduler依赖
- 队列已满时会拒绝加载（需处理此情况）

### 4. LLM推理服务接口

```java
@RestController
@RequestMapping("/api/v1/inference")
public class UnifiedInferenceController {
    @PostMapping("/chat")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> chat(@Valid @RequestBody InferenceRequest request) {
        InferenceResponse response = inferenceService.chat(request);
        // 返回格式: {code: 0, message: "推理成功", data: response}
    }
}
```

**关键发现**：
- 需要构建InferenceRequest对象
- 需要指定modelId（AI角色关联的模型ID）
- 请求参数包括：prompt（用户消息内容）、temperature、maxTokens等
- 响应包含生成的content

### 5. RobotTaskExecutor.executeSendMessage

当前为TODO实现：
```java
private void executeSendMessage(RobotTask task) {
    // TODO: 实现发送消息逻辑
    log.info("发送消息: taskId={}, payload={}", task.getId(), task.getActionPayload());
}
```

**需要实现**：
1. 解析action_payload JSON
2. 调用LLM推理服务
3. 将LLM响应保存为新的Message
4. 更新任务状态

## 调研决策

### 决策 1：action_payload JSON格式

**选择**：
```json
{
  "messageId": 123,
  "senderId": 1001,
  "receiverId": 2001,
  "content": "用户消息内容",
  "modelId": 1
}
```

**理由**：
- 包含完整的消息上下文信息
- messageId用于关联原始消息
- modelId指定使用哪个LLM模型（从AI角色配置获取）
- content是LLM的输入prompt

**备选方案**：
- 仅包含messageId，其他信息从数据库查询：增加数据库查询开销，不够高效
- 包含更多上下文（历史消息）：会使payload过大，影响性能

### 决策 2：AI角色与LLM模型关联

**选择**：通过配置文件或数据库配置AI角色的默认modelId

**理由**：
- AI角色（AI_CHARACTER表）需要关联一个默认的LLM模型
- 当前代码未看到此关联，需要在实现时确认或新增
- 可以先硬编码一个默认modelId，后续再扩展

**备选方案**：
- 每次请求时动态选择模型：增加复杂性，不符合最小化修改原则
- 使用路由策略选择模型：已有RoutingStrategy功能，但本功能范围不包含

**临时方案**：在RobotTaskConfiguration中添加默认modelId配置

### 决策 3：任务创建时机

**选择**：在MessageService.sendMessage方法中，message保存成功后立即创建RobotTask

**理由**：
- 确保message和robot_task在同一事务中创建
- 符合事务一致性原则
- 简化错误处理（事务回滚会同时撤销两个操作）

**备选方案**：
- 通过事件监听器异步创建任务：增加复杂性，可能导致数据不一致
- 在Controller层创建任务：违反分层原则，Service层更合适

### 决策 4：任务scheduled_at设置

**选择**：设置为当前时间加3秒延迟

**理由**：
- 给系统一个缓冲时间，避免瞬间高峰
- 3秒延迟对用户体验影响不大
- 符合规范中的"短延迟"定义

**备选方案**：
- 立即执行（当前时间）：可能导致瞬时并发过高
- 更长延迟（如10秒）：影响用户体验

### 决策 5：RobotTaskExecutor.executeSendMessage实现策略

**选择**：
1. 使用FastJSON解析action_payload
2. 构建InferenceRequest并调用inferenceService.chat()
3. 将响应内容保存为新Message（使用messageRepository）
4. 不更新任务状态（由RobotTaskExecutor框架自动处理）

**理由**：
- 使用inferenceService而非直接调用Controller，更符合分层原则
- messageRepository已有save方法，复用现有代码
- 任务状态更新由executeSendMessage的调用者（execute方法）处理

**备选方案**：
- 直接HTTP调用/api/v1/inference/chat：增加网络开销，不必要
- 手动更新任务状态：与现有框架逻辑重复，违反DRY原则

## 技术选型总结

| 技术点 | 选择 | 替代方案 |
|--------|------|----------|
| JSON解析 | FastJSON (已有依赖) | Jackson, Gson |
| 任务创建位置 | MessageService.sendMessage | Event Listener |
| LLM调用方式 | 注入InferenceService | HTTP调用Controller |
| payload格式 | 完整上下文JSON | 仅messageId |
| scheduled延迟 | 3秒 | 0秒或更长 |
| 模型关联 | 配置文件默认modelId | 动态选择 |

## 待明确项（需要在实现时确认）

### 1. AI角色模型关联
**问题**：AI_CHARACTER表是否有modelId字段？
**影响**：如果没有，需要从配置文件读取默认modelId
**解决方案**：
- 优先从ai_character.model_id字段读取（如果存在）
- 否则使用配置文件中的默认值：robot.task.default-model-id

### 2. MessageService事务管理
**问题**：当前MessageService.sendMessage是否已有@Transactional？
**影响**：需要确保message和robot_task在同一事务中
**解决方案**：
- 如果没有，在方法上添加@Transactional(rollbackFor = Exception.class)
- 如果有，无需修改

### 3. InferenceService访问权限
**问题**：RobotTaskExecutor是否能直接注入InferenceService？
**影响**：可能需要绕过Controller的权限验证
**解决方案**：
- 优先注入InferenceService直接调用
- 如果InferenceService不存在或受限，考虑使用内部调用机制

## 风险与缓解

### 风险 1：队列容量满导致任务创建失败
**缓解措施**：
- 捕获loadTask返回false的情况
- 记录警告日志
- 考虑将任务标记为PENDING但不加载，等待RobotTaskLoaderJob后续加载

### 风险 2：LLM服务调用失败
**缓解措施**：
- 已有重试机制（RobotTaskExecutor框架）
- 确保异常被正确捕获并记录
- 设置合理的max_retry_count（默认3次）

### 风险 3：AI用户不存在或未绑定
**缓解措施**：
- 在创建任务前验证receiverId对应的用户类型
- 如果不是AI用户，跳过任务创建
- 记录错误日志

## 下一步行动

Phase 1将基于以上调研结果：
1. 设计action_payload的JSON结构（contracts/）
2. 定义任务创建和执行的数据流（data-model.md）
3. 编写快速开始指南（quickstart.md）
4. 更新代理上下文文件
