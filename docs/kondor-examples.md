# kondor-examples Module

## Purpose

The `kondor-examples` module is not published. It collects code that shows Kondor in use and the performance benchmarks.
It is a place to look at working code and to experiment, not a library to depend on.

## Contents

| Source set  | Content                                                                                      |
|-------------|----------------------------------------------------------------------------------------------|
| `src/main`  | Helpers to use Kondor converters with http4k, and an experiment with primitive wrapper types |
| `src/test`  | An example of the converter generator applied to a Java class (an AWS API Gateway event)     |
| `src/jmh`   | JMH benchmarks comparing Kondor with Jackson                                                 |

## Http4k Helpers

`Http4kHelpers.kt` shows how to plug a converter into [http4k](https://www.http4k.org/) requests and responses:

```kotlin
// client side
val request = Request(POST, "/people").bodyAsJson(JPerson, person)
val person: Person = response.parseJsonBody(JPerson)

// server side
val person: Person = request.parseJsonBody(JPerson)
val response = Response(OK).bodyAsJson(JPerson, person)

// or as an http4k lens
val personLens: BiDiBodyLens<Person> = JPerson.toBodyLens()
```

`bodyAsJson` also sets the `Content-Type` header. These helpers use `orThrow()` when parsing, to fit http4k lenses;
in your own code you may prefer to handle the `Outcome` explicitly.

## Primitive Wrappers

The `primwrap` package is an experiment with "tiny types": small classes wrapping a primitive (`Age`, `Name`, `DoB`...).
They extend `IntWrap`, `LongWrap` or `StringWrap`, and their companion object registers how to build them from the
primitive. The `strW` and `numW` field functions then map them without writing a converter for each type:

```kotlin
data class UserW(val name: Name, val age: Age, val doB: DoB, val recordedAt: RecordedAt)

object JUserW : JAny<UserW>() {
    val name by strW(UserW::name)
    val age by numW(UserW::age)
    val doB by strW(UserW::doB)
    val recordedAt by numW(UserW::recordedAt)

    override fun JsonNodeObject.deserializeOrThrow() =
        UserW(name = +name, age = +age, doB = +doB, recordedAt = +recordedAt)
}
```

For production code, the standard way is a `JStringRepresentable`, `JIntRepresentable` or `JLongRepresentable` object
for each wrapper type, as described in the main README.

## Converter Generation from a Java Class

`AutoGenerationFromJavaTest.kt` runs `kondorGenerator` (from `kondor-tools`) on the Java class
`APIGatewayProxyRequestEvent` of the AWS Lambda events library, printing the converters' source code. See
[kondor-tools.md](kondor-tools.md) for the generator.

## Benchmarks

The `src/jmh` source set contains JMH benchmarks that serialize and deserialize the same `DemoClass` with:

- Kondor converters (`BenchmarkKondor`, and `BenchmarkKondorNdJson` for newline-delimited Json)
- Jackson with reflection (`JacksonReflectionConverters`)
- Jackson with a hand-written DSL (`JacksonDslConverters`)

Run them with:

```bash
./gradlew :kondor-examples:jmh
```

The JMH parameters (iterations, modes, warm-up) are in `kondor-examples/build.gradle.kts`. There are also load tests
with timing results in `kondor-core/src/test/kotlin/com/ubertob/kondor/loadTests/PerformanceTest.kt`.
