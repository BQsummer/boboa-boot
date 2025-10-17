package com.bqsummer.common.dto.robot;

/**
 * 任务类型枚举
 */
public enum TaskType {
    /**
     * 立即执行
     */
    IMMEDIATE,
    
    /**
     * 短延迟（秒到分钟级，0 < delay_seconds <= 600）
     */
    SHORT_DELAY,
    
    /**
     * 长延迟（小时到月级，delay_seconds > 600）
     */
    LONG_DELAY
}
