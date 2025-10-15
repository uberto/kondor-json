# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Kondor-Json** is a functional Kotlin library for JSON serialization/deserialization without reflection, annotations, or code generation. It uses explicit converters (called JConverters) that define bidirectional mappings between domain classes and JSON representation.

### Core Concepts

- **Converters**: Objects that map domain classes to JSON (e.g., `JProduct` for `Product` class)
- **Profunctors**: The mathematical foundation - each converter is a profunctor mapping types bidirectionally
- **Outcome**: Custom Either monad for error handling (not Kotlin's Result)
- **No Reflection**: All mappings are explicit and compile-time safe
- **JsonNode**: Intermediate representation used during parsing (slower but necessary for polymorphic types)

## Build System

**Requirements**: Java 21 toolchain (configured in gradle/libs.versions.toml)

### Setup for development on MacOS

If you don't have Java 21 installed:
```bash
brew install openjdk@21
```

If you have multiple Java versions installed, you may need to set JAVA_HOME:

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
```

### Common Commands

```bash
# Run all tests
./gradlew test

# Run tests with detailed output
./gradlew test --info

# Run tests for a specific module
./gradlew :kondor-core:test

# Run a single test class
./gradlew :kondor-core:test --tests "com.ubertob.kondor.json.JValuesTest"

# Run a single test method
./gradlew :kondor-core:test --tests "com.ubertob.kondor.json.JValuesTest.Json String with Unicode"

# Build without tests
./gradlew clean build -x test

# Build everything
./gradlew clean build

# Check version
./gradlew printVersion
```

### Project Structure

Multi-module Gradle project with these modules:

- **kondor-core**: Core JSON parsing/serialization, converters, and DSL
- **kondor-outcome**: Outcome monad for error handling (Either implementation)
- **kondor-tools**: Testing utilities and converter generator
- **kondor-auto**: Automatic converter generation from data classes
- **kondor-examples**: Usage examples (not part of distribution)
- **kondor-mongo**: MongoDB integration using Kondor converters
- **kondor-jackson**: Jackson compatibility layer

## Architecture

### Converter Hierarchy

1. **JsonConverter<T, JN>**: Base interface for all converters
   - `T`: Domain type being converted
   - `JN`: JsonNode type (JsonNodeObject, JsonNodeArray, etc.)

2. **Base Converter Classes**:
   - `JAny<T>`: For objects (uses JsonNode intermediate, slower but flexible)
   - `JObj<T>`: For objects (direct token parsing, faster)
   - `JArray<T, CT>`: For arrays/lists/collections
   - `JSealed<T>`: For sealed classes with discriminator field
   - Primitive converters: `JString`, `JInt`, `JDouble`, `JBoolean`, etc.

3. **Field Definitions**:
   - Fields defined using `by` delegation in converter objects
   - Example: `val file_name by str(Product::name)` maps JSON field `file_name` to `Product.name`

### Two-Phase Conversion

**JAny path** (uses JsonNode):
1. Tokens → JsonNode (via `_nodeType.parse()`)
2. JsonNode → Domain object (via `deserializeOrThrow()`)

**JObj path** (direct, faster):
1. Tokens → Domain object directly (via `fromFieldNodeMap()`)

### Key Files

- `JsonConverter.kt` (kondor-core): Base converter interface and extension functions
- `JAny.kt` / `JObj.kt` (kondor-core): Main object converter implementations
- `JsonParser.kt` (kondor-core/parser): Token-based JSON parser
- `JsonLexer.kt` (kondor-core/parser): Lexical analyzer
- `Outcome.kt` (kondor-outcome): Error handling monad
- `ConverterGenerator.kt` (kondor-tools): Automatic converter generation

## Testing Conventions

### Test Structure

- Uses **JUnit 5** (Jupiter) with **Strikt** assertions
- Test fixtures in `kondor-tools` module provide utilities:
  - `expectSuccess()` / `expectFailure()` for Outcome results
  - `isEquivalentJson()` for comparing JSON strings ignoring formatting

### Test Patterns

```kotlin
// Standard test pattern
@Test
fun `test description`() {
    val value = // create test object
    val json = JConverter.toJsonNode(value)

    val actual = JConverter.fromJsonNode(json, NodePathRoot).expectSuccess()

    expectThat(actual).isEqualTo(value)

    // Also test string round-trip
    val jsonStr = JConverter.toJson(value)
    expectThat(JConverter.fromJson(jsonStr).expectSuccess()).isEqualTo(value)
}
```

### Running Tests

Tests are in `src/test/kotlin` for each module. The build is configured to show PASSED, FAILED, and SKIPPED events.

**Note**: MongoDB tests (`:kondor-mongo:test`) require a running MongoDB instance via TestContainers. To run tests without MongoDB:
```bash
./gradlew test -x :kondor-mongo:test
```

## Development Notes

### Adding New Converters

When adding converters for new types:
1. Create converter object extending appropriate base class (JAny, JObj, etc.)
2. Define fields using DSL: `val field_name by <type>(TypeClass::property)`
3. Implement `deserializeOrThrow()` for JAny or `fromFieldNodeMap()` for JObj
4. Test round-trip: object → JSON → object

### Error Handling

- Never use exceptions for control flow
- All parsing returns `Outcome<JsonError, T>`
- Use `orThrow()`, `orNull()`, `onFailure{}`, `transform{}` + `recover{}` to handle results
- Error messages include JSON path for precise debugging

### Performance Considerations

- **JObj** is faster than **JAny** (skips JsonNode intermediate)
- Use JAny when you need JsonNode (e.g., JSealed with discriminator)
- Parser uses token streaming for memory efficiency
- CharWriter abstraction allows streaming output

### Unicode and String Handling

- Unicode characters are preserved and rendered correctly
- Special characters (\n, \r, \t, \\, \") are properly escaped

## Version Information

Current version: 4.0.0-beta2
- Publishing to Maven Central via OSSRH
- Requires Java 8 runtime (compiles with Java 21 toolchain targeting Java 8)
- Kotlin 2.1.0
