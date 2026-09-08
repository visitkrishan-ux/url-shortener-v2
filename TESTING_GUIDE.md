# URL Shortener V2 - Zero-Install Testing Guide

## The Promise
✅ **No software to install before testing. Java 21 + this repo = working shortener in 2 minutes.**

---

## Step-by-Step (30 seconds setup)

### 1. Verify Java is installed
```bash
java -version
# Expected: Java 21 or higher
```

If missing: [Download Java 21](https://www.oracle.com/java/technologies/downloads/#java21)

### 2. Get the code
```bash
git clone https://github.com/visitkrishan-ux/url-shortener-v2.git
cd url-shortener-v2
```

### 3. Build (one command, takes 2-3 minutes the first time)
```bash
mvn clean package -DskipTests
```

### 4. Run (instant startup)
```bash
java -jar target/url-shortener-v2-2.0.0.jar
```

**Output:**
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.2.0)

2026-09-08T12:46:15.123-05:00  INFO ... Started UrlShortenerV2Application in 10.234 seconds
```

✅ App is ready!

---

## Test Scenarios (Copy-Paste Ready)

### Scenario 1: Greenfield (New Short URL)
**What:** Creating a brand new short URL with a custom alias.

**PowerShell:**
```powershell
$body = @{
    url = "https://en.wikipedia.org/wiki/Spring_Framework"
    customAlias = "springfw"
} | ConvertTo-Json

Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/shorten" `
    -ContentType "application/json" -Body $body | ConvertTo-Json
```

**Expected Result (HTTP 200):**
```json
{
  "code": "springfw",
  "url": "https://en.wikipedia.org/wiki/Spring_Framework",
  "shortUrl": "http://localhost:8080/springfw",
  "createdAt": "2026-09-08T12:47:00Z"
}
```

**Bash/cURL:**
```bash
curl -X POST "http://localhost:8080/api/shorten" \
  -H "Content-Type: application/json" \
  -d '{"url":"https://en.wikipedia.org/wiki/Spring_Framework","customAlias":"springfw"}'
```

---

### Scenario 2: Brownfield (Duplicate Alias)
**What:** Trying to create a second short URL with an alias that already exists.

**PowerShell:**
```powershell
$body = @{
    url = "https://google.com"
    customAlias = "springfw"  # Already exists from Scenario 1
} | ConvertTo-Json

try {
    Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/shorten" `
        -ContentType "application/json" -Body $body -ErrorAction Stop
} catch {
    Write-Output "Error Response:"
    $_.ErrorDetails.Message | ConvertFrom-Json | ConvertTo-Json
}
```

**Expected Result (HTTP 400):**
```json
{
  "error": "Alias already exists: springfw"
}
```

**Bash/cURL:**
```bash
curl -X POST "http://localhost:8080/api/shorten" \
  -H "Content-Type: application/json" \
  -d '{"url":"https://google.com","customAlias":"springfw"}'

# Returns: {"error":"Alias already exists: springfw"}
```

---

### Scenario 3: Ambiguous (Missing URL)
**What:** Sending a request without the required `url` field.

**PowerShell:**
```powershell
$body = @{
    customAlias = "test"
} | ConvertTo-Json

try {
    Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/shorten" `
        -ContentType "application/json" -Body $body -ErrorAction Stop
} catch {
    $_.ErrorDetails.Message | ConvertFrom-Json | ConvertTo-Json
}
```

**Expected Result (HTTP 400):**
```json
{
  "error": "Missing required field: url"
}
```

---

### Scenario 4: Redirect (Follow Short URL)
**What:** Using the short URL to redirect to the original URL.

**Browser:**
Simply visit: `http://localhost:8080/springfw`
→ You'll be redirected to `https://en.wikipedia.org/wiki/Spring_Framework`

**PowerShell (show redirect without following):**
```powershell
Invoke-WebRequest -Uri "http://localhost:8080/springfw" `
    -MaximumRedirection 0 -ErrorAction SilentlyContinue | `
    Select-Object @{N="Status";E={$_.StatusCode}}, @{N="Location";E={$_.Headers['Location']}}
```

**Expected:**
```
Status Location
------ --------
  302 https://en.wikipedia.org/wiki/Spring_Framework
```

---

### Scenario 5: Health Check
**What:** Verifying the app is running and healthy.

**PowerShell:**
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/health"
```

**Expected Result:**
```json
{
  "status": "UP"
}
```

**Bash/cURL:**
```bash
curl "http://localhost:8080/health"
```

---

## Test Checklist

Run these in order. If all pass, the system is working correctly.

```
[ ] Scenario 1: POST new alias → 200 + response JSON with code
[ ] Scenario 2: POST duplicate → 400 + "Alias already exists" error
[ ] Scenario 3: POST no URL → 400 + "Missing required field" error
[ ] Scenario 4: GET /{code} → 302 redirect + Location header
[ ] Scenario 5: GET /health → 200 + {"status":"UP"}
```

---

## Why This Works (No Database Install)

```
┌─────────────────────────────────────────┐
│     urlshortener-v2-2.0.0.jar          │
│  (Everything bundled inside JAR)        │
├─────────────────────────────────────────┤
│  Spring Boot 3.2.0                      │
│  Spring WebFlux (Reactive HTTP)         │
│  Spring Data R2DBC                      │
│  H2 Database (embedded)                 │
│  Netty (async server)                   │
└─────────────────────────────────────────┘
         ↑
    No external 
    dependencies!
```

**Key points:**
- **H2 Database**: Embedded inside the JAR, starts automatically with the app
- **No PostgreSQL**: Not needed. H2 provides everything locally.
- **No Docker**: Just Java.
- **No Redis**: Caching not needed for this test size.
- **No configuration**: Works out of the box.

---

## Frequently Asked Questions

### Q: Do I need to install PostgreSQL?
**A:** No. The app uses H2 (embedded database). Zero installation needed.

### Q: Can I use a real database for production?
**A:** Yes. Edit `src/main/resources/application.yml` and switch from H2 to PostgreSQL. See `DEPLOYMENT_GUIDE.md`.

### Q: Why is the first build slow?
**A:** Maven downloads dependencies from the internet (~300 MB). Subsequent builds are instant (cached).

### Q: Where does it store data?
**A:** In memory (RAM). Closes when you stop the app. Perfect for testing. For persistence, use `r2dbc:h2:file:./data` or PostgreSQL.

### Q: Can I run multiple instances?
**A:** Yes, on different ports: `java -jar target/url-shortener-v2-2.0.0.jar --server.port=8081`

### Q: What if I get "Address already in use"?
**A:** Port 8080 is occupied. Either:
- Kill the other process: `netstat -ano | findstr :8080` (Windows)
- Change port: `java -jar target/url-shortener-v2-2.0.0.jar --server.port=9000`

---

## Next Steps

1. ✅ **You've tested it locally** → Works!
2. 📖 **Read deployment guide** → `DEPLOYMENT_GUIDE.md` (scaling to prod)
3. 🚀 **Deploy to cloud** → AWS/Azure/GCP docs included
4. 📊 **Monitor in production** → Actuator metrics enabled

---

## Support

- **Issue?** Check console output for error messages.
- **Stuck?** See `QUICKSTART.md` for common issues.
- **Code questions?** See `ARCHITECTURE.md` for system design.

Enjoy! 🚀
