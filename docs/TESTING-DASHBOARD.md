# Testing with the Ops Dashboard

The dashboard at `http://localhost:8080/dashboard` is a second client of the
exact REST API the Android app uses — not a separate system. Anything you do
there appears in the app, and vice versa. It stands in for the E-Doctor repo
until that lands: pharmacist operations plus a small customer harness.

## 1. The connection, visualized

```
┌──────────────┐   USB: adb reverse tcp:8080 tcp:8080   ┌──────────────┐
│  PHONE (app) │ ─── phone localhost:8080 ────────────► │              │
│  debug build │    (re-run every unplug!)              │    BACKEND   │
│  localhost   │                                        │  :8080       │
└──────────────┘                                        │  ┌────────┐  │
┌──────────────┐                                        │  │ /dash- │  │
│  EMULATOR    │ ─── 10.0.2.2:8080 ───────────────────► │  │ board/ │  │
└──────────────┘                                        │  └────────┘  │
                                                        │  Postgres · Redis · MinIO │
                                                        └──────────────┘
```

- **Emulator → PC:** magic address `10.0.2.2` (no setup).
- **Physical phone → PC:** the phone has no route to your PC's `localhost`.
  `adb reverse tcp:8080 tcp:8080` punches the phone's own `localhost:8080`
  through USB to the PC. Check with `adb reverse --list`. No Wi-Fi, no
  firewall rules. Disconnecting just means calls fail — the app serves cached
  catalog and local-first favorites/reminders/cart, and tells you with retry
  snackbars instead of blank screens.
- **Dashboard → backend:** same machine via `location.origin` — zero setup.
- **Chat socket:** `ws://…/ws/chat?token=JWT` (emulator `10.0.2.2`, phone
  `localhost` through the tunnel).

## 2. Prerequisites checklist

1. `cd eilaji-backend && docker-compose up -d` (postgres `5432`, redis
   `6379`, minio `9000/9001`), `./gradlew :backend:run` →
   `curl localhost:8080/health` returns `UP`.
2. First start seeds v2 (wipe the postgres volume once if you ran older
   versions): 36 medicines, 30 pharmacies, 2 ghost chats.
3. Accounts (`password123`): `pharmacist1@eilaji.com` (pharmacy tab),
   `patient@eilaji.com` (customer tab + app login), `admin@eilaji.com`.
4. Phone: `adb reverse tcp:8080 tcp:8080` + install latest release APK, log in
   as patient.

## 3. Test loops (dashboard does → app shows)

| # | Loop | Dashboard steps | Watch in app |
|---|---|---|---|
| 1 | Order lifecycle | Pharmacy tab → advance `PENDING→CONFIRMED→PREPARING→SHIPPED→DELIVERED` (or Cancel / COD collected) | Orders list status pill; tracking screen stepper + progress (polls every 15s — or enable dashboard **live 10s** and advance while watching) |
| 2 | Live courier | Pick active order → drag simulator slider → **Push position** | Tracking map: backend position overrides the simulation, ETA panel follows; **Clear** returns to sim |
| 3 | Prescription triage | Pending Rx → view image → enter quote → **Quote & accept** (or Reject) | Patient's prescription status advances; accepted Rx can become an order |
| 4 | Stock truth | Stock tab → pick pharmacy → search medicine → set qty/price → Add; Hide to delist | Search/medicine surfaces (read path is live; badges consume it next) |
| 5 | Chat threads | Chats tab → open ghost thread, read last 20 | Same thread in app Chats → realtime via socket |
| 6 | Customer harness | Customer tab → place direct OTC order | Appears in app Orders + dashboard queue (place it, then run loop 1 on it) |

## 4. Role logic to know

- Pharmacist endpoints are **scoped to owned stores** — `pharmacist1` cannot
  advance `pharmacist2`'s orders or stock (a 400 "access denied" means the
  test is passing).
- Order statuses enforce a machine: no skipping `PENDING→SHIPPED`; terminal
  `DELIVERED`/`CANCELLED` are locked; legacy `PAID`/`PROCESSING` auto-map.
- Session persists in browser `localStorage`; **Sign out** clears it. The live
  toggle pauses itself on hidden tabs.

## 5. Troubleshooting

| Symptom | Cause → fix |
|---|---|
| Dashboard `sign in first` loop | Backend down or wrong port → `curl :8080/health` |
| App lists empty on phone | Tunnel down → re-run `adb reverse`; emulator → use `10.0.2.2` build |
| Push courier, map doesn't move | Order `DELIVERED`/`CANCELLED` (sim stops), or app showing sim (clear + re-push) |
| Quote button 4xx | Signed in as patient — switch to pharmacist1 |
| Old data (Tablets×4 taxonomy) | Pre-v2 volume — wipe postgres volume once, restart, reseed |
