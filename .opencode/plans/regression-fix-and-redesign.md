# Eilaji — Regression Fixes + Screen Redesign (approved)

Status key: [x] done, [ ] to do.

## Phase 0 — already applied this session (uncommitted)
- [x] `local.properties` untracked (`git rm --cached`), key restored only on disk
- [x] `MAPS_API_KEY` added to the untracked `local.properties` (map works locally again)
- [x] Seeder: demo credentials default ON for `APP_PROFILE=dev/test`, forced OFF in prod
      (`AppConfig.seedDemoCredentials`, `application.conf`)

## Phase 1 — regressions to fix now

### 1.1 Alarm reliability (the "alarm screen does not show on time with noise" bug)
New file `app/src/main/java/dev/anonymous/eilaji/reminder_system/ReminderReliability.kt`:
- `check(context)` → notifications allowed, exact alarms allowed, battery optimisation ignored
- `ensureChannel(context)` → one channel `eilaji.reminders`, `IMPORTANCE_HIGH`,
  `USAGE_ALARM` sound (not the notification default), `setBypassDnd(true)` (already written, needs commit)
- intents: notification settings, `ACTION_REQUEST_SCHEDULE_EXACT_ALARM`, battery exemption

Edits:
- `utils/AppController.onCreate` → `ReminderReliability.ensureChannel(this)` and start the
  foreground reminder service (push, see 1.3) when a token exists
- `ui/main/MainActivity` → request `POST_NOTIFICATIONS` at start when `!isReady`
- `ui/base/BaseActivity` → show a dismissible reliability banner when `!isReady`; tapping the
  banner opens the matching settings intent (no more silent downgrade)
- `reminder_system/receiver/AlarmReceiver.kt` → remove the `areNotificationsEnabled()`
  guard around `nm.notify` (it made silence worse); notify always and let the channel decide.
  Use `ReminderReliability.channelId()` instead of `"Reminders"` so one channel is reused.
- `reminder_system/worker/ReminderScheduler.kt` → keep `setExactAndAllowWhileIdle`, but when
  `canScheduleExactAlarms()` is false, log it and mark the reminder as "unreliable" so the UI
  can warn (no behaviour change to the schedule itself).

Strings (values + values-ar): `channel_reminders_name`, `channel_reminders_description`,
`reminder_reliability_banner_title/body/action`.

### 1.2 Push — self-owned, no third party
Backend:
- `Dtos`: `UpdateFcmTokenRequest(token, platform)`
- `Routes`: `PUT /users/fcm-token` (auth) — stores on `Users.fcmToken`
- `service/PushService.kt`: delivers a payload to a device over the app's own WebSocket
  (`WebSocketSessionManager.sendToUser`) with type `PUSH`, falling back to a Redis
  `push:pending:<userId>` list drained by the app on connect; payload capped at 4 KB.
- Emit on: order status change (`OrderService.updateOrderStatus`), prescription status change
  (`PrescriptionService.updatePrescriptionStatus`), new chat message
  (`WebSocketController` MESSAGE branch).
Android:
- `utils/PushService.kt` — foreground service holding the existing WebSocket open
  (`WebSocketNotification` channel, low importance, persistent notification), started after
  login and on boot, stopped on logout.
- `worker/PushSyncWorker.kt` — `PeriodicWorkRequest` 15 min: `GET /notifications/pending`,
  posts local notifications as the offline fallback.
- `network/websocket/WebSocketManager` → new `PUSH` frame type → local notification.
- Backend `GET /notifications/pending` + `POST /notifications/pending/ack`.
Verify with: log in, background the app, push an order status change from the dashboard, expect
a local notification within seconds while the socket is alive and within 15 min when it is not.

### 1.3 Docs for the new flows
- `docs/SECURITY.md`: push section (own transport, no FCM), key-rotation reminder.
- `docs/UPGRADING.md`: alarm reliability + push runbook.
- `CHANGELOG.md` entry.

## Phase 2 — the design change (must be visible, must not demolish)

### 2.1 Home + FAB removal
- Delete `fabPrescription` from `res/layout/activity_base.xml`; move the action into
  `res/layout/fragment_home.xml` as a 3-icon action row (Prescription / Chat / Reminders).
- Remove the 4 quick-action cards (`cardUploadPrescription`, `cardFindPharmacy`,
  `cardChat`, `cardReminders`) from `fragment_home.xml` — they were the FAB duplicated.
- Home becomes: collapsing hero (greeting + date + search entry + theme toggle) → 3-icon action
  row → `TabLayout` + `ViewPager2` with three short tabs:
  `For you` (ads carousel + best sellers) · `Categories` (grid) · `Nearby` (map + pharmacy list).
- `HomeFragment` splits into `HomeHeroController` + the three tabs' existing fragments
  (reuse `CategoriesFragment`, `SubCategoriesFragment`, `MapFragment`).
- Keep the offline cache, shimmer, and Arabic greeting exactly as they are.

### 2.2 Custom views (new files)
- `ui/components/DoseWheel.kt` — time picker view (hour / minute wheels) with a spring snap,
  used by the reminder wizard; haptic tick per detent.
- `ui/components/StepStepper.kt` — order state rail (PENDING → DELIVERED / CANCELLED) with the
  brand colour and RTL direction.
- `ui/components/PharmacyCard.kt` — one card used by home nearby, map pager, and search.
- `ui/components/MoneyRow.kt` — label + amount using `utils/Money.kt`.

### 2.3 Set alarm → 3-step wizard
- New `fragment_reminder_wizard.xml` with three steps in a `ViewPager2` + `StepStepper`:
  1. medicine (autocomplete from `CatalogCache`) + dose count
  2. `DoseWheel` time + frequency chips (keep existing daily/weekly/custom/interval)
  3. confirm card: "Next dose in 6h 20m", vibration toggle, save
- `ReminderFragment` keeps its ViewModel/scheduling calls; only the UI shell changes.
- Existing `fragment_reminder.xml` stays for the interval/detail case? No — replace it, delete
  the old layout after the wizard is green.

### 2.4 Orders + tracking merged
- New `MyOrdersScreen`: list of `PharmacyCard`-styled order cards, each expanding in place into
  the `StepStepper` + map + `handoffCode` + ETA.
- `OrdersFragment` and `OrderTrackingFragment` collapse into one destination; the nav graph
  keeps `navigation_orders` and drops `orderTracking`.

### 2.5 Profile grouped
- `fragment_profile.xml` becomes grouped sections (Account / Health / Preferences / Support)
  with the existing edit / orders / favourites / reminders / password rows inside them.
- Theme selector stays (light / dark / system / dynamic) but as segmented buttons, not 4 buttons.

## Phase 3 — verify (device 35d5637e) before calling it done
1. Map renders (Maps key restored).
2. Login with `patient@eilaji.com` / `password123` on a dev seed.
3. Alarm at T+1 min rings with sound on the lock screen.
4. Push: order status change → notification within seconds.
5. Arabic + RTL + dark mode on every changed screen.
6. `./gradlew :app:lintDebug` 0 errors, `:backend:test` green, CI green.
7. Tag `v1.16.0` and confirm the APK asset.

## Out of scope (needs your word first)
- Firebase in any form (stays out).
- New backend dependencies.
- Replacing the brand palette.
