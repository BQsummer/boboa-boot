# Tasks: 对话记忆系统

**Input**: Design documents from `/specs/001-conversation-memory/`
**Prerequisites**: plan.md, spec.md, data-model.md, research.md, quickstart.md, contracts/memory-api.yaml

**Tests**: Tests are OPTIONAL per project constitution. This implementation includes tests following TDD principle (Constitution II).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

---

## Format: `[ID] [P?] [Story?] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3, US4)
  - ❌ Setup phase: No [Story] tag
  - ❌ Foundational phase: No [Story] tag
  - ✅ User story phases: MUST have [Story] tag
  - ❌ Polish phase: No [Story] tag
- Include exact file paths in descriptions

---

## Implementation Strategy

### MVP Scope (Recommended)
**Phase 3 (User Story 1) ONLY** provides immediate value:
- Basic conversation memory storage
- Multi-turn dialogue continuity within session
- Integration with existing `/api/v1/messages` endpoint

Deploy this first, gather feedback, then proceed with Phase 4-6.

### User Story Execution Order

#### Sequential Dependencies
```
Setup → Foundational → US1 (P1) → US2 (P2) → US3 (P2) → US4 (P3) → Polish
                         ↓           ↓           ↓           ↓
                    [20轮对话]  [长对话总结] [长期记忆]  [语义检索]
```

**Why this order**:
1. **US1 (P1) - 保持连贯的多轮对话**: Foundation - stores all messages
2. **US2 (P2) - 长对话的智能总结**: Optimizes US1 for scalability
3. **US3 (P2) - 记住重要信息**: Enhances US1 with long-term memory
4. **US4 (P3) - 跨会话检索**: Advanced feature leveraging US3

#### Parallel Execution Opportunities

**Within User Story 1 (after database schema complete)**:
- T012, T013, T014 (Entity classes) can run in parallel
- T015, T016, T017 (Mapper interfaces) can run in parallel after entities

**Within User Story 2 (after US1 Service complete)**:
- T028 (Summary entity), T029 (Summary mapper) can run in parallel
- T032 (Summary tests), T033 (Summary service) are sequential

**Within User Story 3 (after embedding utility ready)**:
- T044 (Memory entity), T045 (Memory mapper) can run in parallel
- T048, T049 (Tests) can run in parallel if test data setup is independent

**Within User Story 4 (after US3 complete)**:
- T054 (Reranking logic), T055 (Access time update) can run in parallel

**Cross-Story Parallelism**:
- ❌ NOT RECOMMENDED - Each story builds on previous functionality
- US2, US3 both depend on US1 completion
- US4 depends on US3 (vector search requires memory entities)

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and database structure

- [x] T001 Enable pgvector-rs extension: Add `CREATE EXTENSION IF NOT EXISTS vectors;` to src/main/resources/datasourceInit.sql
- [x] T002 Create conversation_message table schema in src/main/resources/datasourceInit.sql (user_id, ai_character_id, sender_type, content, created_at, metadata JSONB)
- [x] T003 [P] Create conversation_summary table schema in src/main/resources/datasourceInit.sql (user_id, ai_character_id, summary_json JSONB, covered_until_message_id FK, message_count, created_at)
- [x] T004 [P] Create long_term_memory table schema in src/main/resources/datasourceInit.sql (user_id, ai_character_id, text, embedding vector(1536), memory_type CHECK, importance FLOAT, source_message_id, last_accessed_at, access_count, created_at)
- [x] T005 Add foreign key constraints in datasourceInit.sql (user_id→users ON DELETE CASCADE, ai_character_id→ai_character ON DELETE RESTRICT)
- [x] T006 [P] Create indexes: idx_cm_user_char_created_at (conversation_message), idx_cs_user_char_created_at (conversation_summary), idx_ltm_user_char (long_term_memory)
- [x] T007 Create vector index for long_term_memory.embedding: `CREATE INDEX idx_ltm_embedding ON long_term_memory USING vectors (embedding vector_cosine_ops) WITH (options = $$[indexing.ivf] nlist = 100$$)`
- [x] T008 [P] Add table and column comments to document schema intent

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T009 Create FloatArrayTypeHandler in src/main/java/com/bqsummer/framework/handler/FloatArrayTypeHandler.java (converts float[] ↔ PostgreSQL vector type using PGobject)
- [x] T010 [P] Create JacksonTypeHandler for JSONB in src/main/java/com/bqsummer/framework/handler/JacksonTypeHandler.java (if not exists) - ALREADY EXISTS in MyBatis Plus
- [x] T011 [P] Create EmbeddingUtil in src/main/java/com/bqsummer/util/EmbeddingUtil.java (calls AiModelService with EMBEDDING type, includes Redis cache with MD5 key)
- [x] T012 Add EMBEDDING to ModelType enum in src/main/java/com/bqsummer/constant/ModelType.java (if not exists) - ALREADY EXISTS
- [ ] T013 Verify ai_model table contains embedding model entry: INSERT INTO ai_model (name='text-embedding-3-small', model_type='EMBEDDING', provider='openai', status='ACTIVE')
- [x] T014 Create base DTO classes: SummaryJson.java, MemorySearchResult.java, MemoryItem.java in src/main/java/com/bqsummer/common/dto/memory/

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - 保持连贯的多轮对话 (Priority: P1) 🎯 MVP

