# 快速开始：用户消息LLM请求任务队列

**功能**: 004-message-llm-queue  
**日期**: 2025-10-22  
**适用人员**: 开发者、测试人员

## 概述

本文档提供快速上手指南，帮助开发者理解和实现"用户消息LLM请求任务队列"功能。

## 前置条件

### 环境要求

- Java 17
- MySQL 8.0+
- Maven 3.8+
- IDE（推荐IntelliJ IDEA）

### 依赖功能

确保以下功能已实现：
- ✅ Feature 001: LLM模型实例管理和推理服务
- ✅ Feature 002: AI角色与用户账户绑定
- ✅ RobotTaskScheduler和RobotTaskExecutor基础设施
- ✅ Message和MessageService基础功能

### 数据准备

1. 确保至少有一个AI用户：
```sql
-- 检查AI用户
SELECT id, user_name, user_type FROM user WHERE user_type = 'AI';
```

2. 确保至少有一个可用的LLM模型：
```sql
-- 检查LLM模型
SELECT id, name, version, enabled FROM ai_model WHERE enabled = true;
```

## 5分钟快速演示

### 步骤 1: 配置默认模型ID

编辑 `src/main/resources/application.properties`：

```properties
# LLM任务配置
robot.task.default-model-id=1
robot.task.message-delay-seconds=3
```

### 步骤 2: 启动应用

```bash
mvn spring-boot:run
```

### 步骤 3: 发送消息给AI用户

使用curl或Postman：

```bash
curl -X POST http://localhost:8080/api/v1/messages \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "receiverId": 2001,
    "type": "text",
    "content": "你好，今天天气怎么样？"
  }'
```

**说明**:
- `receiverId: 2001` 必须是AI用户的ID
- 需要替换 `YOUR_JWT_TOKEN` 为实际的JWT token

### 步骤 4: 观察日志

查看应用日志，应看到类似输出：

```
[INFO] 发送消息: senderId=1001, receiverId=2001
[INFO] 创建RobotTask: taskId=123, actionType=SEND_MESSAGE
[INFO] 任务加载成功: taskId=123
... 3秒后 ...
[INFO] 执行LLM任务: taskId=123, messageId=456, modelId=1
[INFO] LLM响应成功: taskId=123, tokens=50, time=1200ms
[INFO] SEND_MESSAGE任务完成: taskId=123, aiReplyId=457
```

### 步骤 5: 查询AI回复

```bash
curl -X GET 'http://localhost:8080/api/v1/messages/history?peerId=2001&limit=10' \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

应该看到AI的回复消息。

## 核心实现

### 1. 修改MessageService

**文件**: `src/main/java/com/bqsummer/service/im/MessageService.java`

**关键修改**:

```java
@Service
@Slf4j
@RequiredArgsConstructor
public class MessageService {
    
    private final MessageRepository messageRepository;
    private final ConversationMapper conversationMapper;
    private final UserMapper userMapper;  // 新增：检查AI用户
    private final RobotTaskMapper robotTaskMapper;  // 新增：创建任务
    private final RobotTaskScheduler robotTaskScheduler;  // 新增：加载任务
    private final RobotTaskConfiguration config;  // 新增：获取配置
    
    @Transactional(rollbackFor = Exception.class)  // 确保事务一致性
    public void sendMessage(@Valid SendMessageRequest request) {
        // 原有逻辑：创建消息、更新会话、通知用户...
        Long uid = getCurrentUserId();
        Message msg = createAndSaveMessage(uid, request);
        updateConversations(uid, request.getReceiverId(), msg.getId());
        
        // 新增：如果接收者是AI用户，创建LLM任务
        if (isAiUser(request.getReceiverId())) {
            createRobotTask(msg, request.getReceiverId());
        }
    }
    
    /**
     * 判断是否为AI用户
     */
    private boolean isAiUser(Long userId) {
        User user = userMapper.findById(userId);
        return user != null && "AI".equals(user.getUserType());
    }
    
