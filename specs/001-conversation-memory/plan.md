# Implementation Plan: 对话记忆系统

**Branch**: `001-conversation-memory` | **Date**: 2026-01-23 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/001-conversation-memory/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

实现对话记忆系统，为AI对话提供三层记忆能力：原始消息存储、滚动总结压缩、长期记忆向量检索。系统将自动管理对话历史，当达到阈值时生成总结以优化上下文长度，同时识别重要信息提取为长期记忆并支持语义相似度检索。技术方案采用 pgvector-rs 作为向量数据库，利用现有 Spring Boot + MyBatis Plus 架构，集成已有的 LLM 调用能力（通过 ModelAdapter 和 PromptTemplate 系统）。

## Technical Context

**Language/Version**: Java 17  
**Primary Dependencies**: Spring Boot 3.5.5, MyBatis Plus 3.5.14, Spring Security 6.5.3, PostgreSQL JDBC Driver, Lombok, FastJSON2, Guava  
**Storage**: PostgreSQL 16 with pgvector-rs extension (pg16-v0.2.0) for vector search capabilities  
**Testing**: JUnit 5, Spring Boot Test, RestAssured (for integration tests)  
**Target Platform**: Linux/Windows server (JVM-based)  
**Project Type**: Web application (backend only - REST API)  
**Performance Goals**: 支持单用户100轮以上对话不降级，多用户（10+）并发场景下响应时间<2s，向量检索延迟<100ms  
**Constraints**: 总结触发阈值30条消息（可调整），向量检索topK=3-8，记忆重要性评分范围0-1，单次对话上下文不超过模型token限制  
**Scale/Scope**: 预期10k+用户规模，每用户每月1000条消息，长期记忆每用户预期<500条

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### 原则合规检查

| 原则 | 状态 | 说明 |
|------|------|------|
| **I. 中文优先文档** | ✅ PASS | 所有文档、注释、用户故事使用中文 |
| **II. 测试驱动开发 (TDD)** | ✅ PASS | 将先编写测试，使用JUnit 5 + RestAssured验证API |
| **III. 事务一致性优先** | ✅ PASS | 多表操作使用@Transactional（消息+总结+记忆） |
| **IV. 安全第一** | ✅ PASS | API端点验证JWT，使用现有JwtUtil获取userId |
| **V. JWT认证规范** | ✅ PASS | 通过JwtUtil从token提取userId，不接受请求参数传递 |
| **VI. 最小化修改原则** | ✅ PASS | 仅添加记忆相关功能，不改动现有代码 |
| **VII. 数据库管理规范** | ✅ PASS | SQL写入datasourceInit.sql，不使用migration目录 |
| **VIII. 异常处理规范** | ✅ PASS | 使用SnorlaxClientException和SnorlaxServerException |
| **IX. Service类设计规范** | ✅ PASS | Service直接用类实现，不定义接口 |

### 技术栈合规检查

| 项目 | 状态 | 说明 |
|------|------|------|
| **Spring Boot 3.5.5** | ✅ | 使用现有框架 |
| **Java 17** | ✅ | 项目标准版本 |
| **MyBatis Plus 3.5.14** | ✅ | 使用BaseMapper模式 |
| **PostgreSQL + pgvector-rs** | ✅ | 数据库已明确为pg16-v0.2.0 |
| **JUnit 5 + RestAssured** | ✅ | 项目测试标准 |
| **新依赖** | ✅ PASS | 无需新增依赖，复用现有Spring AI OpenAI集成 |

### 设计质量复核（Phase 1 完成后）

| 检查项 | 状态 | 说明 |
|--------|------|------|
| **数据模型清晰** | ✅ PASS | 三个实体定义完整，关系明确，外键约束正确 |
| **API 契约完整** | ✅ PASS | OpenAPI 3.0规范，包含所有必需端点和Schema |
| **向量检索实现** | ✅ PASS | 使用pgvector-rs原生函数，无需额外依赖 |
| **LLM集成复用** | ✅ PASS | 完全复用PromptTemplate + ModelAdapter架构 |
| **异步处理设计** | ✅ PASS | 记忆提取和embedding生成使用Spring @Async |
| **事务边界清晰** | ✅ PASS | 总结生成、批量保存记忆均在事务中 |
| **测试覆盖计划** | ✅ PASS | 单元测试和集成测试示例已在quickstart.md中 |

