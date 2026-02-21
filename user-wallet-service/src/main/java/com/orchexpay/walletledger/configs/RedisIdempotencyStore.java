package com.orchexpay.walletledger.configs;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RedisIdempotencyStore implements IdempotencyStore {

    private static final String KEY_PREFIX = "idempotency:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public Optional<String> getIfPresent(String idempotencyKey) {
        String key = KEY_PREFIX + idempotencyKey;
        String value = redisTemplate.opsForValue().get(key);
        return Optional.ofNullable(value);
    }

    @Override
    public void put(String idempotencyKey, String responsePayload, long ttlSeconds) {
        String key = KEY_PREFIX + idempotencyKey;
        redisTemplate.opsForValue().set(key, responsePayload, ttlSeconds, TimeUnit.SECONDS);
    }
}