    /**
     * 创建并加载机器人任务
     */
    private void createRobotTask(Message msg, Long robotId) {
        // 构建SendMessagePayload
        SendMessagePayload payload = SendMessagePayload.builder()
            .messageId(msg.getId())
            .senderId(msg.getSenderId())
            .receiverId(msg.getReceiverId())
            .content(msg.getContent())
            .modelId(config.getDefaultModelId())
            .build();
        
        // 创建任务（延迟3秒执行）
        RobotTask task = new RobotTask();
        task.setUserId(msg.getSenderId());
        task.setRobotId(robotId);
        task.setTaskType("SHORT_DELAY");
        task.setActionType("SEND_MESSAGE");
        task.setActionPayload(JsonUtil.toJson(payload));
        task.setScheduledAt(LocalDateTime.now().plusSeconds(config.getMessageDelaySeconds()));
        task.setStatus("PENDING");
        task.setRetryCount(0);
        task.setMaxRetryCount(3);
        task.setVersion(0);
        
        robotTaskMapper.insert(task);
        log.info("创建RobotTask成功: taskId={}, messageId={}, scheduledAt={}", 
                task.getId(), msg.getId(), task.getScheduledAt());
        
        // 加载到内存队列
        int loaded = robotTaskScheduler.loadTasks(Collections.singletonList(task));
        if (loaded == 0) {
            log.warn("任务加载失败，队列已满: taskId={}", task.getId());
        }
    }
}
```

### 2. 实现RobotTaskExecutor.executeSendMessage

**文件**: `src/main/java/com/bqsummer/service/robot/RobotTaskExecutor.java`

**关键实现**:

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class RobotTaskExecutor {
    
    private final RobotTaskMapper robotTaskMapper;
    private final RobotTaskExecutionLogMapper executionLogMapper;
    private final UnifiedInferenceService inferenceService;  // 新增
    private final MessageRepository messageRepository;  // 新增
    private final ConversationMapper conversationMapper;  // 新增
    
    /**
     * 执行发送消息行为
     * 1. 解析action_payload
     * 2. 调用LLM推理服务
     * 3. 创建AI回复消息
     * 4. 更新会话表
     */
    private void executeSendMessage(RobotTask task) {
        log.info("开始执行SEND_MESSAGE任务: taskId={}, retryCount={}/{}", 
                task.getId(), task.getRetryCount(), task.getMaxRetryCount());
        
        try {
            // 1. 解析action_payload
            SendMessagePayload payload = JsonUtil.fromJson(
                    task.getActionPayload(), SendMessagePayload.class);
            
            if (payload == null) {
                throw new IllegalArgumentException("action_payload解析失败");
            }
            
            log.info("解析payload成功: messageId={}, modelId={}", 
                    payload.getMessageId(), payload.getModelId());
            
            // 2. 调用LLM推理服务
            InferenceRequest request = new InferenceRequest();
            request.setModelId(payload.getModelId());
            request.setPrompt(payload.getContent());
            request.setTemperature(0.7);
            request.setMaxTokens(2000);
            
            log.info("调用LLM推理服务: modelId={}", payload.getModelId());
            
            InferenceResponse response = inferenceService.chat(request);
            
            // 验证LLM响应
            if (response == null || !Boolean.TRUE.equals(response.getSuccess())) {
                String errorMsg = response != null ? response.getErrorMessage() : "LLM响应为空";
                log.warn("LLM推理失败: taskId={}, error={}", task.getId(), errorMsg);
                throw new RuntimeException("LLM推理失败: " + errorMsg);
            }
            
            log.info("LLM推理成功: taskId={}, tokens={}, responseTime={}ms", 
                    task.getId(), response.getTotalTokens(), response.getResponseTimeMs());
            
            // 3. 创建AI回复消息
            Message aiReply = new Message();
            aiReply.setSenderId(payload.getReceiverId()); // AI用户
            aiReply.setReceiverId(payload.getSenderId()); // 原发送者
            aiReply.setType("text");
            aiReply.setContent(response.getContent());
            aiReply.setStatus("sent");
            aiReply.setIsDeleted(0);
            aiReply.setCreatedAt(LocalDateTime.now());
            aiReply.setUpdatedAt(LocalDateTime.now());
            
            messageRepository.save(aiReply);
            
            log.info("AI回复消息创建成功: messageId={}", aiReply.getId());
            
            // 4. 更新会话表
            try {
                conversationMapper.upsertSender(
                        payload.getReceiverId(), payload.getSenderId(), 
                        aiReply.getId(), aiReply.getCreatedAt());
                conversationMapper.upsertReceiver(
                        payload.getSenderId(), payload.getReceiverId(), 
                        aiReply.getId(), aiReply.getCreatedAt());
            } catch (Exception e) {
                log.warn("更新会话表失败: {}", e.getMessage());
            }
            
            log.info("SEND_MESSAGE任务执行完成: taskId={}, aiReplyId={}", 
                    task.getId(), aiReply.getId());
            
            log.info("SEND_MESSAGE任务完成: taskId={}, aiReplyId={}", task.getId(), aiReply.getId());
            
        } catch (Exception e) {
            log.error("SEND_MESSAGE任务执行失败: taskId={}, error={}", task.getId(), e.getMessage(), e);
            throw e;
        }
    }
}
```

