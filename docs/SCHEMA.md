# Form contract v1

`schemaVersion` is the SDK wire-format version and currently must be `1`.
`revision` is a positive host-managed integer identifying a particular form
definition. Increment it when fields or validation rules change.

```json
{
  "schemaVersion": 1,
  "id": "hazard-report",
  "revision": 1,
  "title": "Hazard report",
  "fields": [
    {"id": "found", "label": "Hazard found?", "type": "toggle", "required": true},
    {
      "id": "details", "label": "Details", "type": "text", "required": true,
      "minLength": 10,
      "visibleWhen": {"fieldId": "found", "equals": "true"}
    }
  ]
}
```

| Property | Meaning |
| --- | --- |
| `id` | Stable unique field key: starts with a letter, followed by letters, digits or underscores; at most 64 characters |
| `label` | Nonblank visible label |
| `type` | `text`, `number`, `choice` or `toggle` |
| `required` | Whether visible fields require an explicit nonblank answer; defaults to false |
| `help`, `placeholder` | Optional display text |
| `minLength`, `maxLength` | Text-only length constraints; trimmed UTF-16 length; maximum 4096 |
| `minValue`, `maxValue` | Number-only finite inclusive bounds |
| `options` | Nonempty unique `{value, label}` entries, only for choice fields |
| `visibleWhen` | Optional equality rule against another visible field |

Definitions allow 1–200 fields. Unknown JSON properties, unsupported field types,
invalid constraints and invalid dependency graphs cause a load failure rather
than quietly rendering a different form. Definition input is capped at 1,000,000
characters; raw field values at 4096 characters during validation.

`visibleWhen.equals` compares the parent's exact raw answer. Toggle conditions use
`true` or `false`; choice conditions must reference a declared option value.
Forward references are allowed. Self references and cycles are rejected.

Numbers allow an optional minus sign, 1–32 integer digits and optionally 1–16
fractional digits. Use `0.5`, not `.5`; commas and exponent notation are not
accepted. The JSON answer remains a string, even after numeric validation.

`FormEngine.submit()` returns either field errors or a payload shaped like:

```json
{"formId":"hazard-report","revision":1,"answers":{"found":"false"}}
```

Error codes: `required`, `too_short`, `too_long`, `number`, `minimum`, `maximum`,
`option`, `boolean`. Blank optional and all hidden answers are omitted from a
submission. Raw drafts retain both for editing.
