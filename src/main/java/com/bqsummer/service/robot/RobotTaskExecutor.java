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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 鏈哄櫒浜轰换鍔℃墽琛屽櫒
 * 鑱岃矗:
 * 1. 浣跨敤澹版槑寮忛鍙栨満鍒舵姠鍗犱换鍔★紙PENDING 鈫?RUNNING锛?
 *    - 鍩轰簬 locked_by 瀛楁鏍囪鎵€鏈夋潈
 *    - 鍘熷瓙UPDATE鎿嶄綔淇濊瘉浜掓枼鎬э紝涓嶅彈闀挎椂鎿嶄綔鏈熼棿骞跺彂鍐欏奖鍝?
 * 2. 鎵ц浠诲姟鐨勫叿浣撹涓猴紙SEND_MESSAGE, SEND_VOICE, SEND_NOTIFICATION锛?
 * 3. 鏇存柊浠诲姟鐘舵€侊紙RUNNING 鈫?DONE / FAILED锛?
 *    - 楠岃瘉 locked_by 鎵€鏈夋潈锛岄槻姝㈣法瀹炰緥璇搷浣?
 * 4. 璁板綍鎵ц鏃ュ織鍒?robot_task_execution_log
 * 5. 澶勭悊澶辫触閲嶈瘯閫昏緫
 *    - 澶辫触閲嶈瘯鏃舵竻绌?locked_by锛屽厑璁稿叾浠栧疄渚嬮鍙?
 * 娉ㄦ剰浜嬮」:
 * - 浣跨敤鑷垜娉ㄥ叆(self)鏉ヨ皟鐢ㄤ簨鍔℃柟娉曪紝纭繚@Transactional娉ㄨВ鐢熸晥
 * - 鐩存帴璋冪敤绫诲唴閮ㄧ殑@Transactional鏂规硶浼氱粫杩嘢pring浠ｇ悊锛屽鑷翠簨鍔″け鏁?
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RobotTaskExecutor {

    private final RobotTaskMapper robotTaskMapper;
    private final RobotTaskExecutionLogMapper executionLogMapper;
    private final UnifiedInferenceService inferenceService;
    private final ModelRoutingService modelRoutingService;
    private final PromptTemplateService promptTemplateService;
    private final BeetlTemplateService beetlTemplateService;
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

    // 鑷垜娉ㄥ叆锛氱敤浜庤皟鐢ㄤ簨鍔℃柟娉曪紝纭繚閫氳繃浠ｇ悊璋冪敤
    private RobotTaskExecutor self;

    /**
     * 娉ㄥ叆鑷韩浠ｇ悊瀵硅薄
     * 鐢ㄤ簬鍦ㄧ被鍐呴儴璋冪敤浜嬪姟鏂规硶鏃讹紝纭繚閫氳繃Spring浠ｇ悊璋冪敤
     */
    @Autowired
    public void setSelf(@Lazy RobotTaskExecutor self) {
        this.self = self;
    }

    /**
     * 寮傛鎵ц浠诲姟
     * 娉ㄦ剰锛欯Async鏂规硶涓嶈兘浣跨敤@Transactional锛屼簨鍔″湪鍐呴儴鐨勫悓姝ユ柟娉曚腑澶勭悊
     */
    @Async
    public CompletableFuture<Void> executeAsync(RobotTask task) {
        execute(task);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 鎵ц浠诲姟鐨勬牳蹇冮€昏緫
     * 娉ㄦ剰锛氭鏂规硶鐢盄Async鏂规硶璋冪敤锛孈Transactional鍦ㄦ澶勪笉鐢熸晥
     * 浜嬪姟澶勭悊宸叉媶鍒嗗埌鍚勪釜瀛愭柟娉曚腑
     *
     * @param task 寰呮墽琛岀殑浠诲姟
     */
    public void execute(RobotTask task) {
        LocalDateTime startTime = LocalDateTime.now();
        Long taskId = task.getId();

        log.info("寮€濮嬫墽琛屼换鍔? taskId={}, actionType={}, scheduledAt={}",
                taskId, task.getActionType(), task.getScheduledAt());

        // Step 1: 灏濊瘯鎶㈠崰浠诲姟锛堥€氳繃self璋冪敤纭繚浜嬪姟鐢熸晥锛?
        boolean acquired = self.tryAcquireTask(task);
        if (!acquired) {
            log.debug("浠诲姟宸茶鍏朵粬瀹炰緥鎶㈠崰锛岃烦杩? taskId={}", taskId);
            return;
        }

        // Step 2: 鎵ц浠诲姟琛屼负
        boolean success = false;
        String errorMessage = null;
        LocalDateTime completedTime = null;

        try {
            executeAction(task);
            success = true;
            completedTime = LocalDateTime.now();

            // 鏇存柊浠诲姟鐘舵€佷负 DONE锛堥€氳繃self璋冪敤纭繚浜嬪姟鐢熸晥锛?
            self.updateTaskStatusToDone(task, completedTime);

            log.info("浠诲姟鎵ц鎴愬姛: taskId={}", taskId);

        } catch (Exception e) {
            log.error("浠诲姟鎵ц澶辫触: taskId=" + taskId, e);
            errorMessage = e.getMessage();
            completedTime = LocalDateTime.now();

            // 澶勭悊澶辫触閲嶈瘯閫昏緫锛堥€氳繃self璋冪敤纭繚浜嬪姟鐢熸晥锛?
            self.handleTaskFailure(task, errorMessage);
        }

        // Step 3: 璁板綍鎵ц鏃ュ織
        recordExecutionLog(task, startTime, completedTime, success, errorMessage);
    }

    /**
     * 鏇存柊浠诲姟鐘舵€佷负 DONE
     * 鐙珛浜嬪姟鏂规硶锛岀‘淇濅簨鍔＄敓鏁?
     * 鎵€鏈夋潈楠岃瘉锛?
     * - 鍩轰簬 locked_by 楠岃瘉褰撳墠瀹炰緥鏄惁鎷ユ湁璇ヤ换鍔?
     * - WHERE locked_by=褰撳墠瀹炰緥ID 纭繚鍙湁浠诲姟鎵€鏈夎€呰兘鏇存柊鐘舵€?
     * - 闃叉璺ㄥ疄渚嬭鎿嶄綔
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateTaskStatusToDone(RobotTask task, LocalDateTime completedTime) {
        String instanceId = InstanceIdGenerator.getInstanceId();

        UpdateWrapper<RobotTask> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", task.getId())
                    .eq("locked_by", instanceId)  // 楠岃瘉鎵€鏈夋潈
                    .set("status", TaskStatus.DONE.name())
                    .set("completed_at", completedTime);

        int updated = robotTaskMapper.update(null, updateWrapper);

        if (updated == 1) {
            log.info("浠诲姟鐘舵€佹洿鏂颁负DONE: taskId={}, instanceId={}, executionTime={}ms",
                    task.getId(), instanceId,
                    Duration.between(task.getStartedAt(), completedTime).toMillis());
        } else {
            log.error("浠诲姟鐘舵€佹洿鏂板け璐ワ紝鎵€鏈夋潈楠岃瘉澶辫触: taskId={}, currentInstanceId={}",
                    task.getId(), instanceId);
        }
    }

    /**
     * 浣跨敤澹版槑寮忛鍙栨満鍒跺皾璇曟姠鍗犱换鍔?
     * 鐙珛浜嬪姟鏂规硶锛岀‘淇濅簨鍔＄敓鏁?
     * 鏈哄埗璇存槑锛?
     * - 浣跨敤 locked_by 瀛楁鏍囪浠诲姟鎵€鏈夋潈
     * - 鍘熷瓙UPDATE鎿嶄綔锛歐HERE status='PENDING' SET locked_by=瀹炰緥ID, status='RUNNING'
     * - 鍙湁涓€涓疄渚嬭兘鎴愬姛棰嗗彇锛堜簰鏂ユ€х敱WHERE鏉′欢淇濊瘉锛?
     *
     * @return true 濡傛灉鎶㈠崰鎴愬姛锛宖alse 濡傛灉琚叾浠栧疄渚嬫姠鍗犳垨鐘舵€佸凡鍙樻洿
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean tryAcquireTask(RobotTask task) {
        LocalDateTime now = LocalDateTime.now();
        String instanceId = InstanceIdGenerator.getInstanceId();

        UpdateWrapper<RobotTask> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", task.getId())
                    .eq("status", TaskStatus.PENDING.name())  // 鍙湁PENDING浠诲姟鍙互琚鍙?
                    .isNull("locked_by")  // 纭繚浠诲姟鏈鍏朵粬瀹炰緥棰嗗彇
                    .set("status", TaskStatus.RUNNING.name())
                    .set("locked_by", instanceId)  // 璁剧疆鎵€鏈夋潈
                    .set("started_at", now)
                    .set("heartbeat_at", now);

        int updated = robotTaskMapper.update(null, updateWrapper);

        if (updated == 1) {
            // 鏇存柊鏈湴task瀵硅薄鐘舵€?
            task.setLockedBy(instanceId);
            task.setStatus(TaskStatus.RUNNING.name());
            task.setStartedAt(now);
            task.setHeartbeatAt(now);

            log.info("浠诲姟棰嗗彇鎴愬姛: taskId={}, instanceId={}, status=RUNNING",
                    task.getId(), instanceId);
            return true;
        }

        log.info("浠诲姟棰嗗彇澶辫触: taskId={}, expectedStatus=PENDING, reason=宸茶鍏朵粬瀹炰緥棰嗗彇鎴栫姸鎬佸凡鍙樻洿",
                task.getId());
        return false;
    }

    /**
     * 鎵ц浠诲姟鐨勫叿浣撹涓?
     */
    private void executeAction(RobotTask task) {
        String actionType = task.getActionType();
        String payload = task.getActionPayload();

        log.debug("鎵ц琛屼负: taskId={}, actionType={}, payload={}",
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
                throw new IllegalArgumentException("涓嶆敮鎸佺殑琛屼负绫诲瀷: " + actionType);
        }
    }

    /**
     * 鎵ц鍙戦€佹秷鎭涓?
     * 1. 瑙ｆ瀽 action_payload JSON
     * 2. 璋冪敤LLM鎺ㄧ悊鏈嶅姟
     * 3. 鍒涘缓AI鍥炲娑堟伅
     * 4. 鏇存柊浼氳瘽琛?
     * T028-T032: 娣诲姞瀹屽杽鐨勫紓甯稿鐞嗗拰閲嶈瘯閫昏緫
     * 娉ㄦ剰锛氭鏂规硶闇€瑕佷繚璇佷簨鍔℃€э紝閫氳繃self璋冪敤纭繚浜嬪姟鐢熸晥
     */
    private void executeSendMessage(RobotTask task) {
        self.executeSendMessageWithTransaction(task);
    }

    /**
     * 鍦ㄤ簨鍔′腑鎵ц鍙戦€佹秷鎭涓?
     * 鎷嗗垎涓虹嫭绔嬬殑浜嬪姟鏂规硶锛岀‘淇濇暟鎹竴鑷存€?
     *
     * US1: 浣跨敤ModelRoutingService鍔ㄦ€侀€夋嫨妯″瀷
     * US2: 浣跨敤PromptTemplateService鍜孊eetlTemplateService娓叉煋鎻愮ず璇?
     */
    @Transactional(rollbackFor = Exception.class)
    protected void executeSendMessageWithTransaction(RobotTask task) {
        log.info("寮€濮嬫墽琛孲END_MESSAGE浠诲姟: taskId={}, retryCount={}/{}, payload={}",
                task.getId(), task.getRetryCount(), task.getMaxRetryCount(),
                task.getActionPayload());

        try {
            // 1. 瑙ｆ瀽 action_payload JSON
            SendMessagePayload payload = JsonUtil.fromJson(
                    task.getActionPayload(), SendMessagePayload.class);

            if (payload == null) {
                throw new IllegalArgumentException("action_payload瑙ｆ瀽澶辫触");
            }

            log.info("瑙ｆ瀽payload鎴愬姛: messageId={}, senderId={}, receiverId={}, modelId={}",
                    payload.getMessageId(), payload.getSenderId(),
                    payload.getReceiverId(), payload.getModelId());

            // 2. US1: 浣跨敤璺敱绛栫暐閫夋嫨妯″瀷
            AiModelBo selectedModel = null;
            try {
                InferenceRequest tempRequest = new InferenceRequest();
                tempRequest.setPrompt(payload.getContent());
                selectedModel = modelRoutingService.selectModelByDefault(tempRequest);
                log.info("浣跨敤榛樿璺敱绛栫暐閫夋嫨妯″瀷: modelId={}, modelName={}",
                        selectedModel.getId(), selectedModel.getName());
            } catch (RoutingException e) {
                // 闄嶇骇锛氫娇鐢╬ayload涓殑modelId
                log.warn("榛樿璺敱绛栫暐澶辫触: {}, 闄嶇骇浣跨敤payload涓殑modelId={}",
                        e.getMessage(), payload.getModelId());
                if (payload.getModelId() != null) {
                    selectedModel = new AiModelBo();
                    selectedModel.setId(payload.getModelId());
                    log.info("闄嶇骇浣跨敤payload涓殑妯″瀷: modelId={}", selectedModel.getId());
                } else {
                    throw new RuntimeException("鏃犳硶閫夋嫨鍙敤妯″瀷: 榛樿绛栫暐澶辫触涓攑ayload鏃爉odelId");
                }
            }

            // 3. US2: 浣跨敤妯℃澘娓叉煋鎻愮ず璇?
            String finalPrompt = payload.getContent();  // 榛樿浣跨敤鍘熷娑堟伅
            Long aiCharacterId = resolveAiCharacterId(payload);
            PromptTemplate template = null;
            try {
                template = promptTemplateService.getLatestByCharId(aiCharacterId);
                if (template != null) {
                    // 妯℃澘瀛樺湪锛岃繘琛屾覆鏌?
                    Map<String, Object> templateParams = buildTemplateParams(payload, aiCharacterId);
                    try {
                        finalPrompt = beetlTemplateService.render(template.getContent(), templateParams);
                        log.info("妯℃澘娓叉煋鎴愬姛: templateId={}, promptLength={}瀛楃",
                                template.getId(), finalPrompt.length());
                    } catch (Exception renderEx) {
                        // 妯℃澘娓叉煋澶辫触锛岄檷绾у埌鍘熷娑堟伅
                        log.error("妯℃澘娓叉煋澶辫触锛岄檷绾т娇鐢ㄥ師濮嬫秷鎭? templateId={}, error={}",
                                template.getId(), renderEx.getMessage());
                        finalPrompt = payload.getContent();
                    }
                } else {
                    // 妯℃澘涓嶅瓨鍦紝浣跨敤鍘熷娑堟伅
                    log.error("瑙掕壊鏈厤缃ā鏉匡紝浣跨敤鍘熷娑堟伅: charId={}", aiCharacterId);
                }
            } catch (Exception e) {
                // 鏌ヨ妯℃澘寮傚父锛岄檷绾у埌鍘熷娑堟伅
                log.warn("鏌ヨ妯℃澘澶辫触锛屼娇鐢ㄥ師濮嬫秷鎭? charId={}, error={}",
                        aiCharacterId, e.getMessage());
            }

            // 4. 璋冪敤LLM鎺ㄧ悊鏈嶅姟
            InferenceRequest inferenceRequest = new InferenceRequest();
            inferenceRequest.setModelId(selectedModel.getId());
            inferenceRequest.setPrompt(finalPrompt);
            inferenceRequest.setTemperature(0.7);  // TODO: US3灏嗕粠妯℃澘閰嶇疆璇诲彇
            inferenceRequest.setMaxTokens(2000);   // TODO: US3灏嗕粠妯℃澘閰嶇疆璇诲彇

            log.info("璋冪敤LLM鎺ㄧ悊鏈嶅姟: modelId={}, prompt={}",
                    selectedModel.getId(), finalPrompt);

            InferenceResponse inferenceResponse = inferenceService.chat(inferenceRequest);

            // 楠岃瘉LLM鍝嶅簲
            if (inferenceResponse == null || !Boolean.TRUE.equals(inferenceResponse.getSuccess())) {
                String errorMsg = inferenceResponse != null ? inferenceResponse.getErrorMessage() : "LLM鍝嶅簲涓虹┖";
                log.warn("LLM鎺ㄧ悊澶辫触: taskId={}, retryCount={}, error={}",
                        task.getId(), task.getRetryCount(), errorMsg);
                throw new RuntimeException("LLM鎺ㄧ悊澶辫触: " + errorMsg);
            }

            log.info("LLM鎺ㄧ悊鎴愬姛: taskId={}, tokens={}, responseTime={}ms",
                    task.getId(), inferenceResponse.getTotalTokens(),
                    inferenceResponse.getResponseTimeMs());

            String finalContent = inferenceResponse.getContent();
            try {
                finalContent = postProcessRuntimeService.process(finalContent, template);
            } catch (Exception e) {
                log.warn("post process failed: taskId={}, error={}", task.getId(), e.getMessage());
            }

            // 5. 鍒涘缓AI鍥炲娑堟伅
            Message aiReply = new Message();
            aiReply.setSenderId(payload.getReceiverId()); // AI鐢ㄦ埛ID
            aiReply.setReceiverId(payload.getSenderId()); // 鍘熷鍙戦€佽€匢D
            aiReply.setType("text");
            aiReply.setContent(finalContent);
            aiReply.setStatus("sent");
            aiReply.setIsDeleted(false);
            aiReply.setCreatedAt(LocalDateTime.now());
            aiReply.setUpdatedAt(LocalDateTime.now());

            messageRepository.save(aiReply);

            log.info("AI鍥炲娑堟伅鍒涘缓鎴愬姛: messageId={}, content={}瀛楃",
                    aiReply.getId(), finalContent.length());

            // 6. 鏇存柊浼氳瘽琛?
            try {
                conversationMapper.upsertSender(
                        payload.getReceiverId(), // AI浣滀负鍙戦€佹柟
                        payload.getSenderId(), // 鐢ㄦ埛浣滀负鎺ユ敹鏂?
                        aiReply.getId(),
                        aiReply.getCreatedAt());
                conversationMapper.upsertReceiver(
                        payload.getSenderId(), // 鐢ㄦ埛浣滀负鎺ユ敹鏂?
                        payload.getReceiverId(), // AI浣滀负鍙戦€佹柟
                        aiReply.getId(),
                        aiReply.getCreatedAt());
            } catch (Exception e) {
                // 浼氳瘽琛ㄦ洿鏂板け璐ヤ笉搴旈樆鏂富娴佺▼
                log.warn("鏇存柊浼氳瘽琛ㄥけ璐? {}", e.getMessage());
            }

            log.info("SEND_MESSAGE浠诲姟鎵ц瀹屾垚: taskId={}, aiReplyId={}",
                    task.getId(), aiReply.getId());

        } catch (Exception e) {
            // 鎹曡幏鎵€鏈夊紓甯革紝鐢眅xecute鏂规硶鐨勫紓甯稿鐞嗛€昏緫澶勭悊
            log.warn("SEND_MESSAGE浠诲姟鎵ц澶辫触: taskId={}, retryCount={}, error={}",
                    task.getId(), task.getRetryCount(), e.getMessage());
            throw e;
        }
    }

    /**
     * 鎵ц鍙戦€佽闊宠涓?
     */
    private void executeSendVoice(RobotTask task) {
        // TODO: 瀹炵幇鍙戦€佽闊抽€昏緫
        log.info("鍙戦€佽闊? taskId={}, payload={}", task.getId(), task.getActionPayload());
    }

    /**
     * 鎵ц鍙戦€侀€氱煡琛屼负
     */
    private void executeSendNotification(RobotTask task) {
        // TODO: 瀹炵幇鍙戦€侀€氱煡閫昏緫
        log.info("鍙戦€侀€氱煡: taskId={}, payload={}", task.getId(), task.getActionPayload());
    }

    /**
     * 澶勭悊浠诲姟澶辫触鍚庣殑閲嶈瘯閫昏緫
     * 鐙珛浜嬪姟鏂规硶锛岀‘淇濅簨鍔＄敓鏁?
     * 鎵€鏈夋潈閲婃斁锛?
     * - 澶辫触閲嶈瘯鏃舵竻绌?locked_by锛圫ET locked_by=NULL锛夛紝閲婃斁浠诲姟鎵€鏈夋潈
     * - 鐘舵€佸彉鏇翠负 PENDING锛屽厑璁镐换鎰忓疄渚嬮噸鏂伴鍙?
     * - 杈惧埌鏈€澶ч噸璇曟鏁版椂锛屾爣璁颁负 FAILED 骞舵竻绌?locked_by
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleTaskFailure(RobotTask task, String errorMessage) {
        int retryCount = task.getRetryCount() + 1;
        int maxRetryCount = task.getMaxRetryCount();
        String previousLockedBy = task.getLockedBy();

        if (retryCount >= maxRetryCount) {
            // 瓒呰繃鏈€澶ч噸璇曟鏁帮紝鏍囪涓?FAILED
            UpdateWrapper<RobotTask> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", task.getId())
                        .set("status", TaskStatus.FAILED.name())
                        .set("retry_count", retryCount)
                        .set("completed_at", LocalDateTime.now())
                        .set("error_message", errorMessage)
                        .set("locked_by", null);  // 娓呯┖鎵€鏈夋潈
            robotTaskMapper.update(null, updateWrapper);

            log.warn("浠诲姟澶辫触涓旇秴杩囨渶澶ч噸璇曟鏁? taskId={}, retryCount={}, previousLockedBy={}",
                    task.getId(), retryCount, previousLockedBy);
        } else {
            // 璁＄畻涓嬫鎵ц鏃堕棿
            String[] retryDelay = configs.getRetryDelay().split(",");
            int delaySeconds = retryCount > retryDelay.length ?
                               Integer.parseInt(retryDelay[retryDelay.length - 1]) :
                               Integer.parseInt(retryDelay[retryCount - 1]);
            LocalDateTime nextExecuteAt = LocalDateTime.now().plusSeconds(delaySeconds);

            // 閲嶇疆鐘舵€佷负 PENDING锛屽鍔犻噸璇曡鏁帮紝閲婃斁鎵€鏈夋潈
            UpdateWrapper<RobotTask> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", task.getId())
                        .set("status", TaskStatus.PENDING.name())
                        .set("retry_count", retryCount)
                        .set("scheduled_at", nextExecuteAt)
                        .set("error_message", errorMessage)
                        .set("locked_by", null);  // 娓呯┖鎵€鏈夋潈锛屽厑璁稿叾浠栧疄渚嬮噸璇?
            robotTaskMapper.update(null, updateWrapper);

            log.warn("浠诲姟澶辫触閲嶈瘯锛岄噴鏀炬墍鏈夋潈: taskId={}, previousLockedBy={}, retryCount={}, nextScheduledAt={}",
                    task.getId(), previousLockedBy, retryCount, nextExecuteAt);
        }
    }

    /**
     * 璁板綍鎵ц鏃ュ織
     * T044: 娣诲姞鎬ц兘鏃ュ織锛圠LM鍝嶅簲鏃堕棿銆佷换鍔＄瓑寰呮椂闂达級
     */
    private void recordExecutionLog(RobotTask task, LocalDateTime startTime,
                                    LocalDateTime completedTime, boolean success,
                                    String errorMessage) {
        try {
            // 璁＄畻鎵ц寤惰繜锛堜换鍔＄瓑寰呮椂闂达級
            long delayMs = Duration.between(task.getScheduledAt(), startTime).toMillis();
            long durationMs = completedTime != null ?
                             Duration.between(startTime, completedTime).toMillis() : 0;

            // T044: 娣诲姞鎬ц兘鏃ュ織
            if (success) {
                log.info("浠诲姟鎵ц鎬ц兘鎸囨爣: taskId={}, executionDuration={}ms, scheduledDelay={}ms, attempt={}",
                        task.getId(), durationMs, delayMs, task.getRetryCount() + 1);

                // 鎬ц兘鍛婅锛氭墽琛屾椂闂磋繃闀?
                if (durationMs > 30000) {
                    log.warn("浠诲姟鎵ц鏃堕棿杩囬暱: taskId={}, duration={}ms (>5s)", task.getId(), durationMs);
                }

                // 鎬ц兘鍛婅锛氳皟搴﹀欢杩熻繃澶?
                if (delayMs > 60000) {
                    log.warn("浠诲姟璋冨害寤惰繜杩囧ぇ: taskId={}, delay={}ms (>60s)", task.getId(), delayMs);
                }
            } else {
                log.warn("浠诲姟鎵ц澶辫触鎬ц兘鎸囨爣: taskId={}, executionDuration={}ms, scheduledDelay={}ms, attempt={}, error={}",
                        task.getId(), durationMs, delayMs, task.getRetryCount() + 1, errorMessage);
            }

            // 鑾峰彇瀹炰緥ID
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
            // 璁板綍鏃ュ織澶辫触涓嶅簲褰卞搷涓绘祦绋?
            RobotTaskExecutor.log.error("璁板綍鎵ц鏃ュ織澶辫触: taskId=" + task.getId(), e);
        }
    }

    /**
     * 鑾峰彇褰撳墠瀹炰緥ID锛坧od鍚嶇О鎴栦富鏈哄悕锛?
     */
    private String getInstanceId() {
        try {
            // 浼樺厛浣跨敤鐜鍙橀噺锛圞ubernetes pod鍚嶇О锛?
            String podName = System.getenv("HOSTNAME");
            if (podName != null && !podName.isEmpty()) {
                return podName;
            }

            // 鍚﹀垯浣跨敤涓绘満鍚?
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * 鏋勫缓妯℃澘娓叉煋鍙傛暟
     * @param payload 娑堟伅鍙戦€佽浇鑽?
     * @return 妯℃澘鍙傛暟Map锛屽寘鍚玼serName銆乽serId銆乧ontent銆乺eceiverId銆乧haracterName
     */
    private Map<String, Object> buildTemplateParams(SendMessagePayload payload, Long aiCharacterId) {
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

            params.put("user", userName);
            params.put("char", characterName);
            params.put("charDetail", buildCharDetailString(defaultSetting, customSetting));
            params.put("charStatus", "");
            params.put("userDetail", buildUserDetailString(userProfile));
            params.put("history", buildHistoryString(payload, aiCharacterId));

            params.put("userName", userName);
            params.put("userId", payload.getSenderId());
            params.put("receiverId", payload.getReceiverId());
            params.put("characterName", characterName);

            log.debug("build template params success: userName={}, characterName={}, historyCount={}",
                    userName, characterName, ((List<?>) params.get("history")).size());

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
        return "鐢ㄦ埛";
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
        return "AI鍔╂墜";
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

        String background = defaultSetting != null ? defaultSetting.getBackground() : null;
        if (customSetting != null && isNotBlank(customSetting.getBackground())) {
            background = customSetting.getBackground();
        }

        List<String> parts = new ArrayList<>();
        if (memorialDay != null) {
            parts.add("绾康鏃ヤ负" + memorialDay);
        }
        if (isNotBlank(relationship)) {
            parts.add("relationship=" + relationship);
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

    private String buildHistoryString(SendMessagePayload payload, Long aiCharacterId) {
        List<Map<String, Object>> history = new ArrayList<>();

        try {
            List<Message> messages = messageRepository.findDialogHistory(
                    payload.getSenderId(), payload.getReceiverId(), null, 20);
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
            String timeText = time != null ? time.toString() : "";
            if ("message".equals(type)) {
                Message message = (Message) item.get("message");
                parts.add("[" + timeText + "] 鑱婂ぉ娑堟伅: " + (message != null ? message.getContent() : ""));
            } else if ("conversation_summary".equals(type)) {
                ConversationSummary summary = (ConversationSummary) item.get("conversation_summary");
                parts.add("[" + timeText + "] 浼氳瘽鎬荤粨: "
                        + (summary != null ? JsonUtil.toJson(summary.getSummaryJson()) : ""));
            } else if ("long_term_memory".equals(type)) {
                LongTermMemory memory = (LongTermMemory) item.get("long_term_memory");
                parts.add("[" + timeText + "] 闀挎湡璁板繂: " + (memory != null ? memory.getText() : ""));
            }
        }
        return String.join("\n", parts);
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


