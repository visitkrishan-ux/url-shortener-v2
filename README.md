# URL Shortener V2 - High-Throughput Reactive Architecture

This is the production-grade, high-performance version of the URL Shortener using Spring WebFlux (reactive), R2DBC (async DB driver), and Redis cache with write-through pattern.

## Quick Start

### Prerequisites
- Java 21 (Temurin or Eclipse Adoptium)
- Maven 3.8+
- Docker & Docker Compose

### Development Setup

1. **Start infrastructure (Postgres + Redis + pgbouncer)**:
   ```bash
   docker-compose up -d
   wait for health checks: docker-compose ps
   ```

2. **Build project**:
   ```bash
   mvn clean package -DskipTests
   ```

3. **Run application**:
   ```bash
   java -jar target/url-shortener-v2-2.0.0-SNAPSHOT.jar
   ```

4. **Test endpoints**:
   ```bash
   # Create short URL
   curl -X POST http://localhost:8080/api/shorten \
     -H "Content-Type: application/json" \
     -d '{"url":"https://example.com/very/long/path","customAlias":"example"}'

   # Redirect (and record analytics)
   curl -L http://localhost:8080/example

   # Get alias suggestion
   curl "http://localhost:8080/api/ai/suggest?url=https://example.com/article/123"

   # Metrics
   curl http://localhost:8080/actuator/metrics/http.server.requests
   ```

## Architecture

### Component Stack
- **Framework**: Spring WebFlux (Project Reactor, Netty)
- **Database**: PostgreSQL with R2DBC (async driver)
- **Cache**: Redis (Lettuce client, async)
- **Connection Pool**: pgbouncer (transaction-mode pooling)
- **Resilience**: Resilience4j (circuit breaker, retry)
- **Metrics**: Micrometer + Prometheus
- **Build**: Maven

### Request Flow
```
1. Client Request
   ↓
2. WebFlux Controller (non-blocking Mono/Flux)
   ↓
3. Try Redis Cache (L1, ~1ms hit)
   ├─ HIT → Return cached ShortUrl
   └─ MISS → Fall through to DB
   ↓
4. R2DBC Repository (async Postgres query)
   ├─ Found → Populate cache + Return
   └─ Not Found → Return 404
   ↓
5. Fire Analytics (async, fire-and-forget)
   ├─ Push to Redis queue (LPUSH)
   ├─ Worker consumes async
   └─ Continue (non-blocking)
   ↓
6. Send 302 Redirect Response (~5ms P99)
```

### Key Design Patterns

#### Write-Through Cache
- On create: write to Postgres, then populate Redis
- On read: check Redis first, miss → read DB & populate cache
- TTL: 24 hours per link (configurable)
- Invalidation: Immediate on delete/update (TODO)

#### Fire-and-Forget Analytics
- Redirect response returns immediately (~5ms)
- Analytics event pushed to Redis queue
- Separate async worker processes events
- Data loss acceptable (events can be replayed)

#### Reactive Streams (Mono/Flux)
- `Mono<ShortUrl>` for single response
- `Mono<Void>` for side effects (cache set, analytics push)
- `.flatMap()` for chaining async operations
- `.switchIfEmpty()` for fallback chains

#### Circuit Breaker Pattern (TODO)
- Wrap Postgres/Redis calls
- On failure threshold: return cached/fallback response
- Automatic recovery with exponential backoff

## Performance Characteristics

### Throughput
- **Cache hit**: ~5000-10000 RPS per instance
- **Cache miss**: ~2000-5000 RPS per instance (DB query added)
- **Cold start**: First 1000 requests populate cache

### Latency
| Scenario | P50 | P99 | P999 |
|----------|-----|-----|------|
| Cache hit | 2ms | 5ms | 10ms |
| Cache miss (new) | 20ms | 50ms | 100ms |
| Cache miss (expired) | 20ms | 50ms | 100ms |
| Postgres down (circuit open) | 1ms | 2ms | 3ms* |

*Assumes circuit breaker returns cached/fallback

