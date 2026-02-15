# Data Model: 对话记忆系统

**Feature**: 对话记忆系统  
**Date**: 2026-01-23  
**Status**: Phase 1 Complete

## 概述

本文档定义对话记忆系统的数据模型，包括三个核心实体及其关系：
1. **ConversationMessage** - 原始对话消息
2. **ConversationSummary** - 对话总结
3. **LongTermMemory** - 长期记忆（含向量）

所有实体均关联到用户（通过 `user_id`），确保数据隔离。

---

## 实体关系图 (ERD)

```
┌─────────────────────┐          ┌─────────────────────┐
│       User          │          │    AiCharacter      │ (已存在的AI角色表)
│   (users)           │          │  (ai_character)     │
└──────────┬──────────┘          └──────────┬──────────┘
           │ 1                              │ 1
           │                                │
           │ N                              │ N
    ┌──────┴──────┬──────────────┬──────────┴────────┐
    │             │              │                   │
    │ N           │ N            │ N                 │
┌───┴──────────┐ ┌┴───────────┐ ┌┴──────────────┐   │
│ Conversation │ │Conversation│ │  LongTerm     │   │
│   Message    │ │  Summary   │ │   Memory      │   │
└──────────────┘ └────────────┘ └───────────────┘   │
                      │ 1                            │
                      │ covers_until                 │
                      │ N                            │
                      └───→ ConversationMessage      │
                                                     │
                                                     │ (支持向量检索)
```

**关系说明**：
- 一个 User 可以有多条 ConversationMessage（与不同AI角色的对话）
- 一个 AiCharacter 可以参与多条 ConversationMessage（与不同用户的对话）
- 一个 User + AiCharacter 组合形成一个独立的对话上下文
- 一个 ConversationSummary 覆盖多条 ConversationMessage（通过 `covered_until_message_id`）
- LongTermMemory 按 User + AiCharacter 隔离，每个对话上下文有独立的长期记忆
- LongTermMemory 支持向量相似度搜索（pgvector-rs）

---

## 实体定义

### 1. ConversationMessage (对话消息)

**用途**: 存储用户与AI之间的原始对话消息，保留完整对话历史。

**表名**: `conversation_message`

**字段**:

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| `id` | BIGSERIAL | PRIMARY KEY | 消息唯一标识，自增 |
| `user_id` | BIGINT | NOT NULL | 归属用户ID（外键关联 users 表） |
| `ai_character_id` | BIGINT | NOT NULL | AI角色ID（外键关联 ai_character 表） |
| `session_id` | VARCHAR(64) | NULL | 会话标识（可选，用于在同一用户+AI角色下区分多个对话） |
| `sender_type` | VARCHAR(16) | NOT NULL | 发送者类型：'USER' 或 'AI' |
| `content` | TEXT | NOT NULL | 消息内容 |
| `created_at` | TIMESTAMP | DEFAULT NOW() | 消息创建时间 |
| `metadata` | JSONB | NULL | 扩展元数据（可存储 model_id, tokens 等） |

**索引**:
- `idx_cm_user_char_created_at` - 按用户ID + AI角色ID + 时间查询（复合索引）
- `idx_cm_session_id` - 按会话ID查询（可选）

**Java实体类**:
```java
@Data
@TableName("conversation_message")
public class ConversationMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    private Long aiCharacterId;  // AI角色ID
    private String sessionId;
    private String senderType;  // "USER" or "AI"
    private String content;
    private LocalDateTime createdAt;
    
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> metadata;
}
```

**校验规则**:
- `sender_type` 必须是 "USER" 或 "AI"
- `content` 不能为空
- `user_id` 必须对应有效用户

**状态流转**: 无（消息一旦创建不可修改，仅可查询）

---

### 2. ConversationSummary (对话总结)

**用途**: 存储对话的结构化总结，压缩对话历史以优化上下文长度。

**表名**: `conversation_summary`

**字段**:

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| `id` | BIGSERIAL | PRIMARY KEY | 总结唯一标识，自增 |
| `user_id` | BIGINT | NOT NULL | 归属用户ID |
| `ai_character_id` | BIGINT | NOT NULL | AI角色ID（确保总结的上下文正确） |
| `session_id` | VARCHAR(64) | NULL | 会话标识（可选） |
| `summary_json` | JSONB | NOT NULL | 结构化总结内容（JSON格式） |
| `covered_until_message_id` | BIGINT | NOT NULL | 总结覆盖到的最后一条消息ID |
| `message_count` | INT | NOT NULL | 总结覆盖的消息数量 |
| `created_at` | TIMESTAMP | DEFAULT NOW() | 总结创建时间 |

