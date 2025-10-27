package com.bqsummer.common.dto.ai;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 策略模型关联实体类（多对多关系表）
 * 对应表：strategy_model_relation
 * 
 * @author Boboa Boot Team
 * @date 2025-10-21
 */
@Data
@TableName("strategy_model_relation")
public class StrategyModelRelation {
    
    /**
     * 关联ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 策略ID
     */
    @TableField("strategy_id")
    private Long strategyId;
    
    /**
     * 模型ID
     */
    @TableField("model_id")
    private Long modelId;
    
    /**
     * 优先级（用于 PRIORITY 策略，数值越大优先级越高）
     */
    @TableField("priority")
    private Integer priority;
    
    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;
}
