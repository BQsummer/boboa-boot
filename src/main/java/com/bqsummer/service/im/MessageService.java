package com.bqsummer.service.im;

import com.bqsummer.common.dto.Message;
import com.bqsummer.repository.MessageRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Service
public class MessageService {

    // 轮询数据库的间隔
    private static final Duration POLL_INTERVAL = Duration.ofSeconds(1);
    // 长轮询总超时时间
    private static final Duration LONG_POLL_TIMEOUT = Duration.ofSeconds(30);

    private final MessageRepository messageRepository;

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    /**
     * 执行长轮询拉取消息
     * @param userId 用户ID
     * @param lastSyncId 客户端最后同步的消息ID
     * @param limit 数量限制
     * @return 包含新消息列表的 Mono
     */
    public Mono<List<Message>> pollMessages(String userId, long lastSyncId, int limit) {
        // 定义一次数据库查询操作
        Mono<List<Message>> queryOnce = Mono.fromCallable(() ->
                messageRepository.findByRecipientIdAndIdGreaterThanOrderByIdAsc(userId, lastSyncId, limit)
        );

        // 核心逻辑
        return queryOnce.flatMap(initialMessages -> {
            if (!initialMessages.isEmpty()) {
                // 1. 首次查询就有消息（追赶离线消息），立即返回
                return Mono.just(initialMessages);
            } else {
                // 2. 首次查询无消息，进入长轮询等待
                return startPollingLoop(userId, lastSyncId, limit);
            }
        });
    }

    /**
     * 启动一个响应式的、非阻塞的数据库轮询循环
     */
    private Mono<List<Message>> startPollingLoop(String userId, long lastSyncId, int limit) {
        // 定义一次数据库查询操作
        Mono<List<Message>> queryOnce = Mono.fromCallable(() ->
                messageRepository.findByRecipientIdAndIdGreaterThanOrderByIdAsc(userId, lastSyncId, limit)
        );

        // Flux.interval 创建一个每隔 POLL_INTERVAL 发射一个信号的流
        return Flux.interval(POLL_INTERVAL)
                // 每次收到信号，都去查询数据库
                .flatMap(tick -> queryOnce)
                // 我们只关心非空的结果
                .filter(messages -> !messages.isEmpty())
                // 只要找到第一个非空结果，就结束这个流
                .next() // .next() 将 Flux<List<Message>> 转换为 Mono<List<Message>>
                // 为整个轮询过程设置总超时。如果超时，返回一个空列表的 Mono
                .timeout(LONG_POLL_TIMEOUT, Mono.just(List.of()));
    }
}
