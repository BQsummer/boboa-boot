package com.bqsummer.service.im;

import com.bqsummer.common.dto.Message;
import com.bqsummer.repository.MessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Service
@Slf4j
public class MessageService {

    // 轮询数据库的间隔
    private static final Duration POLL_INTERVAL = Duration.ofSeconds(1);
    // 长轮询总超时时间
    private static final Duration LONG_POLL_TIMEOUT = Duration.ofSeconds(30);

    private final MessageRepository messageRepository;

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public Mono<List<Message>> pollMessages(String userId, long lastSyncId, int limit) {
        // 这一步使用了 Mono.fromCallable，这是将你阻塞的 MySQL JDBC 查询
        // 包装成响应式 Mono 的正确方式。
        // Reactor 会在一个专门的线程池 (Schedulers.boundedElastic) 上运行这个阻塞调用，
        // 从而不会阻塞主事件循环线程。
        Mono<List<Message>> queryOnce = Mono.fromCallable(() ->
                messageRepository.findByRecipientIdAndIdGreaterThanOrderByIdAsc(userId, lastSyncId, limit)
        );

        return queryOnce.flatMap(initialMessages -> {
                    if (!initialMessages.isEmpty()) {
                        return Mono.just(initialMessages);
                    } else {
                        return startPollingLoop(userId, lastSyncId, limit);
                    }
                })
                // 这一步是修复 401 错误的关键。它确保了即使在 startPollingLoop
                // 的 Flux.interval 切换线程后，安全上下文依然存在。
                .transform(this::attachSecurityContext);
    }

    /**
     * 辅助方法，用于将 SecurityContext 附加到响应式流中
     */
    private <T> Mono<T> attachSecurityContext(Mono<T> publisher) {
        return ReactiveSecurityContextHolder.getContext()
            .flatMap(securityContext ->
                publisher.contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)))
            );
    }

    private Mono<List<Message>> startPollingLoop(String userId, long lastSyncId, int limit) {
        // 同样，这里的 queryOnce 也是对阻塞 JDBC 调用的正确封装
        Mono<List<Message>> queryOnce = Mono.fromCallable(() ->
                messageRepository.findByRecipientIdAndIdGreaterThanOrderByIdAsc(userId, lastSyncId, limit)
        );

        return Flux.interval(POLL_INTERVAL)
                .flatMap(tick -> queryOnce) // 每次轮询都会在一个安全的、非阻塞的上下文中执行
                .filter(messages -> !messages.isEmpty())
                .next()
                .timeout(LONG_POLL_TIMEOUT, Mono.just(List.of()));
    }
}
