package com.bqsummer.common.dto.ai;

import com.baomidou.mybatisplus.annotation.*;
import com.bqsummer.framework.handler.PgJsonStringListTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 模型实例实体类
 * 对应表：ai_model
 * 
 * @author Boboa Boot Team
 * @date 2025-10-21
 */
@Data
@TableName(value = "ai_model", autoResultMap = true)
public class AiModel {
    
    /**
     * 模型ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 模型名称，如 GPT-4
     */
    private String name;
    
    /**
     * 模型版本，如 gpt-4-turbo
     */
    private String version;
    
    /**
     * 提供商：openai/azure/qwen/gemini 等
     */
    private String provider;
    
    /**
     * 模型类型：CHAT/EMBEDDING/RERANKER
     */
    @TableField("model_type")
    private ModelType modelType;
    
    /**
     * API 端点 URL
     */
    @TableField("api_endpoint")
    private String apiEndpoint;
    
    /**
     * API 密钥（AES-256 加密存储）
     */
    @TableField("api_key")
    private String apiKey;
    
    /**
     * 上下文长度（token 数），如 8192
     */
    @TableField("context_length")
    private Integer contextLength;
    
    /**
     * 参数量，如 175B
     */
    @TableField("parameter_count")
    private String parameterCount;
    
    /**
     * 自定义标签，如 ["fast", "cheap"]
     */
    @TableField(value = "tags", typeHandler = PgJsonStringListTypeHandler.class)
    private List<String> tags;
    
    /**
     * 路由权重，用于加权负载均衡
     */
    private Integer weight;
    
    /**
     * 是否启用：true-启用 false-禁用
     */
    private Boolean enabled;
    
    /**
     * 创建人用户ID
     */
    @TableField("created_by")
    private Long createdBy;
    
    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;
    
    /**
     * 最后更新人用户ID
     */
    @TableField("updated_by")
    private Long updatedBy;
    
    /**
     * 更新时间
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
