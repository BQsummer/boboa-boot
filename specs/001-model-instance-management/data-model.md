# 数据模型设计：模型实例管理

**功能**：模型实例管理  
**日期**：2025-10-21  
**版本**：v1.0

---

## 概述

本文档定义模型实例管理功能的数据模型，包括实体设计、表结构、字段说明和关系映射。

### 核心实体

1. **ai_model** - AI 模型实例配置
2. **routing_strategy** - 路由策略配置
3. **model_health_status** - 模型健康状态
4. **model_request_log** - 模型调用日志
5. **strategy_model_relation** - 策略与模型关联表（多对多）

---

## 1. AI 模型实例表 (ai_model)

### 用途
存储已注册的 AI 模型配置信息，包括提供商、版本、API 端点、认证信息等。

### 表结构

```sql
CREATE TABLE ai_model (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '模型ID',
    name VARCHAR(100) NOT NULL COMMENT '模型名称，如 GPT-4',
    version VARCHAR(50) NOT NULL COMMENT '模型版本，如 gpt-4-turbo',
    provider VARCHAR(50) NOT NULL COMMENT '提供商：openai/azure/qwen/gemini 等',
    model_type VARCHAR(20) NOT NULL COMMENT '模型类型：CHAT/EMBEDDING/RERANKER',
    
    api_endpoint VARCHAR(500) NOT NULL COMMENT 'API 端点 URL',
    api_key TEXT NOT NULL COMMENT 'API 密钥（AES-256 加密存储）',
    
    context_length INT COMMENT '上下文长度（token 数），如 8192',
    parameter_count VARCHAR(20) COMMENT '参数量，如 175B',
    
    tags JSON COMMENT '自定义标签，如 ["fast", "cheap"]',
    weight INT DEFAULT 1 COMMENT '路由权重，用于加权负载均衡',
    
    enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用：1-启用 0-禁用',
    
    created_by BIGINT COMMENT '创建人用户ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT COMMENT '最后更新人用户ID',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    UNIQUE KEY uk_name_version (name, version),
    INDEX idx_provider (provider),
    INDEX idx_model_type (model_type),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI模型实例配置表';
```

### 字段说明

| 字段 | 类型 | 必填 | 说明 | 示例值 |
|------|------|------|------|--------|
| id | BIGINT | 是 | 主键，自增 | 1 |
| name | VARCHAR(100) | 是 | 模型名称 | "GPT-4" |
| version | VARCHAR(50) | 是 | 模型版本号 | "gpt-4-turbo" |
| provider | VARCHAR(50) | 是 | 提供商标识 | "openai" |
| model_type | VARCHAR(20) | 是 | 模型类型枚举 | "CHAT" |
| api_endpoint | VARCHAR(500) | 是 | API 基础 URL | "https://api.openai.com/v1" |
| api_key | TEXT | 是 | 加密后的 API 密钥 | "encrypted_string..." |
| context_length | INT | 否 | 最大 token 数 | 8192 |
| parameter_count | VARCHAR(20) | 否 | 参数规模描述 | "175B" |
| tags | JSON | 否 | 标签数组 | ["fast", "cheap"] |
| weight | INT | 否 | 路由权重 | 5 |
| enabled | TINYINT(1) | 是 | 启用状态 | 1 |

### 约束规则
- `(name, version)` 组合唯一
- `api_key` 使用 AES-256-GCM 加密存储
- `provider` 枚举值：openai, azure_openai, qwen, yi, gemini, claude, ollama
- `model_type` 枚举值：CHAT, EMBEDDING, RERANKER

### Java 实体类

```java
@Data
@TableName("ai_model")
public class AiModel {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String name;
    private String version;
    private String provider;
    
    @TableField("model_type")
    private ModelType modelType;
    
    @TableField("api_endpoint")
    private String apiEndpoint;
    
    @TableField("api_key")
    private String apiKey;  // 加密后的密钥
    
    @TableField("context_length")
    private Integer contextLength;
    
    @TableField("parameter_count")
    private String parameterCount;
    
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> tags;
    
    private Integer weight;
    private Boolean enabled;
    
    @TableField("created_by")
    private Long createdBy;
    
    @TableField("created_at")
    private LocalDateTime createdAt;
    
    @TableField("updated_by")
    private Long updatedBy;
    
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

public enum ModelType {
    CHAT,       // 聊天对话
    EMBEDDING,  // 向量嵌入
    RERANKER    // 重排序
}
```

