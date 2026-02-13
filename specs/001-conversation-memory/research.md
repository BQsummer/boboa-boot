# Research: 对话记忆系统技术调研

**Feature**: 对话记忆系统  
**Date**: 2026-01-23  
**Status**: Phase 0 Complete

## 调研目标

根据 Technical Context 中的未知项和技术选型，调研以下关键技术决策：

1. **pgvector-rs 集成方式**：如何在 PostgreSQL + MyBatis Plus 架构中使用向量扩展
2. **Embedding 模型选择**：文本向量化方案及其集成方式
3. **向量检索策略**：topK选择、距离度量、索引优化
4. **LLM 调用模式**：总结生成和记忆提取的 Prompt 设计

---

## R1: pgvector-rs 集成方式

### 调研问题
- 如何在现有 PostgreSQL + MyBatis Plus 架构中启用 pgvector-rs 扩展？
- 向量字段的 DDL 定义和 CRUD 操作如何实现？
- MyBatis 是否需要特殊的 TypeHandler 处理向量类型？

### 决策：通过 SQL 扩展函数实现向量操作

**选择理由**：
- pgvector-rs 是 PostgreSQL 扩展，通过 `CREATE EXTENSION` 启用后即可使用
- 向量字段定义为 `vector(dimension)` 类型，如 `vector(1536)` 表示1536维向量
- MyBatis Plus 可以直接处理向量类型：
  - 存储：将 `float[]` 转换为 PostgreSQL 的 `vector` 格式（通过 JDBC 原生支持）
  - 查询：使用自定义 SQL 调用向量距离函数（`<->` 或 `<=>` 或 `<#>`）
- 无需额外 Java 依赖，PostgreSQL JDBC Driver 已内置向量类型支持

**实施要点**：
1. 在 `datasourceInit.sql` 中添加扩展启用语句：
   ```sql
   CREATE EXTENSION IF NOT EXISTS vectors;  -- pgvector-rs 的扩展名称
   ```

2. 表定义示例：
   ```sql
   CREATE TABLE long_term_memory (
       id BIGSERIAL PRIMARY KEY,
       user_id BIGINT NOT NULL,
       text TEXT NOT NULL,
       embedding vector(1536),  -- 假设使用 OpenAI text-embedding-3-small (1536维)
       memory_type VARCHAR(32),
       importance FLOAT,
       last_accessed_at TIMESTAMP,
       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
   );
   
   -- 创建向量索引（IVFFlat 或 HNSW）
   CREATE INDEX idx_memory_embedding ON long_term_memory 
   USING vectors (embedding vector_cosine_ops);  -- 余弦相似度索引
   ```

3. MyBatis Mapper 自定义向量查询：
   ```xml
   <!-- LongTermMemoryMapper.xml -->
   <select id="searchByEmbedding" resultType="com.bqsummer.common.dto.memory.LongTermMemory">
       SELECT id, user_id, text, memory_type, importance, last_accessed_at, created_at,
              embedding <=> #{queryEmbedding}::vector AS distance
       FROM long_term_memory
       WHERE user_id = #{userId}
       ORDER BY embedding <=> #{queryEmbedding}::vector
       LIMIT #{topK}
   </select>
   ```

4. Java 实体类字段定义：
   ```java
   @Data
   @TableName("long_term_memory")
   public class LongTermMemory {
       @TableId(type = IdType.AUTO)
       private Long id;
       private Long userId;
       private String text;
       
       @TableField(typeHandler = FloatArrayTypeHandler.class)
       private float[] embedding;  // 向量存储为 float[]
       
       private String memoryType;
       private Float importance;
       private LocalDateTime lastAccessedAt;
       private LocalDateTime createdAt;
   }
   ```

5. 自定义 TypeHandler（如果 MyBatis Plus 不自动处理）：
   ```java
   public class FloatArrayTypeHandler extends BaseTypeHandler<float[]> {
       @Override
       public void setNonNullParameter(PreparedStatement ps, int i, float[] parameter, JdbcType jdbcType) throws SQLException {
           PGobject pgObject = new PGobject();
           pgObject.setType("vector");
           pgObject.setValue(Arrays.toString(parameter));  // 转换为 "[0.1, 0.2, ...]" 格式
           ps.setObject(i, pgObject);
       }
       
       @Override
       public float[] getNullableResult(ResultSet rs, String columnName) throws SQLException {
           PGobject pgObject = (PGobject) rs.getObject(columnName);
           if (pgObject == null) return null;
           return parseVector(pgObject.getValue());
       }
       
       private float[] parseVector(String value) {
           // 解析 "[0.1, 0.2, ...]" 格式字符串为 float[]
           ...
       }
   }
   ```

