package com.carecode.domain.user.service.refreshtoken;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;

/**
 * 리프레시 JWT를 서명만으로 두지 않고, 서버(Redis)에 활성 세션으로 등록해 재사용·로그아웃 시 제어합니다.
 */
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStore {

    static final String TOKEN_KEY_PREFIX = "carecode:rt:t:";
    static final String USER_SET_PREFIX = "carecode:rt:u:";

    private final StringRedisTemplate redis;
    private final long refreshTtlMs;

    @Override
    public void register(String userId, String refreshTokenJwt) {
        String hash = RefreshTokenHasher.sha256Hex(refreshTokenJwt);
        String tokenKey = TOKEN_KEY_PREFIX + hash;
        String userSetKey = USER_SET_PREFIX + userId;
        Duration ttl = Duration.ofMillis(Math.max(refreshTtlMs, 60_000L));

        redis.opsForValue().set(tokenKey, userId, ttl);
        redis.opsForSet().add(userSetKey, hash);
        redis.expire(userSetKey, ttl.plusDays(1));
    }

    @Override
    public boolean isRegistered(String refreshTokenJwt, String userId) {
        String hash = RefreshTokenHasher.sha256Hex(refreshTokenJwt);
        String tokenKey = TOKEN_KEY_PREFIX + hash;
        String storedUserId = redis.opsForValue().get(tokenKey);
        return storedUserId != null && Objects.equals(storedUserId, userId);
    }

    @Override
    public void remove(String refreshTokenJwt, String userId) {
        String hash = RefreshTokenHasher.sha256Hex(refreshTokenJwt);
        String tokenKey = TOKEN_KEY_PREFIX + hash;
        String userSetKey = USER_SET_PREFIX + userId;
        redis.delete(tokenKey);
        redis.opsForSet().remove(userSetKey, hash);
    }

    @Override
    public void removeAllForUser(String userId) {
        String userSetKey = USER_SET_PREFIX + userId;
        Set<String> hashes = redis.opsForSet().members(userSetKey);
        if (hashes != null) {
            for (String hash : hashes) {
                redis.delete(TOKEN_KEY_PREFIX + hash);
            }
        }
        redis.delete(userSetKey);
    }
}
