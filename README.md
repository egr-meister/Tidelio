# Tidelio

Tidelio is a lightweight, fully offline water-intake tracker for Android. The day is shown as a
horizontal **wave timeline** split into Morning, Day and Evening; the wave rises at the moment
each entry is recorded.

Tidelio records amounts that the user enters. It does **not** calculate hydration requirements and
gives no health advice.

## Features

- Quick logging with four customizable presets (default 150 / 250 / 350 / 500 mL) and Undo.
  Rapid repeat taps on the same preset are ignored for 800 ms (no accidental duplicates).
- Custom entries (amount, date, time) with validation; editing and deleting with Undo.
- Wave of the Day: step-based cumulative timeline, entry markers at their real times, goal line,
  "later today" shading, optional restrained ripple (off with *Reduce wave animation* or the
  system *Remove animations* setting).
- Morning / Day / Evening summaries (volume, entries, share) that filter the entry list.
- Monthly calendar with recorded-volume dots, goal-reached checkmarks, today/selected highlights.
- 7- and 30-day statistics with an explanation panel.
- User-chosen daily goal with effective-dated history ("starting today" / "starting tomorrow").
- Privacy screen and *Clear all local data*.

Excluded by design: accounts, backend, Firebase, ads, analytics, payments, cloud sync,
notifications, sensors, Health Connect, wearables, location and any network access.

## Architecture

Single `:app` module, Kotlin + Jetpack Compose (customized Material 3), manual DI (`AppContainer`).

```
app/src/main/java/app/tidelio
├── data/local          Room entities, DAOs, database, migrations
├── data/repository    EntryRepository, GoalRepository, PreferencesRepository (DataStore)
├── domain/entries     WaterEntry, DayPeriod, validation, duplicate-tap guard, EntryEditor, Undo
├── domain/goals       Goal limits, GoalTimeline (effective-dated lookup)
├── domain/statistics  WaveCalculator (chart coordinates), StatisticsCalculator
├── domain/time        TimeSource (injectable clock/zone), DST resolver, month grid, TodayProvider
└── ui/{wave,entry,calendar,statistics,settings,theme,common}
```

Period classification, goal lookup, wave coordinates and statistics are pure Kotlin in `domain/`,
separate from rendering, and are unit-tested with an injected clock and time zone.
ViewModels expose `StateFlow`, collected with `collectAsStateWithLifecycle`. Room work runs off the
main thread (suspend DAOs / Flow).

## Toolchain

| Component | Version |
|---|---|
| JDK | 17 (Temurin in CI) |
| Gradle (wrapper, committed) | 8.14.3 |
| Android Gradle Plugin | 8.13.0 |
| Kotlin / Compose compiler plugin | 2.2.10 |
| KSP | 2.2.10-2.0.2 |
| Compose BOM | 2025.09.00 |
| Room | 2.7.2 |
| DataStore | 1.1.7 |
| Navigation Compose | 2.9.3 |
| Lifecycle | 2.9.2 |
| compileSdk / targetSdk / minSdk | **36 / 36 / 26** |
| Build tools (CI) | 36.0.0 |

No chart or date libraries are used — charts are drawn with Compose `Canvas`, dates use `java.time`.

## Build

```bash
./gradlew testDebugUnitTest          # unit tests
./gradlew lintRelease                # release lint
./gradlew assembleDebug              # debug APK, no release credentials needed
./gradlew assembleRelease bundleRelease   # signed release (credentials required, see below)
```

Outputs:

- APK: `app/build/outputs/apk/release/app-release.apk` (local install / verification)
- AAB: `app/build/outputs/bundle/release/app-release.aab` (**the only file uploaded to Google Play**)

## Wave calculation

- X axis: local time of day, 00:00 → 24:00 (`x = secondOfDay / 86400`).
- Y axis: `cumulative volume / goal of the selected day`, capped at the goal line for drawing.
- Entries are ordered by local time; entries with the same local time form one rise.
- The line starts at zero and only rises at entry times (step function), so no intake is implied
  before an entry. Markers sit at the entries' real times.
- Today: hours after "now" have a lighter background and the wave stops at "now".
- Without a goal: a labelled volume scale (total rounded up to the next 500 mL) and no percentage.
- Above the goal: the drawing stays at the goal line, while text shows the real total and percent,
  e.g. `2,200 / 2,000 mL — 110%` and `200 mL above your goal.` Percentages are floored.
- The ripple animation is clipped inside the filled area and never moves the data boundary.

## Period boundaries

| Period | Local time |
|---|---|
| Morning | 00:00–11:59 |
| Day | 12:00–17:59 |
| Evening | 18:00–23:59 |

Every entry belongs to exactly one period based on its **stored local time**.
Period share = `period volume / daily volume × 100`; a day without entries shows "No entries".

## Goals and statistics

- Goal range 100–10,000 mL (whole mL). The user chooses it; setup examples (1,500 / 2,000 /
  2,500 mL) are labelled as examples, not recommendations. Logging works before a goal exists.
- Goals are stored as effective-dated records (`goal_changes`, one row per date, replaced
  transactionally). A day uses the latest goal effective on or before it. Changing the goal asks
  *Starting today* or *Starting tomorrow*; earlier dates are never rewritten.