### 3. 添加配置类属性

**文件**: `src/main/java/com/bqsummer/configuration/RobotTaskConfiguration.java`

**新增字段**:

```java
@Data
@Configuration
@ConfigurationProperties(prefix = "robot.task")
public class RobotTaskConfiguration {
    // 已有字段...
    
    /**
     * 默认LLM模型ID，默认1
     */
    private Long defaultModelId = 1L;
    
    /**
     * 消息任务延迟秒数，默认3秒
     */
    private Integer messageDelaySeconds = 3;
}
```

## 测试指南

### 单元测试

**文件**: `src/test/java/com/bqsummer/service/im/MessageServiceTest.java`

```java
@SpringBootTest
@Transactional
@DisplayName("消息服务测试 - LLM任务队列")
class MessageServiceTest {
    
    @Autowired
    private MessageService messageService;
    
    @Autowired
    private RobotTaskMapper robotTaskMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    @Test
    @DisplayName("发送消息给AI用户 - 应创建RobotTask")
    void testSendMessageToAiUser_ShouldCreateTask() {
        // Given: 准备AI用户
        Long aiUserId = createAiUser();
        SendMessageRequest request = new SendMessageRequest();
        request.setReceiverId(aiUserId);
        request.setType("text");
        request.setContent("测试消息");
        
        // When: 发送消息
        messageService.sendMessage(request);
        
        // Then: 验证任务创建
        QueryWrapper<RobotTask> wrapper = new QueryWrapper<>();
        wrapper.eq("robot_id", aiUserId)
               .eq("action_type", "SEND_MESSAGE");
        List<RobotTask> tasks = robotTaskMapper.selectList(wrapper);
        
        assertThat(tasks).hasSize(1);
        RobotTask task = tasks.get(0);
        assertThat(task.getStatus()).isEqualTo("PENDING");
        assertThat(task.getActionPayload()).contains("messageId");
    }
    
    @Test
    @DisplayName("发送消息给真实用户 - 不应创建RobotTask")
    void testSendMessageToRealUser_ShouldNotCreateTask() {
        // Given: 准备真实用户
        Long realUserId = createRealUser();
        SendMessageRequest request = new SendMessageRequest();
        request.setReceiverId(realUserId);
        request.setType("text");
        request.setContent("测试消息");
        
        // When: 发送消息
        messageService.sendMessage(request);
        
        // Then: 验证无任务创建
        QueryWrapper<RobotTask> wrapper = new QueryWrapper<>();
        wrapper.eq("robot_id", realUserId);
        List<RobotTask> tasks = robotTaskMapper.selectList(wrapper);
        
        assertThat(tasks).isEmpty();
    }
    
    private Long createAiUser() {
        User user = new User();
        user.setUserName("ai_test");
        user.setUserType("AI");
        userMapper.insert(user);
        return user.getId();
    }
    
    private Long createRealUser() {
        User user = new User();
        user.setUserName("real_test");
        user.setUserType("REAL");
        userMapper.insert(user);
        return user.getId();
    }
}
```

