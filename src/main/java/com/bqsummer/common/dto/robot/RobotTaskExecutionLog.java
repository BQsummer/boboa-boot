package com.bqsummer.common.dto.robot;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 任务执行日志实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("robot_task_execution_log")
public class RobotTaskExecutionLog {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long taskId;
    
    private Integer executionAttempt;
    
    private String status;  // SUCCESS, FAILED, TIMEOUT
    
    private LocalDateTime startedAt;
    
    private LocalDateTime completedAt;
    
    private Long executionDurationMs;
    
    private Long delayFromScheduledMs;
    
    private String errorMessage;
    
    private String instanceId;
    
    private LocalDateTime createdTime;
}
