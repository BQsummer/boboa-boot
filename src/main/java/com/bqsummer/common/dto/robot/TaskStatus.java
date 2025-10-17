package com.bqsummer.common.dto.robot;

/**
 * 任务状态枚举
 */
public enum TaskStatus {
    /**
     * 待执行
     */
    PENDING,
    
    /**
     * 执行中
     */
    RUNNING,
    
    /**
     * 已完成
     */
    DONE,
    
    /**
     * 失败
     */
    FAILED,
    
    /**
     * 超时
     */
    TIMEOUT
}
