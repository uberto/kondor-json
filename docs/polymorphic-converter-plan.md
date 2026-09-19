# Polymorphic Converter Refactor Plan

## Background

- `PolymorphicConverter<T>` currently inherits from `JAny<T>`, which forces an intermediate `JsonNodeObject` when
  deserializing.
- `JAny<T>` exists mainly for backwards compatibility; its intermediate node allocation is slower than the direct
  token-to-object pipeline provided by `ObjectNodeConverterProperties<T>`.
- `JSealed<T>` builds on `PolymorphicConverter<T>` to handle discriminator-based sealed hierarchies. Today it depends on
  the `JsonNodeObject` pass to read **all** fields before selecting a subtype.
- We want to refactor the polymorphic stack so that sealed converters can reuse the faster infrastructure, provided we
  can guarantee the discriminator is encountered before other fields.

## Goals

- Make `PolymorphicConverter<T>` inherit directly from `ObjectNodeConverterProperties<T>`.
- Keep the current `JSealed` API working while introducing a variant that can deserialize without an intermediate
  `JsonNode`.
- Enforce that the discriminator field is emitted and parsed before other fields when using the new variant.
- Preserve backwards compatibility and test coverage during the transition.

## Non-Goals

- Changing the public surface of `ObjectNodeConverterProperties` or `JsonProperty`.
- Rewriting existing sealed converters outside of targeted test updates.
- Removing the `JAny` implementation entirely; it still has other callers.

## Proposed Approach

### 1. Extract Shared Polymorphic Utilities

- Identify the common helpers in `PolymorphicConverter` used by both read and write paths (`extractTypeName`,
  `subConverters`, `findSubTypeConverter`, `appendTypeName`, etc.).
- Ensure these helpers do not rely on `JsonNodeObject` so they can survive the base-class change.

### 2. Convert `PolymorphicConverter` to Use `ObjectNodeConverterProperties`

- Change the superclass and reimplement `fromFieldNodeMap` using `FieldNodeMap` directly.
    - Reuse the logic currently living in `JSealed.deserializeOrThrow`, but operate on raw fields instead of a wrapped
      `JsonNodeObject`.
    - Maintain support for `defaultConverter`.
- Audit the writer path to make sure the discriminator stays injected into the generated `FieldNodeMap`.
- Run/extend unit tests that cover polymorphic serialization/deserialization to confirm parity.

### 3. Introduce a Fast-Path `JSealed` Variant

- Define a new sealed converter (working name: `JSealedFirst<T>` or similar) built on the refactored
  `PolymorphicConverter`.
- Implement token parsing that immediately reads the discriminator (first field) and dispatches to the matching subtype
  converter without materializing a `JsonNodeObject`.
    - Validate that writers emit the discriminator first; fail fast if a subtype would register a conflicting ordering.
- Provide API guidance (KDoc + README snippet) describing when to use classic `JSealed` vs the new variant.

### 4. Backwards Compatibility Layer

- Keep the current `JSealed` class functional by delegating to the refactored core while still supporting the previous
  discriminator-anywhere behavior.
- Add migration notes in `MigrationToV4.md` and/or CHANGELOG around the new variant and discriminator-order requirement.

### 5. Testing & Tooling

- Update existing tests in `kondor-core` and `kondor-mongo` to cover both variants:
    - Happy path round-trips.
    - Missing/unknown discriminator.
    - Default converter usage.
- Add new tests that ensure the discriminator-order check fires when the invariant is broken.
- Consider benchmarking (if existing harnesses allow) to validate the performance win, or at least document expected
  improvements.

## Risks & Open Questions

- **Token ordering guarantee**: do current writers always emit properties in registration order? If not, we may need
  extra safeguards.
- **Subtype converters**: some might still depend on `JsonNodeObject` internally; we need to confirm they can operate
  directly on field maps/tokens.
- **Naming**: we must settle on a clear name for the new sealed converter to avoid confusion (`JSealedFirst`,
  `JSealedStrict`, etc.).
- **Default converter semantics**: verify whether the fast-path should still support falling back when the discriminator
  is missing.
- **Binary compatibility**: changing the superclass may impact consumers compiling against the old bytecode; ensure this
  is acceptable or provide migration guidance.

## Next Steps

- Validate the feasibility of parsing the discriminator first without a `JsonNodeObject`.
- Decide on the naming/API surface for the new converter variant.
- Schedule the implementation in two PRs: (1) refactor shared infrastructure, (2) add the new sealed converter and
  docs/tests.
