package com.bqsummer.service.im;

import com.bqsummer.common.dto.Message;
import com.bqsummer.repository.MessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolder;
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

    public Mono<List<Message>> pollMessages(long lastSyncId, int limit) {
        Mono<List<Message>> queryOnce = Mono.fromCallable(() ->
                messageRepository.findByRecipientIdAndIdGreaterThanOrderByIdAsc(null, lastSyncId, limit)
        );

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return queryOnce.flatMap(initialMessages -> {
                    if (!initialMessages.isEmpty()) {
                        return Mono.just(initialMessages);
                    } else {
                        return startPollingLoop(queryOnce);
                    }
                })
                // 直接在入口处注入认证信息到响应式上下文
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
    }

    private Mono<List<Message>> startPollingLoop(Mono<List<Message>> queryOnce) {
        return Flux.interval(POLL_INTERVAL)
                .flatMap(tick -> queryOnce)
                .filter(messages -> !messages.isEmpty())
                .next()
                .timeout(LONG_POLL_TIMEOUT, Mono.just(List.of()));
    }
}
