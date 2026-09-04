package com.example.urlshortener.cache;

import com.example.urlshortener.model.ShortUrl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.time.Duration;

@Service
public class CacheService {
    private static final String CACHE_KEY_PREFIX = "shorturl:";
    private static final Duration CACHE_TTL = Duration.ofHours(24);

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public CacheService(ReactiveRedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public Mono<ShortUrl> get(String code) {
        return redisTemplate.opsForValue()
                .get(cacheKey(code))
                .flatMap(json -> Mono.fromCallable(() -> objectMapper.readValue(json, ShortUrl.class)))
                .onErrorResume(e -> Mono.empty());
    }

    public Mono<Void> set(ShortUrl shortUrl) {
        try {
            String json = objectMapper.writeValueAsString(shortUrl);
            return redisTemplate.opsForValue()
                    .set(cacheKey(shortUrl.getCode()), json, CACHE_TTL)
                    .then();
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    public Mono<Void> invalidate(String code) {
        return redisTemplate.delete(cacheKey(code)).then();
    }

    private String cacheKey(String code) {
        return CACHE_KEY_PREFIX + code;
    }
}
