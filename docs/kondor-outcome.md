# kondor-outcome Module

## Purpose

The `kondor-outcome` module provides `Outcome<E, T>`, a small Either type for error handling without exceptions. Every
Kondor operation that can fail (parsing, conversion, database access) returns an `Outcome`. The module has no
dependencies and can be used on its own, also on Android 13 (API 33) and later: `./gradlew check` verifies it against
the Java API of Android 13, as for kondor-core.

## Key Components

All in `com.ubertob.kondor.outcome`.

| Component                       | Role                                                                                |
|---------------------------------|-------------------------------------------------------------------------------------|
| `Outcome<E : OutcomeError, T>`  | Sealed interface: either `Success<T>` (with `value`) or `Failure<E>` (with `error`) |
| `OutcomeError`                  | Interface for the errors, with a `msg: String`                                      |
| `MessageError`                  | A simple `OutcomeError` with just a message (`"text".asFailure()`)                  |
| `ThrowableError`                | Wraps an exception, produced by `Outcome.tryOrFail { ... }`                         |
| `OutcomeException`              | The exception thrown by `orThrow()`, carrying the `OutcomeError`                    |
| `UnitOutcome`, `BaseOutcome<T>` | Aliases for `Outcome<OutcomeError, Unit>` and `Outcome<OutcomeError, T>`            |

Kondor's Json errors (`JsonError` and its subclasses) are defined in `kondor-core`, and `JsonOutcome<T>` is an alias for
`Outcome<JsonError, T>`. MongoDB errors (`MongoError`) are in `kondor-mongo`.

## Main Operations

| Operation                            | Meaning                                                                       |
|--------------------------------------|-------------------------------------------------------------------------------|
| `value.asSuccess()`                  | Creates a success                                                             |
| `error.asFailure()`                  | Creates a failure from an `OutcomeError`                                      |
| `transform { }`                      | Maps the success value (like `map`)                                           |
| `bind { }`                           | Chains an operation returning an `Outcome` (like `flatMap`)                   |
| `transformFailure { }`               | Maps the error                                                                |
| `bindFailure { }`                    | Tries an alternative `Outcome` when failed                                    |
| `recover { }`                        | Returns the value, or computes one from the error                             |
| `onFailure { }`                      | Returns the value, or runs a block that must exit (`return`, `throw`)         |
| `orNull()` / `orThrow()`             | Returns the value, or `null` / throws `OutcomeException`                      |
| `failIf`, `failUnless`, `failIfNull` | Turns a success into a failure when a condition holds                         |
| `withSuccess`, `withFailure`         | Runs a side effect and returns the same `Outcome`                             |
| `combine`, `Outcome.transform2`      | Combines two independent outcomes                                             |
| `` `!` `` and `` `*` ``              | Applicative style: applies a function to several outcomes                     |
| `traverse`, `extractList`            | From a list of values/outcomes to an outcome of a list (stops at first error) |

There are also `map`, `filter` and `flatMap` for an `Outcome` containing an `Iterable`: they work on the elements of
the list, not on the `Outcome` itself.

## Usage Examples

### Basic Operations

```kotlin
data class NegativeValue(val value: Int) : OutcomeError {
    override val msg = "Negative value: $value"
}

fun checkPositive(value: Int): Outcome<NegativeValue, Int> =
    if (value >= 0) value.asSuccess() else NegativeValue(value).asFailure()

val result: Outcome<NegativeValue, String> =
    checkPositive(21)
        .transform { it * 2 }
        .bind { checkPositive(it) }
        .transform { "The answer is $it" }
```

### Json Parsing

```kotlin
val person: JsonOutcome<Person> = JPerson.fromJson(jsonString)

// get the value or use a default
val name: String = person.transform { it.name }.recover { error -> "unknown (${error.msg})" }

// exit early from the calling function
fun greet(json: String): String {
    val p = JPerson.fromJson(json).onFailure { return "Invalid person: ${it.msg}" }
    return "Hello ${p.name}"
}
```

### Error Types

```kotlin
val message: String = JPerson.fromJson(jsonString)
    .transform { "Parsed ${it.name}" }
    .recover { error ->
        when (error) {
            is InvalidJsonError -> "Not valid Json: ${error.msg}"
            is JsonPropertyError -> "Problem with field ${error.propertyName}"
            is ConverterJsonError -> "Cannot convert: ${error.msg}"
        }
    }
```

`JsonError` is sealed, so the `when` is exhaustive without an `else`.

### Many Values

```kotlin
val people: JsonOutcome<List<Person>> = jsonStrings.traverse { JPerson.fromJson(it) }
```

`traverse` stops at the first failure and returns it. To collect all the errors, map to a list of outcomes and
separate them:

```kotlin
val results: List<JsonOutcome<Person>> = jsonStrings.map { JPerson.fromJson(it) }
val errors: List<JsonError> = results.filterIsInstance<Failure<JsonError>>().map { it.error }
```

### Wrapping Code That Throws

```kotlin
val number: Outcome<ThrowableError, Int> = Outcome.tryOrFail { text.toInt() }
```

## Design Notes

- Errors are values: they can be transformed, combined and reported without stack unwinding.
- `Success` and `Failure` are value classes, so an `Outcome` adds very little overhead.
- `orThrow()` exists for tests and for boundaries where an exception is required (e.g. a framework callback); in the
  rest of the code prefer `transform`, `bind` and `recover`.