---

## 2. 路由策略表 (routing_strategy)

### 用途
定义请求路由规则，决定如何将推理请求分配到合适的模型。

### 表结构

```sql
CREATE TABLE routing_strategy (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '策略ID',
    name VARCHAR(100) NOT NULL COMMENT '策略名称',
    description VARCHAR(500) COMMENT '策略描述',
    
    strategy_type VARCHAR(30) NOT NULL COMMENT '策略类型：ROUND_ROBIN/LEAST_CONN/TAG_BASED/PRIORITY',
    config JSON NOT NULL COMMENT '策略配置（JSON格式）',
    
    is_default TINYINT(1) DEFAULT 0 COMMENT '是否默认策略：1-是 0-否',
    enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用：1-启用 0-禁用',
    
    created_by BIGINT COMMENT '创建人用户ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT COMMENT '最后更新人用户ID',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    UNIQUE KEY uk_name (name),
    INDEX idx_strategy_type (strategy_type),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='路由策略配置表';
```

### 策略配置 (config) 字段格式

#### 标签匹配策略
```json
{
  "type": "TAG_BASED",
  "matchMode": "ANY",  // ANY: 匹配任一标签, ALL: 匹配所有标签
  "tags": ["fast", "cheap"],
  "fallbackStrategy": "ROUND_ROBIN"  // 无匹配时的降级策略
}
```

#### 优先级路由策略
```json
{
  "type": "PRIORITY",
  "priorities": [
    {"modelId": 1, "priority": 1},  // 优先级 1 最高
    {"modelId": 2, "priority": 2},
    {"modelId": 3, "priority": 3}
  ],
  "enableFailover": true  // 高优先级失败时自动降级
}
```

#### 负载均衡策略
```json
{
  "type": "LEAST_CONNECTIONS",
  "resetInterval": 300  // 每 5 分钟重置连接计数
}
```

### Java 实体类

```java
@Data
@TableName("routing_strategy")
public class RoutingStrategy {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String name;
    private String description;
    
    @TableField("strategy_type")
    private StrategyType strategyType;
    
    @TableField(typeHandler = JacksonTypeHandler.class)
    private StrategyConfig config;
    
    @TableField("is_default")
    private Boolean isDefault;
    
    private Boolean enabled;
    
    @TableField("created_by")
    private Long createdBy;
    
    @TableField("created_at")
    private LocalDateTime createdAt;
    
    @TableField("updated_by")
    private Long updatedBy;
    
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

public enum StrategyType {
    ROUND_ROBIN,        // 轮询
    LEAST_CONNECTIONS,  // 最少连接
    TAG_BASED,          // 标签匹配
    PRIORITY,           // 优先级路由
    WEIGHTED            // 加权轮询
}
```

---

## 3. 策略模型关联表 (strategy_model_relation)

### 用途
多对多关系表，定义每个路由策略关联的模型列表。

### 表结构

```sql
CREATE TABLE strategy_model_relation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关联ID',
    strategy_id BIGINT NOT NULL COMMENT '策略ID',
    model_id BIGINT NOT NULL COMMENT '模型ID',
    
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    UNIQUE KEY uk_strategy_model (strategy_id, model_id),
    INDEX idx_strategy_id (strategy_id),
    INDEX idx_model_id (model_id),
    
    CONSTRAINT fk_strategy FOREIGN KEY (strategy_id) REFERENCES routing_strategy(id) ON DELETE CASCADE,
    CONSTRAINT fk_model FOREIGN KEY (model_id) REFERENCES ai_model(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='策略与模型关联表';
```

### Java 实体类

```java
@Data
@TableName("strategy_model_relation")
public class StrategyModelRelation {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("strategy_id")
    private Long strategyId;
    
    @TableField("model_id")
    private Long modelId;
    
    @TableField("created_at")
    private LocalDateTime createdAt;
}
```

---

