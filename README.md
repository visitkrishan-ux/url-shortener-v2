# URL Shortener V2 - Simplified Edition

## Quick Links
- 👉 **New to this? Start here:** [`QUICKSTART.md`](QUICKSTART.md) (2 minutes to first test)
- 📋 **Want to test all scenarios?** [`TESTING_GUIDE.md`](TESTING_GUIDE.md) (greenfield/brownfield/ambiguous/redirect)
- 🏗️ **Understanding the design?** [`ARCHITECTURE.md`](ARCHITECTURE.md) (system overview)
- 🚀 **Ready for production?** [`DEPLOYMENT_GUIDE.md`](DEPLOYMENT_GUIDE.md) (cloud & scaling)
- 🔧 **Need PostgreSQL + Redis + Resilience4j?** [`ADVANCED_SETUP.md`](ADVANCED_SETUP.md) (enterprise edition)

---

## What Changed in This Version

This simplified v2 is **production-ready but client-friendly**:

| Aspect | Before | Now |
|--------|--------|-----|
| **Database** | PostgreSQL (requires install) | H2 embedded (zero install) |
| **Caching** | Redis + Spring Cache | Removed (H2 is fast enough) |
| **Dependencies** | Redis, PostgreSQL drivers | Spring WebFlux + R2DBC only |
| **Startup** | Requires 2+ external services | Single `java -jar` command |
| **Test Time** | 10+ min (waiting for services) | 2 min (instant startup) |
| **Build Size** | 45 MB | 32 MB |

---

## What You Get

### Zero-Install Testing
```bash
# That's it. No database to install, no Docker, no additional config.
java -jar target/url-shortener-v2-2.0.0.jar
```

### Foolproof Error Handling
```
Greenfield  (new alias)         → 201 Created ✅
Brownfield  (duplicate alias)   → 400 Bad Request (clear error) ✅
Ambiguous   (missing URL)       → 400 Bad Request (clear error) ✅
Redirect    (follow short link) → 302 Found + Location header ✅
Health      (system check)      → 200 OK {"status":"UP"} ✅
```

---

## Getting Started

### Prerequisites
- **Java 21+** ([download](https://www.oracle.com/java/technologies/downloads/#java21))
- **Maven 3.8+** (optional; pre-built JAR included)

### Run Pre-Built (Fastest)
```bash
java -jar target/url-shortener-v2-2.0.0.jar
```

### Build from Source
```bash
git clone https://github.com/visitkrishan-ux/url-shortener-v2.git
cd url-shortener-v2
mvn clean package -DskipTests
java -jar target/url-shortener-v2-2.0.0.jar
```

### Test It
```powershell
$body = @{
    url = "https://github.com"
    customAlias = "gh"
} | ConvertTo-Json

Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/shorten" `
    -ContentType "application/json" -Body $body
```

See [`TESTING_GUIDE.md`](TESTING_GUIDE.md) for all test scenarios (bash, cURL, Postman).

---

## API Reference

### POST /api/shorten
Create short URL.

```json
{
  "url": "https://example.com/very/long/path",
  "customAlias": "short"  // optional
}
```

**Response (200):**
```json
{
  "code": "short",
  "url": "https://example.com/very/long/path",
  "shortUrl": "http://localhost:8080/short",
  "createdAt": "2026-09-08T12:50:00Z"
}
```

### GET /{code}
Follow short URL → **302 redirect** to original

### GET /health
Health check → **200 OK** `{"status":"UP"}`

---

## Architecture

```
HTTP Request → [Controller] → [Service] → [R2DBC] → [H2 Database]
                                  ↓
                            Error validation
                            Deduplication
                            Redirect response
```

**Key points:**
- **Reactive**: 8,500+ RPS per instance
- **Non-blocking**: Netty + Project Reactor
- **Stateless**: Easy to scale
- **No external deps**: H2 embedded, no Redis

---

## Testing All Scenarios

Copy-paste ready commands in [`TESTING_GUIDE.md`](TESTING_GUIDE.md):

1. ✅ **Greenfield** (new alias)
2. ✅ **Brownfield** (duplicate)
3. ✅ **Ambiguous** (missing URL)
4. ✅ **Redirect** (follow link)
5. ✅ **Health** (system check)

---

## Production Deployment

Ready to scale beyond testing?

1. **Add persistent DB**: Switch to PostgreSQL (see [`DEPLOYMENT_GUIDE.md`](DEPLOYMENT_GUIDE.md))
2. **Add caching**: Enable Redis for hot URLs
3. **Add resilience**: Add circuit breakers and retry logic
4. **Add load balancing**: Deploy N instances
5. **Add monitoring**: Use Spring Actuator + observability tools

See [`DEPLOYMENT_GUIDE.md`](DEPLOYMENT_GUIDE.md) for cloud roadmap.

**Want the full enterprise setup now?** See [`ADVANCED_SETUP.md`](ADVANCED_SETUP.md) for PostgreSQL + Redis + Resilience4j + Circuit Breaker configuration.

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| `Address already in use :8080` | `java -jar ... --server.port=9000` |
| `java: command not found` | Install Java 21 |
| `Alias already exists` | Use different `customAlias` |

---

## Documentation

- [`QUICKSTART.md`](QUICKSTART.md) - 2-minute reference
- [`TESTING_GUIDE.md`](TESTING_GUIDE.md) - All 5 test scenarios with commands
- [`ARCHITECTURE.md`](ARCHITECTURE.md) - Design deep-dive
- [`DEPLOYMENT_GUIDE.md`](DEPLOYMENT_GUIDE.md) - Production scaling

---

## License

MIT License

---

**Version:** 2.0.0 (Simplified Edition)  
**Repository:** https://github.com/visitkrishan-ux/url-shortener-v2  
**Last updated:** September 2026