**summary_json 结构**:
```json
{
  "topics": [
    {"name": "主题名称", "summary": "简短描述"}
  ],
  "key_points": ["要点1", "要点2"],
  "user_emotion": "calm|excited|frustrated|confused|sad",
  "context_carry_over": "需要延续到下次对话的上下文"
}
```

**索引**:
- `idx_cs_user_char_created_at` - 按用户ID + AI角色ID + 时间查询（复合索引）
- `idx_cs_covered_message_id` - 快速定位总结覆盖范围

**Java实体类**:
```java
@Data
@TableName("conversation_summary")
public class ConversationSummary {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    private Long aiCharacterId;  // AI角色ID
    private String sessionId;
    
    @TableField(typeHandler = JacksonTypeHandler.class)
    private SummaryJson summaryJson;
    
    private Long coveredUntilMessageId;
    private Integer messageCount;
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

**校验规则**:
- `summary_json` 必须符合定义的JSON schema
- `covered_until_message_id` 必须对应有效的 ConversationMessage.id
- `message_count` 必须 > 0

**状态流转**: 无（总结一旦创建不可修改，仅新增）

---

### 3. LongTermMemory (长期记忆)

**用途**: 存储从对话中提取的重要信息，支持语义相似度检索。

**表名**: `long_term_memory`

**字段**:

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| `id` | BIGSERIAL | PRIMARY KEY | 记忆唯一标识，自增 |
| `user_id` | BIGINT | NOT NULL | 归属用户ID |
| `ai_character_id` | BIGINT | NOT NULL | AI角色ID（每个角色独立的记忆空间） |
| `text` | TEXT | NOT NULL | 记忆文本内容（短句或小段） |
| `embedding` | VECTOR(1536) | NOT NULL | 文本的向量表示（1536维，OpenAI embedding） |
| `memory_type` | VARCHAR(32) | NOT NULL | 记忆类型：event/preference/relationship/emotion/fact |
| `importance` | FLOAT | NOT NULL | 重要性评分（0.0 ~ 1.0） |
| `source_message_id` | BIGINT | NULL | 来源消息ID（可选，用于追溯） |
| `last_accessed_at` | TIMESTAMP | NULL | 最后访问时间（用于时间衰减） |
| `access_count` | INT | DEFAULT 0 | 访问次数统计 |
| `created_at` | TIMESTAMP | DEFAULT NOW() | 创建时间 |

**索引**:
- `idx_ltm_user_char` - 按用户ID + AI角色ID查询（复合索引）
- `idx_ltm_embedding` - 向量相似度索引（pgvector-rs HNSW 或 IVFFlat）
- `idx_ltm_importance` - 按重要性排序
- `idx_ltm_type` - 按记忆类型过滤

**Java实体类**:
```java
@Data
@TableName("long_term_memory")
public class LongTermMemory {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    private Long aiCharacterId;  // AI角色ID，记忆按角色隔离
    private String text;
    
    @TableField(typeHandler = FloatArrayTypeHandler.class)
    private float[] embedding;  // 向量字段，使用自定义 TypeHandler
    
    private String memoryType;  // "event", "preference", "relationship", "emotion", "fact"
    private Float importance;   // 0.0 - 1.0
    private Long sourceMessageId;
    private LocalDateTime lastAccessedAt;
    private Integer accessCount;
    private LocalDateTime createdAt;
}
```

**校验规则**:
- `memory_type` 必须是以下之一：event, preference, relationship, emotion, fact
- `importance` 必须在 0.0 ~ 1.0 范围内
- `text` 不能为空且长度建议 ≤ 500字符（保持记忆粒度合适）
- `embedding` 维度必须为 1536（与模型匹配）

**状态流转**: 
- 创建时：`last_accessed_at` = NULL, `access_count` = 0
- 每次检索命中：更新 `last_accessed_at` 和递增 `access_count`
- 可选：定期清理长时间未访问的低重要性记忆（后台任务）

---

## 数据约束与一致性

### 外键约束

```sql
-- 消息表外键
ALTER TABLE conversation_message 
ADD CONSTRAINT fk_cm_user_id 
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE conversation_message
ADD CONSTRAINT fk_cm_ai_character_id
FOREIGN KEY (ai_character_id) REFERENCES ai_character(id) ON DELETE RESTRICT;

-- 总结表外键
ALTER TABLE conversation_summary 
ADD CONSTRAINT fk_cs_user_id 
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE conversation_summary
ADD CONSTRAINT fk_cs_ai_character_id
FOREIGN KEY (ai_character_id) REFERENCES ai_character(id) ON DELETE RESTRICT;

ALTER TABLE conversation_summary
ADD CONSTRAINT fk_cs_covered_message_id
FOREIGN KEY (covered_until_message_id) REFERENCES conversation_message(id) ON DELETE RESTRICT;

