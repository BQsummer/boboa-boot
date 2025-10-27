package com.bqsummer.common.dto.ai;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 路由策略实体类
 * 对应表：routing_strategy
 * 
 * @author Boboa Boot Team
 * @date 2025-10-21
 */
@Data
@TableName(value = "routing_strategy", autoResultMap = true)
public class RoutingStrategy {
    
    /**
     * 策略ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 策略名称
     */
    private String name;
    
    /**
     * 策略描述
     */
    private String description;
    
    /**
     * 策略类型：ROUND_ROBIN/LEAST_CONN/TAG_BASED/PRIORITY
     */
    @TableField("strategy_type")
    private StrategyType strategyType;
    
    /**
     * 策略配置（JSON格式）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private String config;
    
    /**
     * 是否默认策略：true-是 false-否
     */
    @TableField("is_default")
    private Boolean isDefault;
    
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
