package com.bqsummer.service.robot;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.bqsummer.common.dto.robot.RobotTask;
import com.bqsummer.common.dto.robot.RobotTaskExecutionLog;
import com.bqsummer.common.dto.robot.TaskStatus;
import com.bqsummer.configuration.RobotTaskConfiguration;
import com.bqsummer.mapper.robot.RobotTaskExecutionLogMapper;
import com.bqsummer.mapper.robot.RobotTaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * 机器人任务执行器
 * 
 * 职责:
 * 1. 使用乐观锁抢占任务（PENDING → RUNNING）
 * 2. 执行任务的具体行为（SEND_MESSAGE, SEND_VOICE, SEND_NOTIFICATION）
 * 3. 更新任务状态（RUNNING → DONE / FAILED）
 * 4. 记录执行日志到 robot_task_execution_log
 * 5. 处理失败重试逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RobotTaskExecutor {
    
    private final RobotTaskMapper robotTaskMapper;
    private final RobotTaskExecutionLogMapper executionLogMapper;
    private final RobotTaskConfiguration config;
    
    /**
     * 异步执行任务
     */
    @Async
    public CompletableFuture<Void> executeAsync(RobotTask task) {
        execute(task);
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * 执行任务的核心逻辑
     * 
     * @param task 待执行的任务
     */
    @Transactional(rollbackFor = Exception.class)
    public void execute(RobotTask task) {
        LocalDateTime startTime = LocalDateTime.now();
        Long taskId = task.getId();
        
        log.info("开始执行任务: taskId={}, actionType={}, scheduledAt={}", 
                taskId, task.getActionType(), task.getScheduledAt());
        
        // Step 1: 使用乐观锁尝试抢占任务
        boolean acquired = tryAcquireTask(task);
        if (!acquired) {
            log.debug("任务已被其他实例抢占，跳过: taskId={}", taskId);
            return;
        }
        
        // Step 2: 执行任务行为
        boolean success = false;
        String errorMessage = null;
        LocalDateTime completedTime = null;
        
        try {
            executeAction(task);
            success = true;
            completedTime = LocalDateTime.now();
            
            // 更新任务状态为 DONE
            UpdateWrapper<RobotTask> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", taskId)
                        .eq("version", task.getVersion())
                        .set("status", TaskStatus.DONE.name())
                        .set("completed_at", completedTime)
                        .set("version", task.getVersion() + 1);
            robotTaskMapper.update(null, updateWrapper);
            
            log.info("任务执行成功: taskId={}", taskId);
            
        } catch (Exception e) {
            log.error("任务执行失败: taskId=" + taskId, e);
            errorMessage = e.getMessage();
            completedTime = LocalDateTime.now();
            
            // 处理失败重试逻辑
            handleTaskFailure(task, errorMessage);
        }
        
        // Step 3: 记录执行日志
        recordExecutionLog(task, startTime, completedTime, success, errorMessage);
    }
    
    /**
     * 使用乐观锁尝试抢占任务
     * 
     * @return true 如果抢占成功，false 如果被其他实例抢占
     */
    private boolean tryAcquireTask(RobotTask task) {
        LocalDateTime now = LocalDateTime.now();
        
        UpdateWrapper<RobotTask> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", task.getId())
                    .eq("status", TaskStatus.PENDING.name())
                    .eq("version", task.getVersion())
                    .set("status", TaskStatus.RUNNING.name())
                    .set("started_at", now)
                    .set("heartbeat_at", now)
                    .set("version", task.getVersion() + 1);
        
        int updated = robotTaskMapper.update(null, updateWrapper);
        
        if (updated == 1) {
            task.setVersion(task.getVersion() + 1);
            task.setStatus(TaskStatus.RUNNING.name());
            task.setStartedAt(now);
            task.setHeartbeatAt(now);
            return true;
        }
        
        return false;
    }
    
    /**
     * 执行任务的具体行为
     */
    private void executeAction(RobotTask task) {
        String actionType = task.getActionType();
        String payload = task.getActionPayload();
        
        log.debug("执行行为: taskId={}, actionType={}, payload={}", 
                 task.getId(), actionType, payload);
        
        switch (actionType) {
            case "SEND_MESSAGE":
                executeSendMessage(task);
                break;
            case "SEND_VOICE":
                executeSendVoice(task);
                break;
            case "SEND_NOTIFICATION":
                executeSendNotification(task);
                break;
            default:
                throw new IllegalArgumentException("不支持的行为类型: " + actionType);
        }
    }
    
    /**
     * 执行发送消息行为
     */
    private void executeSendMessage(RobotTask task) {
        // TODO: 实现发送消息逻辑
        // 1. 解析 action_payload JSON
        // 2. 调用消息服务发送消息
        // 3. 验证发送结果
        
        log.info("发送消息: taskId={}, payload={}", task.getId(), task.getActionPayload());
        
        // 模拟耗时操作
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 执行发送语音行为
     */
    private void executeSendVoice(RobotTask task) {
        // TODO: 实现发送语音逻辑
        log.info("发送语音: taskId={}, payload={}", task.getId(), task.getActionPayload());
    }
    
    /**
     * 执行发送通知行为
     */
    private void executeSendNotification(RobotTask task) {
        // TODO: 实现发送通知逻辑
        log.info("发送通知: taskId={}, payload={}", task.getId(), task.getActionPayload());
    }
    
    /**
     * 处理任务失败后的重试逻辑
     */
    private void handleTaskFailure(RobotTask task, String errorMessage) {
        int retryCount = task.getRetryCount() + 1;
        int maxRetryCount = task.getMaxRetryCount();
        
        if (retryCount >= maxRetryCount) {
            // 超过最大重试次数，标记为 FAILED
            UpdateWrapper<RobotTask> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", task.getId())
                        .eq("version", task.getVersion())
                        .set("status", TaskStatus.FAILED.name())
                        .set("retry_count", retryCount)
                        .set("completed_at", LocalDateTime.now())
                        .set("error_message", errorMessage)
                        .set("version", task.getVersion() + 1);
            robotTaskMapper.update(null, updateWrapper);
            
            log.warn("任务失败且超过最大重试次数: taskId={}, retryCount={}", 
                    task.getId(), retryCount);
        } else {
            // 计算下次执行时间（指数退避）
            long delayMinutes = (long) Math.pow(retryCount, 2); // 1分钟, 4分钟, 9分钟...
            LocalDateTime nextExecuteAt = LocalDateTime.now().plusMinutes(delayMinutes);
            
            // 重置状态为 PENDING，增加重试计数
            UpdateWrapper<RobotTask> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", task.getId())
                        .eq("version", task.getVersion())
                        .set("status", TaskStatus.PENDING.name())
                        .set("retry_count", retryCount)
                        .set("scheduled_at", nextExecuteAt)
                        .set("error_message", errorMessage)
                        .set("version", task.getVersion() + 1);
            robotTaskMapper.update(null, updateWrapper);
            
            log.info("任务失败，将在 {} 分钟后重试: taskId={}, retryCount={}, nextExecuteAt={}", 
                    delayMinutes, task.getId(), retryCount, nextExecuteAt);
        }
    }
    
    /**
     * 记录执行日志
     */
    private void recordExecutionLog(RobotTask task, LocalDateTime startTime, 
                                    LocalDateTime completedTime, boolean success, 
                                    String errorMessage) {
        try {
            // 计算执行延迟
            long delayMs = Duration.between(task.getScheduledAt(), startTime).toMillis();
            long durationMs = completedTime != null ? 
                             Duration.between(startTime, completedTime).toMillis() : 0;
            
            // 获取实例ID
            String instanceId = getInstanceId();
            
            RobotTaskExecutionLog log = RobotTaskExecutionLog.builder()
                    .taskId(task.getId())
                    .executionAttempt(task.getRetryCount() + 1)
                    .status(success ? "SUCCESS" : "FAILED")
                    .startedAt(startTime)
                    .completedAt(completedTime)
                    .executionDurationMs(durationMs)
                    .delayFromScheduledMs(delayMs)
                    .errorMessage(errorMessage)
                    .instanceId(instanceId)
                    .build();
            
            executionLogMapper.insert(log);
            
        } catch (Exception e) {
            // 记录日志失败不应影响主流程
            RobotTaskExecutor.log.error("记录执行日志失败: taskId=" + task.getId(), e);
        }
    }
    
    /**
     * 获取当前实例ID（pod名称或主机名）
     */
    private String getInstanceId() {
        try {
            // 优先使用环境变量（Kubernetes pod名称）
            String podName = System.getenv("HOSTNAME");
            if (podName != null && !podName.isEmpty()) {
                return podName;
            }
            
            // 否则使用主机名
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