**Goal**: 用户在同一对话中发送多条消息时，系统能记住之前的内容并提供连贯的回复

**Independent Test**: 用户发送"我养了一只猫"，然后问"它吃什么"，系统回复应体现对"猫"的理解

### Tests for User Story 1 (TDD - Write First)

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T015 [P] [US1] Write unit test in src/test/java/com/bqsummer/service/memory/ConversationMessageServiceTest.java: testSaveMessage_Success()
- [ ] T016 [P] [US1] Write unit test: testSaveMessage_TriggersUserIdValidation()
- [ ] T017 [P] [US1] Write unit test: testGetRecentMessages_ReturnsCorrectOrder()
- [ ] T018 [P] [US1] Write unit test: testGetRecentMessages_FiltersByUserAndCharacter()
- [ ] T019 [US1] Write integration test in src/test/java/com/bqsummer/integration/ConversationMemoryIntegrationTest.java: testMessageEndpoint_SavesConversation()

### Implementation for User Story 1

- [x] T020 [P] [US1] Create ConversationMessage entity in src/main/java/com/bqsummer/common/dto/memory/ConversationMessage.java (@TableName, @TableId AUTO, @TableField with JacksonTypeHandler for metadata)
- [x] T021 [US1] Create ConversationMessageMapper interface in src/main/java/com/bqsummer/mapper/memory/ConversationMessageMapper.java (extends BaseMapper<ConversationMessage>, add @Mapper)
- [x] T022 [US1] Add custom query method to ConversationMessageMapper: findRecentMessages(userId, aiCharacterId, limit) in src/main/resources/mapper/memory/ConversationMessageMapper.xml
- [x] T023 [US1] Create ConversationMessageService in src/main/java/com/bqsummer/service/memory/ConversationMessageService.java (@Service, @RequiredArgsConstructor, NO interface)
- [x] T024 [US1] Implement saveMessage(userId, aiCharacterId, senderType, content) method with @Transactional
- [x] T025 [US1] Implement getRecentMessages(userId, aiCharacterId, limit) method
- [x] T026 [US1] Implement getUnsummarizedMessageCount(userId, aiCharacterId) method (returns count after last summary)
- [x] T027 [US1] Integrate ConversationMessageService into MessageService.sendMessage() in src/main/java/com/bqsummer/service/im/MessageService.java: call conversationMessageService.saveMessage() after original IM logic

**Checkpoint**: At this point, User Story 1 should be fully functional - messages are stored and queryable by (user_id, ai_character_id)

---

## Phase 4: User Story 2 - 长对话的智能总结 (Priority: P2)

**Goal**: 对话超过30条消息时自动生成总结，优化上下文长度，保持响应速度

