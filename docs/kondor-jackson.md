# kondor-jackson Module

## Purpose

The `kondor-jackson` module helps projects that use Jackson to adopt Kondor gradually. It converts between Kondor's
`JsonNode` tree and Jackson's `JsonNode` tree, so the two libraries can exchange Json without going through a string.
It does not register anything in an `ObjectMapper`: it is a small set of extension functions.

## Key Components

All the functions are in `com.ubertob.kondor.jackson` (`KondorAdaptors.kt`). In the code below, Jackson's node is
imported as `JJsonNode` and Kondor's as `KJsonNode`, as in the module itself.

### From Kondor to Jackson

| Function                                                   | Result                                     |
|------------------------------------------------------------|--------------------------------------------|
| `ObjectNodeConverter<T>.toJacksonJsonNode(value: T)`       | a Jackson `ObjectNode` for a domain object |
| `T.intoJacksonJsonNode(converter: ObjectNodeConverter<T>)` | the same, called on the value              |
| `KJsonNode.toJacksonJsonNode()`                            | the equivalent Jackson `JsonNode`          |

There are also specific overloads for each Kondor node (`JsonNodeString`, `JsonNodeNumber`, `JsonNodeBoolean`,
`JsonNodeNull`, `JsonNodeArray`, `JsonNodeObject`), which accept an optional `JsonNodeFactory`.

### From Jackson to Kondor

| Function                        | Result                                    |
|---------------------------------|-------------------------------------------|
| `JJsonNode.toKondorJsonNode()`  | the equivalent Kondor `JsonNode`          |
| `ObjectNode.toKondorJsonNode()` | a `JsonNodeObject`, ready for a converter |

and the specific overloads for `TextNode`, `NumericNode`, `BooleanNode`, `NullNode` and `ArrayNode`.

## Integration with Other Modules

- Depends on `kondor-core` and on `jackson-databind`. The Jackson dependency is not exported, so your project needs its
  own `jackson-databind` dependency (which it will have anyway if it uses Jackson).
- It is independent from the other Kondor modules.

## Usage Examples

### Domain Object to Jackson

```kotlin
data class Person(val id: Int, val name: String)

object JPerson : JObj<Person>() {
    private val id by num(Person::id)
    private val name by str(Person::name)

    override fun FieldsValues.deserializeOrThrow(path: NodePath) =
        Person(id = +id, name = +name)
}

val person = Person(1, "Alice")

val jacksonNode: ObjectNode = JPerson.toJacksonJsonNode(person)
// or: person.intoJacksonJsonNode(JPerson)

val json: String = ObjectMapper().writeValueAsString(jacksonNode) // {"id":1,"name":"Alice"}
```

### Jackson to Domain Object

```kotlin
val jacksonNode: JJsonNode = ObjectMapper().readTree("""{"id": 1, "name": "Alice"}""")

val person: JsonOutcome<Person> =
    (jacksonNode as ObjectNode).toKondorJsonNode()
        .let { JPerson.fromJsonNode(it) }
```

The conversion goes through the Kondor `JsonNode`, so the converter is used via `fromJsonNode` rather than `fromJson`.
Any object converter works, `JObj` or `JAny`.

### Converting Json Trees

```kotlin
val kondorNode: KJsonNode = parseJsonNode("""{"tags": ["a", "b"], "size": 12.5}""").orThrow()

val jacksonNode: JJsonNode = kondorNode.toJacksonJsonNode()
val backToKondor: KJsonNode = jacksonNode.toKondorJsonNode()
```

## Behaviour and Limitations

- Numbers coming from Jackson are converted to `BigDecimal`. Kondor number converters accept them, but a
  `JsonNodeNumber` built from Jackson is not `equals` to one holding an `Int` or a `Double`.
- Jackson nodes without a Json equivalent (binary, POJO and missing nodes) are rejected with an
  `IllegalArgumentException`: these functions don't return an `Outcome`.
- Only the tree is converted: Jackson settings (naming strategies, annotations, custom serializers) are not involved.

## Use Cases

- **Gradual migration**: keep an existing Jackson-based HTTP or messaging layer, and use Kondor converters for the
  domain objects.
- **Hybrid code**: pass a Kondor-rendered object to an API that expects a Jackson `JsonNode`, or the other way round.
