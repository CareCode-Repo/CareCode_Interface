package com.carecode.domain.user.config;

import com.carecode.domain.user.service.refreshtoken.NoopRefreshTokenStore;
import com.carecode.domain.user.service.refreshtoken.RedisRefreshTokenStore;
import com.carecode.domain.user.service.refreshtoken.RefreshTokenStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RefreshTokenStoreConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "jwt.refresh-token", name = "store", havingValue = "redis")
    public RefreshTokenStore redisRefreshTokenStore(
            StringRedisTemplate stringRedisTemplate,
            @Value("${jwt.refresh-token.expiration:2592000000}") long refreshTtlMs) {
        return new RedisRefreshTokenStore(stringRedisTemplate, refreshTtlMs);
    }

    @Bean
    @ConditionalOnMissingBean(RefreshTokenStore.class)
    public RefreshTokenStore noopRefreshTokenStore() {
        return new NoopRefreshTokenStore();
    }
}