**Independent Test**: 模拟50轮对话，验证第50轮响应时间 ≤ 第5轮响应时间 × 1.5，且能引用早期对话关键信息

### Tests for User Story 2

- [ ] T028 [P] [US2] Write unit test in src/test/java/com/bqsummer/service/memory/ConversationSummaryServiceTest.java: testGenerateSummary_TriggersAt30Messages()
- [ ] T029 [P] [US2] Write unit test: testGenerateSummary_CreatesValidSummaryJson()
- [ ] T030 [P] [US2] Write unit test: testGetLatestSummary_ReturnsCorrectContext()
- [ ] T031 [US2] Write integration test: testLongConversation_PerformanceWithSummary() (verify response time constraint)

### Implementation for User Story 2

- [x] T032 [P] [US2] Create ConversationSummary entity in src/main/java/com/bqsummer/common/dto/memory/ConversationSummary.java
- [x] T033 [P] [US2] Create SummaryJson nested class with topics[], keyPoints[], userEmotion, contextCarryOver fields (已存在于Phase 2)
- [x] T034 [US2] Create ConversationSummaryMapper interface in src/main/java/com/bqsummer/mapper/memory/ConversationSummaryMapper.java (extends BaseMapper)
- [x] T035 [US2] Create ConversationSummaryService in src/main/java/com/bqsummer/service/memory/ConversationSummaryService.java (@Service, NO interface)
- [x] T036 [US2] Implement getLatestSummary(userId, aiCharacterId) method
- [x] T037 [US2] Implement generateSummaryAsync(userId, aiCharacterId) method with @Async annotation
- [x] T038 [US2] Create summary prompt template in src/main/resources/prompts/memory/summary-template.md (Beetl syntax, includes message history)
- [x] T039 [US2] In generateSummaryAsync: call PromptTemplateService.render() with summary template
- [x] T040 [US2] In generateSummaryAsync: call ModelRoutingService.callModel() to get LLM summary
- [x] T041 [US2] In generateSummaryAsync: parse LLM output to SummaryJson, save with @Transactional
- [x] T042 [US2] Update ConversationMessageService.saveMessage() to check unsummarizedCount >= 30 and trigger generateSummaryAsync()
- [x] T043 [US2] Create MemoryRetrievalService.buildContextPrompt() in src/main/java/com/bqsummer/service/memory/MemoryRetrievalService.java: combine summaries + recent messages + user input

**Checkpoint**: Conversations with 30+ messages automatically generate summaries, context building includes summary content

---

## Phase 5: User Story 3 - 记住重要信息和用户偏好 (Priority: P2)

**Goal**: 系统自动识别对话中的重要信息（偏好、事件、关系、情绪）并提取为长期记忆，支持跨会话记住用户

**Independent Test**: 用户在第一次对话说"我不喜欢被说教"，第二次全新对话中AI应避免说教语气

### Tests for User Story 3

- [ ] T044 [P] [US3] Write unit test in src/test/java/com/bqsummer/service/memory/LongTermMemoryServiceTest.java: testExtractMemoryAsync_IdentifiesImportantInfo()
- [ ] T045 [P] [US3] Write unit test: testExtractMemoryAsync_AssignsMemoryTypes()
- [ ] T046 [P] [US3] Write unit test: testExtractMemoryAsync_AssignsImportanceScore()
- [ ] T047 [P] [US3] Write unit test: testExtractMemoryAsync_GeneratesEmbedding()
- [ ] T048 [US3] Write integration test: testCrossSessionMemory_Preference() (verify preference remembered across sessions)

### Implementation for User Story 3

