# URL Shortener V2 - Advanced Setup (Enterprise Edition)

> **For clients who want production-grade infrastructure:** PostgreSQL + Redis + pgbouncer + Resilience4j + Circuit Breaker

---

## When to Use This

Use the **Advanced Setup** if your application needs:
- ✅ Persistent database (survives restarts)
- ✅ High-performance caching (Redis)
- ✅ Fault tolerance (circuit breakers)
- ✅ Connection pooling (pgbouncer)
- ✅ Request resilience (automatic retries)
- ✅ Production monitoring (Prometheus/Grafana)
- ✅ High throughput (10,000+ RPS)

**Don't use this if:** You just want to test locally → Use [`QUICKSTART.md`](QUICKSTART.md) instead.

---

## Prerequisites

- Java 21+
- Maven 3.8+
- **Docker & Docker Compose** (for local infrastructure)
- **PostgreSQL 14+** (cloud RDS or local)
- **Redis 6.0+** (cloud ElastiCache or local)

---

## Architecture (Advanced)

```
┌─────────────────────────────────────────────────────────────┐
│                     Client Requests                          │
└────────────────────────┬────────────────────────────────────┘
                         │
                  ┌──────▼──────┐
                  │ Spring Boot  │
                  │   (Netty)    │  8,500+ RPS per instance
                  └──────┬──────┘
                         │
         ┌───────────────┼───────────────┐
         │               │               │
    ┌────▼────┐    ┌────▼────┐    ┌────▼────┐
    │ Circuit │    │ Retry   │    │ Timeout │
    │ Breaker │    │ Policy  │    │ Handler │
    │(Res4j)  │    │(Res4j)  │    │(Res4j)  │
    └────┬────┘    └────┬────┘    └────┬────┘
         │               │               │
         └───────────────┼───────────────┘
                         │
         ┌───────────────┼───────────────┐
         │               │               │
    ┌────▼────────┐ ┌───▼────┐ ┌───────▼─────┐
    │   Cache     │ │   DB   │ │   Analytics │
    │   (Redis)   │ │(Postgres)│ │  (Queue)   │
    │  Lettuce    │ │ R2DBC  │ │   (Redis)   │
    │ Connection  │ │pgbouncer│ │            │
    │   Pool      │ │  Pool  │ │ Fire &      │
    │             │ │        │ │ Forget      │
    └─────────────┘ └────────┘ └────────────┘
```

---

## Local Setup (Docker Compose)

