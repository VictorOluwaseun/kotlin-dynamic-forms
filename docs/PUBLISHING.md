# Repository and releases

Repository: [VictorOluwaseun/kotlin-dynamic-forms](https://github.com/VictorOluwaseun/kotlin-dynamic-forms)

Suggested description:

> Reusable Kotlin SDK for JSON-driven native Android forms, with conditional fields, validation, a Compose renderer and offline drafts.

Suggested topics: `kotlin`, `android`, `sdk`, `server-driven-ui`, `jetpack-compose`,
`room`, `coroutines`, `forms`.

## Before a release

Run the demo locally and review the implementation so you can explain the API,
validation behavior and persistence decisions. Keep the verification record
accurate. Add real screenshots after running the app.

## Work from a clone

Clone the repository and run the local checks:

```bash
git clone https://github.com/VictorOluwaseun/kotlin-dynamic-forms.git
cd kotlin-dynamic-forms
./gradlew :forms-core:test :demo:testDebugUnitTest :demo:assembleDebug
```

Install the debug APK on a device or emulator for the walkthrough in the README.
Before tagging a release, run the connected tests and add screenshots from the
actual app. The project is not published to a remote Maven repository yet.

The `.gitignore` excludes local SDK paths, build outputs and IDE metadata. The
Gradle wrapper JAR is intentionally tracked so a fresh clone can build the project.
