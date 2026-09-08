package com.example.urlshortener.service;

import com.example.urlshortener.model.ShortUrl;
import com.example.urlshortener.repository.ShortUrlRepository;
import com.example.urlshortener.util.CodeGenerator;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class ShortUrlService {
    private final ShortUrlRepository repository;

    public ShortUrlService(ShortUrlRepository repository) {
        this.repository = repository;
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
                    return repository.save(shortUrl);
                });
    }

    public Mono<ShortUrl> getShortUrl(String code) {
        return repository.findByCode(code)
                .switchIfEmpty(Mono.error(new RuntimeException("Short URL not found: " + code)));
    }

    public Mono<Void> recordAnalytics(String code) {
        // Increment click count
        return repository.findByCode(code)
                .flatMap(shortUrl -> {
                    shortUrl.setClickCount(shortUrl.getClickCount() + 1);
                    return repository.save(shortUrl);
                })
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
