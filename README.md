# Eilaji (علاجي) — My Cure

Digitized pharmacy platform for Syria. Patients browse medicines, find nearby pharmacies, send prescription photos for quotes, chat in real-time, order, and track medication reminders — bilingual (AR/EN), RTL-first, offline-tolerant.

[![Android CI](https://github.com/anomalyco/Eilaji/actions/workflows/android-ci.yml/badge.svg)](https://github.com/anomalyco/Eilaji/actions/workflows/android-ci.yml)
[![Backend CI](https://github.com/anomalyco/Eilaji/actions/workflows/backend-ci.yml/badge.svg)](https://github.com/anomalyco/Eilaji/actions/workflows/backend-ci.yml)

## Problem

Pharmacy interaction in Syria is still walk-in and phone-based. Earlier iterations of Eilaji relied on Firebase (Auth, Firestore, Realtime DB, FCM) which hit limits on query flexibility (no Haversine search, weak transactions), cost/latency for hosting in-region, and vendor lock-in for prescriptions, chat history, and file storage. Guest browsing was not cleanly separable from auth.

## Solution

Custom backend in **Ktor + Postgres + Redis + MinIO** with a native **Kotlin Android** client. Postgres handles relational data and geospatial search, Redis handles caching/sessions/pub-sub, MinIO stores prescription/medicine images (S3-compatible). JWT auth, WebSocket chat, and Room+WorkManager on-device for offline reminders and cache.

## Architecture

```
                         ┌─────────────────────┐
                         │   Android (Kotlin)   │
                         │  MVVM + Repository   │
                         │  Room / WorkManager  │
                         │  Retrofit + WebSocket│
                         └────────┬────────────┘
                                  │ HTTPS / WSS (JWT)
                         ┌────────▼────────────┐
                         │   Ktor Backend       │
                         │  Auth (JWT)          │
                         │  REST + WebSocket    │
                         │  Exposed ORM         │
                         └──┬─────┬──────┬─────┘
                            │     │      │
                     ┌──────▼┐ ┌──▼──┐ ┌─▼────┐
                     │Postgres│ │Redis│ │MinIO │
                     │(PostGIS│ │cache│ │ S3   │
                     │ Haversine)│     │      │
                     └───────┘ └─────┘ └──────┘
```

Flow: Guest browses catalog/pharmacies without signup → sign-in (JWT) → upload prescription photo (MinIO) → pharmacy quote via chat (WebSocket, persisted in Postgres, pub via Redis) → order → reminders scheduled locally.

## Features

- **Medicines catalog (bilingual)** — 6 categories / 12 subcategories / 36 medicines, Arabic + English names, manufacturer, price, prescription-required flag, alternatives.
- **Nearby pharmacies (Haversine)** — `GET /api/v1/pharmacies/nearby?lat=&lng=&radius=` with Postgres lat/lng index, rating and open/closed status.
- **Prescription upload** — multipart image + notes + `selectedPharmacyId` → MinIO bucket `prescriptions`, status lifecycle: `PENDING → SENT_TO_PHARMACY → RECEIVED_QUOTE → ACCEPTED → COMPLETED`.
- **Real-time chat (WebSocket)** — `WS /api/v1/ws/chat?token=JWT`, REST fallback `POST /api/v1/chat`, `GET /api/v1/chat/{id}/messages`. Redis pub/sub for delivery, Postgres persistence. FCM retained only for push when app is backgrounded.
- **Orders** — create from accepted prescription `POST /api/v1/orders`, track status, delivery address, payment method.
- **Reminders** — Room + WorkManager, local scheduling with `AlarmManager`, `BootReceiver` for reboot persistence, frequency DAILY/WEEKLY/CUSTOM, optional end date. Synced to backend `medication_reminders` table when online.
- **Favorites & ratings** — favorite medicines/pharmacies, 1–5 star pharmacy ratings with avg rollup.
- **Guest flow** — browse catalog, categories, pharmacy list/detail, search without auth. Auth required for prescriptions, chat, orders, reminders.
- **Pill logo & theming** — vector pill logo scaled properly across densities, `#BA324F` accent, Cairo font, full dark mode (`values-night`), SDP/SSP responsive sizing, RTL layouts (`values-ar`).

## Tech Stack

**Android**

| Layer | Tech |
|---|---|
| Language | Kotlin (primary), Java (legacy adapters) |
| Architecture | MVVM + Repository |
| UI | XML + ViewBinding, Material, SDP/SSP, Shimmer, MagicIndicator, RoundedImageView |
| Navigation | Jetpack Navigation 2.7.0, ViewModel + LiveData 2.6.1 |
| Local | Room 2.5.2 (KSP), WorkManager 2.8.1, Security Crypto (EncryptedSharedPreferences) |
| Network | Retrofit 2.9.0 + OkHttp + Gson, WebSocket (`network/websocket/WebSocketManager.kt`) |
| Maps | Play Services Maps 18.1.0 + Location 21.0.1 |
| Media | Glide 4.15.1, Activity Result PickVisualMedia |
| Auth | Firebase Auth (legacy) → JWT against Ktor backend |
| Min SDK 23, target 34, JDK 17, Gradle 8.x | |

**Backend** (`eilaji-backend/`)

| Layer | Tech |
|---|---|
| Framework | Ktor 2.3.7 (Netty, ContentNegotiation, Auth JWT, WebSockets, CallLogging, CORS) |
| Language | Kotlin 1.9.22, JVM 17 (CI uses 21) |
| ORM | Exposed 0.45.0 + HikariCP 5.1.0 |
| DB | Postgres 16 (PostGIS-ready, `init-db.sql`), Redis 7 (Lettuce 6.3.1), MinIO 8.5.7 |
| Auth | JWT (`com.auth0:java-jwt` via Ktor), BCrypt, role-based (PATIENT/PHARMACIST/DOCTOR/ADMIN) |
| Observability | `/health` (DB/Redis/MinIO), `/metrics` (Prometheus), JSON logs, rate limiting |
| Config | `HOCON` (`application.conf`) + env vars |

## How to Run

### Prerequisites

- JDK 17+ (backend CI uses 21), Android SDK 34, Docker & Docker Compose

### 1. Backend

```bash
cd eilaji-backend
docker-compose up -d          # postgres:5432, redis:6379, minio:9000/9001 (+ buckets)
./gradlew :backend:build      # build
./gradlew :backend:run        # run at http://localhost:8080
# or: ./gradlew :backend:test
```

Health check: `curl http://localhost:8080/health`

Env is via `eilaji-backend/src/main/resources/application.conf` (HOCON) reading `database.url`, `redis.url`, `minio.*`, `jwt.*`. Override with env vars or `.env` (see `eilaji-backend/README.md`).

### 2. Android

```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

Configure backend base URL in `app/src/main/java/dev/anonymous/eilaji/network/NetworkModule.kt` / `ApiService.kt` to point at `http://10.0.2.2:8080` for emulator.

## Sample Data

Seeded on first start if DB is empty (`backend/src/main/kotlin/com/eilaji/backend/initialization/DatabaseSeeder.kt` + `eilaji-backend/init-db.sql`):

- **6 categories**: Pain Relievers, Antibiotics, Vitamins & Supplements, Skin Care, Cold & Flu, Digestive Health (AR/EN)
- **12 subcategories**, **36 medicines** (paracetamol, ibuprofen, augmentin, vitamin C/D3, hydrocortisone, etc. with bilingual titles and prices)
- **10 pharmacies** across Damascus, Aleppo, Homs, Latakia, Hama with lat/lng, ratings, open/closed
- **4 test users**: `pharmacist1@eilaji.com`, `pharmacist2@eilaji.com`, `patient@eilaji.com`, `admin@eilaji.com` (password `password123`, admin also `admin@eilaji.com` via init-db.sql)

## Screenshots

| Home | Catalog | Nearby | Prescription | Chat | Reminders |
|---|---|---|---|---|---|
| _placeholder_ | _placeholder_ | _placeholder_ | _placeholder_ | _placeholder_ | _placeholder_ |

Add screenshots to `docs/screenshots/` and link here.

## Project Structure

```
.
├── app/                          # Android client
│   └── src/main/java/dev/anonymous/eilaji/
│       ├── ui/                   # Fragments, ViewModels, Navigation
│       ├── adapters/             # RecyclerView adapters
│       ├── network/              # Retrofit + WebSocketManager
│       ├── reminder_system/      # Room + WorkManager + AlarmReceiver
│       ├── firebase/             # Legacy FCM (being phased out)
│       └── storage/              # AppSharedPreferences
├── eilaji-backend/               # Ktor backend
│   ├── backend/src/main/kotlin/com/eilaji/backend/
│   │   ├── Application.kt
│   │   ├── controller/ / routes/
│   │   ├── service/  (Order, Prescription, Redis)
│   │   ├── data/     (Exposed tables)
│   │   ├── websocket/
│   │   └── initialization/DatabaseSeeder.kt
│   ├── docker-compose.yml
│   └── init-db.sql
├── Horizontal-indicator-for-pager2-and-recycler/  # ViewPager2 indicator library
└── .github/workflows/            # android-ci.yml, backend-ci.yml
```

## CI

- **Android CI** (`.github/workflows/android-ci.yml`): JDK 17, `./gradlew assembleDebug`, uploads `app-debug.apk`, release on `v*` tags.
- **Backend CI** (`.github/workflows/backend-ci.yml`): JDK 21, Postgres 15 + Redis 7 services, `./gradlew :backend:build` + `:backend:test`.

## License

Portfolio / educational use only — not licensed for commercial use or redistribution.
© Hesham AbuShaban, 2025. All rights reserved. See [LICENSE](LICENSE).
