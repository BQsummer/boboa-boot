# Quick Start: 对话记忆系统

**Feature**: 对话记忆系统  
**Date**: 2026-01-23  
**Target Audience**: 开发者

本文档提供对话记忆系统的快速入门指南，包括核心概念、使用流程和代码示例。

---

## 📚 核心概念

### 三层记忆架构

```
┌─────────────────────────────────────────────┐
│  L1: 原始消息 (ConversationMessage)         │
│  - 保存所有对话内容                         │
│  - 完整的对话历史                           │
│  - 按时间顺序存储                           │
└──────────────┬──────────────────────────────┘
               │ 触发阈值: 30条消息
               ↓
┌─────────────────────────────────────────────┐
│  L2: 滚动总结 (ConversationSummary)        │
│  - 压缩对话历史                             │
│  - 结构化总结（主题、要点、情绪）           │
│  - 减少上下文长度                           │
└──────────────┬──────────────────────────────┘
               │ LLM判断: 是否包含重要信息
               ↓
┌─────────────────────────────────────────────┐
│  L3: 长期记忆 (LongTermMemory)             │
│  - 提取关键信息片段                         │
│  - 向量化（embedding）                      │
│  - 支持语义检索                             │
└─────────────────────────────────────────────┘
```

### 记忆类型

| 类型 | 说明 | 示例 |
|------|------|------|
| **event** | 生活事件 | "用户上周刚换了工作" |
| **preference** | 偏好/禁忌 | "用户不喜欢被说教" |
| **relationship** | 人际关系 | "用户的父亲是一名医生" |
| **emotion** | 情绪状态 | "用户最近因工作压力感到焦虑" |
| **fact** | 客观事实 | "用户居住在北京" |

---

## 🚀 快速开始

### 前置条件

1. **数据库准备**：
   ```bash
   # 确保 PostgreSQL 已安装 pgvector-rs 扩展
   psql -U postgres -d snorlax -c "CREATE EXTENSION IF NOT EXISTS vectors;"
   ```

2. **执行数据库初始化**：
   - 所有建表语句已在 `src/main/resources/datasourceInit.sql` 中
   - 应用启动时自动执行

3. **配置 Embedding 模型**：
   ```sql
   -- 在 ai_model 表中添加 embedding 模型
   INSERT INTO ai_model (name, version, provider, model_type, endpoint, api_key, status)
   VALUES ('text-embedding-3-small', 'text-embedding-3-small', 'openai', 'EMBEDDING',
           'https://api.openai.com/v1', 'sk-your-api-key', 'ACTIVE');
   ```

---

## 💡 使用场景

### 场景1: 在现有消息接口中集成记忆系统

**修改现有的 `MessageService.sendMessage()` 方法**：

```java
@Service
@RequiredArgsConstructor
public class MessageService {
    private final ConversationMessageService conversationMessageService;
    private final MemoryRetrievalService memoryRetrievalService;
    private final ModelRoutingService modelRoutingService;
    private final MessageRepository messageRepository;
    
    public void sendMessage(SendMessageRequest request) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
        Long peerId = request.getPeerId();  // peerId 即为 ai_character_id
        
        // 1. 保存用户消息到 IM 系统（原有逻辑）
        Message message = saveToMessageTable(userId, peerId, request.getContent());
        
        // 2. 保存用户消息到对话记忆系统（新增）
        conversationMessageService.saveMessage(userId, peerId, "USER", request.getContent());
        
        // 3. 检索相关记忆并构建上下文（新增）
        String contextualPrompt = memoryRetrievalService.buildContextPrompt(
            userId, peerId, request.getContent()
        );
        
        // 4. 调用 LLM 生成回复（使用增强的上下文）
        InferenceResponse aiResponse = modelRoutingService.callModel(
            InferenceRequest.builder()
                .prompt(contextualPrompt)
                .temperature(0.7)
                .build()
        );
        
        // 5. 保存 AI 回复到 IM 系统（原有逻辑）
        saveToMessageTable(peerId, userId, aiResponse.getContent());
        
        // 6. 保存 AI 回复到对话记忆系统（新增）
        conversationMessageService.saveMessage(userId, peerId, "AI", aiResponse.getContent());
        
        // 7. 发送消息到客户端（原有逻辑 - WebSocket/SSE）
        sendToClient(userId, aiResponse);
    }
}
```

