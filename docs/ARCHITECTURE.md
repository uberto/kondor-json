# KondorJson Architecture

This document provides an overview of the KondorJson library architecture, showing how the different modules work
together to provide JSON parsing, serialization, and conversion capabilities.

## System Overview

KondorJson is a functional JSON library for Kotlin that provides type-safe JSON parsing and serialization through a
converter-based architecture. The system is built around several core concepts:

- **Tokenizer/Lexer**: Converts JSON strings into tokens
- **Parser**: Transforms tokens into JsonNode tree structures
- **Converters**: Bidirectional transformations between JsonNodes and Kotlin objects
- **Modules**: Specialized functionality for different use cases

## Module Dependencies

```mermaid
graph TD
    A[kondor-core] --> B[kondor-auto]
    A --> C[kondor-jackson]
    A --> D[kondor-mongo]
    A --> E[kondor-outcome]
    A --> F[kondor-tools]
    A --> G[kondor-examples]
    
    E --> B
    E --> D
    E --> F
    
    B --> G
    C --> G
    D --> G
    F --> G
    
    style A fill:#e1f5fe
    style E fill:#f3e5f5
    style B fill:#e8f5e8
    style C fill:#fff3e0
    style D fill:#fce4ec
    style F fill:#f1f8e9
    style G fill:#f5f5f5
```

## JSON Processing Pipeline

```mermaid
sequenceDiagram
    participant Client
    participant Converter
    participant Parser
    participant Tokenizer
    participant JsonNode
    
    Note over Client,JsonNode: Parsing Flow (JSON → Object)
    Client->>Converter: fromJson(jsonString)
    Converter->>Tokenizer: tokenize(jsonString)
    Tokenizer-->>Converter: TokensStream
    Converter->>Parser: parseJsonNode(tokens)
    Parser-->>Converter: JsonNode
    Converter->>Converter: fromJsonNode(node)
    Converter-->>Client: T (typed object)
    
    Note over Client,JsonNode: Serialization Flow (Object → JSON)
    Client->>Converter: toJson(object)
    Converter->>Converter: toJsonNode(object)
    Converter-->>JsonNode: JsonNode
    JsonNode->>JsonNode: render(style)
    JsonNode-->>Client: JSON String
```

## Core Component Interaction

```mermaid
graph LR
    subgraph "JSON Input"
        A[JSON String]
    end
    
    subgraph "Tokenization"
        B[JsonLexer]
        C[KondorToken]
        D[TokensStream]
    end
    
    subgraph "Parsing"
        E[JsonParser]
        F[JsonNode Tree]
    end
    
    subgraph "Conversion"
        G[JsonConverter]
        H[Kotlin Object]
    end
    
    A --> B
    B --> C
    C --> D
    D --> E
    E --> F
    F --> G
    G --> H
    
    H --> G
    G --> F
    F --> A
    
    style A fill:#e3f2fd
    style H fill:#e8f5e8
    style F fill:#fff3e0
```

## Detailed Tokenizer and Parser Flow

```mermaid
flowchart TD
    A[JSON String] --> B{JsonLexerEager vs JsonLexerLazy}
    B -->|Memory Efficient| C[JsonLexerLazy]
    B -->|Performance| D[JsonLexerEager]
    
    C --> E[Character-by-character processing]
    D --> F[Full string tokenization]
    
    E --> G[TokensStream]
    F --> G
    
    G --> H[JsonParser.parseNewNode]
    H --> I{Token Type}
    
    I -->|"{"| J[parseJsonNodeObject]
    I -->|"["| K[parseJsonNodeArray]  
    I -->|String| L[parseJsonNodeString]
    I -->|Number| M[parseJsonNodeNum]
    I -->|Boolean| N[parseJsonNodeBoolean]
    I -->|null| O[parseJsonNodeNull]
    
    J --> P[JsonNodeObject]
    K --> Q[JsonNodeArray]
    L --> R[JsonNodeString]
    M --> S[JsonNodeNumber]
    N --> T[JsonNodeBoolean]
    O --> U[JsonNodeNull]
    
    P --> V[JsonNode Tree]
    Q --> V
    R --> V
    S --> V
    T --> V
    U --> V
```

