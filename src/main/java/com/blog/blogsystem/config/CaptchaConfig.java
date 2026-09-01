package com.blog.blogsystem.config;

import com.blog.blogsystem.service.CaptchaStore;
import com.blog.blogsystem.service.impl.InMemoryCaptchaStore;
import com.blog.blogsystem.service.impl.RedisCaptchaStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 验证码存储装配：
 * - app.captcha.store=memory（默认）：单机内存
 * - app.captcha.store=redis：多实例共享（生产推荐，与限流共用 redis 服务）
 */
@Configuration
public class CaptchaConfig {

    private static final Logger log = LoggerFactory.getLogger(CaptchaConfig.class);

    @Bean
    public CaptchaStore captchaStore(ObjectProvider<StringRedisTemplate> redisProvider,
                                     @Value("${app.captcha.store:memory}") String storeType) {
        if ("redis".equalsIgnoreCase(storeType.trim())) {
            log.info("验证码存储已启用 Redis 模式（多实例共享）");
            return new RedisCaptchaStore(redisProvider.getObject());
        }
        log.info("验证码存储为内存模式（单机）；多实例部署请设置环境变量 CAPTCHA_STORE=redis");
        return new InMemoryCaptchaStore();
    }

}