### 潜在复杂度追踪

无需填写 - 所有原则均符合，无违规项需要说明。

**Phase 0 结论**: ✅ 通过所有宪章检查，已进入 Phase 0 研究阶段  
**Phase 1 最终结论**: ✅✅ 通过所有设计质量检查，**可进入 Phase 2 任务分解阶段**

---

## Phase 1 完成总结

✅ **所有 Phase 1 交付物已完成**：

1. ✅ `research.md` - 5个关键技术决策调研完成
2. ✅ `data-model.md` - 3个核心实体定义，完整SQL schema
3. ✅ `contracts/memory-api.yaml` - 完整OpenAPI 3.0规范，7个API端点
4. ✅ `quickstart.md` - 快速入门指南，包含代码示例和测试建议
5. ✅ Agent上下文已更新 - GitHub Copilot指令文件已生成

**关键设计决策汇总**：
- 向量数据库: pgvector-rs (零额外成本)
- Embedding模型: OpenAI text-embedding-3-small (复用现有集成)
- 距离度量: 余弦相似度 + 重排序
- LLM调用: 复用PromptTemplate系统
- 异步处理: Spring @Async
- 无需新增Maven依赖

**下一步**: 执行 `/speckit.tasks` 命令生成详细的实施任务清单（Phase 2）。

## Project Structure

### Documentation (this feature)

```text
specs/001-conversation-memory/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
│   ├── memory-api.yaml  # 记忆管理API规范 (OpenAPI 3.0)
│   └── message-api.yaml # 消息相关API扩展
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
src/main/java/com/bqsummer/
├── common/
│   ├── dto/
│   │   └── memory/
│   │       ├── ConversationMessage.java   # 对话消息实体
│   │       ├── ConversationSummary.java   # 对话总结实体
│   │       └── LongTermMemory.java        # 长期记忆实体
│   └── vo/
│       ├── req/
│       │   └── memory/
│       │       ├── MessageSaveRequest.java       # 保存消息请求
│       │       ├── MemorySearchRequest.java      # 记忆检索请求
│       │       └── SummaryTriggerRequest.java    # 手动触发总结请求
│       └── resp/
│           └── memory/
│               ├── MessageHistoryResponse.java   # 消息历史响应
│               ├── MemorySearchResponse.java     # 记忆检索响应
│               └── SummaryResponse.java          # 总结响应
├── controller/
│   └── MemoryController.java              # 记忆系统API控制器
├── service/
│   └── memory/
│       ├── ConversationMessageService.java    # 消息管理服务
│       ├── ConversationSummaryService.java    # 总结管理服务
│       ├── LongTermMemoryService.java         # 长期记忆服务
│       └── MemoryRetrievalService.java        # 记忆检索编排服务
├── mapper/
│   └── memory/
│       ├── ConversationMessageMapper.java     # 消息Mapper (继承BaseMapper)
│       ├── ConversationSummaryMapper.java     # 总结Mapper
│       └── LongTermMemoryMapper.java          # 记忆Mapper (含向量查询)
└── util/
    └── EmbeddingUtil.java                 # 文本向量化工具类

src/main/resources/
├── datasourceInit.sql                     # 添加三张新表定义 + pgvector配置
└── mapper/
    └── memory/
        └── LongTermMemoryMapper.xml       # 向量检索SQL (如需自定义)

src/test/java/com/bqsummer/
├── controller/
│   └── MemoryControllerTest.java          # API集成测试 (RestAssured)
└── service/
    └── memory/
        ├── ConversationMessageServiceTest.java    # 消息服务单元测试
        ├── ConversationSummaryServiceTest.java    # 总结服务单元测试
        ├── LongTermMemoryServiceTest.java         # 记忆服务单元测试
        └── MemoryRetrievalServiceTest.java        # 检索编排测试
```

**Structure Decision**: 采用 Web application (backend only) 结构。新功能完全独立在 `memory` 包下，遵循项目现有分层架构：DTO/VO → Controller → Service → Mapper。向量检索通过 MyBatis 自定义SQL调用 pgvector-rs 扩展函数。不需要新增Repository层（项目使用Mapper模式）。

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

无违规项 - 本功能完全符合项目宪章，无需额外说明。
