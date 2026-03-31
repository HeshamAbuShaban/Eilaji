# 🚀 Eilaji Backend - Complete Implementation Plan

## 📋 Executive Summary

This document outlines the **complete implementation strategy** for migrating Eilaji from Firebase to a **custom Kotlin backend** using **Ktor + Exposed + PostgreSQL**. This backend will serve both:
- **Eilaji** (Patient App) - Current repo
- **Eilaji-Plus** (Pharmacy/Doctor App) - Separate repo

### Why This Migration?
- ✅ **Cost Reduction**: From $200-500/month → $50-100/month (60-80% savings)
- ✅ **Full Control**: Complete ownership of data, logic, and integrations
- ✅ **Better Integration**: Seamless communication between Eilaji ↔ Eilaji-Plus
- ✅ **No JavaScript/PHP**: 100% Kotlin stack matching your expertise
- ✅ **Cutting-Edge Tech**: Ktor, Coroutines, Type-safe SQL with Exposed

---

## 🏗️ System Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         EILAJI ECOSYSTEM                                 │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌──────────────┐                          ┌──────────────────────┐    │
│  │   Eilaji     │◄────── REST API ────────►│    Eilaji-Plus       │    │
│  │  (Patients)  │      WebSocket           │ (Pharmacies/Doctors) │    │
│  │  Android App │      FCM Push            │    Android App       │    │
│  └──────────────┘                          └──────────────────────┘    │
│         │                                       │                       │
│         └───────────────────┬───────────────────┘                       │
│                             │                                           │
│                    ┌────────▼────────┐                                  │
│                    │  Ktor Server    │                                  │
│                    │  (Kotlin JVM)   │                                  │
│                    └────────┬────────┘                                  │
│                             │                                           │
│         ┌───────────────────┼───────────────────┐                       │
│         │                   │                   │                       │
│  ┌──────▼──────┐   ┌───────▼───────┐   ┌──────▼──────┐                 │
│  │ PostgreSQL  │   │    Redis      │   │   MinIO/S3  │                 │
│  │  (Primary)  │   │  (Cache+PubSub)│  │  (Files)    │                 │
│  └─────────────┘   └───────────────┘   └─────────────┘                 │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### Technology Stack

| Component | Technology | Why? |
|-----------|-----------|------|
| **Language** | Kotlin 2.0+ | Your primary language, type-safe, coroutines |
| **Web Framework** | Ktor 2.3+ | Native Kotlin, lightweight, async-first |
| **ORM** | Exposed 0.45+ | JetBrains' type-safe SQL in Kotlin |
| **Database** | PostgreSQL 16 | Relational + PostGIS for geospatial queries |
| **Cache/PubSub** | Redis 7+ | Real-time messaging, session cache |
| **File Storage** | MinIO (self-hosted) | S3-compatible, free, runs on your machine |
| **Auth** | JWT + BCrypt | Stateless auth, secure password hashing |
| **Real-time** | Ktor WebSockets + Redis PubSub | Chat, notifications, presence |
| **Push Notifications** | Firebase Cloud Messaging | Keep FCM for push, remove rest of Firebase |
| **Containerization** | Docker + Docker Compose | Easy local dev & deployment |
| **API Docs** | Swagger/OpenAPI | Auto-generated API documentation |

---

## 📊 Database Schema (PostgreSQL)

### Core Tables

