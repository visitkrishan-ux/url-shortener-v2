package com.example.urlshortener.service;

import com.example.urlshortener.cache.CacheService;
import com.example.urlshortener.model.ShortUrl;
import com.example.urlshortener.repository.ShortUrlRepository;
import com.example.urlshortener.util.CodeGenerator;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class ShortUrlService {
    private final ShortUrlRepository repository;
    private final CacheService cacheService;
    private final ReactiveRedisTemplate<String, String> redisTemplate;

    public ShortUrlService(ShortUrlRepository repository, CacheService cacheService,
                          ReactiveRedisTemplate<String, String> redisTemplate) {
        this.repository = repository;
        this.cacheService = cacheService;
        this.redisTemplate = redisTemplate;
    }

    public Mono<ShortUrl> createShortUrl(String originalUrl, String customAlias) {
        String code = (customAlias != null && !customAlias.isBlank()) ? customAlias : generateUniqueCode();
        final String finalCode = code;

        return repository.existsByCode(code)
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new IllegalArgumentException("Alias already exists: " + finalCode));
                    }
                    ShortUrl shortUrl = new ShortUrl(finalCode, originalUrl);
                    return repository.save(shortUrl)
                            .flatMap(saved -> cacheService.set(saved).thenReturn(saved));
                });
    }

    public Mono<ShortUrl> getShortUrl(String code) {
        // Try cache first
        return cacheService.get(code)
                .switchIfEmpty(
                    // Cache miss: fetch from DB and populate cache
                    repository.findByCode(code)
                            .flatMap(shortUrl -> cacheService.set(shortUrl).thenReturn(shortUrl))
                );
    }

    public Mono<Void> recordAnalytics(String code) {
        // Fire-and-forget async analytics to Redis queue
        String queueKey = "analytics:queue";
        long timestamp = System.currentTimeMillis();
        String event = code + ":" + timestamp;
        return redisTemplate.opsForList()
                .leftPush(queueKey, event)
                .then();
    }

    private String generateUniqueCode() {
        for (int i = 0; i < 10; i++) {
            String code = CodeGenerator.randomCode(7);
            Mono<Boolean> exists = repository.existsByCode(code)
                    .onErrorReturn(false);
            if (!exists.block()) {
                return code;
            }
        }
        throw new IllegalStateException("Unable to generate unique code");
    }
}
