package com.bqsummer.service.im;

import com.bqsummer.common.dto.auth.User;
import com.bqsummer.common.dto.character.AiCharacter;
import com.bqsummer.common.dto.im.Message;
import com.bqsummer.common.dto.robot.RobotTask;
import com.bqsummer.common.dto.robot.SendMessagePayload;
import com.bqsummer.common.vo.req.im.SendMessageRequest;
import com.bqsummer.configuration.RobotTaskConfiguration;
import com.bqsummer.mapper.AiCharacterMapper;
import com.bqsummer.mapper.RobotTaskMapper;
import com.bqsummer.mapper.UserMapper;
import com.bqsummer.repository.MessageRepository;
import com.bqsummer.service.memory.ConversationMessageService;
import com.bqsummer.service.robot.RobotTaskScheduler;
import com.bqsummer.util.JsonUtil;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class MessageService {

    private final Map<Long, Sinks.Many<String>> userNotifiers = new ConcurrentHashMap<>();

    // Keep business long-poll timeout shorter than servlet async timeout
    // to avoid AsyncRequestTimeoutException at the container layer.
    private static final Duration LONG_POLL_TIMEOUT = Duration.ofSeconds(25);

    private final MessageRepository messageRepository;
    private final com.bqsummer.mapper.ConversationMapper conversationMapper;
    private final AiCharacterMapper aiCharacterMapper;
    private final UserMapper userMapper;
    private final RobotTaskMapper robotTaskMapper;
    private final ObjectProvider<RobotTaskScheduler> robotTaskSchedulerProvider;
    private final RobotTaskConfiguration config;
    private final ConversationMessageService conversationMessageService;

    public MessageService(MessageRepository messageRepository,
                          com.bqsummer.mapper.ConversationMapper conversationMapper,
                          AiCharacterMapper aiCharacterMapper,
                          UserMapper userMapper,
                          RobotTaskMapper robotTaskMapper,
                          ObjectProvider<RobotTaskScheduler> robotTaskSchedulerProvider,
                          RobotTaskConfiguration config,
                          ConversationMessageService conversationMessageService) {
        this.messageRepository = messageRepository;
        this.conversationMapper = conversationMapper;
        this.aiCharacterMapper = aiCharacterMapper;
        this.userMapper = userMapper;
        this.robotTaskMapper = robotTaskMapper;
        this.robotTaskSchedulerProvider = robotTaskSchedulerProvider;
        this.config = config;
        this.conversationMessageService = conversationMessageService;
    }

    private Sinks.Many<String> getNotifier(Long userId) {
        return userNotifiers.computeIfAbsent(userId,
                id -> Sinks.many().multicast().onBackpressureBuffer(256, false));
    }

    public void notifyUser(Long userId) {
        getNotifier(userId).tryEmitNext("notify");
    }

    public Mono<List<Message>> pollMessages(Long userId, long lastSyncId, int limit) {
        Mono<List<Message>> dbQuery = Mono.fromSupplier(
                () -> messageRepository.findByRecipientIdAndIdGreaterThanOrderByIdAsc(userId, lastSyncId, limit)
        );

        return getNotifier(userId).asFlux().next()
                .timeout(LONG_POLL_TIMEOUT, Mono.just("timeout"))
                .flatMap(ignored -> dbQuery);
    }

    @Transactional(rollbackFor = Exception.class)
    public void sendMessage(@Valid SendMessageRequest request) {
        Long uid = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
        ReceiverResolution receiver = resolveReceiver(request.getReceiverId());
        if (!isAiUser(receiver.aiUserId())) {
            log.error("invalid ai receiver: receiverId={}, resolvedAiUserId={}",
                    request.getReceiverId(), receiver.aiUserId());
            return;
        }

        Message msg = new Message();
        msg.setSenderId(uid);
        msg.setReceiverId(receiver.aiUserId());
        msg.setType(request.getType());
        msg.setContent(request.getContent());
        msg.setStatus("sent");
        msg.setIsDeleted(false);
        msg.setCreatedAt(LocalDateTime.now());
        msg.setUpdatedAt(LocalDateTime.now());

        messageRepository.save(msg);

        try {
            conversationMessageService.saveMessage(
                    msg.getId(),
                    uid,
                    receiver.aiCharacterId(),
                    "USER",
                    msg.getContent()
            );
            log.debug("message saved to conversation memory: messageId={}, aiCharacterId={}",
                    msg.getId(), receiver.aiCharacterId());
        } catch (Exception e) {
            log.error("save message to conversation memory failed: messageId={}", msg.getId(), e);
        }

        try {
            conversationMapper.upsertSender(uid, msg.getReceiverId(), msg.getId(), msg.getCreatedAt());
            conversationMapper.upsertReceiver(msg.getReceiverId(), uid, msg.getId(), msg.getCreatedAt());
        } catch (Exception e) {
            log.warn("update conversation failed: {}", e.getMessage());
        }

        notifyUser(receiver.aiUserId());

        RobotTask task = createRobotTask(msg, receiver.aiUserId(), receiver.aiCharacterId());
        robotTaskMapper.insert(task);

        RobotTaskScheduler robotTaskScheduler = robotTaskSchedulerProvider.getIfAvailable();
        if (robotTaskScheduler == null) {
            log.warn("RobotTaskScheduler not ready, skip queue load: taskId={}", task.getId());
            return;
        }

        int loaded = robotTaskScheduler.loadTasks(Collections.singletonList(task));
        if (loaded == 0) {
            log.warn("task load failed, queue full: taskId={}", task.getId());
        } else {
            log.info("task created and loaded: taskId={}, receiverId={}, aiCharacterId={}",
                    task.getId(), receiver.aiUserId(), receiver.aiCharacterId());
        }
    }

    private boolean isAiUser(Long userId) {
        User user = userMapper.findById(userId);
        return user != null && "AI".equals(user.getUserType());
    }

    private RobotTask createRobotTask(Message msg, Long robotId, Long aiCharacterId) {
        SendMessagePayload payload = SendMessagePayload.builder()
                .messageId(msg.getId())
                .senderId(msg.getSenderId())
                .receiverId(robotId)
                .aiCharacterId(aiCharacterId)
                .content(msg.getContent())
                .build();

        RobotTask task = new RobotTask();
        task.setUserId(msg.getSenderId());
        task.setRobotId(robotId);
        task.setTaskType("SHORT_DELAY");
        task.setActionType("SEND_MESSAGE");
        task.setActionPayload(JsonUtil.toJson(payload));
        task.setScheduledAt(LocalDateTime.now());
        task.setStatus("PENDING");
        task.setRetryCount(0);
        task.setMaxRetryCount(3);

        return task;
    }

    private ReceiverResolution resolveReceiver(Long receiverId) {
        AiCharacter character = aiCharacterMapper.findById(receiverId);
        if (character != null && character.getAssociatedUserId() != null) {
            return new ReceiverResolution(character.getAssociatedUserId(), character.getId());
        }

        AiCharacter byAiUser = aiCharacterMapper.findByAssociatedUserId(receiverId);
        if (byAiUser != null) {
            return new ReceiverResolution(receiverId, byAiUser.getId());
        }

        return new ReceiverResolution(receiverId, receiverId);
    }

    private record ReceiverResolution(Long aiUserId, Long aiCharacterId) {
    }
}