-- 记忆表外键
ALTER TABLE long_term_memory
ADD CONSTRAINT fk_ltm_user_id
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE long_term_memory
ADD CONSTRAINT fk_ltm_ai_character_id
FOREIGN KEY (ai_character_id) REFERENCES ai_character(id) ON DELETE RESTRICT;

ALTER TABLE long_term_memory
ADD CONSTRAINT fk_ltm_source_message_id
FOREIGN KEY (source_message_id) REFERENCES conversation_message(id) ON DELETE SET NULL;
```

### 业务约束

1. **消息顺序性**: `conversation_message.id` 必须按时间递增，确保 `covered_until_message_id` 语义正确
2. **对话上下文隔离**: 每个 (user_id, ai_character_id) 组合形成独立的对话上下文，消息、总结、记忆互不干扰
3. **总结覆盖范围**: 每个对话上下文的总结不应有重叠，即新总结的起始消息ID = 上一个总结的 `covered_until_message_id` + 1
4. **记忆唯一性**: 同一对话上下文不应有完全相同的 `text` 记忆（可通过应用层去重）
5. **向量维度一致性**: 所有 `embedding` 必须保持相同维度（1536）

### 事务场景

根据宪章原则III（事务一致性优先），以下操作必须在事务中完成：

1. **创建总结**:
   ```java
   @Transactional
   public void createSummary(Long userId, List<ConversationMessage> messages) {
       // 1. 调用 LLM 生成总结
       // 2. 保存 ConversationSummary
       // 3. 更新相关元数据（如需要）
   }
   ```

2. **批量保存记忆**:
   ```java
   @Transactional
   public void saveMemories(List<LongTermMemory> memories) {
       // 1. 去重检查
       // 2. 批量插入 long_term_memory
   }
   ```

3. **删除用户数据**（级联删除由外键保证，但需在事务中）:
   ```java
   @Transactional
   public void deleteUserData(Long userId) {
       // 外键 CASCADE 自动删除所有关联数据
       userMapper.deleteById(userId);
   }
   ```

---

## 扩展性考虑

### 1. 分区策略（可选，大规模场景）

当单表数据量超过1000万时，考虑按 `user_id` 或 `created_at` 分区：

```sql
-- 按用户ID范围分区（适合用户均匀分布）
CREATE TABLE conversation_message (
    ...
) PARTITION BY RANGE (user_id);

CREATE TABLE conversation_message_p1 PARTITION OF conversation_message
FOR VALUES FROM (1) TO (1000000);

-- 或按时间分区（适合历史数据归档）
CREATE TABLE conversation_message (
    ...
) PARTITION BY RANGE (created_at);

CREATE TABLE conversation_message_2026_01 PARTITION OF conversation_message
FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');
```

### 2. 归档策略

- **消息归档**: 超过6个月的消息可移至冷存储表
- **总结保留**: 总结数据相对小，建议永久保留
- **记忆淘汰**: 低重要性 (< 0.3) 且超过3个月未访问的记忆可标记删除

### 3. 多会话支持

当前设计通过 `session_id` 字段预留了多会话支持：
- 如果不需要区分会话，`session_id` 可设为 NULL
- 如果需要支持，可按会话分组查询消息和总结

---

## SQL Schema (完整定义)

```sql
-- ============================================
-- 对话记忆系统表结构定义
-- ============================================

-- 启用 pgvector-rs 扩展
CREATE EXTENSION IF NOT EXISTS vectors;

-- 1. 对话消息表
CREATE TABLE conversation_message (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    ai_character_id BIGINT NOT NULL,
    session_id VARCHAR(64),
    sender_type VARCHAR(16) NOT NULL CHECK (sender_type IN ('USER', 'AI')),
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB,
    
    CONSTRAINT fk_cm_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_cm_ai_character_id FOREIGN KEY (ai_character_id) REFERENCES ai_character(id) ON DELETE RESTRICT
);

-- 复合索引：按用户+AI角色+时间查询（最常用）
CREATE INDEX idx_cm_user_char_created_at ON conversation_message(user_id, ai_character_id, created_at DESC);
CREATE INDEX idx_cm_session_id ON conversation_message(session_id) WHERE session_id IS NOT NULL;

-- 2. 对话总结表
CREATE TABLE conversation_summary (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    ai_character_id BIGINT NOT NULL,
    session_id VARCHAR(64),
    summary_json JSONB NOT NULL,
    covered_until_message_id BIGINT NOT NULL,
    message_count INT NOT NULL CHECK (message_count > 0),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_cs_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_cs_ai_character_id FOREIGN KEY (ai_character_id) REFERENCES ai_character(id) ON DELETE RESTRICT,
    CONSTRAINT fk_cs_covered_message_id FOREIGN KEY (covered_until_message_id) 
        REFERENCES conversation_message(id) ON DELETE RESTRICT
);

