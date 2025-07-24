# kondor-outcome Module

## Purpose

The `kondor-outcome` module provides functional error handling capabilities for the KondorJson library. It implements
the `Outcome` type, which represents computations that can either succeed with a value or fail with an error, enabling
composable and type-safe error handling throughout the JSON processing pipeline.

## Responsibilities

### Functional Error Handling

- **Outcome Type**: Provides `Outcome<E, T>` as a functional alternative to exceptions
- **Error Composition**: Enables chaining of operations that might fail
- **Type Safety**: Ensures errors are handled explicitly at compile time
- **Monadic Operations**: Supports functional programming patterns like `map`, `flatMap`, and `fold`

### JSON-Specific Error Types

- **JsonError Hierarchy**: Defines specific error types for JSON processing failures
- **Path Tracking**: Maintains location information for errors within JSON structures
- **Error Aggregation**: Supports collecting multiple errors from complex operations
- **Error Transformation**: Enables mapping between different error types

## Key Components

```mermaid
graph TB
    subgraph "Outcome Type System"
        A[Outcome<E, T>] --> B[Success<T>]
        A --> C[Failure<E>]
    end

    subgraph "JSON Error Hierarchy"
        D[JsonError] --> E[InvalidJsonError]
        D --> F[ConverterJsonError]
        D --> G[MissingFieldError]
        D --> H[CustomError]
    end

    subgraph "Functional Operations"
        I[map] --> J[Transform Success]
        K[flatMap] --> L[Chain Operations]
        M[fold] --> N[Handle Both Cases]
        O[recover] --> P[Error Recovery]
    end

    B --> I
    B --> K
    A --> M
    C --> O
    style A fill: #e8f5e8
    style D fill: #fff3e0
    style I fill: #e3f2fd
```

## Integration with Other Modules

### Dependencies

- **Kotlin Standard Library**: Uses functional programming constructs
- No external dependencies (foundation utility module)

### Used By

- **kondor-core**: All JSON operations return `JsonOutcome<T>` (alias for `Outcome<JsonError, T>`)
- **kondor-auto**: Data class conversion operations use Outcome for error handling
- **kondor-mongo**: Database operations return Outcome types for error handling
- **kondor-tools**: Schema generation uses Outcome for validation results

## Error Handling Flow

```mermaid
sequenceDiagram
    participant App as Application
    participant Op1 as Operation 1
    participant Op2 as Operation 2
    participant Op3 as Operation 3
    Note over App, Op3: Success Chain
    App ->> Op1: execute()
    Op1 -->> App: Success(value1)
    App ->> Op2: execute(value1)
    Op2 -->> App: Success(value2)
    App ->> Op3: execute(value2)
    Op3 -->> App: Success(finalValue)
    Note over App, Op3: Failure Chain
    App ->> Op1: execute()
    Op1 -->> App: Success(value1)
    App ->> Op2: execute(value1)
    Op2 -->> App: Failure(error)
    Note right of App: Chain stops, error propagated
    App ->> App: handle(error)
```

## Functional Composition Patterns

```mermaid
flowchart TD
    A[Initial Value] --> B[Operation 1]
    B --> C{Success?}
    C -->|Yes| D[Operation 2]
    C -->|No| E[Error 1]
    D --> F{Success?}
    F -->|Yes| G[Operation 3]
    F -->|No| H[Error 2]
    G --> I{Success?}
    I -->|Yes| J[Final Success]
    I -->|No| K[Error 3]
    E --> L[Error Handling]
    H --> L
    K --> L
    L --> M[Recovery Strategy]
    M --> N[Alternative Path]
    style A fill: #e8f5e8
    style J fill: #e8f5e8
    style L fill: #ffebee
    style N fill: #fff3e0
```

## Error Type Hierarchy

```mermaid
classDiagram
    class JsonError {
        <<abstract>>
        +path: NodePath
        +msg: String
        +toString() String
    }

    class InvalidJsonError {
        +position: Int
        +expected: String
        +actual: String
    }

    class ConverterJsonError {
        +expectedType: String
        +actualType: String
        +value: String?
    }

    class MissingFieldError {
        +fieldName: String
        +availableFields: Set<String>
    }

    class CustomError {
        +details: String
        +cause: Throwable?
    }

    JsonError <|-- InvalidJsonError
    JsonError <|-- ConverterJsonError
    JsonError <|-- MissingFieldError
    JsonError <|-- CustomError
```