## 4. 模型健康状态表 (model_health_status)

### 用途
记录每个模型的实时健康状态和历史检查结果。

### 表结构

```sql
CREATE TABLE model_health_status (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '状态ID',
    model_id BIGINT NOT NULL COMMENT '模型ID',
    
    status VARCHAR(20) NOT NULL COMMENT '健康状态：ONLINE/OFFLINE/TIMEOUT/AUTH_FAILED',
    consecutive_failures INT DEFAULT 0 COMMENT '连续失败次数',
    
    last_check_time DATETIME COMMENT '最后检查时间',
    last_success_time DATETIME COMMENT '最后成功时间',
    last_error TEXT COMMENT '最后错误信息',
    
    response_time_ms INT COMMENT '最近响应时间（毫秒）',
    uptime_percentage DECIMAL(5,2) COMMENT '最近24小时可用率（%）',
    
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    UNIQUE KEY uk_model_id (model_id),
    INDEX idx_status (status),
    INDEX idx_last_check (last_check_time),
    
    CONSTRAINT fk_health_model FOREIGN KEY (model_id) REFERENCES ai_model(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模型健康状态表';
```

### 状态枚举

| 状态 | 说明 | 触发条件 |
|------|------|---------|
| ONLINE | 在线可用 | 健康检查成功 |
| OFFLINE | 离线不可用 | 连续失败 ≥ 3 次 |
| TIMEOUT | 响应超时 | 请求超时 |
| AUTH_FAILED | 认证失败 | API Key 无效或过期 |

### Java 实体类

```java
@Data
@TableName("model_health_status")
public class ModelHealthStatus {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("model_id")
    private Long modelId;
    
    private HealthStatus status;
    
    @TableField("consecutive_failures")
    private Integer consecutiveFailures;
    
    @TableField("last_check_time")
    private LocalDateTime lastCheckTime;
    
    @TableField("last_success_time")
    private LocalDateTime lastSuccessTime;
    
    @TableField("last_error")
    private String lastError;
    
    @TableField("response_time_ms")
    private Integer responseTimeMs;
    
    @TableField("uptime_percentage")
    private BigDecimal uptimePercentage;
    
    @TableField("created_at")
    private LocalDateTime createdAt;
    
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

public enum HealthStatus {
    ONLINE,
    OFFLINE,
    TIMEOUT,
    AUTH_FAILED
}
```

---

## 5. 模型请求日志表 (model_request_log)

### 用途
记录每次模型调用的详细信息，用于审计、分析和故障排查。

### 表结构

```sql
CREATE TABLE model_request_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    request_id VARCHAR(64) NOT NULL COMMENT '请求唯一标识（UUID）',
    
    model_id BIGINT NOT NULL COMMENT '调用的模型ID',
    model_name VARCHAR(100) COMMENT '模型名称快照',
    
    request_type VARCHAR(20) NOT NULL COMMENT '请求类型：CHAT/EMBEDDING/RERANKER',
    prompt_tokens INT COMMENT '输入 token 数',
    completion_tokens INT COMMENT '输出 token 数',
    total_tokens INT COMMENT '总 token 数',
    
    response_status VARCHAR(20) NOT NULL COMMENT '响应状态：SUCCESS/FAILED/TIMEOUT',
    response_time_ms INT COMMENT '响应耗时（毫秒）',
    error_message TEXT COMMENT '错误信息（如有）',
    
    user_id BIGINT COMMENT '发起请求的用户ID',
    source VARCHAR(100) COMMENT '请求来源（IP或服务名）',
    
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '请求时间',
    
    INDEX idx_model_id (model_id),
    INDEX idx_request_id (request_id),
    INDEX idx_created_at (created_at),
    INDEX idx_response_status (response_status),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模型请求日志表'
PARTITION BY RANGE (TO_DAYS(created_at)) (
    PARTITION p_history VALUES LESS THAN (TO_DAYS('2025-10-01')),
    PARTITION p_2025_10 VALUES LESS THAN (TO_DAYS('2025-11-01')),
    PARTITION p_2025_11 VALUES LESS THAN (TO_DAYS('2025-12-01')),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);
```

