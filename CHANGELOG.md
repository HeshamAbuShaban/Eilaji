# Changelog

All notable changes are recorded here. Dates are `YYYY-MM-DD`.

## [v1.15.0] — 2026-10-10

### Security and data integrity (backend)

- Fixed the JWT identity bug: access and refresh tokens now carry a `sub` claim, which the Ktor
  auth plugin requires. Every authenticated route answered 401 before this fix.
- Configuration is typed and read from the environment. The JWT secret is no longer in the repo;
  production refuses to start without `JWT_SECRET`. Development gets a random secret instead.
- Logout revokes the token id in Redis, so a token stops working immediately.
- Refresh tokens are verified as refresh tokens (separate audience) and rotate on use.
- Chat messages, the chat WebSocket, order reads, order cancellation, prescription review and
  stock writes are all scoped to the caller. IDs can no longer be enumerated across accounts.
- Order totals are recomputed on the server, stock is reserved on create and returned on cancel,
  and orders now keep their line items.
- Added the missing prescription review endpoint (`PUT /prescriptions/{id}/status`) and the order
  cancellation endpoint (`PUT /orders/{id}/cancel`).
- The inbound Eilaji-Plus webhook requires an HMAC signature when configured.
- 500 responses no longer contain exception text. The cause is logged, the client gets one line.
- Rate limiting uses the real auth paths and fails closed when Redis is unreachable.
- Request limits: 8 MB uploads, 1 MB socket frames, clamped search radius.
- Nearby pharmacies use a SQL bounding box, and medicine search escapes LIKE wildcards and is
  case-insensitive.
- Seeding is off by default, refuses to run in production, and writes random passwords unless
  `SEED_DEMO_CREDENTIALS=true`.
- Named, idempotent migrations run against a `schema_migrations` table, so upgrades are reviewable.
- Added password reset endpoints to the backend (`forgot-password`, `reset-password`).

### Security and data integrity (Android)

- Removed the FCM server key and the Google Maps key from the repository. Firebase was removed
  entirely; the app now talks only to the Eilaji backend.
- Tokens are stored in `EncryptedSharedPreferences`. The plaintext fallback is wiped on entry and
  excluded from device backup and device transfer.
- A 401 now triggers one automatic token refresh. When refresh is not possible the app clears the
  session and returns to login instead of failing every call.
- Password reset runs against the backend.
- One shared OkHttp client with a connection pool, an HTTP cache and retry, instead of a new
  client per screen.
- `allowMainThreadQueries` and `fallbackToDestructiveMigration` are gone from both databases;
  favourites are read once per list off the main thread.
- The WebSocket client now pings and reconnects with backoff.

### Stability

- `MessagingFragment`, `FavoritesFragment`, `SearchFragment`, `HomeFragment` and the favorites
  view model no longer touch a destroyed view. The favorites screen no longer holds an Activity.
- The Holder of the fake "loading" thread is removed; the lock-screen alarm checks the
  notification permission before it tries to notify.
- `BootReceiver` is no longer exported, and all notifications use one stable channel.
- The reminder database keeps its data across a version bump.

### Design, accessibility and Arabic

- Invisible text is fixed: two chips were white-on-white in both themes.
- Contrast: secondary greys now meet 4.5:1 in light and dark themes; the night chat preview is legible.
- Touch targets: quantity steppers, cart actions, call/chat buttons, send and favourite controls
  are at least 48dp.
- TalkBack: every icon button has a content description, and rows are announced as actionable.
- Wrapped icon buttons keep their icon size and grow only the touch area.
- Night variants for the ripple masks, and identical corner radius for the day/night pill shape.
- Plurals replace hand-rolled singular/plural branches, in English and Arabic.
- Roughly seventy hardcoded literals moved into `strings.xml` with Arabic translations, including
  the navigation graph labels and the dosage labels.
- Currency formatting is locale-aware through `utils/Money.kt`.
- Error states now offer retry: medicine detail, orders and chat history show a Snackbar with an
  action instead of a Toast next to an unrelated empty state.
- The alarm screen text follows the user's font scale.

### Build and CI

- Android CI runs the unit tests and Android Lint, and lint errors now fail the build.
- A release build is signed when `RELEASE_KEYSTORE_BASE64` is configured. Without the secret CI
  publishes the debug APK and states so in the release notes.
- `versionName` and `versionCode` come from the release tag. `versionCode 1.8` is gone.
- Backend CI uses Postgres 16 like production, and passes the variables the service actually reads.
- Removed dead dependencies, the duplicated Retrofit and location declarations, `kotlin-kapt`,
  and `google-services.json`.

### Documentation

- New `AGENTS.md` for repository rules, `docs/SECURITY.md`, and `docs/UPGRADING.md`.
- README rewritten: configuration, security, maintenance, API, and the removal of the demo
  password from the release notes.

## [v1.14.0] — 2025-12-01

### Added

- Edge-to-edge transparent system bars across all activities.
- Animated pill splash with a fade-out exit.
- Spring physics micro-interactions for favourites, cart, rating, save, and card presses.
- Gradient brand text on the home greeting and the login title.
- Ken Burns drift on the home ad carousel.
- Frosted-glass bottom bars on checkout and medicine detail.
- Lottie check animation on order success, with a static fallback.
- Material You dynamic colour as an opt-in theme setting.
- Compose islands: animated tracking steps and checkout totals.
- Predictive back.

## [v1.13.0] — 2025-11-17

### Fixed

- Crash-hardened map, category, medicine, search, and chat flows.
- Cache and shimmer logic on the home screen.

## [v1.12.0] — 2025-11-04

### Added

- Offline catalog cache, night map style, pager peek, press and spring motion.

### Fixed

- Batch of lifecycle and leak bugs across the app.

## [v1.11.0] — 2025-10-21

### Added

- Offline catalogue cache, night map style.

## [v1.10.0] — 2025-10-07

### Added

- Ops dashboard: order queue, courier simulation, prescription triage, stock, chat preview,
  customer view, user verification.

## [v1.9.x] — 2025-09-22

### Added

- Medication reminders: exact alarms, full-screen alarm screen, snooze, dosage, reboot-safe
  scheduling.

## Earlier

`v1.7.1` stability work, and `v1.8.0` catalogue, nearby, prescription, chat, cart, checkout,
order tracking, favourites and ratings.