### 集成测试

**文件**: `src/test/java/com/bqsummer/integration/MessageLlmQueueIntegrationTest.java`

```java
@SpringBootTest
@DisplayName("消息LLM队列集成测试")
class MessageLlmQueueIntegrationTest extends BaseTest {
    
    @Autowired
    private MessageService messageService;
    
    @Autowired
    private RobotTaskScheduler robotTaskScheduler;
    
    @Autowired
    private MessageRepository messageRepository;
    
    @Test
    @DisplayName("端到端测试 - 用户发送消息，AI回复")
    void testEndToEnd_UserSendsMessage_AiReplies() throws InterruptedException {
        // Given: 准备测试数据
        Long userId = 1001L;
        Long aiUserId = 2001L;
        
        // When: 用户发送消息
        SendMessageRequest request = new SendMessageRequest();
        request.setReceiverId(aiUserId);
        request.setType("text");
        request.setContent("你好");
        
        messageService.sendMessage(request);
        
        // Wait for task execution (3秒延迟 + 执行时间)
        Thread.sleep(5000);
        
        // Then: 验证AI回复
        List<Message> messages = messageRepository.findDialogHistory(userId, aiUserId, null, 10);
        
        assertThat(messages).hasSizeGreaterThanOrEqualTo(2);
        Message aiReply = messages.stream()
            .filter(m -> m.getSenderId().equals(aiUserId))
            .findFirst()
            .orElseThrow();
        
        assertThat(aiReply.getContent()).isNotEmpty();
        assertThat(aiReply.getType()).isEqualTo("text");
    }
}
```

## 常见问题

### Q1: 如何判断receiverId是否为AI用户？

A: 查询user表的user_type字段：
```java
User user = userMapper.selectById(receiverId);
boolean isAi = "AI".equals(user.getUserType());
```

### Q2: 如果队列满了怎么办？

A: `robotTaskScheduler.loadTask()`会返回false，任务仍然保存在数据库中，等待RobotTaskLoaderJob定期加载。

### Q3: LLM调用失败会怎样？

A: 任务会被标记为FAILED，并根据retry_count自动重试，最多重试3次（可配置）。

### Q4: 如何配置使用不同的LLM模型？

A: 修改application.properties中的`robot.task.default-model-id`，或者在AI角色表中配置per-character的模型ID。

### Q5: 消息延迟多久执行？

A: 默认3秒，可通过`robot.task.message-delay-seconds`配置。

## 调试技巧

### 1. 启用DEBUG日志

```properties
logging.level.com.bqsummer.service.im=DEBUG
logging.level.com.bqsummer.service.robot=DEBUG
```

### 2. 查看任务状态

```sql
-- 查看最近的任务
SELECT id, user_id, robot_id, action_type, status, scheduled_at, created_time
FROM robot_task
WHERE action_type = 'SEND_MESSAGE'
ORDER BY created_time DESC
LIMIT 10;
```

### 3. 查看执行日志

```sql
-- 查看任务执行详情
SELECT t.id as task_id, t.status, l.execution_attempt, l.status as exec_status, 
       l.execution_duration_ms, l.error_message
FROM robot_task t
LEFT JOIN robot_task_execution_log l ON t.id = l.task_id
WHERE t.action_type = 'SEND_MESSAGE'
ORDER BY t.created_time DESC
LIMIT 10;
```

### 4. 监控队列状态

在管理接口（如果实现）查询：
- 当前队列大小
- SEND_MESSAGE并发槽位使用情况
- 延迟重试次数

## 下一步

完成本功能后，可以考虑：
1. 添加AI角色与模型的关联配置
2. 实现WebSocket实时推送AI回复
3. 优化LLM prompt engineering
4. 添加对话历史上下文
5. 实现流式输出（SSE）

## 参考资料

- [功能规范](../spec.md)
- [数据模型](../data-model.md)
- [API契约](../contracts/message-task-api.md)
- [调研文档](../research.md)
