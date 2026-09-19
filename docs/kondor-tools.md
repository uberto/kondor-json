# kondor-tools Module

## Purpose

The `kondor-tools` module contains development helpers: a generator that writes the source code of Kondor converters
for your classes, and a few functions that make tests on converters shorter. It is meant to be used as a **test**
dependency.

## Key Components

All in the package `com.ubertob.kondortools`.

| Function                                         | What it does                                                                  |
|--------------------------------------------------|-------------------------------------------------------------------------------|
| `kondorGenerator(vararg kClasses: KClass<*>)`    | Returns the Kotlin source of a converter for each class                       |
| `Outcome<*, T>.expectSuccess(): T`               | Returns the value, or fails the test with the error message                   |
| `Outcome<E, *>.expectFailure(): E`               | Returns the error, or fails the test if the outcome was a success             |
| `String.isEquivalentJson(expected: String)`      | Compares two Json strings ignoring formatting; returns an `Outcome`           |
| `T.printIt(prefix: String = "")`                 | Prints the value and returns it, handy inside a chain                         |
| `chronoAndLog(logPrefix: String, fn: () -> T)`   | Runs `fn`, prints the elapsed milliseconds and returns the result             |

## Converter Generator

`kondorGenerator` uses Kotlin reflection (and KotlinPoet) to write the source code of a converter, that you then copy
in your code base and adapt:

```kotlin
data class User(val id: Int, val name: String, val isAdmin: Boolean)

fun main() {
    println(kondorGenerator(User::class))
}
```

prints:

```kotlin
import com.ubertob.kondor.json.jsonnode.JsonNodeObject

object JUser : JAny<User>() {
  private val id by num(User::id)

  private val isAdmin by bool(User::isAdmin)

  private val name by str(User::name)

  override fun JsonNodeObject.deserializeOrThrow(): User = 
      User(
        id = +id,
        isAdmin = +isAdmin,
        name = +name
      )
}
```

Notes on the generated code:

- It is a `JAny` converter. It works as it is; to make it faster, change it to a `JObj` as described in
  [MigrationToV4.md](../kondor-core/MigrationToV4.md).
- The Json field names are the property names, in alphabetical order.
- Numbers use `num`, strings and enums use `str`, booleans use `bool`, collections use `array(J<Element>, ...)`, and
  any other type uses `obj(J<Type>, ...)`, so you need to generate or write the converters of nested types too.
- Types without a matching rule (nullable booleans, maps, dates...) fall back to `obj`, and must be fixed by hand.

## Test Helpers

```kotlin
@Test
fun `person round trip`() {
    val person = Person(1, "Alice")

    val json = JPerson.toJson(person)
    json.isEquivalentJson("""{"name": "Alice", "id": 1}""").expectSuccess()

    val parsed = JPerson.fromJson(json).expectSuccess()
    expectThat(parsed).isEqualTo(person)
}

@Test
fun `invalid json gives an error`() {
    val error = JPerson.fromJson("""{"id": "one"}""").expectFailure()

    expectThat(error.msg).contains("</id>")
}
```

`expectSuccess` and `expectFailure` use JUnit's `fail`, so they work with any assertion library.
`isEquivalentJson` fails with a `MatcherError` showing both Json documents rendered in the same style.

## Integration with Other Modules

- Depends on `kondor-core`, `kondor-outcome`, JUnit (for `fail`), Kotlin reflection and KotlinPoet.
- The other Kondor modules use it in their own tests.
