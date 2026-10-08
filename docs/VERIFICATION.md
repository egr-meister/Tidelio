# Verification notes — Tidelio 1.0.0

Checks that have not actually been performed are marked **PENDING**. Nothing below is reported as
passed unless it was run.

## Automated

| Check | Status | Where |
|---|---|---|
| Domain unit tests (51 tests, JVM) | PASSED locally (Kotlin 2.0.21 compiler, JUnit 4.13.2) before the first push | `app/src/test` |
| Unit tests in Gradle (`testDebugUnitTest`) | PENDING — first CI run | GitHub Actions |
| Release lint (`lintRelease`) | PENDING — first CI run | GitHub Actions |
| Signed release APK + AAB | PENDING — first CI run | GitHub Actions |
| `apksigner verify --print-certs`, no `CN=Android Debug`, signer = keystore | PENDING — first CI run | `verification.txt` artifact |
| AAB `jarsigner -verify` + signer = keystore | PENDING — first CI run | `verification.txt` artifact |
| Packaged permissions (none besides AndroidX internal) | PENDING — first CI run | `verification.txt` artifact |
| Native libraries / 16 KB | PENDING — first CI run (no NDK code; transitive check runs in CI) | `verification.txt` artifact |
| Artifact sizes | PENDING — first CI run | `verification.txt` artifact |
| Instrumented Room tests | PENDING — needs a device/emulator (`connectedDebugAndroidTest`) | `app/src/androidTest` |
| R8 / resource shrinking | NOT ENABLED (by design until the non-minified release is verified on a device) | — |

## Manual device checks (adb install + logcat)

Device / emulator: PENDING · Android version: PENDING

| Check | Status |
|---|---|
| First launch in airplane mode | PENDING |
| Logging before and after goal setup | PENDING |
| Quick and custom entries, duplicate taps | PENDING |
| Past-date editing, moving entries between dates | PENDING |
| Period filtering | PENDING |
| Wave progress and overflow | PENDING |
| Historical goals (today / tomorrow) | PENDING |
| Calendar and statistics | PENDING |
| Process recreation (draft kept) | PENDING |
| Rotation, large fonts, keyboard insets | PENDING |
| TalkBack descriptions | PENDING |
| Back behaviour (predictive back) | PENDING |
| Data reset returns to setup | PENDING |
| No permission prompts, crashes or network use | PENDING |
