package com.example.urlshortener.repository;

import com.example.urlshortener.model.ShortUrl;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import reactor.core.publisher.Mono;

public interface ShortUrlRepository extends R2dbcRepository<ShortUrl, Long> {
    
    @Query("SELECT * FROM short_urls WHERE code = :code")
    Mono<ShortUrl> findByCode(@Param("code") String code);

    @Query("SELECT EXISTS(SELECT 1 FROM short_urls WHERE code = :code)")
    Mono<Boolean> existsByCode(@Param("code") String code);
}
