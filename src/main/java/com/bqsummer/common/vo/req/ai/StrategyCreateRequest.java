package com.bqsummer.common.vo.req.ai;

import com.bqsummer.common.dto.ai.StrategyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 路由策略创建请求
 */
@Data
public class StrategyCreateRequest {
    
    /**
     * 策略名称
     */
    @NotBlank(message = "策略名称不能为空")
    private String name;
    
    /**
     * 策略类型
     */
    @NotNull(message = "策略类型不能为空")
    private StrategyType strategyType;
    
    /**
     * 策略描述
     */
    private String description;
    
    /**
     * JSON 配置（可选，用于 TAG_BASED 等策略）
     */
    private String config;
    
    /**
     * 是否启用
     */
    private Boolean enabled = true;
    
    /**
     * 是否默认策略
     */
    private Boolean isDefault = false;
}
