package com.bqsummer.configuration;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 缓存配置
 * 使用Caffeine作为本地缓存实现
 * 后续可以通过修改此配置切换为Redis或其他缓存实现
 * 
 * @author Boboa Boot Team
 * @date 2026-02-12
 */
@Configuration
@EnableCaching
public class CacheConfig {
    
    /**
     * 配置Caffeine本地缓存管理器
     * 
     * @return CacheManager
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        // 配置Caffeine缓存
        cacheManager.setCaffeine(Caffeine.newBuilder()
                // 初始容量
                .initialCapacity(100)
                // 最大容量
                .maximumSize(1000)
                // 缓存过期时间：1小时
                .expireAfterWrite(1, TimeUnit.HOURS)
                // 开启统计
                .recordStats());
        
        return cacheManager;
    }
}
