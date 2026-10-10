# Security

This document records how secrets are handled, what the backend protects, and what to do when a
credential leaks. Read it before you deploy anything.

## 1. Secrets that must never be in the repository

| Secret | Where it lives | What breaks if it leaks |
|---|---|---|
| `JWT_SECRET` | Environment variable / deploy secret | Anyone can mint an admin token |
| `MAPS_API_KEY` | `local.properties` (git-ignored) or CI secret `MAPS_API_KEY` | Billing abuse, quota exhaustion |
| Release keystore | GitHub secret `RELEASE_KEYSTORE_BASE64` (+ password, alias, key password) | Someone can publish a malicious "update" |
| `MINIO_SECRET_KEY` | Environment variable | Read/write to prescription images |
| `EILAJI_PLUS_WEBHOOK_SECRET` | Environment variable | Forged pharmacist quotes |

Rules:

1. Never commit a key, a token, or a `.jks` file.
2. Never put a key in `application.conf`. Only non-secret defaults go there.
3. Never put a key in a `buildConfigField`. The app reads `MAPS_API_KEY` from `local.properties`.
4. Check every commit with `git log -p -S <value>` before you push, if you touched keys.

### If a key is ever committed

1. Revoke the key in its console first. A commit stays in git history forever.
2. Remove the key from `HEAD`.
3. Force the token or key to be replaced everywhere it is used.
4. Note the rotation in `CHANGELOG.md`.

Two keys leaked in this repository's history and must be treated as compromised: the old FCM
server key (now unused, Firebase was removed from the app) and the old Google Maps key
(now read from `local.properties`). Rotate both in the Google Cloud console.

## 2. Token handling

| Item | Behaviour |
|---|---|
| Access token | JWT, 1 hour, audience `eilaji-client`, carries `sub` |
| Refresh token | JWT, 30 days, audience `eilaji-client-refresh`, rotated on every refresh |
| Logout | The token id (`jti`) is stored in Redis with a TTL, so the token stops working at once |
| Storage on device | `EncryptedSharedPreferences`; a plaintext fallback exists only if the keystore fails, and it is wiped on entry and excluded from backups |
| Interceptor | A 401 triggers one silent refresh; if that fails the app clears the session and returns to login |
| WebSocket | The token is passed in the query string because browsers cannot set headers there. It is accepted only with a short-lived refresh token in future work; today it is one more reason to keep the token TTL short. |

Seeded accounts are a development feature. `SEED_DEMO_CREDENTIALS=true` gives every seeded
account the password `password123`. Production must never set it.

## 3. What the API enforces

- Guests can read the catalog, search, pharmacies and nearby results.
- Everything else needs a valid access token.
- Chat threads: only the patient and the pharmacy can read or write, including over WebSocket.
- Orders: a patient reads and cancels only their own orders. A pharmacist reads and transitions
  only orders for pharmacies they own.
- Prescription review: only the owning pharmacy or an admin.
- Stock: a pharmacist writes only their own pharmacy.
- The Eilaji-Plus webhook requires an HMAC signature (`X-Eilaji-Signature`, `sha256=<hex>`) whenever
  `EILAJI_PLUS_WEBHOOK_SECRET` is configured. Production refuses unsigned webhooks.

## 4. Hard limits

| Setting | Default | Effect |
|---|---|---|
| `MAX_UPLOAD_BYTES` | 8 MB | Rejects a larger prescription image with 413 |
| `MAX_WS_FRAME_BYTES` | 1 MB | Rejects a larger socket frame |
| `SEARCH_MAX_RADIUS_KM` | 50 | Clamps the nearby search radius |
| `RATE_LIMIT_LOGIN` | 5/min | Brute-force limit on login |
| `RATE_LIMIT_REGISTER` | 3/hour | Registration limit |
| `RATE_LIMIT_FAIL_CLOSED` | `true` | Auth routes answer 429 when Redis is down, instead of disabling the limit |

## 5. Error handling

Clients get a short message. `e.message` never reaches the client. The real cause is logged with
the request path by `StatusPages` or by the route's `serverError` helper. Never re-add
`error = e.message` to a response.

## 6. Reporting

Do not open a public issue for a vulnerability. Use GitHub private vulnerability reporting or
contact the maintainer directly. Include the affected path and the steps to reproduce it.