**记忆系统自动行为**（对用户透明）：
- ✅ 每条消息自动保存到 `conversation_message` 表（按 user_id + ai_character_id 隔离）
- ✅ 检查是否达到总结阈值（30条未总结消息），自动异步生成滚动总结
- ✅ 如果是用户消息，异步判断是否包含重要信息，提取为长期记忆
- ✅ 生成回复前自动检索相关记忆，增强 AI 的上下文理解

**用户体验**：
- 用户继续使用 `/api/v1/messages` 接口，无需改变任何调用方式
- AI 自动"记住"重要信息（工作、偏好、生活事件等），对话更连贯、更个性化
- 跨会话的信息延续（如几天后继续讨论之前的话题）
- 与不同 AI 角色对话时，各自的记忆空间独立（如职业顾问记住工作相关，心理咨询师记住情绪相关）

---

### 场景2: 生成 AI 回复时检索记忆

**在调用 LLM 之前**：

```java
@Service
@RequiredArgsConstructor
public class ChatService {
    private final MemoryRetrievalService memoryRetrievalService;
    private final ModelRoutingService modelRoutingService;
    
    public InferenceResponse generateResponse(Long userId, Long aiCharacterId, String userInput) {
        // 1. 检索相关的长期记忆（自动）
        List<MemorySearchResult> memories = longTermMemoryService.searchMemories(
            userId, aiCharacterId, userInput, 5  // topK=5
        );
        
        // 2. 获取最近的对话总结（自动）
        List<ConversationSummary> recentSummaries = conversationSummaryService
            .getRecentSummaries(userId, aiCharacterId, 2);  // 最近2个总结
        
        // 3. 获取最近的原始消息（未被总结覆盖的部分）（自动）
        List<ConversationMessage> recentMessages = conversationMessageService
            .getUnsummarizedMessages(userId, aiCharacterId, 10);  // 最近10条
        
        // 4. 构建上下文 Prompt
        String contextPrompt = buildContextPrompt(memories, recentSummaries, recentMessages, userInput);
        
        // 5. 调用 LLM
        InferenceRequest inferenceReq = InferenceRequest.builder()
            .prompt(contextPrompt)
            .temperature(0.7)
            .build();
        return modelRoutingService.callModel(inferenceReq);
    }
    
    private String buildContextPrompt(List<MemorySearchResult> memories,
                                       List<ConversationSummary> summaries,
                                       List<ConversationMessage> messages,
                                       String userInput) {
        StringBuilder sb = new StringBuilder();
        
        // 添加长期记忆
        if (!memories.isEmpty()) {
            sb.append("## 关于用户的长期记忆\n");
            for (MemorySearchResult mem : memories) {
                sb.append("- ").append(mem.getText()).append("\n");
            }
            sb.append("\n");
        }
        
        // 添加对话总结
        if (!summaries.isEmpty()) {
            sb.append("## 之前的对话总结\n");
            for (ConversationSummary sum : summaries) {
                sb.append(sum.getSummaryJson().getContextCarryOver()).append("\n");
            }
            sb.append("\n");
        }
        
        // 添加最近消息
        sb.append("## 最近的对话\n");
        for (ConversationMessage msg : messages) {
            sb.append(msg.getSenderType()).append(": ").append(msg.getContent()).append("\n");
        }
        
        // 添加当前输入
        sb.append("\nUSER: ").append(userInput).append("\n");
        sb.append("\nAI: ");
        
        return sb.toString();
    }
}
```

**上下文示例**（发送给 LLM 的 Prompt）：

```
## 关于用户的长期记忆
- 用户在一家互联网公司担任技术架构师
- 用户偏好直接的技术建议，不喜欢空泛的理论
- 用户最近因工作压力感到疲惫

## 之前的对话总结
用户正在应对高强度工作压力，提到项目deadline临近。AI建议注意休息和运动。

## 最近的对话
USER: 项目终于上线了，但我感觉身体快撑不住了
AI: 恭喜项目成功上线！不过我注意到你的身体状况...
USER: 现在终于可以好好休息了

AI: [这里是模型生成的回复]
```

---

### 场景3: 记忆自动检索（无需新增API）

**记忆检索在聊天接口中自动完成**，示例对话：