- Statistics (7 or 30 dates ending today):
  - Total recorded = sum of entries.
  - Days with records = dates with ≥ 1 entry.
  - **Average on days with entries** = total ÷ days with records (missing days are excluded, never
    counted as zero).
  - Morning/Day/Evening distribution = period volume ÷ total × 100.
  - Goal reached = recorded days whose total ≥ that date's goal, out of recorded days that had a goal.

## Date and time behaviour

- Each entry stores the absolute timestamp, logged local date, logged local time and zone ID.
- Grouping and periods always use the stored local date/time, so travelling never moves past
  entries between dates. Editing date/time explicitly regroups the entry.
- "Today" is refreshed on launch and every return to the foreground; while the app is open a single
  timer switches to the new date just after local midnight.
- Daylight saving: a **nonexistent** local time (spring-forward gap) is rejected with a message;
  an **ambiguous** local time (fall-back overlap) resolves deterministically to the **earlier**
  instant. The stored local time is exactly what the user entered.
- No online time verification.

## Offline operation, privacy, storage and backup

- No `INTERNET` or `ACCESS_NETWORK_STATE`, no runtime permissions, no networking libraries.
- Data lives in app-private storage: Room database `tidelio.db` and DataStore `tidelio_prefs`.
- `android:allowBackup="false"`, `fullBackupContent` (≤ Android 11) and `dataExtractionRules`
  (Android 12+) exclude all data from cloud backup and device-to-device transfer.
- Room schema is exported to `app/schemas`; migrations live in `Migrations.ALL`; no destructive
  fallback is configured.

## Permission verification

CI dumps the packaged permissions with `aapt2 dump permissions` and fails on anything except
AndroidX's internal `app.tidelio.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (a signature-level
permission androidx.core declares for non-exported dynamic receivers). Locally:

```bash
$ANDROID_HOME/build-tools/36.0.0/aapt2 dump permissions app/build/outputs/apk/release/app-release.apk
```

## Release signing (PKCS12)

`app/build.gradle.kts` defines `signingConfigs.release` with `storeType = "PKCS12"` and assigns it to
the release build type. Credentials come from environment variables, or from an uncommitted
`keystore.properties` in the project root:

```properties
storeFile=/absolute/path/to/tidelio-release.p12
storePassword=...
keyAlias=...
keyPassword=...
```

| Env variable | Purpose |
|---|---|
| `TIDELIO_KEYSTORE_PATH` | path to the `.p12` (CI decodes it to a temp file) |
| `ANDROID_KEYSTORE_PASSWORD` | store password |
| `ANDROID_KEY_ALIAS` | key alias |
| `ANDROID_KEY_PASSWORD` | key password |

Release tasks fail if anything is missing — there is **no fallback to debug signing**. Debug builds
need no credentials. Signing material is git-ignored and never printed.

### GitHub Secrets

`ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`.

### Play App Signing

The key in this keystore is the **upload key**. With Play App Signing, Google holds the app-signing
key and re-signs the APKs it delivers; keep the upload key and passwords backed up offline. If the
upload key is lost, it can be reset through Play Console support; the app-signing key is never
exposed. Submit only `app-release.aab` to Google Play.

## CI

`.github/workflows/android.yml`: JDK 17 + SDK Platform 36 → wrapper validation → unit tests +
`lintRelease` → decode keystore to `$RUNNER_TEMP` → `assembleRelease bundleRelease` →
`apksigner verify --print-certs` (fails on `CN=Android Debug` or a signer different from the
keystore) → `jarsigner -verify` + `keytool -printcert -jarfile` on the AAB (self-signed upload
certificates are accepted) → permission check → native-library / 16 KB check → upload artifacts →
remove signing material. No emulator test runs in CI.

## R8 and resource shrinking

Disabled in 1.0.0 on purpose: the spec requires first verifying a signed, non-minified release.
After that verification, enable `isMinifyEnabled`/`isShrinkResources`, repeat entry, calendar,
chart, goal and persistence checks, and keep `app/build/outputs/mapping/release/mapping.txt`
(CI uploads it when present).

## 16 KB page-size compatibility

Tidelio has no NDK code. CI lists `.so` files in both the APK and the AAB and records the result in
`verification.txt`; if any library ever appears it runs `zipalign -c -P 16` and checks every ELF
`LOAD` segment alignment ≥ 16 KB. API 36 targeting alone is not treated as proof.
See `docs/VERIFICATION.md` for the recorded result.

## Local install and logs

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
adb logcat --pid=$(adb shell pidof app.tidelio)
```

## Tests

Unit tests (`app/src/test`) cover period boundaries, entry validation, duplicate-tap prevention,
edit/delete/Undo, cumulative wave coordinates, identical timestamps, goal overflow, effective-dated
goal lookup, missing-goal behaviour, statistics with missing days, calendar month boundaries and
leap days, midnight rollover and time-zone changes, and DST input.
Instrumented Room tests (`app/src/androidTest`) cover persistence, goal replacement and reset.

Verification status, artifact size and pending manual checks: [`docs/VERIFICATION.md`](docs/VERIFICATION.md).
