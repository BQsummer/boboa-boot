package com.bqsummer.common.bo.ai;

import com.bqsummer.common.dto.ai.ModelType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class AiModelBo {

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
    private ModelType modelType;

    /**
     * API 端点 URL
     */
    private String apiEndpoint;

    /**
     * API 密钥（AES-256 加密存储）
     */
    private String apiKey;

    /**
     * 上下文长度（token 数），如 8192
     */
    private Integer contextLength;

    /**
     * 参数量，如 175B
     */
    private String parameterCount;

    /**
     * 自定义标签，如 ["fast", "cheap"]
     */
    private List<String> tags;

    /**
     * 是否启用：true-启用 false-禁用
     */
    private Boolean enabled;

    /**
     * 创建人用户ID
     */
    private Long createdBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 最后更新人用户ID
     */
    private Long updatedBy;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    private Integer weight;

    /**
     * 来自路由策略绑定的运行参数（JSON）。
     */
    private Map<String, Object> routingParams;
}
