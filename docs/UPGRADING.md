# Maintenance and Upgrading

Three procedures: upgrade the backend, change the schema, and cut a release.
Each one is a checklist. Do the steps in order.

## 1. Upgrade the backend

1. Stop the service. `docker compose -f docker-compose.prod.yml down`
2. Pull the new code. `git fetch && git checkout <tag-or-commit>`
3. Check that `JWT_SECRET` is set. `test -n "$JWT_SECRET" && echo ok`
   The service refuses to start in production without it.
4. Back up the database first, always. `docker compose exec postgres pg_dump -U eilaji eilaji_db > backup.sql`
5. Rebuild the image. `docker compose -f docker-compose.prod.yml build backend`
6. Start the service. `docker compose -f docker-compose.prod.yml up -d backend`
7. Read the startup log. The runs list every applied migration by name.
   `docker compose logs -f backend | grep -i migration`
8. Check health. `curl -fsS http://localhost:8080/health`
   The answer must be `UP`. `DEGRADED` means Postgres or Redis is unreachable.
9. If the start failed, restore the backup and report the error. Do not retry blindly.

## 2. Change the database schema

1. Write the change as SQL in `Migrations.kt`, under `migrations`.
   Use the next number in the sequence, for example `012_add_something`.
2. Make the SQL idempotent. Use `IF NOT EXISTS` and `ALTER TABLE ... DROP NOT NULL IF EXISTS`
   where the platform allows it.
3. Add the migration to the list in `Migrations.kt`, in order.
4. If the change needs an extension such as `pg_trgm`, add it to the to `OPTIONAL_MIGRATIONS`
   set, and create the extension in `init-db.sql` for fresh volumes. A restricted database then
   skips it with a warning instead of failing to boot.
5. Add a test for the new behaviour, then run the backend suite.
   `cd eilaji-backend && ./gradlew :backend:test`
6. Never use `SchemaUtils` to change an existing column. It only adds missing tables and columns,
   and it is a development convenience (`CREATE_MISSING_TABLES`).
7. Add the migration name to `CHANGELOG.md`.

### Adding a Room migration (Android)

1. Bump the `version` of the database.
2. Add a `Migration(...)` object for the schema change.
3. Pass it with `.addMigrations(...)` in the database builder.
4. Never use `fallbackToDestructiveMigration()`. Reminders and favourites are not on the server,
   so a destructive migration deletes the user's data for good.

## 3. Cut a release

1. Finish the work on `master` and confirm both workflows are green.
2. Update `CHANGELOG.md` with the release date and the changes.
3. Tag the release. `git tag -a vX.Y.Z -m "vX.Y.Z — summary"`
4. Push the tag. `git push origin vX.Y.Z`
5. The Android CI reads the tag, sets `versionName` and `versionCode`, and builds the APK.
6. A keystore in `RELEASE_KEYSTORE_BASE64` produces a signed release APK. Without it, CI publishes
   the debug APK and says so in the release notes.
7. Open the release page and confirm the APK is attached.

## 4. Version numbers

| Place | Rule |
|---|---|
| Git tag | `vX.Y.Z`. The release uses it as `versionName` and `X` as `versionCode`. |
| Backend | `org.gradle...version` in `backend/build.gradle.kts`. It is not tied to the app version. |

## 5. Routine tasks

| Task | Command | Frequency |
|---|---|---|
| Backend tests | `cd eilaji-backend && ./gradlew :backend:test` | On every change |
| Android tests and lint | `./gradlew testDebugUnitTest :app:lintDebug` | On every change |
| Local build | `JAVA_HOME=$JAVA17 ./gradlew :app:assembleDebug` | On every change |
| Database backup | See step 1.4 | Before every upgrade |
| Dependency review | `./gradlew dependencyUpdates` or Dependabot | Monthly |
| Token rotation | See the console for the leaked keys | Quarterly |
| Redis memory check | `docker compose exec redis redis-cli info memory` | Weekly |
| Disk for MinIO | Check the volume on the host | Weekly |

## 5b. Push and reminders, verified on a device

1. Start the backend. `docker compose up -d` then `./gradlew :backend:run`
2. Install the app. `adb install -r app/build/outputs/apk/debug/app-debug.apk`
3. Open the app. Allow notifications when asked. The reminder banner must disappear.
4. Create a reminder one minute ahead. Lock the phone. It must ring with the alarm sound.
5. Log in, send an order status change from the dashboard, and background the app.
   A notification must arrive within seconds.

## 6. When something breaks

| Symptom | First check |
|---|---|
| Every private route answers 401 | Is `JWT_SECRET` the same as when the token was issued? Is the app sending a bearer token? |
| `/health` answers `DEGRADED` | `docker compose ps`, then the Postgres and Redis logs |
| Login is suddenly rate limited | `RATE_LIMIT_*` values; check `auth` logs in Redis |
| Upload answers 413 | `MAX_UPLOAD_BYTES`, and the image size on the client |
| A migration fails at boot | The log names the migration. Restore the backup, fix the SQL, redeploy |
| Map is empty in the release build | `MAPS_API_KEY` is missing, so the placeholder is empty |
| Reminders do not fire | Notifications permission, exact-alarm permission, and battery optimisation exemption |

## 7. Local development reference

```bash
# backend
cd eilaji-backend
docker compose up -d postgres redis minio
APP_PROFILE=dev SEED_ON_STARTUP=true ./gradlew :backend:run

# android (JDK 17 is required locally; JDK 21 fails on a build-tools transform)
JAVA_HOME=/usr/lib/jvm/java-17-openjdk ./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
