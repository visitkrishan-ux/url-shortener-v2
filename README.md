# URL Shortener V2 (Reactive, Production-Ready)

A high-performance URL shortener built with **Spring WebFlux + R2DBC**.  
This repository contains **V2 only** (reactive implementation), with two practical run modes:

- **Simple mode**: H2 in-memory (zero external setup)
- **Advanced mode**: PostgreSQL + Redis + resilience patterns (structured rollout)

---

## ✨ Key Features

- ⚡ **Reactive, non-blocking architecture** (WebFlux + Netty)
- 🔗 **Create short URLs** with optional custom alias
- ↪️ **HTTP redirect** from short code to original URL
- 🧪 **Clear request validation** (greenfield / brownfield / ambiguous handling)
- 🩺 **Health endpoint** for quick runtime checks
- 🧱 **Two deployment paths**: quick local and production-grade
- 📈 **Scalable design** with optional cache + resilience layering
- 📚 **Comprehensive docs** for dev, QA, and DevOps workflows

---

## 🧭 What’s in this Repository

> ✅ **Implemented and committed:** V2 reactive solution  
> ℹ️ **V1 sync version:** reference-only (not implemented in this repo)

### Available Documentation

- [`QUICKSTART.md`](QUICKSTART.md) — 2-minute local startup
- [`TESTING_GUIDE.md`](TESTING_GUIDE.md) — API test scenarios
- [`ARCHITECTURE.md`](ARCHITECTURE.md) — design overview
- [`DEPLOYMENT_GUIDE.md`](DEPLOYMENT_GUIDE.md) — deployment strategy
- [`ADVANCED_SETUP.md`](ADVANCED_SETUP.md) — PostgreSQL + Redis + resilience path
- [`SOLUTION_COMPARISON.md`](SOLUTION_COMPARISON.md) — alternatives and tradeoffs
- [`START_HERE.md`](START_HERE.md) — role-based navigation

---

## 🚀 Quick Start (Simple Mode)

### Prerequisites
- Java 21+
- Maven 3.8+ (optional if using prebuilt JAR)

### Run
```bash
java -jar target/url-shortener-v2-2.0.0.jar
```

### Verify
- Health: `GET http://localhost:8080/health`
- Create short URL: `POST http://localhost:8080/api/shorten`
- Redirect: `GET http://localhost:8080/{code}`

---

## 🧪 API Snapshot

### Create short URL
`POST /api/shorten`

Request:
```json
{
  "url": "https://example.com/some/long/path",
  "customAlias": "example"
}
```

Success response (example):
```json
{
  "code": "example",
  "shortUrl": "http://localhost:8080/example",
  "originalUrl": "https://example.com/some/long/path"
}
```

### Redirect
`GET /{code}` → `302 Found` with `Location` header

### Health
`GET /health`
```json
{ "status": "UP" }
```

---

## 🏗️ Structured Upgrade Path (Minimal Client Installs)

### Stage A — Local validation (Java only)
- H2 in-memory
- No Docker, no DB install

### Stage B — Persistent storage
- Add PostgreSQL (Docker Compose)
- Keep Redis off

### Stage C — Performance layer
- Add Redis cache only if needed by latency/throughput targets

### Stage D — Production hardening
- Enable resilience (timeouts, retry, circuit breaker)
- Add metrics/monitoring

> Detailed commands/config: [`ADVANCED_SETUP.md`](ADVANCED_SETUP.md)

---

## ✅ Important Implementation Notes

- **V2-first architecture** selected for better throughput and scalability
- **Stateless API design** supports horizontal scaling
- **Validation and error responses** are explicit and client-friendly
- **Simple mode is intentional** for fast onboarding and evaluation
- **Advanced mode is structured** to avoid unnecessary early infrastructure installs

---

## 📌 Current Status

- ✅ Buildable
- ✅ Runnable locally
- ✅ Core scenarios tested (greenfield, brownfield, ambiguous, redirect, health)
- ✅ Documentation aligned for both quick testing and production evolution

---

## 🤝 Contribution

PRs are welcome for:
- Extended analytics
- Alias governance policies
- Rate limiting and abuse controls
- Helm/Kubernetes packaging improvements

---

## 📄 License

MIT
