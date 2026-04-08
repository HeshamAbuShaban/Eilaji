# Eilaji Backend

Ktor-based backend service for the Eilaji pharmacy delivery platform.

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
| POST | `/api/v1/auth/refresh` | Refresh JWT token |

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
| POST | `/api/v1/orders` | Create order from accepted prescription |
| GET | `/api/v1/orders` | List user orders |
| GET | `/api/v1/orders/{id}` | Get order details |
| PUT | `/api/v1/orders/{id}/status` | Update order status (PHARMACIST/ADMIN only) |

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

### Health & Metrics
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/health` | Service health check |
| GET | `/metrics` | Prometheus metrics |

## Environment Variables

Create a `.env` file in the `eilaji-backend` directory:

```env
# Database
DB_URL=jdbc:postgresql://localhost:5432/eilaji
DB_USER=eilaji_user
DB_PASSWORD=your_secure_password
DB_DRIVER=org.postgresql.Driver

# Redis
REDIS_URL=redis://localhost:6379

# MinIO
MINIO_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadminpassword
MINIO_REGION=us-east-1

# JWT
JWT_SECRET=your_super_secret_jwt_key_min_32_chars
JWT_ISSUER=eilaji-backend
JWT_AUDIENCE=eilaji-users

# Eilaji-Plus (optional)
EILAJI_PLUS_BASE_URL=https://plus.eilaji.com/api
EILAJI_PLUS_API_KEY=your_api_key

# Server
SERVER_PORT=8080
```

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
- **Health**: `GET /health` returns DB, Redis, MinIO status
- **Metrics**: `GET /metrics` exposes Prometheus-compatible metrics
- **Logging**: JSON structured logs (ELK-compatible)

### Rate Limiting
Default: 100 requests per minute per IP. Configurable in `Application.kt`.

## Project Structure

```
backend/src/main/kotlin/com/eilaji/backend/
├── Application.kt          # Entry point
├── config/                 # Configuration & plugins
│   ├── DatabaseConfig.kt
│   └── RateLimitPlugin.kt
├── controller/             # Route handlers
│   ├── Routes.kt
│   └── WebSocketController.kt
├── data/                   # Database tables
│   ├── Tables.kt
│   └── UserTable.kt
├── dto/                    # Data transfer objects
├── model/                  # Domain models
├── security/               # JWT & auth
├── service/                # Business logic
│   ├── OrderService.kt
│   ├── PrescriptionService.kt
│   └── RedisService.kt
└── websocket/              # WebSocket handling
```

## License

Proprietary - Eilaji Inc.
