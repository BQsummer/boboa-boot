package com.bqsummer.common.dto.ai;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 模型健康状态实体类
 * 对应表：model_health_status
 * 
 * @author Boboa Boot Team
 * @date 2025-10-21
 */
@Data
@TableName("model_health_status")
public class ModelHealthStatus {
    
    /**
     * 状态ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 模型ID
     */
    @TableField("model_id")
    private Long modelId;
    
    /**
     * 健康状态：ONLINE/OFFLINE/TIMEOUT/AUTH_FAILED
     */
    private HealthStatus status;
    
    /**
     * 连续失败次数
     */
    @TableField("consecutive_failures")
    private Integer consecutiveFailures;
    
    /**
     * 总检查次数
     */
    @TableField("total_checks")
    private Integer totalChecks;
    
    /**
     * 成功检查次数
     */
    @TableField("successful_checks")
    private Integer successfulChecks;
    
    /**
     * 最后检查时间
     */
    @TableField("last_check_time")
    private LocalDateTime lastCheckTime;
    
    /**
     * 最后成功时间
     */
    @TableField("last_success_time")
    private LocalDateTime lastSuccessTime;
    
    /**
     * 最后错误信息
     */
    @TableField("last_error")
    private String lastError;
    
    /**
     * 最后响应时间（毫秒）
     */
    @TableField("last_response_time")
    private Integer lastResponseTime;
    
    /**
     * 最近响应时间（毫秒）- 已废弃，使用 lastResponseTime
     */
    @Deprecated
    @TableField("response_time_ms")
    private Integer responseTimeMs;
    
    /**
     * 最近24小时可用率（%）
     */
    @TableField("uptime_percentage")
    private BigDecimal uptimePercentage;
    
    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