-- 复合索引：按用户+AI角色+时间查询
CREATE INDEX idx_cs_user_char_created_at ON conversation_summary(user_id, ai_character_id, created_at DESC);
CREATE INDEX idx_cs_covered_message_id ON conversation_summary(covered_until_message_id);

-- 3. 长期记忆表
CREATE TABLE long_term_memory (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    ai_character_id BIGINT NOT NULL,
    text TEXT NOT NULL,
    embedding vector(1536) NOT NULL,
    memory_type VARCHAR(32) NOT NULL CHECK (memory_type IN ('event', 'preference', 'relationship', 'emotion', 'fact')),
    importance FLOAT NOT NULL CHECK (importance >= 0.0 AND importance <= 1.0),
    source_message_id BIGINT,
    last_accessed_at TIMESTAMP,
    access_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_ltm_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_ltm_ai_character_id FOREIGN KEY (ai_character_id) REFERENCES ai_character(id) ON DELETE RESTRICT,
    CONSTRAINT fk_ltm_source_message_id FOREIGN KEY (source_message_id) 
        REFERENCES conversation_message(id) ON DELETE SET NULL
);

-- 复合索引：按用户+AI角色查询（向量检索时的前置过滤）
CREATE INDEX idx_ltm_user_char ON long_term_memory(user_id, ai_character_id);
CREATE INDEX idx_ltm_importance ON long_term_memory(importance DESC);
CREATE INDEX idx_ltm_type ON long_term_memory(memory_type);

-- 向量索引（初期使用 IVFFlat，后期可切换到 HNSW）
CREATE INDEX idx_ltm_embedding ON long_term_memory 
USING vectors (embedding vector_cosine_ops)
WITH (options = $$
    [indexing.ivf]
    nlist = 100
$$);

-- 可选：防止重复记忆（基于文本内容的唯一索引）
-- CREATE UNIQUE INDEX idx_ltm_unique_text ON long_term_memory(user_id, md5(text));

-- ============================================
-- 注释说明
-- ============================================
COMMENT ON TABLE conversation_message IS '存储用户与AI角色之间的原始对话消息，每个(user_id, ai_character_id)形成独立对话';
COMMENT ON TABLE conversation_summary IS '存储对话的结构化总结，按(user_id, ai_character_id)分组压缩上下文';
COMMENT ON TABLE long_term_memory IS '存储长期记忆，按(user_id, ai_character_id)隔离，支持向量相似度检索';

COMMENT ON COLUMN conversation_message.ai_character_id IS 'AI角色ID，与user_id共同确定对话上下文';
COMMENT ON COLUMN conversation_summary.ai_character_id IS 'AI角色ID，确保总结针对特定对话上下文';
COMMENT ON COLUMN long_term_memory.ai_character_id IS 'AI角色ID，记忆按角色隔离避免混淆';

COMMENT ON COLUMN long_term_memory.embedding IS '文本向量表示，1536维，使用 OpenAI text-embedding-3-small 生成';
COMMENT ON COLUMN long_term_memory.importance IS '记忆重要性评分，范围 0.0 - 1.0';
COMMENT ON COLUMN long_term_memory.last_accessed_at IS '最后访问时间，用于时间衰减计算';
```

---

## 数据迁移与初始化

根据宪章原则VII（数据库管理规范），所有 SQL 必须写入 `datasourceInit.sql`：

**路径**: `src/main/resources/datasourceInit.sql`

**添加内容**（追加到文件末尾）:
```sql
-- ============================================
-- Feature: 001-conversation-memory
-- Date: 2026-01-23
-- Description: 对话记忆系统表结构
-- ============================================

-- (将上述完整 SQL Schema 复制到此处)
```

**初始化数据**（可选）:
```sql
-- 插入示例 Prompt 模板（用于总结和记忆提取）
INSERT INTO prompt_template (char_id, description, model_code, lang, content, version, is_latest, status)
VALUES 
(NULL, '对话总结生成模板', 'gpt-4.1', 'zh-CN', 
 '你是一个对话历史总结助手...', -- (完整 Prompt 见 research.md)
 1, true, true, 'ACTIVE'),
 
(NULL, '记忆提取判断模板', 'gpt-3.5-turbo', 'zh-CN',
 '你是一个对话分析助手，判断以下对话内容...', -- (完整 Prompt)
 1, true, true, 'ACTIVE');
```

---

## 总结

数据模型设计完成，关键要点：
- ✅ 三个核心实体清晰定义
- ✅ 支持向量检索（pgvector-rs）
- ✅ 遵循项目宪章（外键约束、事务一致性、命名规范）
- ✅ SQL Schema 完整可执行
- ✅ 扩展性考虑（分区、归档、多会话）

下一步：定义 API 契约（contracts/）。
