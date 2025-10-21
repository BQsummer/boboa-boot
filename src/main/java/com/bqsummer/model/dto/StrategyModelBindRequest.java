package com.bqsummer.model.dto;

import lombok.Data;

/**
 * 策略-模型绑定请求
 * 
 * @author Boboa Boot Team
 * @date 2025-10-21
 */
@Data
public class StrategyModelBindRequest {
    
    /**
     * 模型ID
     */
    private Long modelId;
    
    /**
     * 优先级（用于 PRIORITY 策略）
     */
    private Integer priority;
}
