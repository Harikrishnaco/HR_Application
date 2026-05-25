package com.example.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
public class RedisConfig extends CachingConfigurerSupport {

    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    // Ticket LF-202: Intercepts cache errors so the application degrades gracefully to DB-only mode
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException ex, org.springframework.cache.Cache cache, Object key) {
                log.error("Redis unreachable during GET for key [{}]. Falling back to DB. Error: {}", key, ex.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException ex, org.springframework.cache.Cache cache, Object key, Object value) {
                log.error("Redis unreachable during PUT for key [{}]. Continuing without cache. Error: {}", key, ex.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException ex, org.springframework.cache.Cache cache, Object key) {
                log.error("Redis unreachable during EVICT for key [{}]. Continuing. Error: {}", key, ex.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException ex, org.springframework.cache.Cache cache) {
                log.error("Redis unreachable during CLEAR operations. Continuing. Error: {}", ex.getMessage());
            }
        };
    }
}