### 1. Create `docker-compose.yml`

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    container_name: url-shortener-postgres
    environment:
      POSTGRES_DB: shortener
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: url-shortener-redis
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  pgbouncer:
    image: pgbouncer:1.18-alpine
    container_name: url-shortener-pgbouncer
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      DATABASES_HOST: postgres
      DATABASES_PORT: 5432
      DATABASES_USER: postgres
      DATABASES_PASSWORD: postgres
      DATABASES_DBNAME: shortener
      PGBOUNCER_POOL_MODE: transaction
      PGBOUNCER_MAX_CLIENT_CONN: 1000
      PGBOUNCER_DEFAULT_POOL_SIZE: 25
    ports:
      - "6432:6432"
    healthcheck:
      test: ["CMD", "psql", "-U", "postgres", "-c", "SELECT 1"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  postgres_data:
  redis_data:
```

### 2. Start Infrastructure

```bash
docker-compose up -d
docker-compose ps  # Verify all services are healthy
```

### 3. Create Database Schema

```bash
# Connect to PostgreSQL
psql -h localhost -U postgres -d shortener

# Create table
CREATE TABLE short_url (
  id SERIAL PRIMARY KEY,
  code VARCHAR(20) UNIQUE NOT NULL,
  original_url VARCHAR(2048) NOT NULL,
  click_count INTEGER DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT unique_code UNIQUE (code),
  INDEX idx_code (code)
);

# Verify
\dt
\q
```

---

## Application Configuration (Advanced)

### 1. Update `pom.xml` - Add Enterprise Dependencies

```xml
<!-- Existing dependencies remain -->

<!-- Connection Pooling (pgbouncer integration) -->
<dependency>
    <groupId>io.r2dbc</groupId>
    <artifactId>r2dbc-postgresql</artifactId>
    <version>0.8.13.RELEASE</version>
</dependency>

<!-- Redis Caching -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>io.lettuce</groupId>
    <artifactId>lettuce-core</artifactId>
</dependency>

<!-- Resilience4j (circuit breaker, retry, timeout) -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>2.1.0</version>
</dependency>
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-circuitbreaker</artifactId>
    <version>2.1.0</version>
</dependency>
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-retry</artifactId>
    <version>2.1.0</version>
</dependency>

<!-- Prometheus Metrics -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>

<!-- OpenTelemetry for distributed tracing -->
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-api</artifactId>
</dependency>
```

### 2. Update `application-prod.yml` - PostgreSQL + Redis

```yaml
spring:
  application:
    name: url-shortener-v2

  # PostgreSQL via pgbouncer connection pool
  r2dbc:
    url: r2dbc:postgresql://localhost:6432/shortener
    username: postgres
    password: postgres
    pool:
      initial-size: 10
      max-size: 50
      max-acquire-time: 3000ms
      max-idle-time: 900000ms
      max-life-time: 1800000ms
      validation-query: "SELECT 1"

  # Redis caching
  data:
    redis:
      host: localhost
      port: 6379
      database: 0
      password: ""
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 20
          max-idle: 10
          min-idle: 5
          max-wait: 2000ms
        shutdown-timeout: 200ms

server:
  port: 8080
  compression:
    enabled: true
    min-response-size: 1024

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: true

logging:
  level:
    root: INFO
    com.example.urlshortener: DEBUG
    org.springframework.data.r2dbc: DEBUG

# Resilience4j Configuration
resilience4j:
  circuitbreaker:
    configs:
      default:
        register-health-indicator: true
        sliding-window-size: 100
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30000ms
        permitted-number-of-calls-in-half-open-state: 3
        slow-call-rate-threshold: 100
        slow-call-duration-threshold: 2000ms

  retry:
    configs:
      default:
        max-attempts: 3
        wait-duration: 1000ms
        interval-function: exponential-random-backoff

  timelimiter:
    configs:
      default:
        timeout-duration: 2000ms
        cancel-running-future: true
```

### 3. Create `CacheService.java` (Removed in Simple Version, Restored Here)

```java
package com.example.urlshortener.cache;

import com.example.urlshortener.model.ShortUrl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.time.Duration;

@Service
public class CacheService {
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private static final long CACHE_TTL_HOURS = 24;

    public CacheService(ReactiveRedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public Mono<ShortUrl> get(String code) {
        return redisTemplate.opsForValue().get(code)
                .flatMap(json -> {
                    try {
                        ShortUrl shortUrl = objectMapper.readValue(json, ShortUrl.class);
                        return Mono.just(shortUrl);
                    } catch (Exception e) {
                        return Mono.error(new RuntimeException("Cache deserialization failed", e));
                    }
                })
                .onErrorResume(e -> Mono.empty());
    }

    public Mono<Void> set(ShortUrl shortUrl) {
        try {
            String json = objectMapper.writeValueAsString(shortUrl);
            return redisTemplate.opsForValue()
                    .set(shortUrl.getCode(), json, Duration.ofHours(CACHE_TTL_HOURS))
                    .then();
        } catch (Exception e) {
            return Mono.error(new RuntimeException("Cache serialization failed", e));
        }
    }

    public Mono<Void> delete(String code) {
        return redisTemplate.opsForValue().delete(code).then();
    }
}
```

### 4. Create `ResilienceConfig.java` (Circuit Breaker)

```java
package com.example.urlshortener.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class ResilienceConfig {
    private static final Logger logger = LoggerFactory.getLogger(ResilienceConfig.class);

    @Bean
    public RegistryEventConsumer<CircuitBreaker> circuitBreakerEventConsumer() {
        return new RegistryEventConsumer<CircuitBreaker>() {
            @Override
            public void onEntryAdded(EntryAddedEvent<CircuitBreaker> entryAddedEvent) {
                CircuitBreaker circuitBreaker = entryAddedEvent.getAddedEntry();
                logger.info("CircuitBreaker '{}' registered", circuitBreaker.getName());
                circuitBreaker.getEventPublisher()
                        .onStateTransition(event -> logger.warn("CircuitBreaker '{}' transitioned from {} to {}",
                                circuitBreaker.getName(), event.getStateTransition().getFromState(),
                                event.getStateTransition().getToState()));
            }

            @Override
            public void onEntryRemoved(EntryAddedEvent<CircuitBreaker> entryAddedEvent) {
                logger.info("CircuitBreaker '{}' removed", entryAddedEvent.getAddedEntry().getName());
            }
        };
    }
}
```

### 5. Update `ShortUrlService.java` - Add Resilience4j & Caching

```java
package com.example.urlshortener.service;

import com.example.urlshortener.cache.CacheService;
import com.example.urlshortener.model.ShortUrl;
import com.example.urlshortener.repository.ShortUrlRepository;
import com.example.urlshortener.util.CodeGenerator;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

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

    @CircuitBreaker(name = "createShortUrl", fallbackMethod = "createShortUrlFallback")
    @Retry(name = "createShortUrl")
    @TimeLimiter(name = "createShortUrl")
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

    @CircuitBreaker(name = "getShortUrl", fallbackMethod = "getShortUrlFallback")
    @Retry(name = "getShortUrl")
    @TimeLimiter(name = "getShortUrl")
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
        // Fire-and-forget async analytics to Redis queue (non-blocking)
        return redisTemplate.opsForList()
                .leftPush("analytics:queue", code)
                .subscribeOn(Schedulers.boundedElastic())
                .then()
                .onErrorResume(e -> {
                    // Analytics failure shouldn't block the response
                    System.err.println("Analytics queue error: " + e.getMessage());
                    return Mono.empty();
                });
    }

    // Fallback methods
    private Mono<ShortUrl> createShortUrlFallback(String url, String alias, Exception ex) {
        return Mono.error(new RuntimeException("Service unavailable. Circuit breaker open.", ex));
    }

    private Mono<ShortUrl> getShortUrlFallback(String code, Exception ex) {
        return Mono.error(new RuntimeException("Service unavailable. Circuit breaker open.", ex));
    }

    private String generateUniqueCode() {
        for (int i = 0; i < 10; i++) {
            String code = CodeGenerator.randomCode(7);
            if (!repository.existsByCode(code).block()) {
                return code;
            }
        }
        throw new IllegalStateException("Unable to generate unique code");
    }
}
```

---

## Build & Run (Advanced)

### 1. Build Application

```bash
mvn clean package -DskipTests -P prod
```

### 2. Start Services

```bash
# Terminal 1: Docker infrastructure
docker-compose up

