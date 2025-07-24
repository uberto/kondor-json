# kondor-core Module

## Purpose

The `kondor-core` module is the foundation of the KondorJson library, providing the essential JSON parsing,
serialization, and conversion capabilities. It contains the core abstractions and implementations that all other modules
depend on.

## Responsibilities

### JSON Processing Pipeline

- **Tokenization**: Converts JSON strings into a stream of tokens using `JsonLexer`
- **Parsing**: Transforms tokens into `JsonNode` tree structures using `JsonParser`
- **Serialization**: Renders `JsonNode` trees back to JSON strings with configurable formatting

### Core Abstractions

- **JsonConverter**: Base interface for bidirectional JSON ↔ Kotlin object conversion
- **JsonNode**: Immutable tree representation of JSON data
- **JsonProperty**: Field definitions for object converters
- **JsonError**: Comprehensive error handling with path information

### Built-in Converters

- **JValues**: Primitive type converters (string, number, boolean, null)
- **JArray**: List and array converters
- **JMap**: Map converters for key-value structures
- **JSealed**: Sealed class converters for polymorphic types

### Utilities

- **JsonStyle**: Configurable JSON formatting (compact, pretty, with nulls)
- **ChunkedStringWriter**: Memory-efficient string building
- **Profunctor**: Functional composition utilities

## Key Components

```mermaid
graph TB
    subgraph "Tokenization Layer"
        A[JsonLexerEager] --> C[TokensStream]
        B[JsonLexerLazy] --> C
    end

    subgraph "Parsing Layer"
        C --> D[JsonParser]
        D --> E[JsonNode Tree]
    end

    subgraph "Conversion Layer"
        E --> F[JsonConverter]
        F --> G[Kotlin Objects]
    end

    subgraph "Built-in Converters"
        H[JValues<br/>Primitives]
        I[JArray<br/>Collections]
        J[JMap<br/>Key-Value]
        K[JSealed<br/>Polymorphic]
    end

    F --> H
    F --> I
    F --> J
    F --> K
    style A fill: #e3f2fd
    style B fill: #e8f5e8
    style E fill: #fff3e0
    style G fill: #f3e5f5
```

## Integration with Other Modules

### Dependencies

- **kondor-outcome**: Uses `Outcome` types for functional error handling
- No other internal dependencies (foundation module)

### Used By

- **kondor-auto**: Extends core converters with automatic data class support
- **kondor-jackson**: Integrates with Jackson using core converter interfaces
- **kondor-mongo**: Uses core converters for MongoDB document mapping
- **kondor-tools**: Builds on core converters for schema generation
- **kondor-examples**: Demonstrates core functionality

## Core Workflow

```mermaid
sequenceDiagram
    participant App as Application
    participant Conv as JsonConverter
    participant Lex as JsonLexer
    participant Parse as JsonParser
    participant Node as JsonNode
    Note over App, Node: Deserialization (JSON → Object)
    App ->> Conv: fromJson("{"name":"John"}")
    Conv ->> Lex: tokenize(jsonString)
    Lex -->> Conv: TokensStream
    Conv ->> Parse: parseJsonNode(tokens)
    Parse -->> Conv: JsonNodeObject
    Conv ->> Conv: fromJsonNode(node)
    Conv -->> App: Person(name="John")
    Note over App, Node: Serialization (Object → JSON)
    App ->> Conv: toJson(Person("John"))
    Conv ->> Conv: toJsonNode(person)
    Conv -->> Node: JsonNodeObject
    Node ->> Node: render(JsonStyle.pretty)
    Node -->> App: "{\n \"name\": \"John\"\n}"
```

## Error Handling Strategy

The module uses functional error handling through the `JsonOutcome<T>` type (alias for `Outcome<JsonError, T>`):

```mermaid
graph TD
    A[JSON Operation] --> B{Result}
    B -->|Success| C[Success<T>]
    B -->|Error| D[Failure<JsonError>]
    D --> E{Error Type}
    E --> F[InvalidJsonError<br/>Malformed JSON]
    E --> G[ConverterJsonError<br/>Type Mismatch]
    E --> H[MissingFieldError<br/>Required Field Missing]
    F --> I[NodePath + Position]
    G --> J[NodePath + Expected vs Actual]
    H --> K[NodePath + Field Name]
```

## Performance Considerations

### Memory vs Speed Trade-offs

- **JsonLexerEager**: Faster processing, higher memory usage (loads entire JSON)
- **JsonLexerLazy**: Lower memory usage, streaming processing for large files

### Optimization Features

- **ChunkedStringWriter**: Reduces string concatenation overhead
- **Immutable JsonNodes**: Safe for concurrent access
- **Lazy evaluation**: Deferred processing where possible

## Usage Examples

### Basic Converter Definition

```kotlin
object PersonConverter : JsonConverter<Person, JsonNodeObject> {
    override val _nodeType = ObjectNode

    private val name = JField(Person::name, JValues.str)
    private val age = JField(Person::age, JValues.num)

    override fun fromNullableJsonNode(node: JsonNodeObject?, path: NodePath) =
        node?.let {
            Person(
                name = name.fromJson(it, path).orThrow(),
                age = age.fromJson(it, path).orThrow()
            ).asSuccess()
        } ?: null.asSuccess()

    override fun toJsonNode(value: Person) = JsonNodeObject(
        name.toJson(value),
        age.toJson(value)
    )
}
```

### Error Handling

```kotlin
val result: JsonOutcome<Person> = PersonConverter.fromJson(jsonString)
result.fold(
    onFailure = { error -> println("Parse error: ${error.msg}") },
    onSuccess = { person -> println("Parsed: $person") }
)
```

This module forms the foundation that enables type-safe, functional JSON processing throughout the KondorJson ecosystem.
