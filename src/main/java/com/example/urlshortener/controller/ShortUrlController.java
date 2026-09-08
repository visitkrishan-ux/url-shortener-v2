package com.example.urlshortener.controller;

import com.example.urlshortener.model.ShortUrl;
import com.example.urlshortener.service.ShortUrlService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.net.URI;
import java.util.Map;

@RestController
@Validated
public class ShortUrlController {
    private final ShortUrlService shortUrlService;

    public ShortUrlController(ShortUrlService shortUrlService) {
        this.shortUrlService = shortUrlService;
    }

    @PostMapping("/api/shorten")
    public Mono<ResponseEntity<Object>> shorten(@RequestBody Mono<Map<String, String>> bodyMono) {
        return bodyMono.flatMap(body -> {
            String url = body.get("url");
            String custom = body.get("customAlias");
            
            if (url == null || url.isBlank()) {
                return Mono.just(ResponseEntity.badRequest()
                        .body((Object) Map.of("error", "Missing required field: url")));
            }
            
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                return Mono.just(ResponseEntity.badRequest()
                        .body((Object) Map.of("error", "URL must start with http:// or https://")));
            }

            return shortUrlService.createShortUrl(url, custom)
                    .map(s -> ResponseEntity.ok((Object) Map.of(
                            "code", s.getCode(),
                            "url", s.getOriginalUrl(),
                            "shortUrl", "http://localhost:8080/" + s.getCode(),
                            "createdAt", s.getCreatedAt().toString()
                    )))
                    .onErrorResume(e -> Mono.just(ResponseEntity.badRequest()
                            .body((Object) Map.of("error", e.getMessage()))));
        });
    }

    @GetMapping("/health")
    public Mono<ResponseEntity<Map<String, String>>> health() {
        return Mono.just(ResponseEntity.ok(Map.of("status", "UP")));
    }

    @GetMapping("/{code}")
    public Mono<ResponseEntity<Object>> redirect(@PathVariable String code, ServerWebExchange exchange) {
        // Ignore reserved paths
        if (code.equals("actuator")) {
            return Mono.just(ResponseEntity.notFound().build());
        }
        
        return shortUrlService.getShortUrl(code)
                .doOnNext(s -> shortUrlService.recordAnalytics(code).subscribe())
                .map(s -> ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create(s.getOriginalUrl()))
                        .build())
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @GetMapping("/api/ai/suggest")
    public Mono<ResponseEntity<?>> suggest(@RequestParam String url) {
        String suggestion = determineSuggestion(url);
        return Mono.just(ResponseEntity.ok(Map.of("suggestion", suggestion)));
    }

    private String determineSuggestion(String url) {
        try {
            java.net.URI uri = java.net.URI.create(url);
            String host = uri.getHost();
            if (host == null) host = "link";
            int hash = Math.abs(url.hashCode()) % 10000;
            return (host.replaceAll("\\W+", "").toLowerCase() + hash)
                    .substring(0, Math.min(12, host.length() + 4));
        } catch (Exception e) {
            return "link" + (Math.abs(url.hashCode()) % 10000);
        }
    }
}
