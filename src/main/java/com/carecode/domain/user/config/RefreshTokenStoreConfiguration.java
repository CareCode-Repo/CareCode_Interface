package com.carecode.domain.user.config;

import com.carecode.domain.user.service.refreshtoken.NoopRefreshTokenStore;
import com.carecode.domain.user.service.refreshtoken.RedisRefreshTokenStore;
import com.carecode.domain.user.service.refreshtoken.RefreshTokenStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 리프레시 토큰 저장소 선택.
 *
 * <p>{@code redis} 는 로그아웃 시 토큰을 실제로 폐기한다. {@code none}({@link NoopRefreshTokenStore})
 * 은 아무것도 하지 않는다 — 로컬·CI 가 Redis 없이 뜨기 위한 선택지다.
 */
@Configuration
public class RefreshTokenStoreConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "jwt.refresh-token", name = "store", havingValue = "redis")
    public RefreshTokenStore redisRefreshTokenStore(
            StringRedisTemplate stringRedisTemplate,
            @Value("${jwt.refresh-token.expiration:2592000000}") long refreshTtlMs) {
        return new RedisRefreshTokenStore(stringRedisTemplate, refreshTtlMs);
    }

    /**
     * Redis 를 쓰지 않을 때의 저장소. <b>운영에서는 기동을 막는다.</b>
     *
     * <p>이 구현이 운영에 올라가면 로그아웃이 <b>아무것도 폐기하지 못한다.</b> 응답은 200 이고
     * 클라이언트는 로그아웃된 것으로 보이지만, 탈취된 리프레시 토큰은 만료(기본 30일) 전까지
     * 계속 새 액세스 토큰을 받아갈 수 있다. 조용히 틀린 상태라 운영에서 알아차릴 방법이 없다.
     *
     * <p>운영 프로파일의 기본값은 {@code redis} 지만 {@code JWT_REFRESH_TOKEN_STORE} 로 덮을 수
     * 있다. 그래서 기본값에 기대지 않고 여기서 막는다 — 로그아웃이 동작하지 않는 서버가 뜨는
     * 것보다 뜨지 않는 편이 낫다. 이슈 #90
     */
    @Bean
    @ConditionalOnMissingBean(RefreshTokenStore.class)
    public RefreshTokenStore noopRefreshTokenStore(Environment environment) {
        if (environment.matchesProfiles("prod")) {
            throw new IllegalStateException(
                    "운영에서는 리프레시 토큰 저장소가 필요합니다. jwt.refresh-token.store=redis 로 두세요"
                            + " (현재 설정으로는 로그아웃이 토큰을 폐기하지 못합니다).");
        }
        return new NoopRefreshTokenStore();
    }
}
