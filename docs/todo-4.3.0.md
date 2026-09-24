# ToDo for 4.3.0

Notes taken while working on 4.1.0, to be checked and refined before starting.

## 1. Make the kondor-mongo tests pass here

They need Docker and were never run on this machine, so the module is the least verified part of 4.1.0.

- Run `./gradlew :kondor-mongo:test` with Docker available, using the `mongo:8.0.32` image and the MongoDB driver
  5.5.1 (updated but not yet verified against a running server).
- **A `JObj` converter cannot read back a document written by a `TypedTable`**: `KondorBson.kt` writes every number as
  a BSON double, so an `Int` field is stored as `7.0`. A `JAny` reads it back, a `JObj` fails. Decide between writing
  whole numbers as `Int32`/`Int64` (which also fixes the precision loss below) and making `JObj` accept whole-number
  doubles. `docs/kondor-mongo.md` documents the limitation and must be updated with the fix.
- **A `Long` above 2^53 loses precision** when stored, for the same reason.
- `TypedTableConversionTest` covers the `_id` field without a database: worth extending the same way for the numbers,
  so that part keeps running without Docker.

## 2. Android

Done in 4.2.0, for Android 13 (API 33) and later, which is what matters. kondor-core and kondor-outcome need no change:
no reflection library, and only the Java API Android 13 has (`java.time` since API 26). `./gradlew check` runs
Animal Sniffer against `gradle/android-api-33.signature`, built from the Android 13 SDK platform by
`scripts/generate-android-signature.sh`, so the build fails if they call anything Android 13 lacks.

- No `kondor-datetime` split: `java.time` is there on Android 13.
- Still open: running the tests on an Android device or emulator. The check proves that every class and method exists,
  not that they behave the same (e.g. the `BigDecimal` error texts and the locale data of `DateTimeFormatter`).

## 3. Check the reflection in kondor-auto

- kondor-auto uses `kotlin-reflect` following the Kotlin version (2.1.0 today). Verify it is really needed at runtime,
  and what it costs in size and startup: the module description claims "reflection only at build time", which is not
  what `JDataClassAuto`/`JDataClassWithNames` do.
- `JDataClass` matches properties to constructor parameters *by position* and `JDataClassWithNames` *by name*: check
  they behave the same on the cases that differ (default values, nullable parameters, fields declared out of order).
- `JDataClassWithNames` has few tests: the 4.1.0 work added the first ones for default values and unknown fields.

## 4. Check the number parsing, the lexer and the lazy tokenizer for errors

4.1.0 fixed several of these, and the reviews found more that are still open.

- ~~Kondor cannot read back its own output for non-finite numbers on the `JAny` path.~~ Done: `JDouble` and `JFloat`
  share `fromNumberOrNonFinite`, which reads the same texts the token path accepts (`NonFiniteNumbersTest`).
- ~~A quoted finite number is read by the token path and refused by the `JsonNode` path.~~ Done: both refuse it, only
  `NaN` and `Infinity` are read from text (`QuotedNumbersTest`).
- ~~`toJsonNode` writes a non finite number as a bare `NaN`.~~ Done: a `JsonNode` renders it as text as well
  (`NonFiniteRenderingTest`). Reading it back gives a `JsonNodeString`, so a `JsonNode` holding a non finite number
  does not survive a render and parse as a `JsonNodeNumber`.
- ~~`-0.0` does not round trip.~~ Done: a zero written with a minus is read as a `NegativeZero`, which keeps the sign
  together with the parsed number (`NegativeZeroTest`). Reading it as a plain `Double` was tried first and reverted:
  it loses the number, so `JBigInteger` failed on `-0` from a node, `JBigDecimal` lost the scale of `-0.000`, and
  `render()` rewrote `-0` as `-0.0`.
- **`JsonNodeDsl` and kondor-jackson drop the sign of a zero**: `"x" toNode -0.0` builds a `BigDecimal`, and
  `NumericNode.toKondorJsonNode()` uses `decimalValue()`, so both give `0.0` while the converters keep `-0.0`.
  Jackson cannot represent it at all, so that direction can only be documented; the DSL could keep it, but building a
  node from a `Double` would then hold a `Double` instead of a `BigDecimal` for every value.
- ~~kondor-jackson writes a non finite number bare.~~ Wrong: Jackson (2.18) writes `"NaN"` quoted by default, as
  kondor does, and refuses a bare `NaN` unless `ALLOW_NON_NUMERIC_NUMBERS` is on. A `JsonStyle` option to write them
  bare was built in 4.2.0 and dropped: no common consumer needs it, and adding a property to `JsonStyle` breaks
  `copy` for code compiled against an older version.
- **`NumericNode.toKondorJsonNode()` throws on a Jackson `NaN` node**: it calls `decimalValue()`, which fails with a
  `NumberFormatException` for a non finite double.
- **The schema says `{"type":"number"}` for `JDouble` and `JFloat`, but a non finite value is written as a string, so
  kondor's own output does not validate against its own schema.
- ~~Deeply nested Json overflows the stack.~~ Done for the entry points parsing a `String` or a stream, which report
  an `InvalidJsonError` (`DeeplyNestedJsonTest`). Still open, all only reachable with a `JsonNode` built by hand,
  since a parsed one cannot be deep enough: rendering one (`render`, `toJson`), converting one (`fromJsonNode`) and
  `fromTokens`, which is the recursive part and cannot be guarded without a cost at every level.
- **`InvalidJsonError` now covers two different things**: a malformed Json and a valid one kondor cannot parse. A
  separate error type would let callers tell them apart, but `JsonError` is sealed, so it breaks an exhaustive `when`.
- **Leniency the lexers still allow**, both paths agreeing: trailing commas (`[1,]`, and `{"id":1,}` for `JAny` but not
  `JObj`), text after the end of a document in `parseJsonNode` (`{} x`), raw control characters inside strings, and a
  `\u` escape cut short before the end of the input. Decide which of them to reject.
- **Bare `NaN` and `Infinity` without quotes** are accepted as valid Json numbers. Kept on purpose: Python's
  `json.dumps` writes them that way by default, and reading them costs nothing.
- **An `IOException` while reading a stream** is thrown out of `fromJson` instead of being returned as a `Failure`.
- ~~`JsonStyle.kt` has a dead `Regex` and a `CharRange` written with raw control characters.~~ Done in 4.2.0.
- The positions in the parsing errors are the last position read, not the position of the token: they can point a
  character or two before the problem.

## 5. Move almost all the tests to JObj

Done in 4.2.0. The general tests use `JObj` fixtures; `JAnyTest` covers `JAny` end to end, including what only a
`JAny` can do (reading a field it does not declare from its `JsonNode`, the missing-node error, a `null` from
`deserializeOrThrow`). The tests comparing the two converters keep a real `JAny`, with fixtures named for it
(`JSelectedFileAny`, `JTaskAny`, `JOptionalAddressAny`, ...), and `MappingAndJsonNodeConsistencyTest` checks both kinds.
The kondor-core coverage (JaCoCo) was unchanged by the move, apart from one more `JAny` branch.

Still on `JAny`, on purpose:
- the kondor-mongo fixtures: a `JObj` cannot read back a document a `TypedTable` wrote (see item 1);
- kondor-tools: `ConverterGenerator` generates a `JAny`, so its tests expect one. Generating a `JObj` instead is a
  change of the tool, still to decide;
- `JSealed` subtypes in the shared fixtures (`JVariantString`, `JVariantInt`).