### 分区策略
- 按月分区，便于数据归档和清理
- 保留最近 3 个月数据，历史数据可归档至冷存储

### Java 实体类

```java
@Data
@TableName("model_request_log")
public class ModelRequestLog {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("request_id")
    private String requestId;
    
    @TableField("model_id")
    private Long modelId;
    
    @TableField("model_name")
    private String modelName;
    
    @TableField("request_type")
    private RequestType requestType;
    
    @TableField("prompt_tokens")
    private Integer promptTokens;
    
    @TableField("completion_tokens")
    private Integer completionTokens;
    
    @TableField("total_tokens")
    private Integer totalTokens;
    
    @TableField("response_status")
    private ResponseStatus responseStatus;
    
    @TableField("response_time_ms")
    private Integer responseTimeMs;
    
    @TableField("error_message")
    private String errorMessage;
    
    @TableField("user_id")
    private Long userId;
    
    private String source;
    
    @TableField("created_at")
    private LocalDateTime createdAt;
}

public enum RequestType {
    CHAT,
    EMBEDDING,
    RERANKER
}

public enum ResponseStatus {
    SUCCESS,
    FAILED,
    TIMEOUT
}
```

---

## 实体关系图 (ERD)

```
┌─────────────────┐
│   ai_model      │
│  (模型实例)      │
└────────┬────────┘
         │ 1
         │
         │ N
┌────────┴────────────────────┐
│                             │
│ ┌───────────────────────┐   │ ┌─────────────────────┐
│ │ model_health_status   │   │ │ model_request_log   │
│ │   (健康状态)          │   │ │   (请求日志)        │
│ └───────────────────────┘   │ └─────────────────────┘
│                             │
│         M                   │
│ ┌───────┴───────┐           │
│ │ strategy_model│           │
│ │  _relation    │           │
│ │  (关联表)     │           │
│ └───────┬───────┘           │
│         N                   │
│                             │
│ ┌───────────────────────┐   │
│ │ routing_strategy      │   │
│ │  (路由策略)           │   │
│ └───────────────────────┘   │
└─────────────────────────────┘
```

### 关系说明
- **ai_model** 1:1 **model_health_status** - 每个模型有一个健康状态记录
- **ai_model** 1:N **model_request_log** - 每个模型有多条调用日志
- **ai_model** M:N **routing_strategy** - 多对多关系，通过 strategy_model_relation 关联

---

## 数据迁移脚本

完整的数据库迁移脚本位于：
```
src/main/resources/db/migration/V001__create_model_management_tables.sql
```

### 执行顺序
1. 创建 `ai_model` 表
2. 创建 `routing_strategy` 表
3. 创建 `strategy_model_relation` 表（依赖前两个表）
4. 创建 `model_health_status` 表（依赖 ai_model）
5. 创建 `model_request_log` 表（依赖 ai_model）

### 初始数据
脚本中包含默认路由策略：
```sql
INSERT INTO routing_strategy (name, description, strategy_type, config, is_default, enabled)
VALUES ('默认轮询策略', '按顺序依次选择可用模型', 'ROUND_ROBIN', '{}', 1, 1);
```

---

## 索引优化建议

### 查询场景分析
1. **模型列表查询**：按 provider, model_type, enabled 过滤 → 已建立单列索引
2. **健康状态查询**：按 status, last_check_time 过滤 → 已建立单列索引
3. **日志查询**：按 model_id, created_at, user_id 过滤 → 已建立单列索引
4. **策略模型关联**：按 strategy_id, model_id 查询 → 已建立唯一索引

### 复合索引建议（高并发场景）
```sql
-- 模型列表筛选
CREATE INDEX idx_model_search ON ai_model(enabled, provider, model_type);

-- 日志分析
CREATE INDEX idx_log_analysis ON model_request_log(model_id, created_at, response_status);
```

---

## 数据字典导出

可使用以下工具生成完整数据字典：
- **Screw**：Maven 插件，自动生成 Markdown/HTML 格式文档
- **MyBatis Plus**：内置代码生成器，可生成实体类和 Mapper

---

**文档版本**：v1.0  
**创建日期**：2025-10-21  
**维护人员**：GitHub Copilot  
**审核状态**：待审核
