package com.bqsummer.common.dto.ai;

import com.baomidou.mybatisplus.annotation.*;
import com.bqsummer.framework.handler.PgJsonObjectTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 策略模型关联实体类（多对多关系表�?
 * 对应表：strategy_model_relation
 * 
 * @author Boboa Boot Team
 * @date 2025-10-21
 */
@Data
@TableName(value = "strategy_model_relation", autoResultMap = true)
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
     * 优先级（用于 PRIORITY 策略，数值越大优先级越高�?
     */
    @TableField("priority")
    private Integer priority;

    /**
     * 权重（范围：1-100，用�?WEIGHTED 策略�?     */
    @TableField("weight")
    private Integer weight;

    /**
     * 模型运行参数（JSON），用于路由策略绑定时透传给模型调用。
     */
    @TableField(value = "model_params", typeHandler = PgJsonObjectTypeHandler.class)
    private Map<String, Object> modelParams;
    
    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * ����ʱ��
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}


