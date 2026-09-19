---
name: pre-commit-review
description: Mandatory adversarial code-quality review before every commit — checks logic, tests, docs and changelog, then asks the user to approve the commit. Use before any git commit in this repository.
---

# Pre-Commit Review

No commit happens without this gate. A commit is a complete chunk of work: code + tests green + docs updated + changelog
entry when relevant.

## 1. Verify the chunk is complete

Run in order; stop and fix if anything fails:

```bash
./gradlew test -x :kondor-mongo:test      # all tests green (include mongo if touched + Docker available)
git status && git diff                     # review exactly what will be committed; no stray files
```

Check the non-code parts of the chunk:

- **Docs**: if behaviour or public API changed, is the relevant `docs/<module>.md` (and README.md for user-facing
  features) updated in this same diff?
- **Changelog**: if the change is user-visible (new API, fix, breaking change, performance), is there an entry in
  `CHANGELOG.md` under the current unreleased version? Internal refactors don't need one.

## 2. Adversarial quality review (mandatory)

Launch an adversarial subagent via the Agent tool (`general-purpose`) to review the full diff:

> You are an adversarial code reviewer for kondor-json, a functional Kotlin JSON library (no reflection, Outcome monad
> for errors, immutability, Java 8 runtime target). Run `git diff` (staged + unstaged) in the repo and review every
> change. Hunt for problems — do not summarise or compliment. Specifically:
> 1. **Logical gaps** — unhandled cases in `when` branches, off-by-one in parser/lexer positions, asymmetric
     serialize/deserialize (round-trip would fail), lost error context (NodePath dropped), silent failure swallowing.
> 2. **Functional-style violations** — exceptions for control flow, `orThrow()` outside tests, leaked mutability, `var`
     without justification, JDK-9+ APIs in main sources.
> 3. **API consistency** — naming vs existing converters (J-prefix, snake_case fields), missing KDoc on new public API,
     breaking changes not flagged.
> 4. **Hidden coupling / dead code** — changes that only work due to test ordering, leftover debug code, unused
     imports/functions.
> 5. **Test honesty** — production changes not covered by any test in this diff.
     > For every issue: file:line, why it's wrong, and a concrete suggested fix. Rank findings by severity. If the diff
     is genuinely clean in some category, one line saying so.

## 3. Resolve findings

- Apply fixes for all valid findings, re-run tests, and if fixes were non-trivial run the subagent review once more on
  the new diff.
- Findings you disagree with: keep a one-line justification to include in your report to the user.

## 4. Commit — with user approval

Only when tests are green and the review is clean:

1. Summarise to the user: what the chunk does, review findings and how they were resolved, docs/changelog status.
2. Propose the commit message (imperative summary line; body when the why isn't obvious).
3. Ask the user to confirm, then commit. **Never** push, tag, publish, or bump versions — those are always the user's
   manual actions.
