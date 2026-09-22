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

## 2. An Android version, ideally the vanilla one

The goal is a version usable on Android: no reflection, and nothing from a package Android does not have. The good
news is that kondor-core is almost there already.

- **Reflection is not in the core**: only kondor-auto depends on `kotlin-reflect` (`JDataClass`, `JDataClassAuto`,
  `JDataClassWithNames`). kondor-core and kondor-outcome use none, and the converters are explicit by design.
- **The only problematic package is `java.time`**, and it is confined to `json/datetime/JDateTime.kt` and
  `json/datetime/ShortFunctions.kt` (`Instant`, `LocalDate`, `LocalDateTime`, `LocalTime`, `DateTimeFormatter`). On
  Android it needs API 26, or core library desugaring on anything older.
- Everything else core uses is safe on any Android version: `java.io` streams, `java.math.BigDecimal`/`BigInteger`,
  `java.util` (`UUID`, `Currency`), `java.nio.charset.Charset` and `AtomicReference`.
- So the decision is what to do with the datetime converters:
  1. leave them in core and document the desugaring (no new artifact, simplest);
  2. move them to a `kondor-datetime` module, leaving a core with no `java.time` at all — this is the "vanilla"
     version, and it is a breaking change for whoever imports `com.ubertob.kondor.json.datetime`;
  3. publish a separate Android artifact, which is the most work and probably not needed if 2 is done.
  Option 2 looks the best fit for the goal, with the datetime module still published for everyone else.
- Whatever is chosen: check the artifacts have no `kotlin-reflect` on the compile path, verify on a real Android
  project (a small app parsing some Json), and state the minimum API level in the README.

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
- **A quoted finite number is read by the token path and refused by the `JsonNode` path**, for every number converter:
  `JDouble.fromJson("\"1.5\"")` succeeds, the same value in a `JsonNode` fails. Kondor never writes those, so it does
  not break a round trip, but the two paths should agree.
- ~~`toJsonNode` writes a non finite number as a bare `NaN`.~~ Done: a `JsonNode` renders it as text as well
  (`NonFiniteRenderingTest`). Reading it back gives a `JsonNodeString`, so a `JsonNode` holding a non finite number
  does not survive a render and parse as a `JsonNodeNumber`.
- **`-0.0` does not round trip**: it is read back as `0.0`, on both paths, since `BigDecimal` has no signed zero
  (a side effect of reading numbers as `BigDecimal` in 4.1.0). It matters only for `equals`: `-0.0 == 0.0` is true for
  two `Double`, but `(-0.0).equals(0.0)` is false, so a data class holding one no longer equals itself after a round
  trip. **Tried and reverted in 4.2.0**: reading such a zero as a `Double` keeps the sign, but a `JsonNodeNumber` then
  holds a `Double` instead of a `BigDecimal`, which loses the text: `JBigInteger` fails on `-0` from a `JsonNode`
  (`NumberFormatException: For input string: ".0"`), `JBigDecimal` loses the scale of `-0.000`, and `render()`
  rewrites `-0` and `-0e10` as `-0.0`. A fix has to keep the text of the number: a `Number` subclass holding it, with
  `toDouble()` returning `-0.0`, would work, at the price of a third kind of number in the nodes.
- **kondor-jackson writes a non finite number bare**: `toJacksonJsonNode(value).toString()` gives `NaN` while
  `toJson(value)` gives `"NaN"`, unless Jackson's `QUOTE_NON_NUMERIC_NUMBERS` is on. A `JsonStyle` flag to write them
  bare (as Jackson has) was considered and left out, to keep `JsonStyle` small.
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