**备选方案及拒绝理由**：
- ❌ 使用独立向量数据库（Milvus/Weaviate）：增加系统复杂度，需额外维护，不符合"最小化修改"原则
- ❌ 使用云向量服务（Pinecone）：引入外部依赖，增加成本，数据隐私风险
- ✅ 选择 pgvector-rs：与现有 PostgreSQL 基础设施集成，零额外运维成本，数据一致性有保障

**参考资料**：
- pgvector-rs 官方文档：https://github.com/tensorchord/pgvecto.rs
- PostgreSQL vector type operators: `<->` (L2距离), `<=>` (余弦距离), `<#>` (内积距离)

---

## R2: Embedding 模型选择

### 调研问题
- 应该使用哪个 Embedding 模型？
- 如何集成 Embedding 模型到现有架构？
- Embedding 生成应该同步还是异步？

### 决策：使用 OpenAI text-embedding-3-small，通过现有 ModelAdapter 架构集成

**选择理由**：
- 项目已有 OpenAI 集成（`OpenAiAdapter`），可复用现有基础设施
- text-embedding-3-small 性价比高：
  - 维度：1536（可调整至更小，如 512/256）
  - 性能：优于 ada-002，速度快
  - 成本：$0.02 / 1M tokens
- 备选方案：项目已支持 Qwen 模型，可用 `text-embedding-v3` 作为国内备选

**实施方案**：

1. **扩展 ModelType 枚举**（已存在 EMBEDDING 类型）：
   ```java
   // 已有定义，无需修改
   public enum ModelType {
       CHAT,
       EMBEDDING,  // ✅ 已支持
       RERANKER
   }
   ```

2. **在 ai_model 表中添加 embedding 模型配置**：
   ```sql
   INSERT INTO ai_model (name, version, provider, model_type, endpoint, api_key, status)
   VALUES 
   ('text-embedding-3-small', 'text-embedding-3-small', 'openai', 'EMBEDDING', 
    'https://api.openai.com/v1', 'sk-...', 'ACTIVE'),
   ('text-embedding-v3', 'text-embedding-v3', 'qwen', 'EMBEDDING',
    'https://dashscope.aliyuncs.com/api/v1', 'sk-...', 'ACTIVE');
   ```

3. **创建 EmbeddingUtil 工具类**：
   ```java
   @Component
   @RequiredArgsConstructor
   public class EmbeddingUtil {
       private final AiModelService aiModelService;
       private final ModelRoutingService modelRoutingService;
       
       /**
        * 生成文本的向量表示
        * @param text 待向量化的文本
        * @return 向量数组 (float[])
        */
       public float[] generateEmbedding(String text) {
           // 1. 获取默认 embedding 模型
           AiModel embeddingModel = aiModelService.getDefaultModelByType(ModelType.EMBEDDING);
           
           // 2. 调用 adapter 生成 embedding
           // 注意：需要扩展 ModelAdapter 接口增加 embed() 方法
           ModelAdapter adapter = getAdapterForModel(embeddingModel);
           EmbeddingResponse response = adapter.embed(embeddingModel, text);
           
           return response.getEmbedding();
       }
   }
   ```

4. **扩展 ModelAdapter 接口**：
   ```java
   public interface ModelAdapter {
       boolean supports(AiModel model);
       InferenceResponse chat(AiModel model, InferenceRequest request);
       
       // 新增：Embedding 生成方法
       EmbeddingResponse embed(AiModel model, String text);
       
       String getName();
   }
   ```

5. **在 OpenAiAdapter 中实现 embed 方法**：
   ```java
   @Override
   public EmbeddingResponse embed(AiModel model, String text) {
       // 调用 Spring AI 的 EmbeddingModel
       OpenAiEmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder()
           .openAiApi(openAiApi)
           .build();
       
       EmbeddingRequest request = new EmbeddingRequest(List.of(text), null);
       EmbeddingResponse response = embeddingModel.call(request);
       
       return new EmbeddingResponse(response.getResult().getOutput());
   }
   ```

