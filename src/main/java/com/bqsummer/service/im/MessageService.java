package com.bqsummer.service.im;

import com.bqsummer.common.dto.auth.User;
import com.bqsummer.common.dto.im.Message;
import com.bqsummer.common.dto.robot.RobotTask;
import com.bqsummer.common.dto.robot.SendMessagePayload;
import com.bqsummer.common.vo.req.im.SendMessageRequest;
import com.bqsummer.configuration.RobotTaskConfiguration;
import com.bqsummer.mapper.UserMapper;
import com.bqsummer.mapper.RobotTaskMapper;
import com.bqsummer.repository.MessageRepository;
import com.bqsummer.service.robot.RobotTaskScheduler;
import com.bqsummer.util.JsonUtil;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
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

    // 长轮询超时时间
    private static final Duration LONG_POLL_TIMEOUT = Duration.ofSeconds(30);

    private final MessageRepository messageRepository;
    private final com.bqsummer.mapper.ConversationMapper conversationMapper;
    private final UserMapper userMapper;
    private final RobotTaskMapper robotTaskMapper;
    private final RobotTaskScheduler robotTaskScheduler;
    private final RobotTaskConfiguration config;

    public MessageService(MessageRepository messageRepository,
                          com.bqsummer.mapper.ConversationMapper conversationMapper,
                          UserMapper userMapper,
                          RobotTaskMapper robotTaskMapper,
                          RobotTaskScheduler robotTaskScheduler,
                          RobotTaskConfiguration config) {
        this.messageRepository = messageRepository;
        this.conversationMapper = conversationMapper;
        this.userMapper = userMapper;
        this.robotTaskMapper = robotTaskMapper;
        this.robotTaskScheduler = robotTaskScheduler;
        this.config = config;
    }

    private Sinks.Many<String> getNotifier(Long userId) {
        return userNotifiers.computeIfAbsent(userId,
                id -> Sinks.many().unicast().onBackpressureBuffer());
    }

    /**
     * 入库成功后调用，发一个通知事件
     */
    public void notifyUser(Long userId) {
        getNotifier(userId).tryEmitNext("notify");
    }

    /**
     * 长轮询接口
     */
    public Mono<List<Message>> pollMessages(Long userId, long lastSyncId, int limit) {
        // 数据库查询封装，避免提前执行
        Mono<List<Message>> dbQuery = Mono.fromSupplier(
                () -> messageRepository.findByRecipientIdAndIdGreaterThanOrderByIdAsc(userId, lastSyncId, limit)
        );

        // 等待通知事件；超时则走兜底查询。无论是否收到事件，都会执行一次查询。
        return getNotifier(userId).asFlux().next()
                .timeout(LONG_POLL_TIMEOUT, Mono.just("timeout"))
                .flatMap(ignored -> dbQuery);
    }



    @Transactional(rollbackFor = Exception.class)
    public void sendMessage(@Valid SendMessageRequest request) {
        Long uid = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
        Message msg = new Message();
        msg.setSenderId(uid);
        msg.setReceiverId(request.getReceiverId());
        msg.setType(request.getType());
        msg.setContent(request.getContent());
        msg.setStatus("sent");
        msg.setIsDeleted(0);
        msg.setCreatedAt(LocalDateTime.now());
        msg.setUpdatedAt(LocalDateTime.now());

        // 入库
        messageRepository.save(msg);

        // 更新会话（发送方、接收方）
        try {
            conversationMapper.upsertSender(uid, msg.getReceiverId(), msg.getId(), msg.getCreatedAt());
            conversationMapper.upsertReceiver(msg.getReceiverId(), uid, msg.getId(), msg.getCreatedAt());
        } catch (Exception e) {
            log.warn("update conversation failed: {}", e.getMessage());
        }

        // 通知接收方有新消息
        notifyUser(request.getReceiverId());
        
        // 新增逻辑：如果接收方是AI用户，创建RobotTask
        if (isAiUser(request.getReceiverId())) {
            RobotTask task = createRobotTask(msg, request.getReceiverId());
            robotTaskMapper.insert(task);
            
            // 尝试加载到内存队列
            int loaded = robotTaskScheduler.loadTasks(Collections.singletonList(task));
            // TODO 加监控
            if (loaded == 0) {
                log.warn("任务加载失败，队列已满: taskId={}", task.getId());
            } else {
                log.info("任务创建并加载成功: taskId={}, receiverId={}", task.getId(), request.getReceiverId());
            }
        } else {
            log.error("非法的AI用户消息发送请求，receiverId={}", request.getReceiverId());
        }
    }
    
    /**
     * 判断用户是否为AI用户
     * 
     * @param userId 用户ID
     * @return true表示AI用户，false表示普通用户
     */
    private boolean isAiUser(Long userId) {
        User user = userMapper.findById(userId);
        return user != null && "AI".equals(user.getUserType());
    }
    
    /**
     * 创建RobotTask任务
     * 
     * @param msg 消息对象
     * @param robotId AI用户ID
     * @return 创建的RobotTask对象
     */
    private RobotTask createRobotTask(Message msg, Long robotId) {
        // 构建action_payload
        SendMessagePayload payload = SendMessagePayload.builder()
                .messageId(msg.getId())
                .senderId(msg.getSenderId())
                .receiverId(msg.getReceiverId())
                .content(msg.getContent())
                .build();
        
        // 创建RobotTask
        RobotTask task = new RobotTask();
        // TODO 加个xxx_id字段做索引
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
}
