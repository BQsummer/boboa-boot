package com.bqsummer.model.entity;

/**
 * 路由策略类型枚举
 * 
 * @author Boboa Boot Team
 * @date 2025-10-21
 */
public enum StrategyType {
    /**
     * 轮询
     */
    ROUND_ROBIN,
    
    /**
     * 最少连接
     */
    LEAST_CONNECTIONS,
    
    /**
     * 标签匹配
     */
    TAG_BASED,
    
    /**
     * 优先级路由
     */
    PRIORITY,
    
    /**
     * 加权轮询
     */
    WEIGHTED
}