**同步 vs 异步选择**：
- **记忆写入（memory write）**：✅ **异步**
  - 用户不需要等待 embedding 生成完成
  - 通过消息队列或异步任务处理
  - 失败重试机制
- **记忆检索（memory retrieve）**：✅ **同步**
  - 需要立即返回相关记忆用于生成回复
  - query embedding 生成通常<100ms，可接受
  - 可增加缓存优化（相似查询复用 embedding）

**备选方案及拒绝理由**：
- ❌ 使用开源本地模型（sentence-transformers）：需部署推理服务，增加运维复杂度
- ❌ 使用阿里云/腾讯云 embedding 服务：API 兼容性差，需单独适配
- ✅ 选择 OpenAI/Qwen embedding：与现有架构无缝集成，API 成熟稳定

**成本估算**：
- 假设每条记忆平均 50 tokens
- 10k 用户，每用户 500 条记忆 = 5M 条记忆
- Embedding 成本：(5M * 50) / 1M * $0.02 = $5（一次性）
- 月度新增（10k 用户 * 1000条/月 * 10%提取率 * 50 tokens）= $1/月

---

## R3: 向量检索策略

### 调研问题
- topK 应该设置为多少？
- 使用什么距离度量（L2/cosine/dot product）？
- 如何优化大规模向量检索性能？
- 如何平衡检索结果的相关性和多样性？

### 决策：使用余弦相似度 + 动态 topK + 重排序过滤

**检索流程设计**：

```
用户输入 
  ↓
1. 生成 query embedding (同步)
  ↓
2. 向量检索 topK=10 (初筛)
  ↓
3. 规则过滤与重排序：
   - 过滤：距离阈值 > 0.7
   - 过滤：过旧且长时间未访问的记忆
   - 加权：importance 高的记忆 +分
   - 降权：最近已使用的记忆 -分
  ↓
4. 返回最终 top3-8 条记忆
  ↓
5. 更新 last_accessed_at (异步)
```

**关键参数选择**：

| 参数 | 值 | 理由 |
|------|---|------|
| **距离度量** | 余弦相似度 (`<=>`) | 对文本 embedding 效果最好，不受向量长度影响 |
| **初筛 topK** | 10 | 提供足够候选，同时保持查询效率 |
| **最终返回数** | 3-8 (动态) | 根据相关性动态调整，避免无关记忆污染 context |
| **相似度阈值** | 0.7 | 低于此值认为不相关，直接过滤 |
| **时间衰减** | 30天未访问 -0.1分 | 防止过时信息干扰 |
| **重复抑制** | 最近3轮用过 -0.2分 | 避免重复调用同一记忆 |

**SQL查询示例**：
```sql
-- 初筛：向量检索 top 10
WITH vector_matches AS (
    SELECT 
        id, user_id, text, memory_type, importance, last_accessed_at, created_at,
        1 - (embedding <=> #{queryEmbedding}::vector) AS similarity
    FROM long_term_memory
    WHERE user_id = #{userId}
      AND (embedding <=> #{queryEmbedding}::vector) < 0.3  -- 余弦距离 < 0.3 (相似度 > 0.7)
    ORDER BY embedding <=> #{queryEmbedding}::vector
    LIMIT 10
),
-- 重排序：综合评分
reranked AS (
    SELECT *,
           similarity * 0.6  -- 相似度权重 60%
           + importance * 0.3  -- 重要性权重 30%
           + CASE 
               WHEN last_accessed_at > NOW() - INTERVAL '3 days' THEN -0.1  -- 最近用过，降权
               WHEN last_accessed_at < NOW() - INTERVAL '30 days' THEN -0.1  -- 太久未用，降权
               ELSE 0.1
             END AS final_score
    FROM vector_matches
)
SELECT id, user_id, text, memory_type, importance, last_accessed_at, created_at, similarity, final_score
FROM reranked
WHERE final_score > 0.5  -- 最终评分阈值
ORDER BY final_score DESC
LIMIT #{finalTopK};  -- 动态 top 3-8
```