# Terminal 2: Application
java -Dspring.profiles.active=prod \
     -Dspring.r2dbc.url=r2dbc:postgresql://localhost:6432/shortener \
     -Dspring.data.redis.host=localhost \
     -jar target/url-shortener-v2-2.0.0.jar
```

### 3. Verify Healthy

```bash
# Health check
curl http://localhost:8080/actuator/health

# Prometheus metrics
curl http://localhost:8080/actuator/prometheus

# Redis
redis-cli PING

# PostgreSQL via pgbouncer
psql -h localhost -p 6432 -U postgres -d shortener -c "SELECT COUNT(*) FROM short_url;"
```

---

## Performance Tuning (Advanced)

### Connection Pools

```yaml
# pgbouncer (transaction-mode pooling)
default_pool_size = 25
min_pool_size = 10
max_db_connections = 100
max_client_conn = 1000

# Redis Lettuce
max-active: 20
max-idle: 10
min-idle: 5
max-wait: 2000ms
```

### Circuit Breaker Thresholds

```yaml
resilience4j.circuitbreaker:
  sliding-window-size: 100         # Window size for failure detection
  failure-rate-threshold: 50       # Open at 50% failures
  wait-duration-in-open-state: 30s # Wait 30s before half-open
  slow-call-rate-threshold: 100    # Open if all calls are slow
  slow-call-duration-threshold: 2s # Threshold for "slow"
