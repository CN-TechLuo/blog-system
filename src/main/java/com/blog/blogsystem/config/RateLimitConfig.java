package com.blog.blogsystem.config;

import com.blog.blogsystem.util.InMemoryRateLimitStore;
import com.blog.blogsystem.util.RateLimitStore;
import com.blog.blogsystem.util.RateLimiterUtil;
import com.blog.blogsystem.util.RedisRateLimitStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 限流存储装配：
 * - app.rate-limit.store=memory（默认）：进程内内存，适合单机
 * - app.rate-limit.store=redis：多实例共享（生产推荐，docker-compose 已内置 redis 服务）
 */
@Configuration
public class RateLimitConfig {

    private static final Logger log = LoggerFactory.getLogger(RateLimitConfig.class);

    @Bean
    public RateLimitStore rateLimitStore(ObjectProvider<StringRedisTemplate> redisProvider,
                                         @Value("${app.rate-limit.store:memory}") String storeType) {
        RateLimitStore store;
        if ("redis".equalsIgnoreCase(storeType.trim())) {
            StringRedisTemplate redis = redisProvider.getObject();
            store = new RedisRateLimitStore(redis);
            log.info("限流存储已启用 Redis 模式（多实例共享）");
        } else {
            store = new InMemoryRateLimitStore();
            log.info("限流存储为内存模式（单机）；多实例部署请设置环境变量 RATE_LIMIT_STORE=redis");
        }
        RateLimiterUtil.init(store);
        return store;
    }

}
