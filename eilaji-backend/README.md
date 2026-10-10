# Eilaji Backend

Ktor-based backend for the Eilaji pharmacy platform. See root [README.md](../README.md) for overview, architecture, and Android setup.

## Quick Start

### Prerequisites
- Java 21 (or Java 17 with adjusted `build.gradle.kts`)
- Docker & Docker Compose

### One-Command Setup
```bash
cd eilaji-backend
docker-compose up -d
./gradlew :backend:run
```

The API will be available at `http://localhost:8080`.

## API Endpoints

### Authentication
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/auth/register` | Register new user |
| POST | `/api/v1/auth/login` | Login user |
| POST | `/api/v1/auth/refresh` | Exchange a refresh token for a new pair (rotated on use) |
| POST | `/api/v1/auth/forgot-password` | Start a password reset |
| POST | `/api/v1/auth/reset-password` | Finish a password reset with the code |

### Medicines (Public)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/medicines` | List all medicines |
| GET | `/api/v1/medicines/search?q=query` | Search medicines |
| GET | `/api/v1/medicines/{id}` | Get medicine by ID |
| GET | `/api/v1/medicines/categories` | List categories |

### Pharmacies
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/pharmacies/nearby?lat=&lng=&radius=` | Find nearby pharmacies |
| POST | `/api/v1/pharmacies/register` | Register pharmacy (PHARMACIST only) |

### Prescriptions (Protected)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/prescriptions` | Upload prescription (multipart: image, notes, selectedPharmacyId) |
| GET | `/api/v1/prescriptions` | List user's prescriptions |
| GET | `/api/v1/prescriptions/{id}` | Get prescription details |
| PUT | `/api/v1/prescriptions/{id}/status` | Update status (PHARMACIST only) |

### Orders (Protected)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/orders` | Create order (prescription-linked or direct OTC with `prescriptionId: null`) |
| GET | `/api/v1/orders` | List user orders (pharmacists see only owned stores) |
| GET | `/api/v1/orders/{id}` | Get order details |
| PUT | `/api/v1/orders/{id}/status` | Update status `PENDING→CONFIRMED→PREPARING→SHIPPED→DELIVERED` (PHARMACIST/ADMIN only, terminal states locked) |
| PUT | `/api/v1/orders/{id}/cancel` | Cancel an order: patient cancels their own, pharmacy/admin their store |
| PUT | `/api/v1/orders/{id}/courier` | Update live courier position `{lat, lng, etaMinutes}` or `{clear: true}` (PHARMACIST/ADMIN only) |

### Stock (read public, write PHARMACIST/ADMIN)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/pharmacies/{id}/stock` | List stock rows for a pharmacy |
| PUT | `/api/v1/pharmacies/{id}/stock` | Upsert `{medicineId, price?, stockQuantity?, isAvailable?}` (own stores only) |

**Example: Create Order**
```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"prescriptionId":1,"totalAmount":50.00,"paymentMethod":"CARD","deliveryAddress":"123 Main St"}'
```

### Chat (Protected)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/chat` | Create chat session |
| GET | `/api/v1/chat/{chatId}/messages` | Get messages |
| POST | `/api/v1/chat/{chatId}/read` | Mark as read |
| WS | `/api/v1/ws/chat?token=JWT` | WebSocket connection |

**WebSocket Example (JavaScript)**
```javascript
const ws = new WebSocket('ws://localhost:8080/api/v1/ws/chat?token=' + jwtToken);
ws.onmessage = (event) => console.log('Message:', event.data);
ws.send(JSON.stringify({ type: 'MESSAGE', content: 'Hello!' }));
```

### Admin (ADMIN role only)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/admin/users` | List all users |
| PUT | `/api/v1/admin/users/{id}/verify` | Verify pharmacist |
| GET | `/api/v1/admin/prescriptions` | List all prescriptions |
| GET | `/api/v1/admin/analytics` | Get platform statistics |

### Health
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/health` | Liveness + Postgres and Redis checks (`UP` or `DEGRADED`) |

## Ops Dashboard (dev)

Full testing guide (loops, roles, troubleshooting): [`docs/TESTING-DASHBOARD.md`](../docs/TESTING-DASHBOARD.md).

Visual tester for pharmacy flows at `http://localhost:8080/dashboard` — sign in with a seed account, no E-Doctor repo needed:

