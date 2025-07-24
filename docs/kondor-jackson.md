# kondor-jackson Module

## Purpose

The `kondor-jackson` module provides integration between KondorJson and the Jackson JSON processing library. It allows
KondorJson converters to be used within Jackson-based applications and enables interoperability between the two JSON
processing approaches.

## Responsibilities

### Jackson Integration

- **Converter Bridging**: Adapts KondorJson converters to work with Jackson's `JsonSerializer` and `JsonDeserializer`
- **ObjectMapper Integration**: Enables registration of KondorJson converters with Jackson's `ObjectMapper`
- **Type Safety Preservation**: Maintains KondorJson's type safety within Jackson's framework
- **Error Handling Translation**: Converts between KondorJson and Jackson error models

### Interoperability Features

- **Bidirectional Conversion**: Supports both serialization and deserialization through Jackson
- **Configuration Compatibility**: Works with Jackson's configuration and feature settings
- **Performance Optimization**: Leverages Jackson's optimized processing while using KondorJson's type safety
- **Annotation Support**: Compatible with Jackson annotations where applicable

## Key Components

```mermaid
graph TB
    subgraph "KondorJson Side"
        A[JsonConverter<T>] --> B[KondorJson Logic]
        B --> C[Type-Safe Conversion]
    end
    
    subgraph "Jackson Integration Layer"
        D[KondorJsonSerializer] --> E[Jackson Adapter]
        F[KondorJsonDeserializer] --> E
        E --> G[Error Translation]
    end
    
    subgraph "Jackson Side"
        H[ObjectMapper] --> I[JsonGenerator]
        H --> J[JsonParser]
        I --> K[JSON Output]
        J --> L[JSON Input]
    end
    
    A --> D
    A --> F
    E --> H
    
    style A fill:#e8f5e8
    style E fill:#fff3e0
    style H fill:#e3f2fd
```

## Integration with Other Modules

### Dependencies

- **kondor-core**: Uses core converter interfaces and JSON processing
- **Jackson Core**: Integrates with Jackson's serialization framework
- **kondor-outcome**: Uses functional error handling

### Used By

- **kondor-examples**: Demonstrates Jackson integration patterns
- **Enterprise Applications**: Applications already using Jackson infrastructure
- **Spring Boot Applications**: Integration with Spring's Jackson configuration

## Integration Workflow

```mermaid
sequenceDiagram
    participant App as Application
    participant OM as ObjectMapper
    participant KJS as KondorJsonSerializer
    participant KC as KondorConverter
    participant JG as JsonGenerator
    
    Note over App,JG: Serialization via Jackson
    App->>OM: writeValueAsString(object)
    OM->>KJS: serialize(object, generator)
    KJS->>KC: toJsonNode(object)
    KC-->>KJS: JsonNode
    KJS->>KJS: writeJsonNode(node, generator)
    KJS->>JG: writeStartObject(), writeField(), etc.
    JG-->>OM: JSON bytes
    OM-->>App: JSON String
    
    Note over App,JG: Deserialization via Jackson
    App->>OM: readValue(json, Class)
    OM->>KJS: deserialize(parser, context)
    KJS->>KJS: parseToJsonNode(parser)
    KJS->>KC: fromJsonNode(node)
    KC-->>KJS: Typed Object
    KJS-->>OM: Object
    OM-->>App: Typed Object
```

## Configuration and Setup

```mermaid
flowchart TD
    A[ObjectMapper Creation] --> B[Module Registration]
    B --> C[Converter Registration]
    C --> D[Configuration]
    
    E[KondorJson Converter] --> F[Wrap in Jackson Adapter]
    F --> G[Register with ObjectMapper]
    G --> H[Ready for Use]
    
    D --> I[Serialization Features]
    D --> J[Deserialization Features]
    D --> K[Error Handling Config]
    
    I --> L[JSON Output Format]
    J --> M[Type Handling]
    K --> N[Exception Translation]
    
    style A fill:#e3f2fd
    style E fill:#e8f5e8
    style H fill:#f3e5f5
```

## Error Handling Translation

The module translates between KondorJson's functional error handling and Jackson's exception-based approach:

```mermaid
graph TD
    A[KondorJson Operation] --> B{Result Type}
    
    B -->|Success<T>| C[Return T to Jackson]
    B -->|Failure<JsonError>| D[Error Translation]
    
    D --> E{Error Type}
    E --> F[InvalidJsonError] 
    E --> G[ConverterJsonError]
    E --> H[MissingFieldError]
    
    F --> I[JsonParseException]
    G --> J[JsonMappingException]
    H --> K[JsonMappingException]
    
    I --> L[Jackson Error Handling]
    J --> L
    K --> L
    
    style C fill:#e8f5e8
    style L fill:#ffebee
```

## Usage Examples

### Basic ObjectMapper Setup

```kotlin
val objectMapper = ObjectMapper().apply {
    registerModule(KondorJsonModule())
    registerKondorConverter(PersonJson)
    registerKondorConverter(AddressJson)
}

// Use with Jackson APIs
val json = objectMapper.writeValueAsString(person)
val person = objectMapper.readValue(json, Person::class.java)
```

### Spring Boot Integration

```kotlin
@Configuration
class JacksonConfig {
    
    @Bean
    @Primary
    fun objectMapper(): ObjectMapper {
        return ObjectMapper().apply {
            registerModule(KondorJsonModule())
            // Register your KondorJson converters
            registerKondorConverter(PersonJson)
            registerKondorConverter(OrderJson)
        }
    }
}
```

### Custom Serializer Registration

```kotlin
val module = SimpleModule().apply {
    addSerializer(Person::class.java, KondorJsonSerializer(PersonJson))
    addDeserializer(Person::class.java, KondorJsonDeserializer(PersonJson))
}

objectMapper.registerModule(module)
```

## Performance Considerations

### Advantages

- **Jackson's Optimizations**: Benefits from Jackson's highly optimized JSON processing
- **Streaming Support**: Can leverage Jackson's streaming APIs for large datasets
- **Memory Efficiency**: Uses Jackson's efficient memory management

### Trade-offs

- **Additional Layer**: Introduces adapter overhead between KondorJson and Jackson
- **Error Translation Cost**: Converting between error models has performance impact
- **Configuration Complexity**: Requires understanding both Jackson and KondorJson configuration

## Use Cases

### Migration Scenarios

- **Gradual Migration**: Allows incremental adoption of KondorJson in Jackson-based applications
- **Library Integration**: Enables KondorJson converters in libraries that must support Jackson
- **Framework Compatibility**: Works with frameworks that expect Jackson serializers

### Hybrid Approaches

- **Type-Safe Subsets**: Use KondorJson for critical type-safe conversions within Jackson ecosystem
- **Custom Serialization**: Leverage KondorJson's converter composition within Jackson workflows
- **Error Handling**: Benefit from KondorJson's superior error reporting in Jackson applications

This module serves as a bridge between KondorJson's functional, type-safe approach and Jackson's widespread ecosystem
adoption, enabling the best of both worlds in enterprise applications.
