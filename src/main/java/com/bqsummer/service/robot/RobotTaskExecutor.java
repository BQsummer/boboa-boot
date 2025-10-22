package com.bqsummer.service.robot;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.bqsummer.common.dto.im.Message;
import com.bqsummer.common.dto.robot.RobotTask;
import com.bqsummer.common.dto.robot.RobotTaskExecutionLog;
import com.bqsummer.common.dto.robot.SendMessagePayload;
import com.bqsummer.common.dto.robot.TaskStatus;
import com.bqsummer.configuration.RobotTaskConfiguration;
import com.bqsummer.mapper.ConversationMapper;
import com.bqsummer.mapper.robot.RobotTaskExecutionLogMapper;
import com.bqsummer.mapper.robot.RobotTaskMapper;
import com.bqsummer.model.dto.InferenceRequest;
import com.bqsummer.model.dto.InferenceResponse;
import com.bqsummer.model.service.UnifiedInferenceService;
import com.bqsummer.repository.MessageRepository;
import com.bqsummer.util.JsonUtil;
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
    private final UnifiedInferenceService inferenceService;
    private final MessageRepository messageRepository;
    private final ConversationMapper conversationMapper;
    
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
     * 1. 解析 action_payload JSON
     * 2. 调用LLM推理服务
     * 3. 创建AI回复消息
     * 4. 更新会话表
     * 
     * T028-T032: 添加完善的异常处理和重试逻辑
     */
    private void executeSendMessage(RobotTask task) {
        log.info("开始执行SEND_MESSAGE任务: taskId={}, retryCount={}/{}, payload={}", 
                task.getId(), task.getRetryCount(), task.getMaxRetryCount(), 
                task.getActionPayload());
        
        try {
            // 1. 解析 action_payload JSON (T028: try-catch捕获异常)
            SendMessagePayload payload = JsonUtil.fromJson(
                    task.getActionPayload(), SendMessagePayload.class);
            
            if (payload == null) {
                throw new IllegalArgumentException("action_payload解析失败");
            }
            
            log.info("解析payload成功: messageId={}, senderId={}, receiverId={}, modelId={}", 
                    payload.getMessageId(), payload.getSenderId(), 
                    payload.getReceiverId(), payload.getModelId());
            
            // 2. 调用LLM推理服务 (T028: 捕获LLM调用异常)
            InferenceRequest inferenceRequest = new InferenceRequest();
            inferenceRequest.setModelId(payload.getModelId());
            inferenceRequest.setPrompt(payload.getContent());
            inferenceRequest.setTemperature(0.7);
            inferenceRequest.setMaxTokens(2000);
            
            log.info("调用LLM推理服务: modelId={}, promptLength={}", 
                    payload.getModelId(), payload.getContent().length());
            
            InferenceResponse inferenceResponse = inferenceService.chat(inferenceRequest);
            
            // 验证LLM响应
            if (inferenceResponse == null || !Boolean.TRUE.equals(inferenceResponse.getSuccess())) {
                String errorMsg = inferenceResponse != null ? inferenceResponse.getErrorMessage() : "LLM响应为空";
                log.warn("LLM推理失败: taskId={}, retryCount={}, error={}", 
                        task.getId(), task.getRetryCount(), errorMsg);
                // T032: WARN级别日志记录失败信息
                throw new RuntimeException("LLM推理失败: " + errorMsg);
            }
            
            log.info("LLM推理成功: taskId={}, tokens={}, responseTime={}ms", 
                    task.getId(), inferenceResponse.getTotalTokens(), 
                    inferenceResponse.getResponseTimeMs());
            
            // 3. 创建AI回复消息
            Message aiReply = new Message();
            aiReply.setSenderId(payload.getReceiverId()); // AI用户ID
            aiReply.setReceiverId(payload.getSenderId()); // 原始发送者ID
            aiReply.setType("text");
            aiReply.setContent(inferenceResponse.getContent());
            aiReply.setStatus("sent");
            aiReply.setIsDeleted(0);
            aiReply.setCreatedAt(LocalDateTime.now());
            aiReply.setUpdatedAt(LocalDateTime.now());
            
            messageRepository.save(aiReply);
            
            log.info("AI回复消息创建成功: messageId={}, content={}字符", 
                    aiReply.getId(), inferenceResponse.getContent().length());
            
            // 4. 更新会话表
            try {
                conversationMapper.upsertSender(
                        payload.getReceiverId(), // AI作为发送方
                        payload.getSenderId(), // 用户作为接收方
                        aiReply.getId(), 
                        aiReply.getCreatedAt());
                conversationMapper.upsertReceiver(
                        payload.getSenderId(), // 用户作为接收方
                        payload.getReceiverId(), // AI作为发送方
                        aiReply.getId(), 
                        aiReply.getCreatedAt());
            } catch (Exception e) {
                // 会话表更新失败不应阻断主流程
                log.warn("更新会话表失败: {}", e.getMessage());
            }
            
            log.info("SEND_MESSAGE任务执行完成: taskId={}, aiReplyId={}", 
                    task.getId(), aiReply.getId());
                    
        } catch (Exception e) {
            // T028: 捕获所有LLM调用异常
            // T029: 捕获异常后，状态更新和error_message记录在handleTaskFailure中完成
            // T030: 重试次数判断在handleTaskFailure中完成
            // T031: 重新调度逻辑在handleTaskFailure中完成
            // T032: 记录WARN级别日志
            log.warn("SEND_MESSAGE任务执行失败: taskId={}, retryCount={}, error={}", 
                    task.getId(), task.getRetryCount(), e.getMessage());
            // 重新抛出异常，由execute方法的异常处理逻辑处理
            throw e;
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
     * T044: 添加性能日志（LLM响应时间、任务等待时间）
     */
    private void recordExecutionLog(RobotTask task, LocalDateTime startTime, 
                                    LocalDateTime completedTime, boolean success, 
                                    String errorMessage) {
        try {
            // 计算执行延迟（任务等待时间）
            long delayMs = Duration.between(task.getScheduledAt(), startTime).toMillis();
            long durationMs = completedTime != null ? 
                             Duration.between(startTime, completedTime).toMillis() : 0;
            
            // T044: 添加性能日志
            if (success) {
                log.info("任务执行性能指标: taskId={}, executionDuration={}ms, scheduledDelay={}ms, attempt={}", 
                        task.getId(), durationMs, delayMs, task.getRetryCount() + 1);
                
                // 性能告警：执行时间过长
                if (durationMs > 5000) {
                    log.warn("任务执行时间过长: taskId={}, duration={}ms (>5s)", task.getId(), durationMs);
                }
                
                // 性能告警：调度延迟过大
                if (delayMs > 60000) {
                    log.warn("任务调度延迟过大: taskId={}, delay={}ms (>60s)", task.getId(), delayMs);
                }
            } else {
                log.warn("任务执行失败性能指标: taskId={}, executionDuration={}ms, scheduledDelay={}ms, attempt={}, error={}", 
                        task.getId(), durationMs, delayMs, task.getRetryCount() + 1, errorMessage);
            }
            
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
