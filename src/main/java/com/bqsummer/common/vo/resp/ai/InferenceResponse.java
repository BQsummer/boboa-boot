package com.bqsummer.common.vo.resp.ai;

import lombok.Data;

/**
 * 统一推理响应 DTO
 * 
 * @author Boboa Boot Team
 * @date 2025-10-21
 */
@Data
public class InferenceResponse {
    
    /**
     * 生成的内容
     */
    private String content;
    
    /**
     * 使用的模型ID
     */
    private Long modelId;
    
    /**
     * 使用的模型名称
     */
    private String modelName;
    
    /**
     * 提示词 token 数
     */
    private Integer promptTokens;
    
    /**
     * 完成 token 数
     */
    private Integer completionTokens;
    
    /**
     * 总 token 数
     */
    private Integer totalTokens;
    
    /**
     * 响应时间（毫秒）
     */
    private Integer responseTimeMs;
    
    /**
     * 请求ID（用于追踪）
     */
    private String requestId;
    
    /**
     * 是否成功
     */
    private Boolean success;
    
    /**
     * 错误信息（如有）
     */
    private String errorMessage;
}
