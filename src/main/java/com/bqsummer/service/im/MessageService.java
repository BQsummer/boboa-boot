package com.bqsummer.service.im;

import com.bqsummer.common.dto.Message;
import com.bqsummer.common.vo.req.SendMessageRequest;
import com.bqsummer.repository.MessageRepository;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class MessageService {

    private final Map<Long, Sinks.Many<String>> userNotifiers = new ConcurrentHashMap<>();

    // 长轮询总超时时间
    private static final Duration LONG_POLL_TIMEOUT = Duration.ofSeconds(30);

    private final MessageRepository messageRepository;

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
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

        // 通知接收方有新消息
        notifyUser(request.getReceiverId());
    }
}
