# Migration to Kondor-Json v.4.x

Most code written for Kondor 3.x works unchanged: `JAny` converters, `JSealed`, `JMap`, `JList` and all the field
functions (`str`, `num`, `bool`, `obj`, `array`, `flatten`...) keep the same DSL. There could be issues if you derived
from intermediate types, look at how similar examples are working.

## New and Old Converters

- `JAny` is kept for compatibility and for the cases that still need the `JsonNode` intermediate step when parsing.
- `JObj` is the new converter that parses directly from the tokens, and it is faster than `JAny`.
- `JSealed` still derives from `JAny`, because the discriminator field can be in any position in the Json object.
- `JDataClass` (kondor-auto) doesn't need the constructor call (like the old `deserializeOrThrow`).
- `JDataClassAuto` (kondor-auto) doesn't need anything, the fields are discovered by reflection.
- `JDataClassWithNames` (kondor-auto) binds the constructor parameters by name, so the fields can be declared in any
  order.

## From JAny to JObj

Moving a converter from `JAny` to `JObj` changes the deserialization function signature: instead of a
`JsonNodeObject` receiver it has a `FieldsValues` receiver and the current path. The `+field` syntax is the same.

There is also a change of behaviour: a `JObj` fails when the Json contains a field that is not declared in the
converter (with a `JsonPropertyError` naming the field), while a `JAny` ignores unknown fields. If you need to accept
unknown fields, keep using `JAny`.

```kotlin
// Kondor 3.x (still valid in 4.x)
object JPerson : JAny<Person>() {
    private val id by num(Person::id)
    private val name by str(Person::name)

    override fun JsonNodeObject.deserializeOrThrow() =
        Person(id = +id, name = +name)
}

// Kondor 4.x, faster
object JPerson : JObj<Person>() {
    private val id by num(Person::id)
    private val name by str(Person::name)

    override fun FieldsValues.deserializeOrThrow(path: NodePath) =
        Person(id = +id, name = +name)
}
```

A `JObj` can be used everywhere a `JAny` is accepted, including as a subtype converter inside a `JSealed`.

If you used the experimental `JObj` of Kondor 3.x, replace `deserFieldMapOrThrow(fieldMap)` with
`FieldsValues.deserializeOrThrow(path)`.

## Other Breaking Changes

- `JsonNodeObject._fieldMap` is now a `FieldNodeMap`: use `_fieldMap.map` to access the plain `Map<String, JsonNode>`.
- `asObjFieldMap()` returns a `Map<String, JsonNode>`.
- Custom `ObjectNodeConverter` implementations must return a `FieldNodeMap` from `convertFields`.
- `JMap`, `JInstance` and `JDataClass` are now `JObj`; `JJsonNode` and `JSealed` are `JAny`. Since `JObj` rejects
  unknown fields, `JDataClass` and `JInstance` are now stricter when parsing.
- If you implemented the object converter interfaces directly instead of extending `JAny` or `JObj`:
    - `deserializeOrThrow` moved from `ObjectNodeConverterBase` to `JAny`
    - `ObjectNodeConverter` no longer provides default `fromTokens` and `fromJsonNode`
    - `ObjectNodeConverterWriters` has a new abstract `resolveConverter`
    - `registerPropertyHack`, the `FieldMap` typealias and `withParentNode` have been removed
- `JDataClass` fails if the number of declared fields is different from the number of constructor parameters.
- The deprecated `checkForJsonTail` has been removed: `fromJson` already fails when there is Json after the end.
- kondor-auto: `JAnyAuto` and `JDataClassReflect` have been removed: use `JDataClass`, or `JDataClassAuto`, which doesn't
  need `registerAllProperties`.

## Known Limitations

- A `flatten` field inside a `JObj` is rendered correctly and works with `fromJsonNode`, but `fromJson` fails on the
  flattened fields (they are reported as unknown). Keep using `JAny` for converters with `flatten` fields.
- The polymorphic `JSealed` still parses through `JsonNode`; a faster variant requiring the discriminator as first field
  is planned (see `docs/polymorphic-converter-plan.md`).
