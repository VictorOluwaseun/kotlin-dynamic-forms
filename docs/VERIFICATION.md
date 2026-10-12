# Verification

Checked on 12 October 2026 with JDK 17, Gradle 8.11.1 and Android SDK 35.

| Check | Result |
| --- | --- |
| Core JVM tests | 23 passed; no failures or skipped tests |
| Demo ViewModel tests | 5 passed; no failures or skipped tests |
| Actual sample JSON contract test | 1 passed |
| Standalone core configuration (`-PcoreOnly`) | Successful |
| Demo debug APK | Built successfully |
| Compose release AAR | Built successfully |
| Demo and Compose Android test APKs | Built successfully |
| Demo and Compose Android lint | No issues found |

The device test sources cover Room draft persistence and Compose conditional-field
interaction. They were compiled and packaged, **not executed**: no Android device
or emulator was available. The app has not yet had a visual, accessibility or
real-device lifecycle walkthrough. A successful build does not replace those checks.

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
```

Dependencies were cached before the final build; the final commands also used
`--offline`. After the demo icon and backup configuration were added, its APK,
Android test APK and lint were checked again successfully.