## Converter Type Hierarchy

```mermaid
classDiagram
    class JsonConverter~T, JN~ {
        <<interface>>
        +fromJson(json: String) JsonOutcome~T~
        +toJson(value: T) String
        +fromJsonNode(node: JN, path: NodePath) JsonOutcome~T~
        +toJsonNode(value: T) JN
    }
    
    class ObjectNodeConverter~T~ {
        +deserializeOrThrow(path: NodePath) JsonOutcome~T~
        +serialize(value: T) JsonNodeObject
    }
    
    class JDataClass~T~ {
        +buildInstance(args: ObjectFields, path: NodePath) JsonOutcome~T~
        +properties: List~JsonProperty~T, *~~
    }
    
    class JValues {
        +str: JsonConverter~String, JsonNodeString~
        +num: JsonConverter~Number, JsonNodeNumber~
        +bool: JsonConverter~Boolean, JsonNodeBoolean~
        +array~T~: JsonConverter~List~T~, JsonNodeArray~
    }
    
    JsonConverter <|-- ObjectNodeConverter
    ObjectNodeConverter <|-- JDataClass
    JsonConverter <|-- JValues
    
    class JField~T~ {
        +fieldName: String
        +converter: JsonConverter~T, *~
        +isOptional: Boolean
    }
    
    JDataClass --> JField : uses
```

## Error Handling Flow

```mermaid
flowchart TD
    A[JSON Processing] --> B{Success?}
    B -->|Yes| C[Return Success~T~]
    B -->|No| D[JsonError]
    
    D --> E{Error Type}
    E -->|Parsing| F[InvalidJsonError]
    E -->|Conversion| G[ConverterJsonError]
    E -->|Missing Field| H[MissingFieldError]
    
    F --> I[NodePath + Position Info]
    G --> J[NodePath + Type Mismatch]
    H --> K[NodePath + Field Name]
    
    I --> L[JsonOutcome.Failure]
    J --> L
    K --> L
    
    L --> M[Error Handling Strategy]
    M --> N[Log/Report/Recover]
```

## Integration Patterns

```mermaid
graph TB
    subgraph "Application Layer"
        A[User Code]
    end
    
    subgraph "KondorJson Core"
        B[JsonConverter]
        C[JDataClass]
        D[JValues]
    end
    
    subgraph "Integration Modules"
        E[kondor-jackson<br/>Jackson Integration]
        F[kondor-mongo<br/>MongoDB Integration]
        G[kondor-outcome<br/>Functional Error Handling]
    end
    
    subgraph "Development Tools"
        H[kondor-auto<br/>Auto-generation]
        I[kondor-tools<br/>Schema Generation]
    end
    
    A --> B
    A --> C
    A --> D
    
    B --> E
    B --> F
    C --> G
    
    B --> H
    C --> H
    D --> I
    
    E --> J[External Systems]
    F --> J
    G --> K[Functional Programming]
    H --> L[Code Generation]
    I --> M[API Documentation]
```

## Performance Characteristics

```mermaid
graph LR
    subgraph "Memory Usage"
        A[JsonLexerLazy<br/>Low Memory]
        B[JsonLexerEager<br/>High Memory]
    end
    
    subgraph "Processing Speed"
        C[Streaming<br/>Slower]
        D[Batch<br/>Faster]
    end
    
    subgraph "Use Cases"
        E[Large Files<br/>Limited Memory]
        F[Small Files<br/>High Performance]
    end
    
    A --> C
    B --> D
    C --> E
    D --> F
    
    style A fill:#e8f5e8
    style B fill:#ffebee
    style C fill:#e3f2fd
    style D fill:#f3e5f5
```

## Key Design Principles

1. **Type Safety**: All conversions are type-safe at compile time
2. **Functional Approach**: Immutable data structures and functional error handling
3. **Composability**: Converters can be combined and transformed
4. **Performance**: Multiple strategies for different performance/memory trade-offs
5. **Extensibility**: Modular architecture allows for specialized integrations
6. **Error Handling**: Comprehensive error reporting with path information
