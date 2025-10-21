-- =============================================
-- 模型实例管理功能 - 数据库迁移脚本
-- 版本: V001
-- 创建日期: 2025-10-21
-- 描述: 创建模型管理所需的 5 张表
-- =============================================

-- 1. AI 模型实例表
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

-- 2. 路由策略表
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

-- 3. 策略与模型关联表（多对多）
CREATE TABLE strategy_model_relation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关联ID',
    strategy_id BIGINT NOT NULL COMMENT '策略ID',
    model_id BIGINT NOT NULL COMMENT '模型ID',
    priority INT DEFAULT 0 COMMENT '优先级（用于 PRIORITY 策略，数值越大优先级越高）',
    
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    UNIQUE KEY uk_strategy_model (strategy_id, model_id),
    INDEX idx_strategy_id (strategy_id),
    INDEX idx_model_id (model_id),
    
    CONSTRAINT fk_strategy FOREIGN KEY (strategy_id) REFERENCES routing_strategy(id) ON DELETE CASCADE,
    CONSTRAINT fk_model FOREIGN KEY (model_id) REFERENCES ai_model(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='策略与模型关联表';

-- 4. 模型健康状态表
CREATE TABLE model_health_status (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '状态ID',
    model_id BIGINT NOT NULL COMMENT '模型ID',
    
    status VARCHAR(20) NOT NULL COMMENT '健康状态：ONLINE/OFFLINE/TIMEOUT/AUTH_FAILED',
    consecutive_failures INT DEFAULT 0 COMMENT '连续失败次数',
    total_checks INT DEFAULT 0 COMMENT '总检查次数',
    successful_checks INT DEFAULT 0 COMMENT '成功检查次数',
    
    last_check_time DATETIME COMMENT '最后检查时间',
    last_success_time DATETIME COMMENT '最后成功时间',
    last_error TEXT COMMENT '最后错误信息',
    
    last_response_time INT COMMENT '最后响应时间（毫秒）',
    response_time_ms INT COMMENT '平均响应时间（毫秒）',
    uptime_percentage DECIMAL(5,2) COMMENT '可用率（%）',
    
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    UNIQUE KEY uk_model_id (model_id),
    INDEX idx_status (status),
    INDEX idx_last_check (last_check_time),
    
    CONSTRAINT fk_health_model FOREIGN KEY (model_id) REFERENCES ai_model(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模型健康状态表';

-- 5. 模型请求日志表 (带分区)
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

-- 插入默认路由策略
INSERT INTO routing_strategy (name, description, strategy_type, config, is_default, enabled)
VALUES ('默认轮询策略', '按顺序依次选择可用模型', 'ROUND_ROBIN', '{}', 1, 1);