**索引优化**：
```sql
-- pgvector-rs 支持两种索引算法
-- 1. IVFFlat：速度快，召回率 ~95%，适合中等规模 (<100万向量)
CREATE INDEX idx_memory_embedding_ivf ON long_term_memory 
USING vectors (embedding vector_cosine_ops)
WITH (options = $$
    [indexing.ivf]
    nlist = 100  -- 聚类中心数量，约为 sqrt(N)
$$);

-- 2. HNSW：高精度，召回率 ~99%，适合大规模或高精度场景
CREATE INDEX idx_memory_embedding_hnsw ON long_term_memory
USING vectors (embedding vector_cosine_ops)
WITH (options = $$
    [indexing.hnsw]
    m = 16          -- 每层连接数，越大越精确但越慢
    ef_construction = 64  -- 构建时搜索宽度
$$);
```

**备选方案及拒绝理由**：
- ❌ L2 距离（欧氏距离）：对 embedding 向量长度敏感，需要归一化，不如余弦简洁
- ❌ 内积距离：无法处理负值embedding，适用场景有限
- ❌ 纯向量检索（无重排序）：会返回相似但不重要的记忆，影响质量
- ✅ 余弦相似度 + 重排序：平衡相关性、重要性和时效性

---

## R4: LLM 调用模式 - 总结生成与记忆提取

### 调研问题
- 如何设计 Prompt 让 LLM 生成高质量的对话总结？
- 如何让 LLM 判断是否需要创建长期记忆？
- 如何设计 Prompt 提取结构化的记忆数据？
- 应该使用什么模型（chat model）来执行这些任务？

### 决策：使用项目现有 PromptTemplate + ModelAdapter 架构，设计专门的 Prompt 模板

**架构复用**：
- ✅ 使用现有 `PromptTemplateService` 管理 Prompt 版本
- ✅ 通过 `ModelRoutingService` 自动选择合适的 LLM 模型
- ✅ 复用 `BeetlTemplateService` 渲染动态 Prompt
- ✅ 无需修改现有代码，仅添加新的 Prompt 模板记录

---

### 4.1 对话总结生成 (Summarization)

**触发时机**：
- 未总结消息数 ≥ 30 条
- 或估算 token 数超过阈值（context 占用 > 70%）

**Prompt 设计**：
```
你是一个对话历史总结助手。你的任务是将用户与AI的对话历史总结为结构化的JSON格式，以便未来快速回顾。

**对话历史**：
${conversationHistory}

**总结要求**：
1. 提取对话中的关键主题和讨论点
2. 记录用户的主要问题和AI的核心回答
3. 识别用户的情绪变化（如有）
4. 输出格式必须是有效的 JSON，包含以下字段：
   - topics: 主题列表，每个主题包含 {name: 主题名, summary: 简短描述}
   - key_points: 关键要点列表（字符串数组）
   - user_emotion: 用户整体情绪（calm/excited/frustrated/confused/sad等）
   - context_carry_over: 需要延续到下次对话的上下文（字符串）

**输出示例**：
{
  "topics": [
    {"name": "工作压力", "summary": "用户表示最近项目deadline临近，加班频繁"},
    {"name": "健康建议", "summary": "AI建议用户注意休息和运动"}
  ],
  "key_points": [
    "用户在一家互联网公司工作",
    "最近在做一个重要项目的技术架构设计",
    "感觉身体有些疲惫，但还在坚持"
  ],
  "user_emotion": "frustrated",
  "context_carry_over": "用户正在应对高强度工作压力，需要持续关注其身心状态"
}

请只输出 JSON，不要有任何额外说明。
```

**数据库存储**：
```java
@Data
@TableName("conversation_summary")
public class ConversationSummary {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    
    @TableField(typeHandler = JacksonTypeHandler.class)
    private SummaryJson summaryJson;  // 存储为 JSONB
    
    private Long coveredUntilMessageId;  // 总结覆盖到的最后一条消息ID
    private LocalDateTime createdAt;
}

@Data
public class SummaryJson {
    private List<Topic> topics;
    private List<String> keyPoints;
    private String userEmotion;
    private String contextCarryOver;
    
    @Data
    public static class Topic {
        private String name;
        private String summary;
    }
}
```

