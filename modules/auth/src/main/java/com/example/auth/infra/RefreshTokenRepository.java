package com.example.auth.infra;

import java.time.Duration;
import java.util.Objects;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RefreshTokenRepository {

    private static final String REFRESH_TOKEN_KEY_PREFIX = "auth:refresh-token:";

    private final StringRedisTemplate redisTemplate;

    public RefreshTokenRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // Refresh Token은 서버에도 저장해 재발급과 로그아웃 시 유효성을 확인
    public void save(String subject, String refreshToken, long ttlMillis) {
        redisTemplate.opsForValue()
                .set(createKey(subject), refreshToken, Duration.ofMillis(ttlMillis));
    }

    public boolean existsBySubjectAndToken(String subject, String refreshToken) {
        String storedRefreshToken = redisTemplate.opsForValue().get(createKey(subject));
        return Objects.equals(storedRefreshToken, refreshToken);
    }

    public void deleteBySubject(String subject) {
        redisTemplate.delete(createKey(subject));
    }

    private String createKey(String subject) {
        return REFRESH_TOKEN_KEY_PREFIX + subject;
    }
}