```sql
-- Users (both patients and pharmacy staff)
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    avatar_url VARCHAR(500),
    fcm_token VARCHAR(500),
    role USER_ROLE NOT NULL DEFAULT 'PATIENT', -- PATIENT, PHARMACIST, DOCTOR, ADMIN
    is_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Pharmacies
CREATE TABLE pharmacies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    image_url VARCHAR(500),
    address TEXT NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    phone VARCHAR(20) NOT NULL,
    license_number VARCHAR(100),
    is_open BOOLEAN DEFAULT TRUE,
    opening_hours JSONB, -- {monday: {open: "09:00", close: "21:00"}, ...}
    rating_avg DECIMAL(3,2) DEFAULT 0.0,
    total_ratings INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Categories
CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name_ar VARCHAR(255) NOT NULL,
    name_en VARCHAR(255) NOT NULL,
    icon_url VARCHAR(500),
    parent_id UUID REFERENCES categories(id) ON DELETE SET NULL,
    display_order INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- SubCategories
CREATE TABLE subcategories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id UUID REFERENCES categories(id) ON DELETE CASCADE,
    name_ar VARCHAR(255) NOT NULL,
    name_en VARCHAR(255) NOT NULL,
    icon_url VARCHAR(500),
    display_order INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Medicines
CREATE TABLE medicines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title_ar VARCHAR(255) NOT NULL,
    title_en VARCHAR(255) NOT NULL,
    description_ar TEXT,
    description_en TEXT,
    image_url VARCHAR(500),
    price DECIMAL(10,2),
    subcategory_id UUID REFERENCES subcategories(id) ON DELETE SET NULL,
    manufacturer VARCHAR(255),
    requires_prescription BOOLEAN DEFAULT FALSE,
    alternatives_ids UUID[] DEFAULT '{}',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Prescriptions
CREATE TABLE prescriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    image_url VARCHAR(500) NOT NULL,
    notes TEXT,
    status PRESCRIPTION_STATUS NOT NULL DEFAULT 'PENDING', -- PENDING, SENT_TO_PHARMACY, RECEIVED_QUOTE, ACCEPTED, REJECTED, COMPLETED, CANCELLED
    selected_pharmacy_id UUID REFERENCES pharmacies(id) ON DELETE SET NULL,
    quoted_price DECIMAL(10,2),
    pharmacist_notes TEXT,
    sent_to_eilaji_plus BOOLEAN DEFAULT FALSE,
    eilaji_plus_prescription_id UUID,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Pharmacy-Medicine Inventory (which pharmacy has which medicine)
CREATE TABLE pharmacy_medicines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pharmacy_id UUID REFERENCES pharmacies(id) ON DELETE CASCADE,
    medicine_id UUID REFERENCES medicines(id) ON DELETE CASCADE,
    price DECIMAL(10,2),
    stock_quantity INTEGER DEFAULT 0,
    is_available BOOLEAN DEFAULT TRUE,
    UNIQUE(pharmacy_id, medicine_id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Chats (between patients and pharmacies)
CREATE TABLE chats (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    pharmacy_user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    prescription_id UUID REFERENCES prescriptions(id) ON DELETE SET NULL,
    last_message_text TEXT,
    last_message_image_url VARCHAR(500),
    last_message_sender_id UUID REFERENCES users(id),
    last_message_at TIMESTAMP WITH TIME ZONE,
    unread_count_patient INTEGER DEFAULT 0,
    unread_count_pharmacy INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Messages
CREATE TABLE messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chat_id UUID REFERENCES chats(id) ON DELETE CASCADE,
    sender_user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    message_text TEXT,
    message_image_url VARCHAR(500),
    medicine_name VARCHAR(255),
    is_read BOOLEAN DEFAULT FALSE,
    read_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Ratings
CREATE TABLE ratings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    pharmacy_id UUID REFERENCES pharmacies(id) ON DELETE CASCADE,
    medicine_id UUID REFERENCES medicines(id) ON DELETE CASCADE,
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(user_id, pharmacy_id, medicine_id) -- One rating per user per item
);

-- Favorites
CREATE TABLE favorites (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    pharmacy_id UUID REFERENCES pharmacies(id) ON DELETE CASCADE,
    medicine_id UUID REFERENCES medicines(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(user_id, pharmacy_id, medicine_id)
);

-- Reminders (migrated from Room to server for multi-device sync)
CREATE TABLE reminders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    medicine_name VARCHAR(255) NOT NULL,
    dosage VARCHAR(100),
    frequency REMINDER_FREQUENCY NOT NULL, -- ONCE_DAILY, TWICE_DAILY, THREE_TIMES_DAILY, CUSTOM
    custom_times TIME[] DEFAULT '{}',
    start_date DATE NOT NULL,
    end_date DATE,
    notes TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Ads (for future monetization)
CREATE TABLE ads (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title_ar VARCHAR(255) NOT NULL,
    title_en VARCHAR(255) NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    target_url VARCHAR(500),
    impression_count INTEGER DEFAULT 0,
    click_count INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    starts_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Audit Logs (for compliance)
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50),
    entity_id UUID,
    old_values JSONB,
    new_values JSONB,
    ip_address INET,
    user_agent TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Enums
CREATE TYPE USER_ROLE AS ENUM ('PATIENT', 'PHARMACIST', 'DOCTOR', 'ADMIN');
CREATE TYPE PRESCRIPTION_STATUS AS ENUM ('PENDING', 'SENT_TO_PHARMACY', 'RECEIVED_QUOTE', 'ACCEPTED', 'REJECTED', 'COMPLETED', 'CANCELLED');
CREATE TYPE REMINDER_FREQUENCY AS ENUM ('ONCE_DAILY', 'TWICE_DAILY', THREE_TIMES_DAILY', 'CUSTOM');

-- Indexes for performance
CREATE INDEX idx_pharmacies_location ON pharmacies USING GIST (ll_to_earth(latitude, longitude));
CREATE INDEX idx_medicines_subcategory ON medicines(subcategory_id);
CREATE INDEX idx_medicines_search ON medicines USING GIN (to_tsvector('english', title_en || ' ' || title_ar));
CREATE INDEX idx_messages_chat ON messages(chat_id, created_at DESC);
CREATE INDEX idx_chats_users ON chats(patient_user_id, pharmacy_user_id);
CREATE INDEX idx_prescriptions_patient ON prescriptions(patient_user_id, created_at DESC);
```

---

## 🔌 API Endpoints Design

### Authentication & User Management

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/v1/auth/register` | Register new user (patient/pharmacist) | No |
| POST | `/api/v1/auth/login` | Login with email/password | No |
| POST | `/api/v1/auth/logout` | Logout (invalidate token) | Yes |
| POST | `/api/v1/auth/refresh` | Refresh JWT token | Yes (valid refresh token) |
| POST | `/api/v1/auth/forgot-password` | Request password reset | No |
| POST | `/api/v1/auth/reset-password` | Reset password with token | No |
| GET | `/api/v1/users/me` | Get current user profile | Yes |
| PUT | `/api/v1/users/me` | Update current user profile | Yes |
| PUT | `/api/v1/users/me/avatar` | Upload avatar image | Yes |
| PUT | `/api/v1/users/me/fcm-token` | Update FCM token | Yes |

### Categories & Medicines

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/v1/categories` | Get all categories (with nested subcategories) | No |
| GET | `/api/v1/categories/{id}/subcategories` | Get subcategories by category | No |
| GET | `/api/v1/medicines` | Search medicines with filters | No |
| GET | `/api/v1/medicines/{id}` | Get medicine details | No |
| GET | `/api/v1/medicines/subcategory/{subcategoryId}` | Get medicines by subcategory | No |
| POST | `/api/v1/medicines` | Create new medicine (admin only) | Yes (ADMIN) |
| PUT | `/api/v1/medicines/{id}` | Update medicine (admin only) | Yes (ADMIN) |
| DELETE | `/api/v1/medicines/{id}` | Delete medicine (admin only) | Yes (ADMIN) |

