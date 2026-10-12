# Architecture and decisions

## Data flow

The host loads a definition through `FormRepository`. `FormCodec` decodes it
strictly and `SchemaValidator` checks the field contract and dependency graph.
`FormEngine` calculates visibility and validation. `FormController` exposes the
result as a `StateFlow`. `DynamicForm` renders it and sends edits back to the host.

The demo coordinates state in `FormViewModel`. It persists snapshots only through
`DraftRepository`; `RoomDraftRepository` owns serialization and DAO access.
Neither the core engine nor the ViewModel imports a database.

## Why a JVM core

The reusable logic contains no Android context, Compose types or Room annotations.
It is independently testable and can also be consumed by JVM services or custom
Android View renderers. Kotlin Multiplatform is not claimed: decimal validation
currently uses JVM `BigDecimal`. Moving it to common code would require an
explicit decimal representation and platform support policy.

## Visibility before validation

Schema validation rejects duplicate IDs, unknown references, self references and
cycles. A nonrecursive topological pass orders fields for visibility evaluation;
rendering still follows the original schema order. A condition matches only when
its parent is itself visible, so a hidden ancestor cannot reveal descendants
through stale draft values.

Hidden answers are retained locally to make toggling conditions reversible. They
are never included in a valid submission and never produce blocking errors while
hidden. The submission boundary revalidates the current snapshot.

## Raw inputs and normalized contracts

Answers are strings, allowing incomplete input such as `-` while a person is
editing a number. Validation decides when it is suitable for submission. Number
validation supports bounded plain decimal notation and excludes exponents,
Infinity and NaN. Boolean values must be exactly `true` or `false`; missing input
is distinct from false. Choice payloads use stable option values, not labels.

Whitespace-only answers count as absent. Text length is checked after trimming,
but submitted text preserves the user's original input. Numeric and choice
values are matched exactly; no silent coercion occurs.

## Persistence and concurrency

An explicit Save action captures one immutable snapshot. The UI blocks editing,
save, clear and review while the disk operation is running. Save success is shown
only after Room completes. On a storage failure the input remains visible and
the user can retry. Clearing removes the database row before resetting the UI.

The ViewModel is called on the UI thread. The controller uses atomic StateFlow
updates; its submission evaluates a captured immutable answer map. Consumers
needing a larger edit/submit transaction should serialize their host actions.

Draft keys contain both form ID and revision. Changed schemas do not silently
reuse old answers. Old revision records remain until a host implements retention
or migration. No destructive database migration fallback is enabled.

## Extension points

- Implement `FormRepository` for authenticated remote delivery and caching.
- Implement `DraftRepository` for a different persistence mechanism.
- Supply `DynamicForm.fieldContent` for custom native widgets.
- Consume error codes rather than English messages when localizing a host.
- Consume `FormSubmission` with the host's own transport and retry policy.

Adding a genuinely new field type requires changing the versioned SDK contract,
its validator and a renderer. Arbitrary remote widget execution is not supported.

## Known scope

The initial renderer targets compact forms, capped at 200 fields by the schema
validator. It uses a Column rather than claiming a virtualized large-form engine.
The demo supplies scrolling and keyboard insets. Accessibility uses native input
labels and selectable radio groups; TalkBack and device-level accessibility
verification remain part of the real-device review.

## Build references

- [AGP 8.9 compatibility](https://developer.android.com/build/releases/agp-8-9-0-release-notes)
- [Compose compiler plugin](https://kotlinlang.org/docs/compose-compiler-migration-guide.html)
- [Room releases](https://developer.android.com/jetpack/androidx/releases/room)
- [KSP 2.1.20-1.0.32](https://github.com/google/ksp/releases/tag/2.1.20-1.0.32)
