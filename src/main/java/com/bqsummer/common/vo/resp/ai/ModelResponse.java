package com.bqsummer.common.vo.resp.ai;

import com.bqsummer.common.dto.ai.ModelType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 模型响应 DTO
 * 
 * @author Boboa Boot Team
 * @date 2025-10-21
 */
@Data
public class ModelResponse {
    
    /**
     * 模型ID
     */
    private Long id;
    
    /**
     * 模型名称
     */
    private String name;
    
    /**
     * 模型版本
     */
    private String version;
    
    /**
     * 提供商
     */
    private String provider;
    
    /**
     * 模型类型
     */
    private ModelType modelType;
    
    /**
     * API 端点 URL
     */
    private String apiEndpoint;
    
    /**
     * API 密钥（不返回，保持为 null）
     */
    private String apiKey;
    
    /**
     * 上下文长度
     */
    private Integer contextLength;
    
    /**
     * 参数量
     */
    private String parameterCount;
    
    /**
     * 自定义标签
     */
    private List<String> tags;
    
    /**
     * 路由权重
     */
    private Integer weight;
    
    /**
     * 是否启用
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
}