### Pharmacies

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/v1/pharmacies/nearby` | Get nearby pharmacies (lat, lon, radius) | No |
| GET | `/api/v1/pharmacies` | Search pharmacies with filters | No |
| GET | `/api/v1/pharmacies/{id}` | Get pharmacy details | No |
| GET | `/api/v1/pharmacies/{id}/medicines` | Get pharmacy's medicine inventory | No |
| POST | `/api/v1/pharmacies` | Register new pharmacy | Yes (PHARMACIST) |
| PUT | `/api/v1/pharmacies/{id}` | Update pharmacy info | Yes (OWNER) |
| PUT | `/api/v1/pharmacies/{id}/inventory` | Update medicine inventory | Yes (OWNER) |
| POST | `/api/v1/pharmacies/{id}/toggle-status` | Toggle open/close status | Yes (OWNER) |

### Prescriptions

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/v1/prescriptions` | Create new prescription (upload image) | Yes |
| GET | `/api/v1/prescriptions` | Get user's prescriptions (paginated) | Yes |
| GET | `/api/v1/prescriptions/{id}` | Get prescription details | Yes (OWNER or assigned pharmacy) |
| PUT | `/api/v1/prescriptions/{id}/send-to-pharmacy` | Send prescription to specific pharmacy | Yes (PATIENT) |
| PUT | `/api/v1/prescriptions/{id}/quote` | Submit price quote (pharmacy) | Yes (PHARMACIST) |
| PUT | `/api/v1/prescriptions/{id}/accept` | Accept quote (patient) | Yes (PATIENT) |
| PUT | `/api/v1/prescriptions/{id}/reject` | Reject quote (patient) | Yes (PATIENT) |
| PUT | `/api/v1/prescriptions/{id}/status` | Update prescription status | Yes (PHARMACIST/ADMIN) |
| POST | `/api/v1/prescriptions/{id}/send-to-eilaji-plus` | Forward to Eilaji-Plus system | Yes (PHARMACIST) |

### Chat & Messaging

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/v1/chats` | Get user's chat list | Yes |
| GET | `/api/v1/chats/{chatId}` | Get chat messages (paginated) | Yes (PARTICIPANT) |
| POST | `/api/v1/chats` | Create new chat with pharmacy | Yes |
| POST | `/api/v1/chats/{chatId}/messages` | Send text message | Yes (PARTICIPANT) |
| POST | `/api/v1/chats/{chatId}/messages/image` | Send image message | Yes (PARTICIPANT) |
| PUT | `/api/v1/chats/{chatId}/read` | Mark messages as read | Yes (PARTICIPANT) |
| **WS** | `/ws/chat` | WebSocket for real-time messaging | Yes (JWT in handshake) |

### Ratings & Favorites

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/v1/ratings/pharmacy` | Rate a pharmacy | Yes |
| POST | `/api/v1/ratings/medicine` | Rate a medicine | Yes |
| GET | `/api/v1/ratings/pharmacy/{pharmacyId}` | Get pharmacy ratings | No |
| GET | `/api/v1/ratings/medicine/{medicineId}` | Get medicine ratings | No |
| POST | `/api/v1/favorites/pharmacy` | Add pharmacy to favorites | Yes |
| POST | `/api/v1/favorites/medicine` | Add medicine to favorites | Yes |
| DELETE | `/api/v1/favorites/pharmacy/{pharmacyId}` | Remove from favorites | Yes |
| DELETE | `/api/v1/favorites/medicine/{medicineId}` | Remove from favorites | Yes |
| GET | `/api/v1/favorites` | Get user's favorites | Yes |

### Reminders

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/v1/reminders` | Get user's reminders | Yes |
| POST | `/api/v1/reminders` | Create new reminder | Yes |
| PUT | `/api/v1/reminders/{id}` | Update reminder | Yes |
| DELETE | `/api/v1/reminders/{id}` | Delete reminder | Yes |
| PUT | `/api/v1/reminders/{id}/toggle` | Toggle reminder active/inactive | Yes |

### Ads

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/v1/ads` | Get active ads | No |
| POST | `/api/v1/ads` | Create new ad (admin only) | Yes (ADMIN) |
| PUT | `/api/v1/ads/{id}` | Update ad (admin only) | Yes (ADMIN) |
| DELETE | `/api/v1/ads/{id}` | Delete ad (admin only) | Yes (ADMIN) |
| POST | `/api/v1/ads/{id}/track-impression` | Track ad view | No |
| POST | `/api/v1/ads/{id}/track-click` | Track ad click | No |

### Admin endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/v1/admin/users` | List all users (paginated) | Yes (ADMIN) |
| PUT | `/api/v1/admin/users/{id}/role` | Change user role | Yes (ADMIN) |
| PUT | `/api/v1/admin/users/{id}/verify` | Verify user account | Yes (ADMIN) |
| GET | `/api/v1/admin/analytics/overview` | Get platform analytics | Yes (ADMIN) |
| GET | `/api/v1/admin/audit-logs` | View audit logs | Yes (ADMIN) |

---

## 📦 Project Structure

