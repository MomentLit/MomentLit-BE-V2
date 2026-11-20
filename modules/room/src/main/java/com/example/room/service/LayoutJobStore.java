package com.example.room.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Optional;

/** 추론 작업 상태를 Redis에 둔다. 결과를 받은 프론트가 방을 저장하면 더 쓸 일이 없어서 짧게 보관한다. */
@Component
@RequiredArgsConstructor
public class LayoutJobStore {

    private static final String KEY_PREFIX = "room:layout-job:";
    private static final Duration TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void save(LayoutJob job) {
        redisTemplate.opsForValue().set(KEY_PREFIX + job.id(), objectMapper.writeValueAsString(job), TTL);
    }

    public Optional<LayoutJob> find(String jobId) {
        String json = redisTemplate.opsForValue().get(KEY_PREFIX + jobId);
        return Optional.ofNullable(json).map(value -> objectMapper.readValue(value, LayoutJob.class));
    }
}
