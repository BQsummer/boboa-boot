package com.bqsummer.common.dto.robot;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 并发配置响应 DTO
 * 
 * 用于查询接口，返回某个动作类型的并发配置和实时状态
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConcurrencyConfigDto {
    
    /**
     * 动作类型（SEND_MESSAGE, SEND_VOICE, SEND_NOTIFICATION）
     */
    private String actionType;
    
    /**
     * 并发限制上限
     */
    private Integer concurrencyLimit;
    
    /**
     * 当前可用的并发槽位数
     */
    private Integer availablePermits;
    
    /**
     * 当前使用中的并发槽位数
     * 计算公式：concurrencyLimit - availablePermits
     */
    private Integer usedPermits;
    
    /**
     * 并发使用率（0.0 ~ 1.0）
     * 计算公式：usedPermits / concurrencyLimit
     */
    private Double usageRate;
}