```
[第1天对话]
USER: 我在一家AI公司做技术架构师，最近压力很大
AI: 理解你的压力...
[系统自动提取记忆："用户在AI公司担任技术架构师" - 类型:fact, 重要性:0.8]

[3天后对话]
USER: 今天工作遇到了技术难题
AI: 作为架构师，你可能需要...
    (AI自动检索到之前的记忆，知道用户的职业背景)
```

**记忆检索逻辑**（在 `ChatService.generateResponse()` 中）：

```java
public InferenceResponse generateResponse(Long userId, Long aiCharacterId, String userInput) {
    // 1. 语义检索相关记忆（自动）
    List<MemorySearchResult> memories = longTermMemoryService.searchMemories(
        userId, aiCharacterId, userInput, 5  // topK=5
    );
    
    // 2. 获取对话历史上下文（自动）
    String conversationContext = conversationMessageService.getRecentContext(
        userId, aiCharacterId, 10  // 最近10条消息
    );
    
    // 3. 构建增强 Prompt（自动注入记忆）
    String enhancedPrompt = buildPromptWithMemory(memories, conversationContext, userInput);
    
    // 4. 调用 LLM
    return modelRoutingService.callModel(enhancedPrompt);
}

---

## 🔧 核心服务类

### ConversationMessageService

**职责**: 管理对话消息的保存和查询

```java
@Service
@RequiredArgsConstructor
public class ConversationMessageService {
    private final ConversationMessageMapper messageMapper;
    private final ConversationSummaryService summaryService;
    private final LongTermMemoryService memoryService;
    
    /**
     * 保存消息（核心方法）- 在现有聊天接口中调用
     */
    @Transactional
    public Long saveMessage(Long userId, Long aiCharacterId, String senderType, String content) {
        // 1. 保存消息到数据库
        ConversationMessage message = ConversationMessage.builder()
            .userId(userId)
            .aiCharacterId(aiCharacterId)
            .senderType(senderType)
            .content(content)
            .createdAt(LocalDateTime.now())
            .build();
        messageMapper.insert(message);
        
        // 2. 检查是否需要生成总结（异步，不阻塞响应）
        int unsummarizedCount = getUnsummarizedMessageCount(userId, aiCharacterId);
        if (unsummarizedCount >= 30) {
            summaryService.generateSummaryAsync(userId, aiCharacterId);
        }
        
        // 3. 如果是用户消息，异步判断是否需要提取记忆
        if ("USER".equals(senderType)) {
            memoryService.extractMemoryAsync(userId, aiCharacterId, content);
        }
        
        return message.getId();
    }
    
    /**
     * 查询未被总结覆盖的消息
     */
    public List<ConversationMessage> getUnsummarizedMessages(Long userId, Long aiCharacterId, int limit) {
        // 获取最后一次总结
        ConversationSummary lastSummary = summaryService.getLatestSummary(userId, aiCharacterId);
        Long afterMessageId = (lastSummary != null) ? lastSummary.getCoveredUntilMessageId() : 0L;
        
        // 查询该ID之后的消息
        return messageMapper.selectList(new QueryWrapper<ConversationMessage>()
            .eq("user_id", userId)
            .eq("ai_character_id", aiCharacterId)
            .gt("id", afterMessageId)
            .orderByAsc("id")
            .last("LIMIT " + limit));
    }
}
```

---

### LongTermMemoryService

**职责**: 管理长期记忆的提取、存储和检索

```java
@Service
@RequiredArgsConstructor
public class LongTermMemoryService {
    private final LongTermMemoryMapper memoryMapper;
    private final PromptTemplateService promptTemplateService;
    private final ModelRoutingService modelRoutingService;
    private final EmbeddingUtil embeddingUtil;
    
    /**
     * 异步提取记忆（在后台自动运行）
     */
    @Async
    public void extractMemoryAsync(Long userId, Long aiCharacterId, String userMessage) {
        try {
            // 1. 判断是否需要提取
            boolean shouldExtract = judgeNeedExtraction(userMessage);
            if (!shouldExtract) return;
            
            // 2. 提取记忆内容
            List<MemoryItem> items = extractMemoryItems(userMessage);
            
            // 3. 生成 embedding 并保存
            for (MemoryItem item : items) {
                float[] embedding = embeddingUtil.generateEmbedding(item.getText());
                
                LongTermMemory memory = LongTermMemory.builder()
                    .userId(userId)
                    .aiCharacterId(aiCharacterId)  // 按 AI 角色隔离记忆
                    .text(item.getText())
                    .embedding(embedding)
                    .memoryType(item.getType())
                    .importance(item.getImportance())
                    .createdAt(LocalDateTime.now())
                    .build();
                
                memoryMapper.insert(memory);
            }
            
            log.info("Extracted {} memories for user {} with AI character {}", items.size(), userId, aiCharacterId);
        } catch (Exception e) {
            log.error("Failed to extract memory for user {}", userId, e);
        }
    }
    
