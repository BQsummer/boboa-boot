package com.bqsummer.configuration;

import com.bqsummer.mapper.ConfigHistoryMapper;
import com.bqsummer.mapper.ConfigMapper;
import com.bqsummer.framework.configplus.proxy.ConfigService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class ConfigConfiguration {

    @Value("${spring.application.name}")
    private String appName;

    @Value("${spring.profiles.active}")
    private String env;

    @Bean
    @ConditionalOnMissingBean
    public ConfigService configService(ConfigMapper configMapper, ConfigHistoryMapper configHistoryMapper, ApplicationContext applicationContext) {
        return new ConfigService(appName, env, configMapper, configHistoryMapper, applicationContext);
    }

    @Bean
    public CacheLoader<String, Object> cacheLoader(ConfigService configService) {
        return key -> {
            String[] eles = key.split("#");
            if (eles.length < 2) {
                throw new IllegalArgumentException("Invalid cache key format: " + key + ". Expected format: 'configName#methodName'");
            }
            return configService.getConfig(eles[0], configService.getFieldFromMethodName(eles[1]));
        };
    }

    @Bean
    public Cache<String, Object> configCache(CacheLoader<String, Object> cacheLoader) {
        return Caffeine.newBuilder()
                .refreshAfterWrite(10, TimeUnit.SECONDS)
                .build(cacheLoader);
    }

}
