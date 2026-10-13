# Verification

Rechecked on 12 October 2026 after selecting JDK 17.0.14 for Gradle 8.11.1,
with Android SDK 35 for compilation and an API 36 emulator for device tests.

| Check | Result |
| --- | --- |
| Core JVM tests | 23 passed; no failures or skipped tests |
| Demo ViewModel tests | 5 passed; no failures or skipped tests |
| Actual sample JSON contract test | 1 passed |
| Standalone core configuration (`-PcoreOnly`) | Successful |
| Demo debug APK | Built successfully |
| Compose release AAR | Built successfully |
| Demo and Compose Android test APKs | Built successfully |
| Room draft persistence device test | 1 passed on the API 36 emulator; also passed on a Samsung SM-A556E running Android 16 |
| Compose conditional-field interaction device test | 1 passed on the API 36 emulator |
| Demo Android lint | No errors; 46 warnings about pinned dependencies, AGP and target SDK |
| Compose Android lint | No issues found |

The device tests executed Room draft persistence and Compose conditional-field
interaction. The Compose test APK now explicitly targets API 35; its default API 26
target triggered a compatibility dialog on Android 16 and obstructed the UI test.
Both device tests passed on an API 36 emulator after that configuration change.
The app has not yet had a manual visual, accessibility or lifecycle walkthrough.
A successful automated run does not replace those checks.

This record covers local checks. See the repository's [GitHub Actions runs](https://github.com/VictorOluwaseun/kotlin-dynamic-forms/actions)
for CI results. Maven publication is configured but has not been exercised or uploaded.

The download bundle contains `distribution/demo-debug.apk` for a local walkthrough.
This is a debug build, not an app-store release. `distribution/` is gitignored;
source and the workflow are sufficient to rebuild it.

## Commands checked

```bash
./gradlew :forms-core:test :demo:testDebugUnitTest :demo:assembleDebug \
  :demo:assembleDebugAndroidTest :forms-compose:assembleDebugAndroidTest \
  :demo:lintDebug :forms-compose:lintDebug :forms-compose:bundleReleaseAar
./gradlew -PcoreOnly :forms-core:test
./gradlew :demo:connectedDebugAndroidTest :forms-compose:connectedDebugAndroidTest \
  --no-parallel
```

All commands used JDK 17.0.14. Dependencies were downloaded for the initial build
and device run. The standalone core check and final emulator run used `--offline`;
the emulator run also reran Compose lint. `ANDROID_SERIAL` selected the test device.