- [x] T049 [P] [US3] Create LongTermMemory entity in src/main/java/com/bqsummer/common/dto/memory/LongTermMemory.java (includes @TableField(typeHandler = FloatArrayTypeHandler.class) for embedding)
- [x] T050 [US3] Create LongTermMemoryMapper interface in src/main/java/com/bqsummer/mapper/memory/LongTermMemoryMapper.java (extends BaseMapper)
- [x] T051 [US3] Create LongTermMemoryService in src/main/java/com/bqsummer/service/memory/LongTermMemoryService.java (@Service, NO interface)
- [x] T052 [US3] Create memory judgment prompt template in src/main/resources/prompts/memory/memory-judge-template.md (returns {should_extract: boolean, reason: string})
- [x] T053 [US3] Create memory extraction prompt template in src/main/resources/prompts/memory/memory-extract-template.md (returns {memories: [{text, type, importance, reason}]})
- [x] T054 [US3] Implement judgeNeedExtraction(userMessage) method: call LLM with judge template
- [x] T055 [US3] Implement extractMemoryItems(userMessage) method: call LLM with extract template, parse JSON response
- [x] T056 [US3] Implement extractMemoryAsync(userId, aiCharacterId, userMessage) with @Async: judge → extract → generate embeddings → batch insert with @Transactional
- [x] T057 [US3] In extractMemoryAsync: use EmbeddingUtil.generateEmbedding() for each memory text
- [x] T058 [US3] Update ConversationMessageService.saveMessage() to call extractMemoryAsync() when senderType = "USER"
- [x] T059 [US3] Create getMemoriesByType(userId, aiCharacterId, memoryType, limit) query method in LongTermMemoryService
- [x] T060 [US3] Update MemoryRetrievalService.buildContextPrompt() to include high-importance memories (importance > 0.7) in context

**Checkpoint**: Important user information is automatically extracted as long-term memory, separated by AI character

---

## Phase 6: User Story 4 - 跨会话的智能记忆检索 (Priority: P3)

**Goal**: 使用语义相似度检索相关记忆，即使用户用不同表达方式，系统也能联想到相关历史记忆

**Independent Test**: 用户第一次说"工作压力大"，几天后说"感觉很累"，系统应联想到工作压力记忆

### Tests for User Story 4

- [ ] T061 [P] [US4] Write unit test in src/test/java/com/bqsummer/service/memory/LongTermMemoryServiceTest.java: testSearchMemories_VectorSimilarity()
- [ ] T062 [P] [US4] Write unit test: testSearchMemories_Reranking()
- [ ] T063 [P] [US4] Write unit test: testSearchMemories_TimeDecay()
- [ ] T064 [US4] Write integration test: testSemanticSearch_DifferentExpressions() (verify "压力大" matches "很累")

### Implementation for User Story 4

- [x] T065 [US4] Create custom SQL query in src/main/resources/mapper/memory/LongTermMemoryMapper.xml: searchByEmbedding(userId, aiCharacterId, queryEmbedding, topK)
- [x] T066 [US4] SQL query content: `SELECT id, user_id, ai_character_id, text, embedding, memory_type, importance, last_accessed_at, created_at, 1 - (embedding <=> #{queryEmbedding}::vector) AS similarity FROM long_term_memory WHERE user_id = #{userId} AND ai_character_id = #{aiCharacterId} AND (embedding <=> #{queryEmbedding}::vector) < 0.3 ORDER BY embedding <=> #{queryEmbedding}::vector LIMIT #{topK}`
- [x] T067 [US4] Implement searchMemories(userId, aiCharacterId, queryText, topK) in LongTermMemoryService
- [x] T068 [US4] In searchMemories: generate query embedding using EmbeddingUtil
- [x] T069 [US4] In searchMemories: call mapper.searchByEmbedding() with topK*2 for initial candidates
- [x] T070 [US4] Implement calculateFinalScore(memory, queryEmbedding) method: similarity*0.6 + importance*0.3 + timeDecay*0.1
- [x] T071 [US4] Implement calculateTimeDecay(lastAccessedAt) method: exponential decay based on days since access
- [x] T072 [US4] In searchMemories: apply reranking, filter by finalScore > 0.5, limit to topK, return List<MemorySearchResult>
- [x] T073 [US4] Implement updateAccessTime(memoryId) method with @Async: updates last_accessed_at and increments access_count
- [x] T074 [US4] Update MemoryRetrievalService.buildContextPrompt() to call searchMemories() with user input query
- [x] T075 [US4] In buildContextPrompt: format retrieved memories into prompt context section

