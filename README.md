# Kotlin Dynamic Forms

A reusable Kotlin form engine and native Android renderer for JSON-defined forms.
The sample is a site-inspection app with conditional fields and offline drafts.

## What works

- Four field types: text, decimal number, single choice and explicit yes/no.
- Declarative required fields, text length constraints and numeric bounds.
- Conditional visibility with chained and forward dependencies; cycles are rejected.
- Reactive state through `StateFlow`, with a pure evaluator underneath.
- Hidden answers survive edits in a draft but are excluded from submission.
- Strict JSON decoding and separate schema-format and form-revision versions.
- A stateless Compose renderer with a field-rendering extension point.
- A sample Android app with explicit Room-backed draft save, restore and delete.
- Local validation and a selectable JSON payload preview.
- Core tests, ViewModel tests, Room device tests and a Compose interaction test.

**Status:** initial working implementation, not a production-certified SDK.
See [verification](docs/VERIFICATION.md) for checks actually run.

## Run

Prerequisites: JDK 17, Android SDK Platform 35 and Build Tools 35.0.0.
Open this directory in an Android Studio version that supports AGP 8.9.2.
Set `sdk.dir` in your own `local.properties`, or set `ANDROID_HOME`.

```bash
./gradlew :forms-core:test :demo:testDebugUnitTest :demo:assembleDebug
./gradlew :demo:installDebug
```

The generated debug APK is at
`demo/build/outputs/apk/debug/demo-debug.apk`.
The app runs on Android 8.0 / API 26 and later.

To test only the SDK on a machine without an Android SDK:

```bash
./gradlew -PcoreOnly :forms-core:test
```

On Windows, use `gradlew.bat` in place of `./gradlew`.

### Explore the demo

1. Enter a site and inspector name.
2. Select **Follow-up visit** to reveal a required reference field.
3. Select **Yes** for the hazard question to reveal its details field.
4. Enter details, then select **No**: the field disappears and its answer is
   excluded from the JSON preview. Switching back restores the local input.
5. Press **Save draft**, force-stop and reopen the app: the saved input returns.
6. Press **Review submission** to validate and inspect the payload locally.

There is no backend in this version. The app reads a JSON asset and requires no
network permission. It demonstrates a server-driven contract without pretending
to fetch from or submit to a running service. Drafts are saved explicitly, not
automatically; unsaved edits survive rotation through the ViewModel but do not
survive process death.

## Use the core

```kotlin
val definition = FormCodec().decode(json)
val controller = FormController(FormEngine(definition))
controller.setAnswer("hazard_found", "true")

when (val result = controller.submit()) {
    is SubmitResult.Accepted -> sendToYourApi(result.submission)
    is SubmitResult.Rejected -> showErrors(result.errors)
}
```

`sendToYourApi` and `showErrors` above represent callbacks supplied by the host.
The SDK has no transport, database or application-context dependency.

For a Compose host:

```kotlin
val state by controller.state.collectAsStateWithLifecycle()
DynamicForm(state = state, onAnswerChange = controller::setAnswer)
```

Place `DynamicForm` inside a scrollable host. Its optional `fieldContent` slot lets
you replace widgets while retaining the engine's visibility and validation rules.
Compose is one renderer; the core also works with XML/View-based Android UIs.

## Modules

| Module | Responsibility | Android dependency |
| --- | --- | --- |
| `forms-core` | Contract, schema validation, pure evaluation, controller and JSON codec | None; JVM 17 |
| `forms-compose` | Stateless native Material field rendering | Android + Compose |
| `demo` | Sample app, repository adapters, ViewModel, Room draft persistence | Android |

The sample ViewModel depends on `FormRepository` and `DraftRepository` interfaces.
Only the data layer sees Room and Android assets. Concrete dependencies are wired
in the application composition root using constructor injection.

See [architecture and trade-offs](docs/ARCHITECTURE.md),
[the JSON contract](docs/SCHEMA.md) and [the sample definition](demo/src/main/assets/site-inspection.json).

## Build and checks

```bash
# Local JVM unit tests + Android static analysis
./gradlew :forms-core:test :demo:testDebugUnitTest :demo:lintDebug :forms-compose:lintDebug

# Requires a running emulator or connected Android device
./gradlew :demo:connectedDebugAndroidTest :forms-compose:connectedDebugAndroidTest

# Publish libraries locally for another application; nothing is uploaded remotely
./gradlew :forms-core:publishToMavenLocal :forms-compose:publishToMavenLocal
```

The Gradle wrapper distribution is pinned and checksum-verified. Library versions
are centralized in `gradle/libs.versions.toml`. This baseline deliberately uses
AGP 8.9.2, Gradle 8.11.1, Kotlin 2.1.20 and SDK 35; it is not a claim that these are
the latest releases or that the demo meets current app-store submission rules.

GitHub Actions is configured for core tests plus Android build, unit tests and lint.
A configured workflow is not a claim that it has already passed on GitHub.

## Boundaries and next steps

- No remote schema delivery, authentication, telemetry or synchronization yet.
- No date, file, camera, repeat-group or multi-page field types yet.
- One exact-equality visibility condition per field; no embedded scripts.
- Defaults and runtime messages are English. Hosts can replace widgets and map error codes.
- Drafts use ordinary Room storage. App backups are disabled; database encryption is not implemented.
- New form revisions use separate drafts. Cross-revision migration requires an explicit host policy.
- Definition collection properties are Kotlin read-only interfaces; callers should not mutate their backing collections.

The useful next milestone is a real-device walkthrough, screenshots and a tagged
release after reviewing the public API. Future work is tracked in [ROADMAP.md](ROADMAP.md).

## License

MIT. See [LICENSE](LICENSE).
