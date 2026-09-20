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

## 2. Check the Android compatibility

The library has no Android module and no Android build, so this is unverified territory.

- The runtime target is Java 8 (`common-kotlin.gradle.kts`), but kondor-core uses `java.time` (`Instant`, `LocalDate`,
  `LocalDateTime`) in about 17 places: on Android below API 26 it needs core library desugaring. Document the minimum
  API level, or the desugaring the user has to enable.
- Check the size and the method count of the artifacts, and whether anything pulls in a dependency Android dislikes.
- Decide whether kondor-auto is usable at all on Android (see below) and say so in the README.

## 3. Check the reflection in kondor-auto

- kondor-auto uses `kotlin-reflect` following the Kotlin version (2.1.0 today). Verify it is really needed at runtime,
  and what it costs in size and startup: the module description claims "reflection only at build time", which is not
  what `JDataClassAuto`/`JDataClassWithNames` do.
- `JDataClass` matches properties to constructor parameters *by position* and `JDataClassWithNames` *by name*: check
  they behave the same on the cases that differ (default values, nullable parameters, fields declared out of order).
- `JDataClassWithNames` has few tests: the 4.1.0 work added the first ones for default values and unknown fields.

## 4. Check the number parsing, the lexer and the lazy tokenizer for errors

4.1.0 fixed several of these, and the reviews found more that are still open.

- **Kondor cannot read back its own output for non-finite numbers on the `JAny` path**: `JDoubleRepresentable.tryNanNode`
  accepts `"NaN"`, `"+Infinity"` and `"-Infinity"` but not `"Infinity"`, which is exactly what it writes.
  `JFloatRepresentable` has no `tryNanNode` at all, so all three non-finite floats fail through `JAny`, while `JObj`
  reads them. Move `tryNanNode` into `JNumRepresentable`.
- **Deeply nested Json overflows the stack**: about 1000 levels of `[` throw a `StackOverflowError` out of
  `parseJsonNode`, which returns an `Outcome`. Needs a depth limit.
- **Leniency the lexers still allow**, both paths agreeing: trailing commas (`[1,]`, and `{"id":1,}` for `JAny` but not
  `JObj`), text after the end of a document in `parseJsonNode` (`{} x`), raw control characters inside strings, and a
  `\u` escape cut short before the end of the input. Decide which of them to reject.
- **Bare `NaN` and `Infinity` without quotes** are accepted as valid Json numbers.
- **An `IOException` while reading a stream** is thrown out of `fromJson` instead of being returned as a `Failure`.
- `JsonStyle.kt` has a dead `Regex` and a `CharRange` written with raw control characters in the source.
- The positions in the parsing errors are the last position read, not the position of the token: they can point a
  character or two before the problem.

## 5. Move almost all the tests to JObj

`JObj` is the recommended converter, but most fixtures in `DomainExampleForTest.kt` and most tests still use `JAny`.

- Convert the fixtures and the tests to `JObj`, **adding** the `JObj` variant instead of replacing the `JAny` one
  wherever the test is about a behaviour the two share: both must keep working, and a test that silently swaps
  converter hides a difference (this is how the `flatten` and unknown-fields bugs stayed hidden until 4.1.0).
- Keep a small, explicit set of `JAny` tests for what only `JAny` does: `JSealed`, `JJsonNode`, and the `JsonNode` path
  in general.
- Check `MappingAndJsonNodeConsistencyTest` covers the two paths for every converter kind, so that a difference
  between them fails a test instead of being found by a review.
