package com.bqsummer.common.dto.ai;

/**
 * 健康状态枚举
 * 
 * @author Boboa Boot Team
 * @date 2025-10-21
 */
public enum HealthStatus {
    /**
     * 在线可用
     */
    ONLINE,
    
    /**
     * 离线不可用
     */
    OFFLINE,
    
    /**
     * 响应超时
     */
    TIMEOUT,
    
    /**
     * 认证失败
     */
    AUTH_FAILED
}