- **Pharmacy tab** — order queue with status advance/cancel/COD-collect, courier simulator slider (drives the app's live tracking map), pending prescriptions with image preview + quote/accept/reject.
- **Stock tab** — search medicines, set quantity/price, add or hide items (powers future stock badges).
- **Chats tab** — thread list with message peek for testing ghost conversations.
- **Customer tab** — place direct OTC test orders, view your orders.
- Session persists in browser, 10s live-polling toggle, dark mode. All calls use the same public API above.

## Environment Variables

Defaults live in `backend/src/main/resources/application.conf`.
Every value has an environment override. Production uses `docker-compose.prod.yml`.

```env
# Profile: dev | test | prod. prod runs migrations only and refuses to seed.
APP_PROFILE=dev

# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/eilaji_db
DATABASE_USER=eilaji
DATABASE_PASSWORD=change_me

# Redis (credentials supported: redis://:password@host:port)
REDIS_URL=redis://localhost:6379

# MinIO
MINIO_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=eilaji_minio_admin
MINIO_SECRET_KEY=change_me

# JWT — required in production, 32+ characters, never committed
JWT_SECRET=replace_with_a_long_random_string
JWT_ISSUER=eilaji-backend
JWT_AUDIENCE=eilaji-client
JWT_EXPIRES_IN=3600000
JWT_REFRESH_EXPIRES_IN=2592000000

# Browser origins allowed with credentials, comma separated
CORS_ALLOWED_HOSTS=

# Seeding (development only; keep both false in production)
SEED_ON_STARTUP=false
SEED_DEMO_CREDENTIALS=false

# Limits
MAX_UPLOAD_BYTES=8388608
MAX_WS_FRAME_BYTES=1048576
SEARCH_MAX_RADIUS_KM=50
BCRYPT_COST=12

# Rate limiting
RATE_LIMIT_PER_MINUTE=100
RATE_LIMIT_LOGIN=5
RATE_LIMIT_REGISTER=3
RATE_LIMIT_REFRESH=10
RATE_LIMIT_FAIL_CLOSED=true

# Eilaji-Plus (optional)
EILAJI_PLUS_BASE_URL=https://plus.eilaji.com/api
EILAJI_PLUS_API_KEY=your_api_key
EILAJI_PLUS_WEBHOOK_SECRET=shared_webhook_signing_secret
```

Rules for secrets:

1. `JWT_SECRET` has no default. `APP_PROFILE=prod` without it is a startup failure.
2. Nothing secret belongs in `application.conf`.
3. Seeding never runs in production, and demo accounts are opt-in.

## Testing

Run tests:
```bash
./gradlew :backend:test
```

## Production Deployment

### Docker Compose (Production)
```bash
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

### Monitoring
- **Health**: `GET /health` reports `UP` only when Postgres and Redis answer
- **Logging**: JSON structured logs (ELK-compatible), request path without token query strings

### Rate limiting
100 requests per minute per IP by default, with strict buckets on the auth routes
(5 logins/min, 3 registrations/hour). When Redis is unreachable, the strict buckets answer 429
instead of silently dropping all limits.

## Project Structure

```
backend/src/main/kotlin/com/eilaji/backend/
├── Application.kt          # Entry point
├── config/                 # Configuration & plugins
│   ├── DatabaseConfig.kt
│   └── RateLimitPlugin.kt
├── controller/             # Route handlers
│   ├── Routes.kt           # incl. courier + stock routes
│   ├── AdminController.kt
│   └── AuthController.kt
├── data/                   # Database tables + migrations
│   ├── Tables.kt
│   └── Migrations.kt        # named, idempotent schema changes
├── dto/                    # Data transfer objects
├── initialization/         # DatabaseSeeder (v2 taxonomy)
├── security/               # JWT, AppConfig (env), auth helpers, audit
├── service/                # Business logic
│   ├── OrderService.kt     # state machine + courier updates
│   ├── PrescriptionService.kt
│   ├── ChatService.kt / MessageService.kt
│   ├── EilajiPlusService.kt / MinioService.kt / RedisService.kt
├── websocket/              # WebSocketController + SessionManager
└── resources/static/dashboard/  # Ops dashboard (index.html + app.js)
```

## License

Portfolio / educational use only — not licensed for commercial use or redistribution.
© Hesham AbuShaban, 2025. All rights reserved. See [LICENSE](../LICENSE).