    /**
     * 语义检索记忆（自动调用，对用户透明）
     */
    public List<MemorySearchResult> searchMemories(Long userId, Long aiCharacterId, String queryText, int topK) {
        // 1. 生成查询向量
        float[] queryEmbedding = embeddingUtil.generateEmbedding(queryText);
        
        // 2. 向量检索 (调用自定义 SQL，限定 user_id + ai_character_id)
        List<LongTermMemory> candidates = memoryMapper.searchByEmbedding(
            userId, aiCharacterId, queryEmbedding, topK * 2  // 初筛取2倍
        );
        
        // 3. 重排序与过滤
        return candidates.stream()
            .map(mem -> calculateFinalScore(mem, queryEmbedding))
            .filter(result -> result.getFinalScore() > 0.5)  // 评分阈值
            .sorted(Comparator.comparing(MemorySearchResult::getFinalScore).reversed())
            .limit(topK)
            .peek(result -> updateAccessTime(result.getMemoryId()))  // 异步更新访问时间
            .collect(Collectors.toList());
    }
    
    private MemorySearchResult calculateFinalScore(LongTermMemory mem, float[] queryEmbedding) {
        // 计算相似度
        float similarity = cosineSimilarity(mem.getEmbedding(), queryEmbedding);
        
        // 综合评分：相似度60% + 重要性30% + 时效性10%
        float timeDecay = calculateTimeDecay(mem.getLastAccessedAt());
        float finalScore = similarity * 0.6f + mem.getImportance() * 0.3f + timeDecay * 0.1f;
        
        return MemorySearchResult.builder()
            .memoryId(mem.getId())
            .text(mem.getText())
            .memoryType(mem.getMemoryType())
            .importance(mem.getImportance())
            .similarity(similarity)
            .finalScore(finalScore)
            .build();
    }
}
```

---

## 📊 性能考虑

### Embedding 生成优化

```java
@Component
@RequiredArgsConstructor
public class EmbeddingUtil {
    private final AiModelService aiModelService;
    private final RedisTemplate<String, float[]> redisTemplate;
    
    /**
     * 生成 embedding（带缓存）
     */
    public float[] generateEmbedding(String text) {
        // 1. 尝试从缓存获取
        String cacheKey = "embedding:" + DigestUtils.md5Hex(text);
        float[] cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        // 2. 调用 API 生成
        AiModel embeddingModel = aiModelService.getDefaultModelByType(ModelType.EMBEDDING);
        float[] embedding = callEmbeddingAPI(embeddingModel, text);
        
        // 3. 写入缓存（TTL 1小时）
        redisTemplate.opsForValue().set(cacheKey, embedding, 1, TimeUnit.HOURS);
        
        return embedding;
    }
    
    /**
     * 批量生成 embedding
     */
    public List<float[]> generateEmbeddingBatch(List<String> texts) {
        // OpenAI embedding API 支持批量（最多2048条）
        return callEmbeddingAPIBatch(texts);
    }
}
```

### 向量检索优化

```sql
-- 定期维护向量索引
REINDEX INDEX idx_ltm_embedding;

-- 定期清理过时数据（可选）
DELETE FROM long_term_memory
WHERE importance < 0.3
  AND last_accessed_at < NOW() - INTERVAL '3 months';
```

---

## 🧪 测试建议

### 单元测试

```java
@SpringBootTest
@Transactional
@DisplayName("长期记忆服务测试")
class LongTermMemoryServiceTest {
    