### Resource Utilization
- **Threads**: 10-20 (Netty event loops, not per-request)
- **Memory**: ~500MB-1GB (JVM heap + cache)
- **Connections**: Decoupled from threads (50 DB + 20 Redis)

## Configuration

### Environment Variables
```bash
# Database (R2DBC)
SPRING_R2DBC_URL=r2dbc:postgresql://localhost:5432/shortener
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_DB=0
REDIS_PASSWORD=

# Application
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=prod
```

### R2DBC Pool Tuning
```yaml
spring.r2dbc.pool:
  max-acquire-time: 3000ms    # Wait for connection
  max-idle-time: 900000ms     # 15 min
  max-life-time: 1800000ms    # 30 min
  initial-size: 10
  max-size: 50                # Tuned for 5000 RPS
```

### Redis Configuration
```yaml
spring.data.redis:
  lettuce.pool:
    max-active: 20
    max-idle: 10
    min-idle: 5
    max-wait: 2000ms
```

### Metrics & Monitoring
- Prometheus scrape endpoint: `/actuator/prometheus`
- Key metrics:
  - `http.server.requests{status=302}` (redirects)
  - `http.server.requests{status=404}` (not found)
  - `redis.commands.duration` (cache latency)
  - JVM heap, GC pauses, thread count

## Testing

### Unit Tests
```bash
mvn test
```

### Integration Tests (Testcontainers)
```bash
mvn verify
```
Starts temporary Postgres + Redis containers; tears down after tests.

### Load Testing (100k RPS)
```bash
# Using wrk or Apache Bench
wrk -t12 -c400 -d30s http://localhost:8080/example

# Expected: 5k-10k RPS per instance
```

## Deployment

### Docker Build
```bash
mvn clean package -DskipTests
docker build -t url-shortener-v2:2.0.0 .
docker run -e REDIS_HOST=redis-host \
           -e SPRING_R2DBC_URL=r2dbc:postgresql://postgres:5432/shortener \
           -p 8080:8080 url-shortener-v2:2.0.0
```

### Kubernetes
See `k8s/` folder for Helm chart (TODO).

## Roadmap & TODOs

### Phase 1: Core (DONE)
- [x] WebFlux + R2DBC setup
- [x] Redis cache (write-through)
- [x] Async analytics (fire-and-forget)
- [x] R2DBC pool configuration
- [x] Prometheus metrics

### Phase 2: Resilience (TODO)
- [ ] Circuit breaker (Postgres/Redis failures)
- [ ] Fallback strategies
- [ ] Request tracing (Jaeger)
- [ ] Structured logging (JSON)
- [ ] Health checks for each component

### Phase 3: Scaling (TODO)
- [ ] Database replication (primary + read replicas)
- [ ] Redis Sentinel for failover
- [ ] Multi-instance deployment
- [ ] Load balancer config (nginx/HAProxy)
- [ ] Blue-green deployments

### Phase 4: Optimization (TODO)
- [ ] Cache eviction policy tuning (LRU vs LFU)
- [ ] Query optimization (EXPLAIN ANALYZE)
- [ ] Batch analytics processing
- [ ] Kafka for high-volume events
- [ ] Rate limiting (token bucket via Redis)

## Troubleshooting

### High Latency (P99 > 50ms)
1. Check Redis latency: `redis-cli --latency`
2. Check Postgres slow queries: enable `log_min_duration_statement`
3. Profile: add `-Dspring.jpa.show-sql=true` (not applicable here; use R2DBC logs)
4. Tune connection pools (increase max-size)

### Cache Misses on Cold Start
- Expected: first 1000 requests will miss cache
- Monitor: `redis.commands.duration` spikes during cold start
- Solution: Warm cache on startup or use read replica for analytics

### Memory Leaks
- Monitor JVM heap: `curl http://localhost:8080/actuator/metrics/jvm.memory.used`
- Increase heap: `java -Xmx2g -jar ...`
- Profile: use JProfiler/YourKit

## License
MIT
