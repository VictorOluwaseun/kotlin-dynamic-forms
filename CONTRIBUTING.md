# Contributing

Changes should preserve the separation between the pure core, renderer and host
application. Keep database and other data-source imports in repository adapters.

- Prefer guard clauses and explicit state transitions over nested `if` statements.
- Document public contracts and explain methods with non-obvious semantics.
- Test observable behavior, especially visibility, validation and persistence boundaries.
- Changing the wire contract requires a schema-version decision and documentation.
- New field types need core tests and renderer coverage.
- Do not add employer code, real identity records or production customer data.

Before opening a pull request, run:

```bash
./gradlew :forms-core:test :demo:testDebugUnitTest :demo:lintDebug :forms-compose:lintDebug :demo:assembleDebug
```

For UI or Room changes, also run the connected device tests documented in README.
