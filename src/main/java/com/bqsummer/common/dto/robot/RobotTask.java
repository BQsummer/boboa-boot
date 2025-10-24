package com.bqsummer.common.dto.robot;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 机器人调度任务实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("robot_task")
public class RobotTask {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private Long robotId;
    
    private String taskType;  // IMMEDIATE, SHORT_DELAY, LONG_DELAY
    
    private String actionType;  // SEND_MESSAGE, SEND_VOICE, SEND_NOTIFICATION
    
    private String actionPayload;  // JSON 格式
    
    private LocalDateTime scheduledAt;
    
    private String status;  // PENDING, RUNNING, DONE, FAILED, TIMEOUT
    
    /**
     * 领取任务的实例ID（格式：hostname:pid）
     * 用于验证任务所有权，防止跨实例误操作
     * NULL表示任务未被领取（PENDING状态）
     */
    private String lockedBy;
    
    private Integer retryCount;
    
    private Integer maxRetryCount;
    
    private LocalDateTime startedAt;
    
    private LocalDateTime completedAt;
    
    private LocalDateTime heartbeatAt;
    
    private String errorMessage;
    
    private LocalDateTime createdTime;
    
    private LocalDateTime updatedTime;
}
