package com.bqsummer.framework.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Token 黑名单服务，支持按条目过期
 */
@Service
public class TokenBlacklistService {

    private final Cache<String, Long> blacklist;

    public TokenBlacklistService() {
        // 使用每个条目自定义过期时间（基于值中存放的过期毫秒时间戳）
        this.blacklist = Caffeine.newBuilder()
                .maximumSize(100_000)
                .expireAfter(new Expiry<String, Long>() {
                    @Override
                    public long expireAfterCreate(String key, Long expiresAtMillis, long currentTimeNanos) {
                        return ttlNanos(expiresAtMillis);
                    }

                    @Override
                    public long expireAfterUpdate(String key, Long expiresAtMillis, long currentTimeNanos, long currentDurationNanos) {
                        return ttlNanos(expiresAtMillis);
                    }

                    @Override
                    public long expireAfterRead(String key, Long expiresAtMillis, long currentTimeNanos, long currentDurationNanos) {
                        // 读取不改变过期时间，维持剩余时间
                        return currentDurationNanos;
                    }

                    private long ttlNanos(Long expiresAtMillis) {
                        long now = System.currentTimeMillis();
                        long remainingMillis = Math.max(1L, (Objects.requireNonNullElse(expiresAtMillis, now) - now));
                        return TimeUnit.MILLISECONDS.toNanos(remainingMillis);
                    }
                })
                .build();
    }

    /**
     * 将访问令牌加入黑名单
     *
     * @param token           访问令牌原文（建议包含前缀去除后的token）
     * @param expiresAtMillis 该令牌的到期时间（毫秒时间戳）
     */
    public void add(String token, long expiresAtMillis) {
        if (token == null || token.isEmpty()) {
            return;
        }
        blacklist.put(token, expiresAtMillis);
    }

    /**
     * 判断令牌是否在黑名单中
     */
    public boolean contains(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        return blacklist.getIfPresent(token) != null;
    }
}