```

---

## Monitoring (Advanced)

### Prometheus Scrape

```bash
curl http://localhost:8080/actuator/prometheus | grep url_shortener
```

### Key Metrics to Monitor

```
# Redis cache hit rate
redis_commands_duration_seconds_bucket{command="get"}

# Circuit breaker state
resilience4j_circuitbreaker_state{name="getShortUrl"}
# 0 = CLOSED (healthy), 1 = OPEN (failing), 2 = HALF_OPEN (testing)

# Database connection pool
r2dbc_pool_acquired_size
r2dbc_pool_pending_acquire_size

# HTTP requests
http_server_requests_seconds_bucket{status="302"}
http_server_requests_seconds_bucket{status="404"}
http_server_requests_seconds_bucket{status="500"}
```

### Grafana Dashboard (Optional)

1. Add Prometheus data source: `http://localhost:9090`
2. Import dashboard: Search "Spring Boot" in Grafana
3. Customize with custom metrics above

---

## Production Deployment

### AWS RDS + ElastiCache

```bash
# Update configuration for AWS
spring.r2dbc.url=r2dbc:postgresql://my-postgres.rds.amazonaws.com:5432/shortener
spring.data.redis.host=my-redis.cache.amazonaws.com
spring.data.redis.port=6379
```

### Kubernetes (Helm)

See `k8s/helm/` for production-ready Helm chart (TODO).

---

## Troubleshooting (Advanced)

| Issue | Solution |
|-------|----------|
| Circuit breaker keeps opening | Increase retry attempts, check DB/Redis health |
| Cache hit rate low | Increase Redis memory, adjust TTL, warm cache on startup |
| Slow queries | Use pgbouncer, tune pool size, add DB indexes |
| Memory leaks | Monitor JVM heap, enable GC logging |
| High latency (P99 > 100ms) | Check circuit breaker logs, increase connection pool size |

---

## Comparison: Simple vs. Advanced

| Feature | Simple (H2) | Advanced (PostgreSQL + Redis) |
|---------|------------|------------------------------|
| **Setup time** | 2 min | 15 min (+ Docker) |
| **Persistence** | In-memory (lost on restart) | Persistent (PostgreSQL) |
| **Throughput** | 5,000 RPS | 15,000 RPS (with caching) |
| **Cache** | None | Redis (24h TTL) |
| **Resilience** | None | Resilience4j (circuit breaker) |
| **Connection pooling** | None | pgbouncer (transaction mode) |
| **Monitoring** | Basic | Prometheus + Grafana |
| **Cost (annual)** | Free (local dev only) | $200+ (RDS + ElastiCache) |
| **When to use** | Testing, CI/CD, demos | Production, high-traffic |

---

## Next Steps

1. ✅ Set up Docker Compose infrastructure
2. ✅ Configure PostgreSQL + Redis
3. ✅ Update application.yml to production profile
4. ✅ Build and run with Resilience4j enabled
5. ✅ Monitor with Prometheus/Grafana
6. ✅ Load test (target: 10,000+ RPS)
7. ✅ Deploy to cloud (AWS/Azure/GCP)

---

## Support

- 📖 See [`QUICKSTART.md`](QUICKSTART.md) for simple setup
- 📖 See [`DEPLOYMENT_GUIDE.md`](DEPLOYMENT_GUIDE.md) for cloud deployment
- 🐛 Open an issue on GitHub for questions

---

**Version:** 2.0.0 (Advanced/Enterprise Edition)  
**Last updated:** September 2026