```
eilaji-backend/
├── build.gradle.kts
├── settings.gradle.kts
├── docker-compose.yml
├── Dockerfile
├── .env.example
├── README.md
│
├── src/main/kotlin/dev/eilaji/backend/
│   ├── Application.kt                 # Main entry point
│   ├── config/
│   │   ├── DatabaseConfig.kt          # Exposed + HikariCP setup
│   │   ├── RedisConfig.kt             # Redis connection
│   │   ├── StorageConfig.kt           # MinIO/S3 configuration
│   │   ├── JwtConfig.kt               # JWT secret & expiration
│   │   └── CorsConfig.kt              # CORS policies
│   │
│   ├── domain/
│   │   ├── model/                     # Business entities
│   │   │   ├── User.kt
│   │   │   ├── Pharmacy.kt
│   │   │   ├── Medicine.kt
│   │   │   ├── Prescription.kt
│   │   │   ├── Chat.kt
│   │   │   ├── Message.kt
│   │   │   └── ...
│   │   ├── repository/                # Data access interfaces
│   │   │   ├── UserRepository.kt
│   │   │   ├── PharmacyRepository.kt
│   │   │   └── ...
│   │   └── service/                   # Business logic
│   │       ├── AuthService.kt
│   │       ├── PrescriptionService.kt
│   │       ├── ChatService.kt
│   │       ├── EilajiPlusIntegrationService.kt
│   │       └── ...
│   │
│   ├── infrastructure/
│   │   ├── persistence/               # Exposed table definitions
│   │   │   ├── tables/
│   │   │   │   ├── UsersTable.kt
│   │   │   │   ├── PharmaciesTable.kt
│   │   │   │   └── ...
│   │   │   └── repositories/          # Repository implementations
│   │   │       ├── UserRepositoryImpl.kt
│   │   │       └── ...
│   │   ├── storage/                   # File storage abstraction
│   │   │   ├── StorageService.kt
│   │   │   └── MinioStorageService.kt
│   │   ├── messaging/                 # Redis pub/sub
│   │   │   ├── RedisPubSubService.kt
│   │   │   └── ChatMessagePublisher.kt
│   │   └── external/                  # External integrations
│   │       ├── FcmService.kt          # Firebase Cloud Messaging
│   │       └── EilajiPlusClient.kt    # HTTP client for Eilaji-Plus
│   │
│   ├── presentation/
│   │   ├── routes/                    # Ktor routing
│   │   │   ├── AuthRoutes.kt
│   │   │   ├── UserRoutes.kt
│   │   │   ├── MedicineRoutes.kt
│   │   │   ├── PharmacyRoutes.kt
│   │   │   ├── PrescriptionRoutes.kt
│   │   │   ├── ChatRoutes.kt
│   │   │   ├── RatingRoutes.kt
│   │   │   ├── ReminderRoutes.kt
│   │   │   ├── AdRoutes.kt
│   │   │   └── AdminRoutes.kt
│   │   ├── plugins/                   # Ktor plugins
│   │   │   ├── Authentication.kt
│   │   │   ├── Serialization.kt
│   │   │   ├── ContentNegotiation.kt
│   │   │   ├── CORS.kt
│   │   │   ├── RateLimiting.kt
│   │   │   └── SwaggerUI.kt
│   │   ├── dto/                       # Request/Response DTOs
│   │   │   ├── request/
│   │   │   │   ├── LoginRequest.kt
│   │   │   │   ├── RegisterRequest.kt
│   │   │   │   ├── CreatePrescriptionRequest.kt
│   │   │   │   └── ...
│   │   │   └── response/
│   │   │       ├── AuthResponse.kt
│   │   │       ├── UserResponse.kt
│   │   │       ├── PaginatedResponse.kt
│   │   │       └── ...
│   │   └── websocket/
│   │       ├── ChatWebSocketHandler.kt
│   │       └── PresenceTracker.kt
│   │
│   └── shared/
│       ├── exceptions/
│       │   ├── ApiException.kt
│       │   ├── ValidationException.kt
│       │   └── GlobalExceptionHandler.kt
│       ├── utils/
│       │   ├── PasswordHasher.kt
│       │   ├── JwtTokenGenerator.kt
│       │   ├── GeoUtils.kt
│       │   └── ImageProcessor.kt
│       └── constants/
│           ├── ErrorCodes.kt
│           └── AppConstants.kt
│
├── src/test/kotlin/dev/eilaji/backend/
│   ├── integration/
│   │   ├── AuthIntegrationTest.kt
│   │   ├── PrescriptionIntegrationTest.kt
│   │   └── ...
│   └── unit/
│       ├── AuthServiceTest.kt
│       └── ...
│
└── resources/
    ├── application.conf               # HOCON configuration
    ├── logback.xml
    └── migrations/                    # SQL migration files
        ├── V1__initial_schema.sql
        ├── V2__add_indexes.sql
        └── ...
```

---

## 🔐 Authentication Flow

```
┌──────────┐                              ┌─────────────┐
│  Client  │                              │ Ktor Server │
└────┬─────┘                              └──────┬──────┘
     │                                           │
     │  POST /auth/login {email, password}       │
     │──────────────────────────────────────────>│
     │                                           │
     │                         Verify credentials
     │                         Generate JWT tokens
     │                         (access + refresh)
     │                                           │
     │  {accessToken, refreshToken, user}        │
     │<──────────────────────────────────────────│
     │                                           │
     │  Store tokens securely (EncryptedSharedPreferences)
     │
     │  For subsequent requests:
     │  Authorization: Bearer <accessToken>
     │──────────────────────────────────────────>│
     │                                           │
     │                         Validate JWT signature
     │                         Check expiration
     │                         Load user from context
     │                                           │
     │  Response with protected data             │
     │<──────────────────────────────────────────│
     │                                           │
     │  When accessToken expires:
     │  POST /auth/refresh {refreshToken}        │
     │──────────────────────────────────────────>│
     │                                           │
     │  New {accessToken, refreshToken}          │
     │<──────────────────────────────────────────│
```

### JWT Token Structure