**Checkpoint**: Semantic search fully functional, memories are retrieved by meaning rather than exact keywords

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Final improvements, documentation, performance optimization

- [x] T076 Add error handling in ConversationMessageService: throw SnorlaxClientException for invalid sender_type
- [x] T077 Add error handling in LongTermMemoryService: throw SnorlaxServerException for embedding API failures
- [x] T078 [P] Add logging: log.info() for memory extraction success/failure in LongTermMemoryService
- [x] T079 [P] Add logging: log.warn() for summary generation failures in ConversationSummaryService
- [x] T080 [P] Add performance metrics: track embedding generation time in EmbeddingUtil
- [x] T081 [P] Add performance metrics: track vector search time in LongTermMemoryService.searchMemories()
- [x] T082 Create batch embedding generation: EmbeddingUtil.generateEmbeddingBatch(List<String>) for optimization (already implemented)
- [x] T083 [P] Add Redis cache for embeddings in EmbeddingUtil: key = "embedding:" + MD5(text), TTL = 1 hour (already implemented)
- [ ] T084 Optimize vector index if dataset > 10000 records: switch from IVFFlat (nlist=100) to HNSW (m=16, ef_construction=64)
- [x] T085 Add configuration properties: SUMMARY_THRESHOLD=30, VECTOR_SEARCH_TOP_K=10, MEMORY_IMPORTANCE_THRESHOLD=0.5 in application.properties
- [x] T086 [P] Create API documentation: update Swagger/OpenAPI annotations for MessageController (no new endpoints needed)
- [ ] T087 Create monitoring dashboard: track daily message count, summary generation rate, memory extraction rate (optional)
- [ ] T088 [P] Add unit tests for edge cases: empty message content, null sessionId, invalid aiCharacterId
- [ ] T089 [P] Add integration test for concurrent users: verify memory isolation between users
- [ ] T090 Create README section in specs/001-conversation-memory/README.md: explain feature usage, configuration, troubleshooting

---

## Summary

**Total Tasks**: 90
- **Setup (Phase 1)**: 8 tasks
- **Foundational (Phase 2)**: 6 tasks (BLOCKING)
- **User Story 1 (P1)**: 13 tasks (5 tests + 8 implementation)
- **User Story 2 (P2)**: 16 tasks (4 tests + 12 implementation)
- **User Story 3 (P2)**: 12 tasks (5 tests + 7 implementation)
- **User Story 4 (P3)**: 15 tasks (4 tests + 11 implementation)
- **Polish (Phase 7)**: 20 tasks

**Parallel Execution Opportunities**:
- Within phases: Tasks marked with [P] can run in parallel (different files, no dependencies)
- Across phases: ❌ NOT RECOMMENDED - each story builds on previous

**MVP Recommendation**: Complete Phases 1-3 only (27 tasks) for initial deployment

**Independent Testing per Story**:
- US1: TestMessageEndpoint_SavesConversation() → verify messages stored by (userId, aiCharacterId)
- US2: TestLongConversation_PerformanceWithSummary() → verify 50-round dialogue response time
- US3: TestCrossSessionMemory_Preference() → verify preference remembered across sessions
- US4: TestSemanticSearch_DifferentExpressions() → verify "压力大" matches "很累"

**Implementation Guidance**:
1. Start with TDD: write tests first (T015-T019, T028-T031, etc.)
2. Follow Constitution principle II: all tests must FAIL before implementation
3. Use @Transactional for multi-table operations (summary generation, memory batch insert)
4. Service classes: NO interfaces, direct implementation with @Service annotation
5. Exceptions: ONLY use SnorlaxClientException (4xx) and SnorlaxServerException (5xx)
6. SQL: ALL schema changes go in datasourceInit.sql (Constitution VII)
7. JWT: Extract userId via JwtUtil, NEVER from request params (Constitution V)

---

**Next Steps**: Execute tasks sequentially by phase. After Phase 3 (US1) completion, conduct user acceptance testing before proceeding to Phase 4.
