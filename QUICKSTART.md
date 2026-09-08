# URL Shortener V2 - Quick Start (2 Minutes)

## What You Need
- **Java 21+** (That's it! No database, no Docker, no Redis required.)
- **Maven 3.8+** (if you want to rebuild from source)

## Option 1: Run Pre-Built (Fastest)

```bash
# Download and run the ready-to-go JAR
java -jar target/url-shortener-v2-2.0.0.jar
```

✅ App starts on `http://localhost:8080`

## Option 2: Build & Run from Source

```bash
# Clone or download the repo
cd url-shortener-v2

# Build (2-3 min)
mvn clean package -DskipTests

# Run
java -jar target/url-shortener-v2-2.0.0.jar
```

## Test It (PowerShell)

```powershell
# 1. Create a short URL
$body = @{
    url = "https://github.com/visitkrishan-ux/url-shortener-v2"
    customAlias = "myrepo"
} | ConvertTo-Json

$result = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/shorten" `
    -ContentType "application/json" -Body $body

Write-Output $result
# Output: code=myrepo, shortUrl=http://localhost:8080/myrepo

# 2. Use the short URL (browser or curl)
# http://localhost:8080/myrepo
# → Redirects to original URL

# 3. Try duplicates (should error)
$dup = @{ url = "https://google.com"; customAlias = "myrepo" } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/shorten" `
    -ContentType "application/json" -Body $dup
# Error: "Alias already exists: myrepo"
```

## Test It (cURL / Bash)

```bash
# Create short URL
curl -X POST "http://localhost:8080/api/shorten" \
  -H "Content-Type: application/json" \
  -d '{"url":"https://github.com/visitkrishan-ux","customAlias":"gh"}'

# Follow redirect
curl -L "http://localhost:8080/gh"

# Check health
curl "http://localhost:8080/health"
```

## Test It (Browser)

1. Open Postman or Insomnia
2. **POST** to `http://localhost:8080/api/shorten`
3. Body (JSON):
   ```json
   {
     "url": "https://en.wikipedia.org/wiki/URL_shortening",
     "customAlias": "wiki"
   }
   ```
4. Click **Send** → See response with short URL
5. Visit `http://localhost:8080/wiki` in browser → Redirects to Wikipedia

## What Works

| Scenario | Test | Result |
|----------|------|--------|
| **Greenfield** | POST new alias | ✅ Creates short URL (200 OK) |
| **Brownfield** | POST duplicate alias | ✅ Returns error (400 Bad Request) |
| **Ambiguous** | POST missing URL | ✅ Returns error (400 Bad Request) |
| **Redirect** | GET /{alias} | ✅ 302 redirect to original URL |
| **Health** | GET /health | ✅ Returns `{"status":"UP"}` |

## API Reference

### POST /api/shorten
Create a short URL.

**Request:**
```json
{
  "url": "https://example.com/very/long/path?param=value",
  "customAlias": "mylink"  // optional; auto-generated if omitted
}
```

**Response (200):**
```json
{
  "code": "mylink",
  "url": "https://example.com/very/long/path?param=value",
  "shortUrl": "http://localhost:8080/mylink",
  "createdAt": "2026-09-08T12:45:00Z"
}
```

**Error (400):**
```json
{
  "error": "Alias already exists: mylink"
}
```

### GET /{code}
Redirect to original URL.

**Request:**
```
GET http://localhost:8080/mylink
```

**Response:**
- HTTP 302 (Found)
- Location header: `https://example.com/...`

### GET /health
Health check.

**Response (200):**
```json
{
  "status": "UP"
}
```

## Troubleshooting

| Issue | Fix |
|-------|-----|
| `Address already in use` | Another app on port 8080. Stop it or use `java -jar ... --server.port=9000` |
| `java: command not found` | Java not installed or not in PATH. Download Java 21 from oracle.com. |
| `Connection refused` | App not running. Check console for startup errors. |
| `Alias already exists` | That short code is taken. Choose a different `customAlias`. |

## Architecture

```
User Request
    ↓
[ShortUrlController] - HTTP routing
    ↓
[ShortUrlService] - Business logic
    ↓
[ShortUrlRepository] - Data access via R2DBC
    ↓
[H2 Database] - In-memory, no setup needed
```

**No external dependencies:**
- ✅ Database: H2 (embedded in JAR)
- ✅ Cache: Unnecessary for in-memory DB
- ✅ Message queue: Fire-and-forget click tracking

## Production Upgrade Path

When ready to scale:
1. **Persistence**: Switch to PostgreSQL (update `application.yml`)
2. **Caching**: Add Redis for hot URLs
3. **Monitoring**: Enable Spring Actuator metrics
4. **Load Balancing**: Deploy multiple instances behind Nginx

See `DEPLOYMENT_GUIDE.md` for details.

---

**Questions?** Check `README.md` or open an issue on GitHub.
