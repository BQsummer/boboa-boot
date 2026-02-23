package com.bqsummer.service.robot;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.bqsummer.common.bo.ai.AiModelBo;
import com.bqsummer.common.dto.auth.UserProfile;
import com.bqsummer.common.dto.character.AiCharacterSetting;
import com.bqsummer.common.dto.im.Message;
import com.bqsummer.common.dto.memory.ConversationSummary;
import com.bqsummer.common.dto.memory.LongTermMemory;
import com.bqsummer.common.dto.robot.RobotTask;
import com.bqsummer.common.dto.robot.RobotTaskExecutionLog;
import com.bqsummer.common.dto.robot.SendMessagePayload;
import com.bqsummer.common.dto.robot.TaskStatus;
import com.bqsummer.configuration.Configs;
import com.bqsummer.mapper.AiCharacterSettingMapper;
import com.bqsummer.mapper.ConversationMapper;
import com.bqsummer.mapper.RobotTaskExecutionLogMapper;
import com.bqsummer.mapper.RobotTaskMapper;
import com.bqsummer.mapper.UserMapper;
import com.bqsummer.mapper.AiCharacterMapper;
import com.bqsummer.mapper.UserProfileMapper;
import com.bqsummer.mapper.memory.ConversationSummaryMapper;
import com.bqsummer.mapper.memory.LongTermMemoryMapper;
import com.bqsummer.common.vo.req.ai.InferenceRequest;
import com.bqsummer.common.vo.resp.ai.InferenceResponse;
import com.bqsummer.common.dto.auth.User;
import com.bqsummer.common.dto.character.AiCharacter;
import com.bqsummer.common.dto.ai.AiModel;
import com.bqsummer.common.dto.prompt.PromptTemplate;
import com.bqsummer.exception.RoutingException;
import com.bqsummer.repository.MessageRepository;
import com.bqsummer.service.ai.UnifiedInferenceService;
import com.bqsummer.service.ai.ModelRoutingService;
import com.bqsummer.service.prompt.PromptTemplateService;
import com.bqsummer.service.prompt.BeetlTemplateService;
import com.bqsummer.service.prompt.KnowledgeBaseTriggerService;
import com.bqsummer.service.prompt.PostProcessRuntimeService;
import com.bqsummer.util.InstanceIdGenerator;
import com.bqsummer.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 机器人任务执行器
 * 职责:
 * 1. 使用声明式领取机制抢占任务（PENDING -> RUNNING）
 *    - 基于 locked_by 字段标记所有权
 *    - 原子UPDATE操作保证互斥性，不受长时操作期间并发写影响
 * 2. 执行任务的具体行为（SEND_MESSAGE, SEND_VOICE, SEND_NOTIFICATION）
 * 3. 更新任务状态（RUNNING -> DONE / FAILED）
 *    - 验证 locked_by 所有权，防止跨实例误操作
 * 4. 记录执行日志到 robot_task_execution_log
 * 5. 处理失败重试逻辑
 *    - 失败重试时清空 locked_by，允许其他实例领取
 * 注意事项:
 * - 使用自我注入(self)来调用事务方法，确保@Transactional注解生效
 * - 直接调用类内部的@Transactional方法会绕过Spring代理，导致事务失效
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RobotTaskExecutor {
    private static final String HISTORY_PREFIX_MODE_ROLE = "role";
    private static final String HISTORY_PREFIX_MODE_SYSTEM = "system";
    private static final int DEFAULT_PROMPT_HISTORY_MESSAGE_LIMIT = 20;

    private final RobotTaskMapper robotTaskMapper;
    private final RobotTaskExecutionLogMapper executionLogMapper;
    private final UnifiedInferenceService inferenceService;
    private final ModelRoutingService modelRoutingService;
    private final PromptTemplateService promptTemplateService;
    private final BeetlTemplateService beetlTemplateService;
    private final KnowledgeBaseTriggerService knowledgeBaseTriggerService;
    private final MessageRepository messageRepository;
    private final ConversationMapper conversationMapper;
    private final UserMapper userMapper;
    private final AiCharacterMapper aiCharacterMapper;
    private final AiCharacterSettingMapper aiCharacterSettingMapper;
    private final UserProfileMapper userProfileMapper;
    private final ConversationSummaryMapper conversationSummaryMapper;
    private final LongTermMemoryMapper longTermMemoryMapper;
    private final Configs configs;
    @Autowired
    private PostProcessRuntimeService postProcessRuntimeService;

    // 自我注入：用于调用事务方法，确保通过代理调用
    private RobotTaskExecutor self;

    /**
     * 注入自身代理对象
     * 用于在类内部调用事务方法时，确保通过Spring代理调用
     */
    @Autowired
    public void setSelf(@Lazy RobotTaskExecutor self) {
        this.self = self;
    }

    /**
     * 异步执行任务
     * 注意：@Async方法不能使用@Transactional，事务在内部的同步方法中处理
     */
    @Async
    public CompletableFuture<Void> executeAsync(RobotTask task) {
        execute(task);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 执行任务的核心逻辑
     * 注意：此方法由@Async方法调用，@Transactional在此处不生效
     * 事务处理已拆分到各个子方法中
     *
     * @param task 待执行的任务
     */
    public void execute(RobotTask task) {
        LocalDateTime startTime = LocalDateTime.now();
        Long taskId = task.getId();

        log.info("开始执行任务: taskId={}, actionType={}, scheduledAt={}",
                taskId, task.getActionType(), task.getScheduledAt());

        // Step 1: 尝试抢占任务（通过self调用确保事务生效）
        boolean acquired = self.tryAcquireTask(task);
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

            // 更新任务状态为 DONE（通过self调用确保事务生效）
            self.updateTaskStatusToDone(task, completedTime);

            log.info("任务执行成功: taskId={}", taskId);

        } catch (Exception e) {
            log.error("任务执行失败: taskId=" + taskId, e);
            errorMessage = e.getMessage();
            completedTime = LocalDateTime.now();

            // 处理失败重试逻辑（通过self调用确保事务生效）
            self.handleTaskFailure(task, errorMessage);
        }

        // Step 3: 记录执行日志
        recordExecutionLog(task, startTime, completedTime, success, errorMessage);
    }

    /**
     * 更新任务状态为 DONE
     * 独立事务方法，确保事务生效
     * 所有权验证：
     * - 基于 locked_by 验证当前实例是否拥有该任务
     * - WHERE locked_by=当前实例ID 确保只有任务所有者能更新状态
     * - 防止跨实例误操作
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateTaskStatusToDone(RobotTask task, LocalDateTime completedTime) {
        String instanceId = InstanceIdGenerator.getInstanceId();

        UpdateWrapper<RobotTask> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", task.getId())
                    .eq("locked_by", instanceId)  // 验证所有权
                    .set("status", TaskStatus.DONE.name())
                    .set("completed_at", completedTime);

        int updated = robotTaskMapper.update(null, updateWrapper);

        if (updated == 1) {
            log.info("任务状态更新为DONE: taskId={}, instanceId={}, executionTime={}ms",
                    task.getId(), instanceId,
                    Duration.between(task.getStartedAt(), completedTime).toMillis());
        } else {
            log.error("任务状态更新失败，所有权验证失败: taskId={}, currentInstanceId={}",
                    task.getId(), instanceId);
        }
    }

    /**
     * 使用声明式领取机制尝试抢占任务
     * 独立事务方法，确保事务生效
     * 机制说明：
     * - 使用 locked_by 字段标记任务所有权
     * - 原子UPDATE操作：WHERE status='PENDING' SET locked_by=实例ID, status='RUNNING'
     * - 只有一个实例能成功领取（互斥性由WHERE条件保证）
     *
     * @return true 如果抢占成功，false 如果被其他实例抢占或状态已变更
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean tryAcquireTask(RobotTask task) {
        LocalDateTime now = LocalDateTime.now();
        String instanceId = InstanceIdGenerator.getInstanceId();

        UpdateWrapper<RobotTask> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", task.getId())
                    .eq("status", TaskStatus.PENDING.name())  // 只有PENDING任务可以被领取
                    .isNull("locked_by")  // 确保任务未被其他实例领取
                    .set("status", TaskStatus.RUNNING.name())
                    .set("locked_by", instanceId)  // 设置所有权
                    .set("started_at", now)
                    .set("heartbeat_at", now);

        int updated = robotTaskMapper.update(null, updateWrapper);

        if (updated == 1) {
            // 更新本地task对象状态
            task.setLockedBy(instanceId);
            task.setStatus(TaskStatus.RUNNING.name());
            task.setStartedAt(now);
            task.setHeartbeatAt(now);

            log.info("任务领取成功: taskId={}, instanceId={}, status=RUNNING",
                    task.getId(), instanceId);
            return true;
        }
// TODO  上面分支会不走
        log.info("任务领取失败: taskId={}, expectedStatus=PENDING, reason=已被其他实例领取或状态已变更",
                task.getId());
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
     * T028-T032: 添加完善的异常处理和重试逻辑
     * 注意：此方法需要保证事务性，通过self调用确保事务生效
     */
    private void executeSendMessage(RobotTask task) {
        self.executeSendMessageWithTransaction(task);
    }

    /**
     * 在事务中执行发送消息行为
     * 拆分为独立的事务方法，确保数据一致性
     *
     * US1: 使用ModelRoutingService动态选择模型
     * US2: 使用PromptTemplateService和BeetlTemplateService渲染提示词
     */
    @Transactional(rollbackFor = Exception.class)
    protected void executeSendMessageWithTransaction(RobotTask task) {
        log.info("开始执行SEND_MESSAGE任务: taskId={}, retryCount={}/{}, payload={}",
                task.getId(), task.getRetryCount(), task.getMaxRetryCount(),
                task.getActionPayload());

        try {
            // 1. 解析 action_payload JSON
            SendMessagePayload payload = JsonUtil.fromJson(
                    task.getActionPayload(), SendMessagePayload.class);

            if (payload == null) {
                throw new IllegalArgumentException("action_payload解析失败");
            }

            log.info("解析payload成功: messageId={}, senderId={}, receiverId={}, modelId={}",
                    payload.getMessageId(), payload.getSenderId(),
                    payload.getReceiverId(), payload.getModelId());

            // 2. US1: 使用路由策略选择模型
            AiModelBo selectedModel = null;
            try {
                InferenceRequest tempRequest = new InferenceRequest();
                tempRequest.setPrompt(payload.getContent());
                selectedModel = modelRoutingService.selectModelByDefault(tempRequest);
                log.info("使用默认路由策略选择模型: modelId={}, modelName={}",
                        selectedModel.getId(), selectedModel.getName());
            } catch (RoutingException e) {
                // 降级：使用payload中的modelId
                log.warn("默认路由策略失败: {}, 降级使用payload中的modelId={}",
                        e.getMessage(), payload.getModelId());
                if (payload.getModelId() != null) {
                    selectedModel = new AiModelBo();
                    selectedModel.setId(payload.getModelId());
                    log.info("降级使用payload中的模型: modelId={}", selectedModel.getId());
                } else {
                    throw new RuntimeException("无法选择可用模型: 默认策略失败且payload无modelId");
                }
            }

            // 3. US2: 使用模板渲染提示词
            String finalPrompt = payload.getContent();  // 默认使用原始消息
            Long aiCharacterId = resolveAiCharacterId(payload);
            PromptTemplate template = null;
            try {
                template = promptTemplateService.getLatestByCharId(aiCharacterId);
                if (template != null) {
                    // 模板存在，进行渲染
                    Map<String, Object> templateParams = buildTemplateParams(payload, aiCharacterId, template);
                    List<KnowledgeBaseTriggerService.TriggeredKnowledge> triggeredKnowledge =
                            knowledgeBaseTriggerService.resolveTriggeredKnowledge(
                                    template,
                                    payload.getSenderId(),
                                    payload.getReceiverId(),
                                    payload.getContent(),
                                    templateParams
                            );
                    String knowledgeBlock = buildKnowledgeBlock(triggeredKnowledge);
                    for (KnowledgeBaseTriggerService.TriggeredKnowledge item : triggeredKnowledge) {
                        if (item == null || !isNotBlank(item.content())) {
                            continue;
                        }
                        templateParams.put("knowledgeItem_" + item.id(), item.content());
                    }
                    templateParams.put("knowledge", knowledgeBlock);

                    try {
                        finalPrompt = beetlTemplateService.render(template.getContent(), templateParams);
                        if (isNotBlank(knowledgeBlock)) {
                            finalPrompt = finalPrompt + "\n\n[Knowledge Base]\n" + knowledgeBlock;
                        }
                        log.info("模板渲染成功: templateId={}, promptLength={}字符",
                                template.getId(), finalPrompt.length());
                    } catch (Exception renderEx) {
                        // 模板渲染失败，降级到原始消息
                        log.error("模板渲染失败，降级使用原始消息: templateId={}, error={}",
                                template.getId(), renderEx.getMessage());
                        finalPrompt = payload.getContent();
                    }
                } else {
                    // 模板不存在，使用原始消息
                    log.error("角色未配置模板，使用原始消息: charId={}", aiCharacterId);
                }
            } catch (Exception e) {
                // 查询模板异常，降级到原始消息
                log.warn("查询模板失败，使用原始消息: charId={}, error={}",
                        aiCharacterId, e.getMessage());
            }

            // 4. 调用LLM推理服务
            InferenceRequest inferenceRequest = new InferenceRequest();

            // 后面策略会选id
            // inferenceRequest.setModelId(selectedModel.getId());
            inferenceRequest.setPrompt(finalPrompt);

            log.debug("调用LLM推理服务: modelId={}, prompt={}",
                    selectedModel.getId(), finalPrompt);

            InferenceResponse inferenceResponse = inferenceService.chat(inferenceRequest);

            // 验证LLM响应
            if (inferenceResponse == null || !Boolean.TRUE.equals(inferenceResponse.getSuccess())) {
                String errorMsg = inferenceResponse != null ? inferenceResponse.getErrorMessage() : "LLM响应为空";
                log.warn("LLM推理失败: taskId={}, retryCount={}, error={}",
                        task.getId(), task.getRetryCount(), errorMsg);
                throw new RuntimeException("LLM推理失败: " + errorMsg);
            }

            log.info("LLM推理成功: taskId={}, tokens={}, responseTime={}ms",
                    task.getId(), inferenceResponse.getTotalTokens(),
                    inferenceResponse.getResponseTimeMs());

            String finalContent = inferenceResponse.getContent();
            try {
                finalContent = postProcessRuntimeService.process(finalContent, template);
            } catch (Exception e) {
                log.warn("post process failed: taskId={}, error={}", task.getId(), e.getMessage());
            }

            // 5. 创建AI回复消息
            Message aiReply = new Message();
            aiReply.setSenderId(payload.getReceiverId()); // AI用户ID
            aiReply.setReceiverId(payload.getSenderId()); // 原始发送者ID
            aiReply.setType("text");
            aiReply.setContent(finalContent);
            aiReply.setModel(inferenceResponse.getModelName());
            aiReply.setProvider(inferenceResponse.getProvider());
            aiReply.setCost(inferenceResponse.getCost());
            aiReply.setStatus("sent");
            aiReply.setIsDeleted(false);
            aiReply.setIsInContext(true);
            aiReply.setCreatedAt(LocalDateTime.now());
            aiReply.setUpdatedAt(LocalDateTime.now());

            messageRepository.save(aiReply);

            log.info("AI回复消息创建成功: messageId={}, content={}字符",
                    aiReply.getId(), finalContent.length());

            // 6. 更新会话表
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
            // 捕获所有异常，由execute方法的异常处理逻辑处理
            log.warn("SEND_MESSAGE任务执行失败: taskId={}, retryCount={}, error={}",
                    task.getId(), task.getRetryCount(), e.getMessage());
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
     * 独立事务方法，确保事务生效
     * 所有权释放：
     * - 失败重试时清空 locked_by（SET locked_by=NULL），释放任务所有权
     * - 状态变更为 PENDING，允许任意实例重新领取
     * - 达到最大重试次数时，标记为 FAILED 并清空 locked_by
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleTaskFailure(RobotTask task, String errorMessage) {
        int retryCount = task.getRetryCount() + 1;
        int maxRetryCount = task.getMaxRetryCount();
        String previousLockedBy = task.getLockedBy();

        if (retryCount >= maxRetryCount) {
            // 超过最大重试次数，标记为 FAILED
            UpdateWrapper<RobotTask> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", task.getId())
                        .set("status", TaskStatus.FAILED.name())
                        .set("retry_count", retryCount)
                        .set("completed_at", LocalDateTime.now())
                        .set("error_message", errorMessage)
                        .set("locked_by", null);  // 清空所有权
            robotTaskMapper.update(null, updateWrapper);

            log.warn("任务失败且超过最大重试次数: taskId={}, retryCount={}, previousLockedBy={}",
                    task.getId(), retryCount, previousLockedBy);
        } else {
            // 计算下次执行时间
            String[] retryDelay = configs.getRetryDelay().split(",");
            int delaySeconds = retryCount > retryDelay.length ?
                               Integer.parseInt(retryDelay[retryDelay.length - 1]) :
                               Integer.parseInt(retryDelay[retryCount - 1]);
            LocalDateTime nextExecuteAt = LocalDateTime.now().plusSeconds(delaySeconds);

            // 重置状态为 PENDING，增加重试计数，释放所有权
            UpdateWrapper<RobotTask> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", task.getId())
                        .set("status", TaskStatus.PENDING.name())
                        .set("retry_count", retryCount)
                        .set("scheduled_at", nextExecuteAt)
                        .set("error_message", errorMessage)
                        .set("locked_by", null);  // 清空所有权，允许其他实例重试
            robotTaskMapper.update(null, updateWrapper);

            log.warn("任务失败重试，释放所有权: taskId={}, previousLockedBy={}, retryCount={}, nextScheduledAt={}",
                    task.getId(), previousLockedBy, retryCount, nextExecuteAt);
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
                if (durationMs > 30000) {
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

    /**
     * 构建模板渲染参数
     * @param payload 消息发送载荷
     * @return 模板参数Map，包含userName、userId、content、receiverId、characterName
     */
    private Map<String, Object> buildTemplateParams(SendMessagePayload payload,
                                                    Long aiCharacterId,
                                                    PromptTemplate template) {
        Map<String, Object> params = new HashMap<>();

        try {
            params.put("content", payload.getContent());

            User user = null;
            AiCharacter character = null;
            AiCharacterSetting defaultSetting = null;
            AiCharacterSetting customSetting = null;
            UserProfile userProfile = null;

            try {
                user = userMapper.findById(payload.getSenderId());
            } catch (Exception e) {
                log.warn("query user failed: userId={}, error={}", payload.getSenderId(), e.getMessage());
            }

            try {
                character = aiCharacterMapper.findById(aiCharacterId);
            } catch (Exception e) {
                log.warn("query ai character failed: charId={}, error={}", aiCharacterId, e.getMessage());
            }

            try {
                defaultSetting = aiCharacterSettingMapper.findDefaultByCharacter(aiCharacterId);
            } catch (Exception e) {
                log.warn("query default ai character setting failed: charId={}, error={}", aiCharacterId, e.getMessage());
            }

            try {
                customSetting = aiCharacterSettingMapper.findByUserAndCharacter(payload.getSenderId(), aiCharacterId);
            } catch (Exception e) {
                log.warn("query custom ai character setting failed: userId={}, charId={}, error={}",
                        payload.getSenderId(), aiCharacterId, e.getMessage());
            }

            try {
                userProfile = userProfileMapper.selectByUserId(payload.getSenderId());
            } catch (Exception e) {
                log.warn("query user profile failed: userId={}, error={}", payload.getSenderId(), e.getMessage());
            }

            String userName = resolveUserName(user);
            String characterName = resolveCharacterName(character, defaultSetting, customSetting);
            boolean useSystemHistoryPrefix = shouldUseSystemHistoryPrefix(template);
            String historyUserPrefix = useSystemHistoryPrefix ? "User" : userName;
            String historyAssistantPrefix = useSystemHistoryPrefix ? "Assistant" : characterName;

            params.put("user", userName);
            params.put("char", characterName);
            params.put("charDetail", buildCharDetailString(defaultSetting, customSetting));
            params.put("charStatus", "");
            params.put("userDetail", buildUserDetailString(userProfile));
            params.put("history", buildHistoryString(
                    payload,
                    aiCharacterId,
                    template,
                    useSystemHistoryPrefix,
                    historyUserPrefix,
                    historyAssistantPrefix));

            params.put("userName", userName);
            params.put("userId", payload.getSenderId());
            params.put("receiverId", payload.getReceiverId());
            params.put("characterName", characterName);


        } catch (Exception e) {
            log.error("build template params error: {}", e.getMessage(), e);
        }

        return params;
    }

    private String resolveUserName(User user) {
        if (user != null && isNotBlank(user.getNickName())) {
            return user.getNickName();
        }
        if (user != null && isNotBlank(user.getUsername())) {
            return user.getUsername();
        }
        return "用户";
    }

    private String resolveCharacterName(AiCharacter character,
                                        AiCharacterSetting defaultSetting,
                                        AiCharacterSetting customSetting) {
        if (customSetting != null && isNotBlank(customSetting.getName())) {
            return customSetting.getName();
        }
        if (character != null && isNotBlank(character.getName())) {
            return character.getName();
        }
        if (defaultSetting != null && isNotBlank(defaultSetting.getName())) {
            return defaultSetting.getName();
        }
        return "AI助手";
    }

    private String buildCharDetailString(AiCharacterSetting defaultSetting, AiCharacterSetting customSetting) {
        Object memorialDay = defaultSetting != null ? defaultSetting.getMemorialDay() : null;
        if (customSetting != null && customSetting.getMemorialDay() != null) {
            memorialDay = customSetting.getMemorialDay();
        }

        String relationship = defaultSetting != null ? defaultSetting.getRelationship() : null;
        if (customSetting != null && isNotBlank(customSetting.getRelationship())) {
            relationship = customSetting.getRelationship();
        }

        String emotion = defaultSetting != null ? defaultSetting.getEmotion() : null;
        if (customSetting != null && isNotBlank(customSetting.getEmotion())) {
            emotion = customSetting.getEmotion();
        }

        String background = defaultSetting != null ? defaultSetting.getBackground() : null;
        if (customSetting != null && isNotBlank(customSetting.getBackground())) {
            background = customSetting.getBackground();
        }

        List<String> parts = new ArrayList<>();
        if (memorialDay != null) {
            parts.add("纪念日为" + memorialDay);
        }
        if (isNotBlank(relationship)) {
            parts.add("relationship=" + relationship);
        }
        if (isNotBlank(emotion)) {
            parts.add("emotion=" + emotion);
        }
        if (isNotBlank(background)) {
            parts.add("background=" + background);
        }
        return String.join(", ", parts);
    }

    private String buildUserDetailString(UserProfile userProfile) {
        if (userProfile == null) {
            return "";
        }

        List<String> parts = new ArrayList<>();
        if (isNotBlank(userProfile.getGender())) {
            parts.add("gender=" + userProfile.getGender());
        }
        if (userProfile.getBirthday() != null) {
            parts.add("birthday=" + userProfile.getBirthday());
        }
        if (userProfile.getHeightCm() != null) {
            parts.add("height=" + userProfile.getHeightCm() + "cm");
        }
        if (isNotBlank(userProfile.getMbti())) {
            parts.add("mbti=" + userProfile.getMbti());
        }
        if (isNotBlank(userProfile.getOccupation())) {
            parts.add("occupation=" + userProfile.getOccupation());
        }
        if (isNotBlank(userProfile.getInterests())) {
            parts.add("interests=" + userProfile.getInterests());
        }
        if (isNotBlank(userProfile.getDesc())) {
            parts.add("desc=" + userProfile.getDesc());
        }
        return String.join(", ", parts);
    }

    private String buildHistoryString(SendMessagePayload payload,
                                      Long aiCharacterId,
                                      PromptTemplate template,
                                      boolean useSystemHistoryPrefix,
                                      String historyUserPrefix,
                                      String historyAssistantPrefix) {
        List<Map<String, Object>> history = new ArrayList<>();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        boolean includeHistoryTime = shouldIncludeHistoryTime(template);

        // 缓存用户名和角色名
        Map<Long, String> userNameCache = new HashMap<>();
        userNameCache.put(payload.getSenderId(), historyUserPrefix);
        userNameCache.put(payload.getReceiverId(), historyAssistantPrefix);

        try {
            Integer configuredHistoryLimit = configs.getPromptHistoryMessageLimit();
            int historyMessageLimit = configuredHistoryLimit != null && configuredHistoryLimit > 0
                    ? configuredHistoryLimit
                    : DEFAULT_PROMPT_HISTORY_MESSAGE_LIMIT;
            List<Message> messages = messageRepository.findDialogHistoryForPrompt(
                    payload.getSenderId(), payload.getReceiverId(), null, historyMessageLimit);
            for (Message message : messages) {
                Map<String, Object> item = new HashMap<>();
                item.put("type", "message");
                item.put("time", message.getCreatedAt());
                item.put("message", message);
                history.add(item);
            }
        } catch (Exception e) {
            log.warn("query message history failed: userId={}, peerId={}, error={}",
                    payload.getSenderId(), payload.getReceiverId(), e.getMessage());
        }

        try {
            List<ConversationSummary> summaries = conversationSummaryMapper.findLatestSummaries(
                    payload.getSenderId(), aiCharacterId, 10);
            for (ConversationSummary summary : summaries) {
                Map<String, Object> item = new HashMap<>();
                item.put("type", "conversation_summary");
                item.put("time", summary.getCreatedAt());
                item.put("conversation_summary", summary);
                history.add(item);
            }
        } catch (Exception e) {
            log.warn("query conversation summary history failed: userId={}, charId={}, error={}",
                    payload.getSenderId(), aiCharacterId, e.getMessage());
        }

        try {
            QueryWrapper<LongTermMemory> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("user_id", payload.getSenderId())
                    .eq("ai_character_id", aiCharacterId)
                    .orderByDesc("created_at")
                    .last("LIMIT 10");
            List<LongTermMemory> memories = longTermMemoryMapper.selectList(queryWrapper);
            for (LongTermMemory memory : memories) {
                Map<String, Object> item = new HashMap<>();
                item.put("type", "long_term_memory");
                item.put("time", memory.getCreatedAt());
                item.put("long_term_memory", memory);
                history.add(item);
            }
        } catch (Exception e) {
            log.warn("query long term memory history failed: userId={}, charId={}, error={}",
                    payload.getSenderId(), aiCharacterId, e.getMessage());
        }

        history.sort(Comparator.comparing(
                item -> (LocalDateTime) item.get("time"),
                Comparator.nullsLast(Comparator.reverseOrder())));

        List<String> parts = new ArrayList<>();
        for (Map<String, Object> item : history) {
            String type = (String) item.get("type");
            LocalDateTime time = (LocalDateTime) item.get("time");
            String timeText = time != null ? time.format(timeFormatter) : "";
            String timePrefix = includeHistoryTime && isNotBlank(timeText) ? "[" + timeText + "] " : "";
            if ("message".equals(type)) {
                Message message = (Message) item.get("message");
                if (message != null) {
                    // 获取发送者名称
                    String senderName = userNameCache.get(message.getSenderId());
                    if (senderName == null) {
                        if (useSystemHistoryPrefix) {
                            senderName = payload.getSenderId().equals(message.getSenderId())
                                    ? "User"
                                    : "Assistant";
                        } else {
                            try {
                                User sender = userMapper.findById(message.getSenderId());
                                senderName = resolveUserName(sender);
                                userNameCache.put(message.getSenderId(), senderName);
                            } catch (Exception e) {
                                senderName = "用户";
                            }
                        }
                    }
                    parts.add(timePrefix + senderName + ": " + message.getContent());
                }
            } else if ("conversation_summary".equals(type)) {
                ConversationSummary summary = (ConversationSummary) item.get("conversation_summary");
                parts.add(timePrefix + "会话总结: "
                        + (summary != null ? JsonUtil.toJson(summary.getSummaryJson()) : ""));
            } else if ("long_term_memory".equals(type)) {
                LongTermMemory memory = (LongTermMemory) item.get("long_term_memory");
                parts.add(timePrefix + "长期记忆: " + (memory != null ? memory.getText() : ""));
            }
        }
        return String.join("\n", parts);
    }

    private boolean shouldUseSystemHistoryPrefix(PromptTemplate template) {
        String prefixMode = resolveHistoryPrefixMode(template);
        return HISTORY_PREFIX_MODE_SYSTEM.equals(prefixMode);
    }

    private String resolveHistoryPrefixMode(PromptTemplate template) {
        if (template == null || template.getParamSchema() == null) {
            return HISTORY_PREFIX_MODE_ROLE;
        }

        Map<String, Object> paramSchema = template.getParamSchema();
        String topLevelMode = parseHistoryPrefixMode(paramSchema.get("historyPrefixMode"));
        if (topLevelMode != null) {
            return topLevelMode;
        }

        Object historyConfig = paramSchema.get("history");
        if (historyConfig instanceof Map<?, ?> historyMap) {
            String nestedMode = parseHistoryPrefixMode(historyMap.get("prefixMode"));
            if (nestedMode != null) {
                return nestedMode;
            }
        }

        return HISTORY_PREFIX_MODE_ROLE;
    }

    private String parseHistoryPrefixMode(Object value) {
        if (!(value instanceof String stringValue)) {
            return null;
        }

        String normalized = stringValue.trim().toLowerCase();
        if (HISTORY_PREFIX_MODE_ROLE.equals(normalized) || HISTORY_PREFIX_MODE_SYSTEM.equals(normalized)) {
            return normalized;
        }

        return null;
    }

    private String buildKnowledgeBlock(List<KnowledgeBaseTriggerService.TriggeredKnowledge> knowledgeList) {
        if (knowledgeList == null || knowledgeList.isEmpty()) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        for (KnowledgeBaseTriggerService.TriggeredKnowledge item : knowledgeList) {
            if (item == null || !isNotBlank(item.content())) {
                continue;
            }
            if (isNotBlank(item.title())) {
                parts.add("[" + item.title() + "] " + item.content());
            } else {
                parts.add(item.content());
            }
        }
        return String.join("\n", parts);
    }

    private boolean shouldIncludeHistoryTime(PromptTemplate template) {
        if (template == null || template.getParamSchema() == null) {
            return true;
        }

        Map<String, Object> paramSchema = template.getParamSchema();
        Boolean topLevelValue = parseBooleanValue(paramSchema.get("historyShowTime"));
        if (topLevelValue != null) {
            return topLevelValue;
        }

        Object historyConfig = paramSchema.get("history");
        if (historyConfig instanceof Map<?, ?> historyMap) {
            Boolean nestedValue = parseBooleanValue(historyMap.get("showTime"));
            if (nestedValue != null) {
                return nestedValue;
            }
        }

        return true;
    }

    private Boolean parseBooleanValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean boolValue) {
            return boolValue;
        }
        if (value instanceof Number numberValue) {
            return numberValue.intValue() != 0;
        }
        if (value instanceof String stringValue) {
            String normalized = stringValue.trim().toLowerCase();
            if ("true".equals(normalized) || "1".equals(normalized) || "yes".equals(normalized)) {
                return true;
            }
            if ("false".equals(normalized) || "0".equals(normalized) || "no".equals(normalized)) {
                return false;
            }
        }
        return null;
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private Long resolveAiCharacterId(SendMessagePayload payload) {
        if (payload.getAiCharacterId() != null) {
            return payload.getAiCharacterId();
        }

        AiCharacter character = aiCharacterMapper.findByAssociatedUserId(payload.getReceiverId());
        if (character != null) {
            return character.getId();
        }

        return payload.getReceiverId();
    }
}