```kotlin
data class JwtClaims(
    val userId: String,
    val email: String,
    val role: UserRole,
    val issuedAt: Long,
    val expiresAt: Long
)

// Access Token: 15 minutes expiry
// Refresh Token: 7 days expiry (stored in DB for revocation)
```

---

## 💬 Real-Time Chat Architecture

### WebSocket Connection Flow

```
┌──────────────┐                    ┌─────────────┐                   ┌──────────┐
│   Patient    │                    │  Ktor WS    │                   │  Pharmacy │
│    App       │                    │   Server    │                   │    App    │
└──────┬───────┘                    └──────┬──────┘                   └────┬─────┘
       │                                   │                               │
       │ WS /ws/chat?token=<jwt>           │                               │
       │──────────────────────────────────>│                               │
       │                                   │                               │
       │  Authenticate & join room         │                               │
       │  (room = chatId)                  │                               │
       │                                   │                               │
       │  Ack: connected                   │                               │
       │<──────────────────────────────────│                               │
       │                                   │                               │
       │                                   │  WS /ws/chat?token=<jwt>      │
       │                                   │<──────────────────────────────│
       │                                   │                               │
       │                                   │  Both in same room now        │
       │                                   │                               │
       │  Send message                     │                               │
       │──────────────────────────────────>│                               │
       │                                   │                               │
       │                                   │  Save to DB                   │
       │                                   │  Publish to Redis channel     │
       │                                   │  (chat:{chatId})              │
       │                                   │                               │
       │                                   │──────────────────────────────>│
       │                                   │     Push notification via FCM │
       │                                   │     (if recipient offline)    │
       │                                   │                               │
       │  Real-time delivery               │                               │
       │<──────────────────────────────────│──────────────────────────────>│
       │                                   │                               │
```

### Redis Pub/Sub for Horizontal Scaling

```kotlin
// When user sends message:
redis.publish("chat:$chatId", messageJson)

// Each Ktor instance subscribes:
redis.subscribe("chat:*") { channel, message ->
    val chatId = channel.removePrefix("chat:")
    // Forward to all connected WebSocket sessions in this room
    webSocketSessionManager.sendToRoom(chatId, message)
}
```

---

## 🔄 Eilaji-Plus Integration

### Option 1: Direct REST API (Recommended for MVP)

Your backend acts as the **single source of truth** and exposes APIs that Eilaji-Plus consumes:

```
Eilaji-Plus App → Your Ktor Backend ← Eilaji App
                      ↓
                PostgreSQL
```

**Eilaji-Plus API Endpoints:**

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/pharmacy/prescriptions/receive` | Receive prescription from patient |
| PUT | `/api/v1/pharmacy/prescriptions/{id}/quote` | Submit price quote |
| PUT | `/api/v1/pharmacy/prescriptions/{id}/status` | Update prescription status |
| GET | `/api/v1/pharmacy/prescriptions` | Get prescriptions for pharmacy |
| POST | `/api/v1/pharmacy/chat/message` | Send chat message |
| GET | `/api/v1/pharmacy/chats` | Get chat list |

### Option 2: Message Queue (For High Scale)

Use Redis Streams or RabbitMQ for async prescription processing:

```kotlin
// When prescription created:
redis.xadd("prescription:queue", mapOf(
    "prescriptionId" to prescriptionId,
    "patientId" to patientId,
    "targetPharmacyId" to pharmacyId,
    "imageUrl" to imageUrl,
    "notes" to notes
))

// Eilaji-Plus backend consumes from queue
```

### Data Contract for Prescription Transfer

```kotlin
data class PrescriptionTransferDto(
    val prescriptionId: String,
    val patientName: String,
    val patientPhone: String,
    val prescriptionImageUrl: String,
    val notes: String?,
    val createdAt: Instant,
    val targetPharmacyId: String,
    val targetPharmacyName: String
)

// Response from Eilaji-Plus
data class PrescriptionAcknowledgment(
    val prescriptionId: String,
    val eilajiPlusPrescriptionId: String,
    val status: String,
    val acknowledgedAt: Instant,
    val assignedPharmacistId: String?
)
```

---

## 🗺️ Geospatial Queries (Pharmacy Discovery)

### PostgreSQL + PostGIS Setup

```sql
-- Enable PostGIS extension
CREATE EXTENSION IF NOT EXISTS postgis;

-- Add geography column for better accuracy
ALTER TABLE pharmacies 
ADD COLUMN location GEOGRAPHY(POINT, 4326);

-- Update existing records
UPDATE pharmacies 
SET location = ST_MakePoint(longitude, latitude)::geography;

-- Create spatial index
CREATE INDEX idx_pharmacies_geo ON pharmacies USING GIST (location);
```

### Find Nearby Pharmacies Query

```kotlin
// In repository:
fun findNearbyPharmacies(lat: Double, lon: Double, radiusKm: Double): List<Pharmacy> {
    return PharmacyTable.select {
        Op.build {
            ST_DWithin(
                PharmacyTable.location,
                ST_MakePoint(lon, lat).geography,
                radiusKm * 1000 // Convert to meters
            ) eq true
        }
    }
    .orderBy(ST_Distance(PharmacyTable.location, ST_MakePoint(lon, lat).geography) to SortOrder.ASC)
    .map { row -> row.toPharmacy() }
}
```

### API Endpoint

```kotlin
get("/pharmacies/nearby") {
    val lat = call.parameters["lat"]?.toDoubleOrNull() 
        ?: throw IllegalArgumentException("Missing lat parameter")
    val lon = call.parameters["lon"]?.toDoubleOrNull() 
        ?: throw IllegalArgumentException("Missing lon parameter")
    val radius = call.parameters["radius"]?.toDoubleOrNull() ?: 5.0 // Default 5km
    
    val pharmacies = pharmacyService.findNearby(lat, lon, radius)
    
    call.respond(pharmacies.map { it.toDto() })
}
```

---

## 📸 File Upload & Storage

### MinIO Setup (Self-Hosted S3 Alternative)

```yaml
# docker-compose.yml
minio:
  image: minio/minio:latest
  ports:
    - "9000:9000"
    - "9001:9001"
  volumes:
    - minio_data:/data
  environment:
    MINIO_ROOT_USER: eilaji-admin
    MINIO_ROOT_PASSWORD: change-this-password
  command: server /data --console-address ":9001"
