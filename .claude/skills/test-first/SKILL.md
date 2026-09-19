---
name: test-first
description: Test-first workflow for new features and bug fixes, followed by an adversarial subagent review of the tests. Use whenever adding features, fixing bugs, or writing/changing tests.
---

# Test-First Development

Everything gets tested. New behaviour starts from a failing test, and finished tests are audited by an adversarial
subagent before the work is considered done.

## 1. Red — write the test first

- For a **new feature**: write the test expressing the desired behaviour *before* any production code. Run it and
  confirm it fails for the right reason (assertion failure or missing symbol, not a typo).
- For a **bug fix**: write a test reproducing the bug first; confirm it fails on current code.
- Tests use JUnit 5 + Strikt, live in the module's `src/test/kotlin`, and follow existing patterns:
    - `expectSuccess()` / `expectFailure()` for `Outcome` results (from kondor-tools fixtures)
    - `isEquivalentJson()` for JSON string comparison ignoring formatting
    - Round-trip pattern: value → `toJsonNode` → `fromJsonNode` → equal; and value → `toJson` string → `fromJson` →
      equal
- Cover both directions: happy path **and** failure path (malformed JSON, missing fields, wrong types — asserting on the
  error message/path, not just "it failed").

## 2. Green — minimal implementation

Write the simplest code that passes (following the `kotlin-functional-code` skill), then refactor with tests staying
green.

Run the relevant module first for speed, then the full suite:

```bash
./gradlew :kondor-core:test --tests "com.ubertob.kondor.json.SomeTest"
./gradlew test -x :kondor-mongo:test   # full suite; mongo needs TestContainers
```

Include `:kondor-mongo:test` only if the change touches kondor-mongo and a Docker daemon is available.

## 3. Adversarial test review (mandatory)

Once tests are green, launch an adversarial subagent to audit the tests. Use the Agent tool (`general-purpose`), with a
prompt of this shape:

> You are an adversarial test reviewer for the kondor-json Kotlin library. Review these test files: `<paths>`, which
> test this production code: `<paths>`. Your job is to find weaknesses, NOT to praise. Report concretely:
> 1. **Clarity** — can each test's intent be understood from its name and body alone? Flag tests whose names lie or
     whose assertions don't match the name.
> 2. **Exhaustiveness** — list specific missing cases: empty/blank strings, unicode and escaped characters (\n, \", \\,
     \uXXXX), empty collections, null/absent optional fields, boundary numbers (0, negatives, Long.MAX_VALUE,
     precision-losing doubles), malformed JSON, deeply nested structures, and error-path assertions that check the
     NodePath/message.
> 3. **Duplication** — tests that exercise the same behaviour twice, or that would all fail together for a single cause;
     suggest which to merge or delete.
> 4. **Weak assertions** — tests that pass trivially (e.g. only asserting no exception, or round-tripping a value that
     can't expose the bug).
     > Return a numbered list of findings with file:line references and a suggested fix for each. If a category is
     genuinely fine, say so in one line.

Then:

- Fix every finding you agree with; re-run the tests.
- For findings you reject, note why in one sentence when reporting back to the user.
- If the subagent found significant gaps, run one more review pass after fixing.

## Definition of done

Tests written first, all green (module + full suite minus mongo), adversarial review completed and findings addressed.
Only then move to committing (see `pre-commit-review` skill).
