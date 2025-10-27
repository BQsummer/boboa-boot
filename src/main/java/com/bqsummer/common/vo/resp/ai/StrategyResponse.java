package com.bqsummer.common.vo.resp.ai;

import com.bqsummer.common.dto.ai.StrategyType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 路由策略响应
 * 
 * @author Boboa Boot Team
 * @date 2025-10-21
 */
@Data
public class StrategyResponse {
    
    private Long id;
    private String name;
    private StrategyType strategyType;
    private String description;
    private String config;
    private Boolean enabled;
    private Boolean isDefault;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
}