```

### Upload Flow

```kotlin
// In route:
post("/prescriptions/image") {
    val multipart = call.receiveMultipart()
    val filePart = multipart.formData.firstOrNull { it.name == "image" }
        ?: throw BadRequestException("No image provided")
    
    val prescriptionId = generateUuid()
    val fileName = "prescriptions/$prescriptionId/${UUID.randomUUID()}.jpg"
    
    // Upload to MinIO
    val url = storageService.upload(filePart.streamProvider(), fileName)
    
    // Save URL to prescription
    prescriptionService.createPrescription(userId, url, ...)
    
    call.respond(PrescriptionUploadResponse(prescriptionId, url))
}
```

### Image Processing (Thumbnails)

```kotlin
fun processImage(inputStream: InputStream): ProcessedImage {
    val original = ImageIO.read(inputStream)
    
    // Create thumbnail (400x400 max)
    val thumbnail = Thumbnails.of(original)
        .size(400, 400)
        .keepAspectRatio(true)
        .asBufferedImage()
    
    // Compress to JPEG 85% quality
    val compressed = ByteArrayOutputStream()
    ImageIO.write(thumbnail, "jpg", compressed)
    
    return ProcessedImage(
        originalBytes = inputStream.readAllBytes(),
        thumbnailBytes = compressed.toByteArray()
    )
}
```

---

## 🚀 Development & Deployment Strategy

### Phase 1: Local Development (Week 1-2)

**Goal**: Get everything running on your machine

```bash
# Clone the backend repo (I'll create this for you)
git clone https://github.com/HeshamAbuShaban/eilaji-backend.git
cd eilaji-backend

# Start all services with Docker Compose
docker-compose up -d

# Run the backend
./gradlew run

# Access Swagger UI at http://localhost:8080/swagger-ui
```

**Docker Compose includes:**
- PostgreSQL 16 with PostGIS
- Redis 7
- MinIO (S3-compatible storage)
- pgAdmin (database GUI)
- Redis Commander (Redis GUI)

### Phase 2: Core Features (Week 3-6)

Build in this order:
1. ✅ Authentication (register, login, JWT)
2. ✅ User management
3. ✅ Categories & Medicines (CRUD)
4. ✅ Pharmacies (CRUD + geospatial)
5. ✅ Prescriptions (upload, list, status)
6. ✅ Basic chat (REST, no WebSocket yet)

### Phase 3: Real-Time Features (Week 7-9)

1. ✅ WebSocket chat implementation
2. ✅ Redis Pub/Sub for scaling
3. ✅ Presence tracking (online/offline)
4. ✅ Push notifications (FCM integration)
5. ✅ Read receipts

### Phase 4: Eilaji-Plus Integration (Week 10-11)

1. ✅ Define API contract
2. ✅ Implement prescription transfer
3. ✅ Status update callbacks
4. ✅ Test end-to-end flow

### Phase 5: Advanced Features (Week 12-14)

1. ✅ Ratings & reviews
2. ✅ Favorites
3. ✅ Reminders (server-side)
4. ✅ Ads system
5. ✅ Analytics dashboard

### Phase 6: Mobile Integration (Week 15-16)

1. ✅ Update Eilaji app to use new API
2. ✅ Replace Firebase calls with Retrofit
3. ✅ Implement WebSocket client
4. ✅ Token management
5. ✅ Offline caching strategy

### Phase 7: Production Prep (Week 17-18)

1. ✅ Load testing
2. ✅ Security audit
3. ✅ Backup strategy
4. ✅ Monitoring setup (Prometheus + Grafana)
5. ✅ CI/CD pipeline

---

## 💰 Cost Analysis

### Current Firebase Costs (at 10K users)

| Service | Estimated Cost/Month |
|---------|---------------------|
| Firestore (reads/writes) | $150-250 |
| Realtime Database | $50-100 |
| Storage (images) | $30-50 |
| Authentication | $0-25 |
| Cloud Messaging | $0 (free) |
| **Total** | **$230-425/month** |

### Self-Hosted Costs

| Service | Cost/Month | Notes |
|---------|-----------|-------|
| VPS (4 vCPU, 8GB RAM) | $40-60 | DigitalOcean/Linode/Hetzner |
| Managed PostgreSQL | $15-25 | Optional, can self-host |
| Managed Redis | $10-15 | Optional, can self-host |
| Object Storage | $5-10 | Backblaze B2 or Wasabi |
| Domain + SSL | $2 | Let's Encrypt is free |
| **Total** | **$72-112/month** |

**💰 Savings: 60-75% ($158-313/month saved)**

### Free Tier (Development)

Run everything locally on your machine:
- PostgreSQL: Free (Docker)
- Redis: Free (Docker)
- MinIO: Free (Docker)
- Backend: Free (your machine)
- **Total: $0/month** until you're ready to deploy

---

## 🔒 Security Considerations

### 1. Authentication & Authorization

```kotlin
// Role-based access control
authenticate("jwt") {
    route("/prescriptions") {
        get { /* Any authenticated user */ }
        
        authenticate("jwt") {
            post { /* Only PATIENT role */ }
        }
        
        authenticate("jwt", "pharmacist") {
            put("/{id}/quote") { /* Only PHARMACIST role */ }
        }
        
        authenticate("jwt", "admin") {
            delete("/{id}") { /* Only ADMIN role */ }
        }
    }
}
```

### 2. Input Validation

```kotlin
data class CreatePrescriptionRequest {
    @NotBlank(message = "Notes cannot be empty")
    @Size(max = 1000, message = "Notes too long")
    val notes: String?
    