    @Test
    @DisplayName("测试记忆提取流程")
    void testMemoryExtraction() {
        // Given: 准备测试数据
        Long userId = 1001L;
        Long aiCharacterId = 1L;  // 职业顾问AI
        String userMessage = "我最近换了工作，在一家AI公司做架构师";
        
        // When: 提取记忆
        memoryService.extractMemoryAsync(userId, aiCharacterId, userMessage);
        
        // Then: 验证记忆已保存且按 AI 角色隔离
        List<LongTermMemory> memories = memoryMapper.selectList(
            new QueryWrapper<LongTermMemory>()
                .eq("user_id", userId)
                .eq("ai_character_id", aiCharacterId)
        );
        assertThat(memories).isNotEmpty();
        assertThat(memories.get(0).getMemoryType()).isEqualTo("fact");
    }
    
    @Test
    @DisplayName("测试语义检索")
    void testSemanticSearch() {
        // Given: 准备测试记忆
        Long aiCharacterId = 1L;
        insertTestMemory(userId, aiCharacterId, "用户在AI公司工作", "fact", 0.8f);
        insertTestMemory(userId, aiCharacterId, "用户喜欢打篮球", "preference", 0.6f);
        
        // When: 语义检索
        List<MemorySearchResult> results = memoryService.searchMemories(
            userId, aiCharacterId, "用户的职业信息", 3
        );
        
        // Then: 验证相关性排序
        assertThat(results).isNotEmpty();
        assertThat(results.get(0).getText()).contains("AI公司");
        assertThat(results.get(0).getSimilarity()).isGreaterThan(0.7f);
    }
}
```

### 集成测试

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@DisplayName("对话记忆系统集成测试")
class ConversationMemoryIntegrationTest {
    
    @Test消息接口自动保存记忆")
    void testMessageWithMemory() {
        // 第一次对话：告诉AI自己的工作
        given()
            .header("Authorization", "Bearer " + validToken)
            .contentType(ContentType.JSON)
            .body("""
                {
                  "peerId": 1001,
                  "content": "我在一家AI公司做技术架构师"
                }
                """)
        .when()
            .post("/api/v1/messages")
        .then()
            .statusCode(200);
        
        // 等待异步记忆提取完成
        Thread.sleep(2000);
        
        // 验证记忆已保存
        List<LongTermMemory> memories = memoryMapper.selectList(
            new QueryWrapper<LongTermMemory>()
                .eq("user_id", testUserId)
                .eq("ai_character_id", 1001L)
        );
        assertThat(memories).isNotEmpty();
        assertThat(memories.get(0).getText()).contains("架构师");
        
        // 第二次对话：AI 应该记住用户的职业
        given()
            .header("Authorization", "Bearer " + validToken)
            .contentType(ContentType.JSON)
            .body("""
                {
                  "peerId": 1001,
                  "content": "工作中遇到了技术难题"
                }
                """)
        .when()
            .post("/api/v1/messages")
        .then()
            .statusCode(200);
        
        // 验证 AI 回复包含上下文（通过消息历史查询）
        List<Message> history = messageRepository.findDialogHistory(testUserId, 1001L, null, 2);
        assertThat(history.get(0).getContent()).containsIgnoringCase("架构师");
            .body("data.content", containsString("架构师"));  // AI 回复应包含上下文
    }
}
```

---

## 📖 相关文档

- [数据模型设计](./data-model.md) - 详细的表结构和实体关系
- [API 契约](./contracts/memory-api.yaml) - OpenAPI 规范
- [技术调研](./research.md) - 技术选型和决策依据
- [实施计划](./plan.md) - 完整的实施规划

---

## 🆘 常见问题

### Q1: 如何调整总结触发阈值？

A: 修改 `ConversationMessageService` 中的阈值常量：
```java
private static final int SUMMARY_THRESHOLD = 30;  // 修改此值
```

### Q2: 如何切换 embedding 模型？

A: 在数据库中更新 `ai_model` 表，将新模型的 `status` 设为 `ACTIVE`，旧模型设为 `INACTIVE`。

### Q3: 向量检索性能慢怎么办？

A: 
1. 检查向量索引是否已创建：`\d+ long_term_memory` 
2. 切换到 HNSW 索引（精度更高但构建慢）
3. 增加服务器内存配置
4. 考虑分区表（大规模场景）

### Q4: 记忆越来越多，会影响性能吗？

A: 向量检索的时间复杂度为 O(log N)，百万级数据仍可在<100ms完成。建议定期清理低重要性且长期未访问的记忆。

---

**下一步**: 参考 [实施计划](./plan.md) 开始编写代码实现。
