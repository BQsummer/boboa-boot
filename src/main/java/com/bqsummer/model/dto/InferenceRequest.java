package com.bqsummer.model.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 统一推理请求 DTO
 * 
 * @author Boboa Boot Team
 * @date 2025-10-21
 */
@Data
public class InferenceRequest {
    
    /**
     * 提示词/输入内容
     */
    @NotBlank(message = "提示词不能为空")
    private String prompt;
    
    /**
     * 指定模型ID（可选，不指定则自动路由）
     */
    private Long modelId;
    
    /**
     * 温度参数（0-1，控制随机性）
     */
    private Double temperature;
    
    /**
     * 最大生成 token 数
     */
    private Integer maxTokens;
    
    /**
     * Top-P 采样参数
     */
    private Double topP;
    
    /**
     * 频率惩罚参数
     */
    private Double frequencyPenalty;
    
    /**
     * 存在惩罚参数
     */
    private Double presencePenalty;
    
    /**
     * 停止词列表
     */
    private String[] stopSequences;
    
    /**
     * 是否流式响应
     */
    private Boolean stream;
    
    /**
     * 用户ID（用于日志记录）
     */
    private Long userId;
    
    /**
     * 请求来源
     */
    private String source;
}