---

### 4.2 长期记忆提取 (Memory Extraction)

**触发时机**：
- 每次用户发送消息后，异步判断是否需要提取记忆
- 使用轻量级判断模型快速筛选（如 GPT-3.5-turbo）
- 满足条件再调用完整的提取流程

**Prompt 设计 - 第一阶段：判断是否需要记忆**
```
你是一个对话分析助手，判断以下对话内容是否包含需要长期记住的重要信息。

**对话内容**：
用户: ${userMessage}
AI: ${aiResponse}

**判断标准**（满足任一即为需要记忆）：
1. 用户表达了强烈情绪（哭泣、愤怒、极度喜悦、恐惧等）
2. 用户明确陈述了个人偏好或禁忌（"我不喜欢...""请不要...""我希望你..."）
3. 用户分享了重要生活事件（失业、失恋、生病、搬家、结婚等）
4. 用户透露了关键个人信息（职业、家庭状况、健康问题）
5. 用户的某个观点或需求被反复提及（主题重复出现）

**输出格式**：
{
  "should_extract": true/false,
  "reason": "简短说明理由"
}

请只输出 JSON。
```

**Prompt 设计 - 第二阶段：提取记忆内容**（仅在 should_extract=true 时调用）
```
你是一个记忆提取助手，从对话中提取值得长期记住的信息片段。

**对话内容**：
用户: ${userMessage}
AI: ${aiResponse}

**最近的总结上下文** (optional):
${recentSummary}

**提取要求**：
1. 每条记忆应该是独立的、可理解的短句或小段
2. 记忆应该是客观事实或明确表达的主观偏好
3. 为每条记忆分配类型和重要性

**输出格式**：
{
  "memories": [
    {
      "text": "记忆内容（一句话或小段）",
      "type": "event|preference|relationship|emotion|fact",
      "importance": 0.0-1.0,
      "reason": "为什么这条信息重要"
    }
  ]
}

**类型说明**：
- event: 生活事件（失业、搬家、生病等）
- preference: 偏好/禁忌（"不要说教""喜欢直接的建议"）
- relationship: 人际关系（家人、朋友、同事的信息）
- emotion: 情绪状态（长期情绪模式，非临时性情绪）
- fact: 客观事实（职业、居住地、年龄等）

**重要性评分指南**：
- 0.9-1.0: 核心身份信息或极强烈情绪
- 0.7-0.8: 重要生活事件或明确偏好
- 0.5-0.6: 一般性事实或轻度偏好
- 0.3-0.4: 辅助性信息

请只输出 JSON。
```

**示例输出**：
```json
{
  "memories": [
    {
      "text": "用户在一家互联网公司担任技术架构师",
      "type": "fact",
      "importance": 0.7,
      "reason": "关键职业信息，影响后续技术话题的深度"
    },
    {
      "text": "用户明确表示不喜欢空洞的安慰，更希望得到实际可行的建议",
      "type": "preference",
      "importance": 0.85,
      "reason": "核心沟通偏好，直接影响AI的回复风格"
    },
    {
      "text": "用户最近因工作压力感到身心疲惫，有轻度焦虑倾向",
      "type": "emotion",
      "importance": 0.75,
      "reason": "当前重要情绪状态，需要持续关注"
    }
  ]
}
```

