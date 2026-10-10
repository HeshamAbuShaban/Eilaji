# AGENTS.md

Instructions for anyone (human or agent) working in this repository.
Follow them exactly. They keep the code reviewable and the app safe for patients.

## 0. Read this first

The product goal: a pharmacy app for people who need medicine now — a Gaza-first audience that
suffers bad connectivity, power cuts, and urgent need. Everything you build must survive a slow
network, a dark room, an Arabic screen, and a one-handed phone. When two choices are equal, pick
the one that keeps working when the network fails.

## 1. Build and verify

Run these before you say that a change is done.

| Check | Command |
|---|---|
| Backend tests | `cd eilaji-backend && ./gradlew :backend:test` |
| Android unit tests | `JAVA_HOME=$JAVA17 ./gradlew testDebugUnitTest` |
| Android lint | `JAVA_HOME=$JAVA17 ./gradlew :app:lintDebug` |
| Android build | `JAVA_HOME=$JAVA17 ./gradlew :app:assembleDebug` |

Notes:

- Android builds need JDK 17. JDK 21 fails in this project on a build-tools transform.
- Backend compiles and tests run fine on JDK 21.
- Lint must report **0 errors**. Warnings may remain, but do not add new ones.
- If a check fails because of a file you did not touch, report it and stop. Do not "fix" it blindly.

## 2. Secrets

1. Never add a key, a token, a password, or a keystore to the repository.
2. `MAPS_API_KEY` comes from `local.properties` or a CI secret.
3. `JWT_SECRET` comes from the environment.
4. If you find a secret in the repo, stop and report it. See `docs/SECURITY.md`.

## 3. Backend rules

1. Every private route starts with `call.callerOr401() ?: return@<verb>`. Do not use `principal!!`.
2. Check ownership in the service, not only the route. A route check alone is how IDOR bugs ship.
3. Return deliberate errors with `ApiException(status, message)`. Never leak `e.message`.
4. Recompute money and quantities on the server. Never trust a client-supplied total.
5. Add a named migration for any schema change. Do not rely on `SchemaUtils` for existing columns.
6. Every new behaviour gets a test in `backend/src/test/kotlin`.
7. Keep pagination on list endpoints. `pageSize` is clamped to 50.
8. Do not swallow exceptions. An empty `catch` hides a data-loss bug.

## 4. Android rules

1. Views: use `private var _binding: X? = null`, null it in `onDestroyView`, and guard every
   network, socket and post-delayed callback with `if (binding == null) return`.
2. Never touch a database on the main thread. Use `Dispatchers.IO` or Room `suspend` DAO methods.
3. Every network call is cancelled or guarded when the view goes away.
4. No Firebase. The backend owns identity, storage, and push.
5. No hardcoded colours, sizes, or user-facing strings in layouts. Use theme attributes and
   `values/strings.xml` + `values-ar/strings.xml`.
6. Every interactive view needs a content description and a 48dp minimum touch target.
7. Every new screen needs three states: loading, empty, and error-with-retry.
8. Use `dev.anonymous.eilaji.utils.Money.format()` for prices. Never `"${price} $"`.
9. Prefer Snackbar with a retry action over a Toast for errors.
10. No `notifyDataSetChanged()` where a diff will do, and never run Room inside `onBindViewHolder`.

## 5. Review checklist (run before you commit)

- [ ] Both CI workflows are green on the branch.
- [ ] No secrets, keys, or tokens anywhere in the diff.
- [ ] New endpoints are authenticated, authorised, validated, and paginated.
- [ ] New screens work in Arabic, in RTL, at 1.3x font scale, and in dark mode.
- [ ] New UI has loading, empty, and error states with retry.
- [ ] Chat messages, orders and prescriptions respect ownership.
- [ ] No `printStackTrace`, no `Log.d` of personal data, no `e.message` in a response.
- [ ] `CHANGELOG.md` records user-visible changes.
- [ ] The doc that describes the change is updated in the same commit.

## 6. Style

- Kotlin: 4-space indent, trailing commas, explicit types at public API boundaries.
- Commit messages: imperative, one line subject under 72 characters, explain *why* in the body.
- Small commits that each leave the build green. Do not stack a feature on a red build.

## 7. Out of scope without approval

- Firebase in any form.
- New backend dependencies (ask first).
- Changing the brand palette (`#BA324F`).
- Rewriting navigation or the whole UI toolkit.
