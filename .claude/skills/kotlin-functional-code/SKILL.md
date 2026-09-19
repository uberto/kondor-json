---
name: kotlin-functional-code
description: Kotlin functional programming guidelines for writing or modifying code in kondor-json. Use whenever generating, refactoring, or reviewing Kotlin code in this repository.
---

# Kotlin Functional Code Guidelines

Follow the functional style already established in this codebase. When in doubt, find a similar existing
function/converter and imitate it — consistency beats personal preference.

## Error handling: Outcome, never exceptions

- All fallible operations return `Outcome<E, T>` (from kondor-outcome), never throw for control flow.
- Chain with `transform` (map), `bind` (flatMap), `transformFailure`, `recover`. Combine independent outcomes with the
  applicative operators `` `!` `` and `` `*` `` or `Outcome.transform2`.
- `orThrow()` is acceptable **only in tests**. Production code handles failures explicitly.
- Wrap genuinely exceptional third-party calls at the boundary (e.g. `Outcome.tryOrFail { ... }`) and convert to a typed
  `OutcomeError` immediately.
- Errors carry context: JSON errors must include the `NodePath` so users can locate the problem.

## Immutability and types

- `val` everywhere; no `var` unless in a tight, local, performance-critical loop (parser/lexer internals only, with a
  comment explaining why).
- Data classes for values, sealed interfaces/classes for sum types, exhaustive `when` without `else` branch so the
  compiler catches new cases.
- No mutable collections in public APIs. Internal mutable builders are fine when hidden behind a pure function.
- Nullability is a domain decision: use `T?` for genuinely optional data, `Outcome` for operations that can fail.

## Style

- Prefer expression bodies (`fun f(x: X): Y = ...`) over block bodies with `return`.
- Small pure functions composed together; extension functions for adapting types you don't own.
- Generic type parameters follow existing conventions: `T` domain type, `JN` JsonNode type, `E` error type.
- No reflection, no annotations, no code generation — everything explicit and compile-time safe (kondor-auto is the
  single sanctioned exception and uses reflection only at test time).
- Public API gets KDoc (`@param`, `@return`) matching the density of `Outcome.kt`.
- Runtime target is Java 8: do not use JDK APIs newer than 8 in main source sets, even though the toolchain is Java 21.

## Converter conventions

- Converter objects are prefixed with `J` (`JProduct` for `Product`) and live near their domain class.
- Fields via `by` delegation DSL: `val file_name by str(Product::name)` — the val name is the JSON field name (
  snake_case).
- Extend the right base: `JObj` for plain objects (faster, direct token path), `JAny` when you need the JsonNode
  intermediate (e.g. under `JSealed`), `JSealed` for polymorphism with discriminator.
- Every new converter must round-trip: value → JSON → value and value → JsonNode → value.

## Checklist before considering code done

1. No exceptions used for control flow; no `orThrow()` outside tests.
2. No `var`/mutable state leaking out of function scope.
3. `when` over sealed types is exhaustive without `else`.
4. Compiles under Java 8 API surface.
5. Reads like the surrounding code (naming, KDoc, expression style).
