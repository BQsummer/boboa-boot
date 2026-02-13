# 缓存实现切换指南

## 当前实现

项目目前使用 **Caffeine 本地缓存**，通过 Spring Cache 抽象层实现。

## 如何切换到 Redis

如果将来需要使用 Redis 作为分布式缓存，只需要以下几步：

### 1. 添加 Redis 依赖到 pom.xml

```xml
<!-- Spring Data Redis -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- Redis 连接池 (可选) -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-pool2</artifactId>
</dependency>
```

### 2. 修改 application.properties

```properties
# Redis 配置
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.password=
spring.data.redis.database=0

# Redis 连接池配置
spring.data.redis.lettuce.pool.max-active=8
spring.data.redis.lettuce.pool.max-idle=8
spring.data.redis.lettuce.pool.min-idle=0
spring.data.redis.lettuce.pool.max-wait=-1ms

# 缓存配置
spring.cache.type=redis
spring.cache.redis.time-to-live=3600000
```

### 3. 修改 CacheConfig.java

将 `CacheConfig.java` 中的 Caffeine 配置改为 Redis 配置：

```java
package com.bqsummer.configuration;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Redis 缓存配置
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                // 设置过期时间：1小时
                .entryTtl(Duration.ofHours(1))
                // 配置序列化
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer()))
                // 禁止缓存 null 值
                .disableCachingNullValues();
        
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }
}
```

### 4. 无需修改业务代码

由于使用了 Spring Cache 抽象，`EmbeddingUtil.java` 中的 `@Cacheable` 注解**无需任何修改**即可自动使用 Redis 缓存。

## 优势

通过使用 Spring Cache 抽象层：

1. **业务代码零侵入**：切换缓存实现无需修改业务代码
2. **灵活切换**：可以在本地缓存、Redis、Ehcache 等多种实现之间自由切换
3. **统一管理**：所有缓存配置集中在一个配置类中
4. **易于测试**：本地开发可以使用 Caffeine，生产环境使用 Redis

## 支持的缓存实现

Spring Cache 支持以下缓存实现：

- **Caffeine** (当前使用)：高性能本地缓存
- **Redis**：分布式缓存
- **Ehcache**：企业级缓存
- **Hazelcast**：分布式内存数据网格
- **Simple**：基于 ConcurrentHashMap 的简单实现