## Usage Examples

### Basic Outcome Operations

```kotlin
// Creating outcomes
val success: Outcome<String, Int> = 42.asSuccess()
val failure: Outcome<String, Int> = "Error message".asFailure()

// Transforming success values
val doubled = success.map { it * 2 } // Success(84)

// Chaining operations that might fail
val result = success
    .flatMap { value ->
        if (value > 0) (value * 2).asSuccess()
        else "Negative value".asFailure()
    }
    .map { it.toString() }
```

### JSON Processing with Outcomes

```kotlin
val jsonResult: JsonOutcome<Person> = PersonJson.fromJson(jsonString)

// Handle both success and failure cases
val message = jsonResult.fold(
    onFailure = { error -> "Failed to parse: ${error.msg}" },
    onSuccess = { person -> "Parsed: ${person.name}" }
)

// Chain JSON operations
val processedResult = jsonResult
    .flatMap { person -> validatePerson(person) }
    .flatMap { person -> savePerson(person) }
    .map { person -> "Successfully processed ${person.name}" }
```

### Error Recovery

```kotlin
val result = PersonJson.fromJson(jsonString)
    .recover { error ->
        when (error) {
            is MissingFieldError -> createDefaultPerson().asSuccess()
            is InvalidJsonError -> tryAlternativeParser(jsonString)
            else -> error.asFailure()
        }
    }
```

### Collecting Multiple Errors

```kotlin
fun validatePersons(persons: List<String>): Outcome<List<JsonError>, List<Person>> {
    val results = persons.map { PersonJson.fromJson(it) }
    val errors = results.mapNotNull { it.failureOrNull() }
    val successes = results.mapNotNull { it.successOrNull() }

    return if (errors.isEmpty()) {
        successes.asSuccess()
    } else {
        errors.asFailure()
    }
}
```

## Monadic Laws and Properties

The Outcome type follows monadic laws, ensuring predictable composition:

```mermaid
graph TD
    A[Left Identity] --> B[return(a).flatMap(f) ≡ f(a)]
C[Right Identity] --> D[m.flatMap(return) ≡ m]
E[Associativity] --> F[m.flatMap(f).flatMap(g) ≡ m.flatMap(x => f(x).flatMap(g))]

G[Functor Laws] --> H[map(id) ≡ id]
G --> I[map(f).map(g) ≡ map(g ∘ f)]

style A fill: #e8f5e8
style C fill:#e8f5e8
style E fill: #e8f5e8
style G fill: #fff3e0
```

## Performance Considerations

### Advantages

- **No Exception Overhead**: Avoids the performance cost of exception throwing/catching
- **Explicit Error Handling**: Compile-time verification that errors are handled
- **Composable Operations**: Efficient chaining without intermediate exception handling
- **Memory Efficiency**: Lightweight wrapper around success/failure values

### Design Trade-offs

- **Explicit Handling Required**: All error cases must be explicitly handled
- **Learning Curve**: Requires understanding of functional programming concepts
- **Verbose Syntax**: More verbose than exception-based error handling in some cases

## Integration Patterns

### With Coroutines

```kotlin
suspend fun processJsonAsync(json: String): JsonOutcome<ProcessedData> =
    withContext(Dispatchers.IO) {
        PersonJson.fromJson(json)
            .flatMap { person -> validatePersonAsync(person) }
            .flatMap { person -> savePersonAsync(person) }
    }
```

### With Nullable Types

```kotlin
fun JsonOutcome<T>.orNull(): T? = successOrNull()

fun T?.asOutcome(error: () -> JsonError): JsonOutcome<T> =
    this?.asSuccess() ?: error().asFailure()
```

This module provides the foundation for robust, functional error handling throughout the KondorJson ecosystem, enabling
applications to handle JSON processing errors in a type-safe and composable manner.
