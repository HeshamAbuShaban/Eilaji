# Eilaji (علاجي) — Pharmacy, Delivered

Bilingual (AR/EN), RTL-first pharmacy platform for Gaza, Palestine and MENA: browse medicines as a guest, find nearby pharmacies on a live map, upload prescriptions, chat with pharmacists in real time, order to your door, and never miss a dose with exact-alarm reminders — online or off.

[![Android CI](https://github.com/HeshamAbuShaban/Eilaji/actions/workflows/android-ci.yml/badge.svg)](https://github.com/HeshamAbuShaban/Eilaji/actions/workflows/android-ci.yml)
[![Backend CI](https://github.com/HeshamAbuShaban/Eilaji/actions/workflows/backend-ci.yml/badge.svg)](https://github.com/HeshamAbuShaban/Eilaji/actions/workflows/backend-ci.yml)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)
![Ktor](https://img.shields.io/badge/Ktor-3.0-087CFA)
![Android](https://img.shields.io/badge/Android-23%2B-3DDC84?logo=android&logoColor=white)

> **Try it in 5 minutes:** download `app-debug.apk` from [Releases](https://github.com/HeshamAbuShaban/Eilaji/releases/latest), start the backend with one compose file, log in as `patient@eilaji.com` / `password123` — details below.

## Why Eilaji

Walk-in and phone-based pharmacy breaks down under closures, stockouts and distance. Firebase-based prototypes hit query limits (no geo-search, weak transactions), regional latency/cost, and lock-in. Eilaji replaces that with a self-hosted **Ktor + Postgres + Redis + MinIO** backend and a native Kotlin client: relational geo-search, real transactions, S3-compatible prescription storage, JWT auth, persisted WebSocket chat, and Room + WorkManager offline-first on device.

## What it does

| Area | Experience |
|---|---|
| Catalog | 6 categories, 12 subcategories, 36 medicines (AR/EN, manufacturer, price, Rx flag, alternatives rail) |
| Nearby | Haversine search with radius, live map markers + bottom-sheet pager (rating, distance, Open/Closed, call & chat) |
| Prescriptions | Photo upload to MinIO, `PENDING → SENT → QUOTED → ACCEPTED → COMPLETED`, Rx gate blocks Rx checkout until a pharmacist confirms |
| Chat | WebSocket with heartbeat + offline queue, history persisted in Postgres, ghost Gaza threads seeded for testing |
| Orders & cart | Cart with badge, checkout (address + COD/card), order tracking timeline |
| Reminders | Exact alarms + WorkManager fallback, DAILY/WEEKLY/CUSTOM with real intervals, dosage field, reboot-safe, backend sync |
| Favorites & ratings | Favorite medicines/pharmacies (offline-first sync), 1–5 star pharmacy ratings with rollup |
| Guest-first | Browse catalog, search, map without signup; auth only for prescriptions, chat, orders |
| Theming | Cairo font, SDP/SSP responsive sizing, full dark mode, `values-ar` RTL layouts |

## Architecture

```
Android (Kotlin, MVVM) ──HTTPS/WSS (JWT)──► Ktor (REST + WebSocket, Exposed)
                                                    ├── Postgres 16 (geo + transactions)
                                                    ├── Redis 7 (cache / pub-sub)
                                                    └── MinIO (prescriptions, images)
On-device: Room + WorkManager + AlarmManager (reminders, favorites, cart cache)
```

Guest browses → signs in (JWT) → sets delivery pin on an interactive map → uploads prescription → pharmacist quotes over chat → orders → adheres with reminders.

## Quickstart

**Prerequisites:** JDK 17+ (backend CI uses 21), Android SDK 34, Docker & Compose.

```bash
# 1. Backend + infra
cd eilaji-backend
docker-compose up -d        # postgres:5432, redis:6379, minio:9000/9001
./gradlew :backend:run      # http://localhost:8080

# 2. Health + smoke test
curl http://localhost:8080/health
curl "http://localhost:8080/api/v1/medicines?page=0&pageSize=2"
curl "http://localhost:8080/api/v1/pharmacies/nearby?lat=31.5&lng=34.46&radius=10"

# 3. App (physical device)
adb reverse tcp:8080 tcp:8080   # app talks to http://localhost:8080
adb install -r app-debug.apk
# Emulator: backend is already http://10.0.2.2:8080 in debug builds
```

Config lives in `eilaji-backend/backend/src/main/resources/application.conf` (HOCON) — override `database.*`, `redis.*`, `minio.*`, `jwt.*` with env vars (see `eilaji-backend/README.md`).

**Demo accounts (password `password123`):** `patient@eilaji.com`, `pharmacist1@eilaji.com`, `pharmacist2@eilaji.com`, `admin@eilaji.com`.

## API at a glance

```
GET    /health
GET    /api/v1/medicines?page=&pageSize=&subcategoryId=
GET    /api/v1/medicines/{id}            GET /api/v1/medicines/search?q=
GET    /api/v1/medicines/categories
GET    /api/v1/pharmacies?page=&pageSize=&city=
GET    /api/v1/pharmacies/nearby?lat=&lng=&radius=
POST   /api/v1/auth/register  /auth/login  /auth/refresh
POST   /api/v1/prescriptions (multipart)   GET /api/v1/prescriptions
POST   /api/v1/orders                      GET /api/v1/orders
GET    /api/v1/chats  POST /api/v1/chats   GET /api/v1/chats/{id}/messages
WS     /api/v1/ws/chat?token=JWT
GET    /api/v1/favorites  POST /api/v1/favorites
POST   /api/v1/ratings                     GET /api/v1/pharmacies/{id}/ratings
```

## Sample data

Seeded automatically when the DB is empty (`DatabaseSeeder.kt`):

- **36 medicines** across Pain Relievers, Antibiotics, Vitamins & Supplements, Skin Care, Cold & Flu, Digestive Health
- **30 pharmacies** — 10 Gaza (+970), 5 Syria, 5 Egypt, 10 Saudi Arabia — with lat/lng, ratings, open/closed
- **2 ghost chat threads** (`patient@eilaji.com` ↔ Gaza pharmacies) so chat is testable without a pharmacist online

## Project layout

```
app/src/main/java/dev/anonymous/eilaji/
├── ui/base/.../home|categories|chatting|profile|send_prescription
├── ui/other/{map,add_address,medicine,checkout,search,favorite,reminder,messaging}
├── adapters/  network/ (Retrofit + WebSocketManager)  data/repository/ (CartRepository)
├── reminder_system/ (Room + WorkManager + AlarmReceiver + BootReceiver)
└── storage/ (AppSharedPreferences)
eilaji-backend/backend/src/main/kotlin/com/eilaji/backend/
├── controller/ (Routes, AdminController, AuthController)  service/  data/ (Tables)
├── websocket/  security/ (JwtConfig)  initialization/ (DatabaseSeeder)
└── config/ (DatabaseConfig, RateLimitPlugin)
.github/workflows/  android-ci.yml (APK artifact + v* releases)  backend-ci.yml
```

## CI & releases

- **Android CI:** JDK 17, `./gradlew assembleDebug`, `app-debug.apk` artifact on every push; `v*` tags create a GitHub Release with the APK + spin-up instructions.
- **Backend CI:** JDK 21, Postgres 15 + Redis 7 services, `:backend:build` + `:backend:test`.
- Latest stable: [v1.7.1](https://github.com/HeshamAbuShaban/Eilaji/releases/latest).

## Roadmap

- [ ] Pharmacist web workbench (quote inbox, stock toggle, order queue)
- [ ] COD ledger + delivery zones/courier tracking
- [ ] Postgres FTS search ranking + filters
- [ ] Phone OTP onboarding, refill prediction, loyalty/referrals

## License

Portfolio / educational use only — not licensed for commercial use or redistribution.
© Hesham AbuShaban, 2025. See [LICENSE](LICENSE).