    @NotNull(message = "Pharmacy ID required")
    val pharmacyId: UUID?
}
```

### 3. Rate Limiting

```kotlin
install(RateLimit) {
    global {
        limit(1000, Duration.ofSeconds(60)) // 1000 req/min
    }
    
    route("/auth/login") {
        limit(5, Duration.ofMinutes(5)) // 5 attempts per 5 min
    }
}
```

### 4. Audit Logging

```kotlin
// Log all sensitive operations
fun logAudit(
    userId: String,
    action: String,
    entityType: String,
    entityId: UUID,
    oldValues: Map<String, Any?>?,
    newValues: Map<String, Any?>?
) {
    auditLogger.log(
        userId = userId,
        action = action,
        entityType = entityType,
        entityId = entityId,
        oldValues = oldValues,
        newValues = newValues,
        ipAddress = call.request.local.remoteAddress,
        userAgent = call.request.headers["User-Agent"]
    )
}
```

### 5. Healthcare Data Compliance

- ✅ Encrypt sensitive data at rest (PostgreSQL TDE)
- ✅ Encrypt data in transit (TLS 1.3)
- ✅ Audit trail for all prescription access
- ✅ Automatic data retention policies
- ✅ GDPR compliance tools (export/delete user data)

---

## 📱 Mobile App Integration Guide

### Step 1: Add Dependencies to Eilaji App

```gradle
// Remove Firebase dependencies (keep only FCM)
// implementation platform('com.google.firebase:firebase-bom:32.2.2')
// implementation 'com.google.firebase:firebase-auth-ktx'
// implementation 'com.google.firebase:firebase-firestore-ktx'
// implementation 'com.google.firebase:firebase-storage-ktx'
// Keep only:
implementation 'com.google.firebase:firebase-messaging-ktx'

// Add Retrofit + OkHttp
implementation 'com.squareup.retrofit2:retrofit:2.9.0'
implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
implementation 'com.squareup.okhttp3:logging-interceptor:4.11.0'
implementation 'com.squareup.okhttp3:okhttp:4.11.0'

// Add Kotlin Coroutines for async
implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'

// Add JWT token management
implementation 'com.auth0.android:jwtdecode:2.0.2'

// Add WebSocket client
implementation 'com.squareup.okhttp3:okhttp:4.11.0' // Has WebSocket support
```

### Step 2: Create API Client

```kotlin
// api/EilajiApiClient.kt
object EilajiApiClient {
    private const val BASE_URL = "https://api.eilaji.dev/api/v1/"
    
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(AuthInterceptor()) // Adds JWT token
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val authService: AuthService = retrofit.create(AuthService::class.java)
    val medicineService: MedicineService = retrofit.create(MedicineService::class.java)
    val pharmacyService: PharmacyService = retrofit.create(PharmacyService::class.java)
    val prescriptionService: PrescriptionService = retrofit.create(PrescriptionService::class.java)
    val chatService: ChatService = retrofit.create(ChatService::class.java)
}