**流程实现**：
```java
@Service
@RequiredArgsConstructor
public class LongTermMemoryService {
    private final PromptTemplateService promptTemplateService;
    private final ModelRoutingService modelRoutingService;
    private final EmbeddingUtil embeddingUtil;
    private final LongTermMemoryMapper memoryMapper;
    
    @Async
    public void extractAndSaveMemories(Long userId, String userMessage, String aiResponse) {
        // 1. 判断是否需要提取记忆
        Map<String, Object> params = Map.of(
            "userMessage", userMessage,
            "aiResponse", aiResponse
        );
        String judgePrompt = promptTemplateService.render("memory-judge-template", params);
        InferenceRequest judgeRequest = InferenceRequest.builder()
            .prompt(judgePrompt)
            .temperature(0.3)  // 低温度，保证稳定判断
            .build();
        
        InferenceResponse judgeResponse = modelRoutingService.callModel(judgeRequest);
        MemoryJudgment judgment = JSON.parseObject(judgeResponse.getContent(), MemoryJudgment.class);
        
        if (!judgment.shouldExtract()) {
            log.info("No memory extraction needed for userId={}", userId);
            return;
        }
        
        // 2. 提取记忆内容
        String extractPrompt = promptTemplateService.render("memory-extract-template", params);
        InferenceRequest extractRequest = InferenceRequest.builder()
            .prompt(extractPrompt)
            .temperature(0.5)
            .build();
        
        InferenceResponse extractResponse = modelRoutingService.callModel(extractRequest);
        MemoryExtraction extraction = JSON.parseObject(extractResponse.getContent(), MemoryExtraction.class);
        
        // 3. 为每条记忆生成 embedding 并保存
        for (Memory mem : extraction.getMemories()) {
            float[] embedding = embeddingUtil.generateEmbedding(mem.getText());
            
            LongTermMemory entity = LongTermMemory.builder()
                .userId(userId)
                .text(mem.getText())
                .embedding(embedding)
                .memoryType(mem.getType())
                .importance(mem.getImportance())
                .createdAt(LocalDateTime.now())
                .build();
            
            memoryMapper.insert(entity);
        }
        
        log.info("Extracted and saved {} memories for userId={}", extraction.getMemories().size(), userId);
    }
}
```

**备选方案及拒绝理由**：
- ❌ 规则引擎判断：难以覆盖所有场景，灵活性差
- ❌ 固定 Prompt 不使用模板系统：无法版本管理和灰度发布
- ❌ 使用独立的记忆模型（memory-specific LLM）：增加成本和复杂度
- ✅ LLM + 结构化 Prompt：灵活、准确、可版本控制

---

## R5: 性能与成本优化策略

### 关键优化点

1. **Embedding 缓存**：
   - 对于相同或相似的 query，缓存 embedding 结果（使用 Redis）
   - 缓存 key: `embedding:md5(text)`, TTL: 1小时
   - 预期缓存命中率：30-40%

2. **异步记忆写入**：
   - 记忆提取和 embedding 生成全部异步化
   - 使用 Spring `@Async` + 线程池
   - 失败重试：最多3次，指数退避

3. **批量 Embedding**：
   - 当有多条记忆需要生成 embedding 时，批量调用 API
   - OpenAI embedding API 支持批量（最多2048条）
   - 节省 API 调用次数和时间

4. **向量索引选择**：
   - 初期（<10万条记忆）：使用 IVFFlat 索引
   - 后期（>10万条）：切换到 HNSW 索引
   - 定期 VACUUM 和 REINDEX

5. **总结触发优化**：
   - 不严格按30条触发，使用滑动窗口
   - 如果最近5条消息都很短（<10 tokens each），延后触发
   - 如果检测到话题切换，立即触发总结

**成本预估（月度）**：
| 项目 | 用量 | 单价 | 月度成本 |
|------|------|------|---------|
| Embedding API | 10k用户 * 100条/月 * 50 tokens | $0.02/1M | $1 |
| 总结 LLM调用 | 10k用户 * 30次/月 * 500 tokens | $0.50/1M (GPT-3.5) | $7.5 |
| 记忆提取 LLM | 10k用户 * 100次/月 * 300 tokens | $0.50/1M | $15 |
| **总计** | | | **~$24/月** (10k MAU) |

---

## 总结

所有调研项均已完成决策，关键技术选型汇总：

| 技术项 | 决策 | 关键依赖 |
|--------|------|----------|
| **向量数据库** | pgvector-rs (PostgreSQL扩展) | 已有PostgreSQL |
| **Embedding模型** | OpenAI text-embedding-3-small | Spring AI OpenAiApi |
| **距离度量** | 余弦相似度 + 重排序 | pgvector-rs functions |
| **LLM调用** | 复用 PromptTemplate + ModelAdapter | 现有架构 |
| **异步处理** | Spring @Async | Spring Boot 自带 |
| **缓存** | Redis (可选优化) | 暂不引入新依赖，Phase 2考虑 |

**无需引入新的外部依赖**，所有功能均基于现有技术栈实现。下一步进入 Phase 1：数据模型设计和 API 契约定义。