interface AuthService {
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>
    
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>
    
    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<AuthResponse>
}
```

### Step 3: Token Manager

```kotlin
// auth/TokenManager.kt
class TokenManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("eilaji_tokens", Context.MODE_PRIVATE)
    
    fun saveTokens(accessToken: String, refreshToken: String, expiresAt: Long) {
        prefs.edit().apply {
            putString("access_token", accessToken)
            putString("refresh_token", refreshToken)
            putLong("expires_at", expiresAt)
            apply()
        }
    }
    
    fun getAccessToken(): String? {
        val token = prefs.getString("access_token", null)
        val expiresAt = prefs.getLong("expires_at", 0)
        
        // Return null if expired
        return if (System.currentTimeMillis() < expiresAt) token else null
    }
    
    suspend fun getValidAccessToken(): String? {
        var token = getAccessToken()
        
        if (token == null) {
            // Try to refresh
            val refreshToken = prefs.getString("refresh_token", null) ?: return null
            val response = EilajiApiClient.authService.refreshToken(
                RefreshTokenRequest(refreshToken)
            )
            
            if (response.isSuccessful) {
                val authResponse = response.body()!!
                saveTokens(authResponse.accessToken, authResponse.refreshToken, authResponse.expiresAt)
                token = authResponse.accessToken
            }
        }
        
        return token
    }
    
    fun clearTokens() {
        prefs.edit().clear().apply()
    }
}
```

### Step 4: Repository Pattern Update

```kotlin
// repository/PrescriptionRepository.kt
class PrescriptionRepository(
    private val apiService: PrescriptionService,
    private val tokenManager: TokenManager
) {
    suspend fun createPrescription(
        imageUri: Uri,
        notes: String?,
        pharmacyId: UUID?
    ): Result<Prescription> = withContext(Dispatchers.IO) {
        try {
            // Upload image
            val uploadResponse = apiService.uploadPrescriptionImage(
                imageUri.toMultipartBodyPart()
            )
            
            if (!uploadResponse.isSuccessful) {
                return@withContext Result.failure(ApiException(uploadResponse.code()))
            }
            
            val imageUrl = uploadResponse.body()!!.imageUrl
            
            // Create prescription
            val response = apiService.createPrescription(
                CreatePrescriptionRequest(imageUrl, notes, pharmacyId)
            )
            
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(ApiException(response.code()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

### Step 5: WebSocket Chat Client

```kotlin
// websocket/ChatWebSocketClient.kt
class ChatWebSocketClient(
    private val tokenManager: TokenManager,
    private val messageListener: (Message) -> Unit,
    private val connectionListener: (Boolean) -> Unit
) {
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient()
    
    fun connect(chatId: String) {
        val token = tokenManager.getAccessToken() ?: return
        
        val request = Request.Builder()
            .url("wss://api.eilaji.dev/ws/chat?token=$token&chatId=$chatId")
            .build()
        
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                connectionListener(true)
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                val message = Gson().fromJson(text, Message::class.java)
                messageListener(message)
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                connectionListener(false)
                // Reconnect logic
            }
        })
    }
    
    fun sendMessage(messageText: String, imageUrl: String?) {
        val message = SendMessageRequest(messageText, imageUrl)
        webSocket?.send(Gson().toJson(message))
    }
    
    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
    }
}
```

---

## 🧪 Testing Strategy

### Unit Tests

```kotlin
// AuthServiceTest.kt
@Test
fun `login with valid credentials returns JWT tokens`() = runTest {
    // Arrange
    val email = "test@example.com"
    val password = "SecurePass123!"
    val hashedPassword = passwordHasher.hash(password)
    
    coEvery { userRepository.findByEmail(email) } returns User(
        id = UUID.randomUUID(),
        email = email,
        passwordHash = hashedPassword,
        fullName = "Test User"
    )
    
    // Act
    val result = authService.login(email, password)
    
    // Assert
    assertTrue(result.isSuccess)
    assertNotNull(result.getOrNull()?.accessToken)
    assertNotNull(result.getOrNull()?.refreshToken)
}
```

### Integration Tests

```kotlin
// PrescriptionIntegrationTest.kt
@Test
fun `create prescription uploads image and saves to database`() = testApplication {
    // Arrange
    val authToken = loginAndGetToken("patient@example.com", "password")
    val imageFile = File("src/test/resources/test-prescription.jpg")
    
    // Act
    val response = client.post("/api/v1/prescriptions") {
        header(HttpHeaders.Authorization, "Bearer $authToken")
        setBody(MultiPartFormDataContent(formData {
            append("image", imageFile.readBytes(), Headers.build {
                append(HttpHeaders.ContentType, "image/jpeg")
                append(HttpHeaders.ContentDisposition, """form-data; name="image"; filename="prescription.jpg"""")
            })
            append("notes", "Urgent prescription")
        }))
    }
    
    // Assert
    assertEquals(HttpStatusCode.Created, response.status)
    val prescription = response.body<PrescriptionResponse>()
    assertNotNull(prescription.id)
    assertTrue(prescription.imageUrl.contains("minio"))
}
```

---

## 📈 Monitoring & Observability

### Metrics to Track

1. **API Performance**
   - Request latency (p50, p95, p99)
   - Error rates by endpoint
   - Requests per second

2. **Database**
   - Query execution time
   - Connection pool usage
   - Slow queries

3. **Business Metrics**
   - Active users (DAU/MAU)
   - Prescriptions created per day
   - Chat messages per day
   - Average response time for quotes

### Prometheus + Grafana Setup

```yaml
# docker-compose.yml
prometheus:
  image: prom/prometheus:latest
  volumes:
    - ./prometheus.yml:/etc/prometheus/prometheus.yml
    - prometheus_data:/prometheus
  ports:
    - "9090:9090"

grafana:
  image: grafana/grafana:latest
  volumes:
    - grafana_data:/var/lib/grafana
  ports:
    - "3000:3000"
  environment:
    - GF_SECURITY_ADMIN_PASSWORD=admin
```

---

## 🎯 Success Metrics

### Technical KPIs

- ✅ API response time < 200ms (p95)
- ✅ 99.9% uptime
- ✅ Zero data loss
- ✅ < 1% error rate

### Business KPIs

- ✅ Prescription processing time < 2 hours average
- ✅ Chat message delivery < 1 second
- ✅ Pharmacy discovery accuracy > 95%
- ✅ User satisfaction score > 4.5/5

---

## 🚦 Next Steps

### What I Need From You:

1. **Confirm the tech stack**: Kotlin + Ktor + Exposed + PostgreSQL ✅ (Already confirmed)
2. **Decide on hosting**: 
   - Start local (free) ✅
   - Later: DigitalOcean ($40/mo) or Hetzner (€5/mo)
3. **Eilaji-Plus integration approach**: REST API (recommended) ✅
4. **Priority features**: Which features are must-have for MVP?

### What I'll Deliver:

1. **Complete backend codebase** with all endpoints
2. **Docker Compose** for one-command local setup
3. **Database migrations** ready to run
4. **API documentation** (Swagger UI)
5. **Mobile integration guide** with code examples
6. **Deployment scripts** for production

---

## 🎉 Conclusion

This plan gives you:
- ✅ **100% Kotlin** backend (no JS/PHP)
- ✅ **Full control** over your data and logic
- ✅ **Seamless integration** between Eilaji and Eilaji-Plus
- ✅ **60-75% cost reduction**
- ✅ **Modern, scalable architecture**
- ✅ **Type-safe development** from DB to API

Ready to start building? Let me know which phase you want to tackle first!
