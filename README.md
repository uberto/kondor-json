[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.ubertob.kondor/kondor-core/badge.svg?style=plastic)](https://maven-badges.herokuapp.com/maven-central/com.ubertob.kondor/kondor-core)


# KondorJson
A library to serialize/deserialize Json fast and safely without reflection or generators.

Loosely inspired by the concept of functional adjunctions,

## Dependency declaration
Maven
```
<dependency>
  <groupId>com.ubertob.kondor</groupId>
  <artifactId>kondor-core</artifactId>
  <version>1.0.0</version>
</dependency>
```

Gradle
```
implementation 'com.ubertob.kondor:kondor-core:1.0.0'
```

## Quick Start

To transform a value (string in this case) into and from Json:
```
val jsonString = JString.toJson("my string is here")
val value = JString.fromJson(jsonStr).orThrow()  //"my string is here"
```

`JString` is the Json decoder, there are others for all primitive types

To transform an object we need to write the decoder first with a simple DSL:
```
data class Customer(val id: Int, val name: String)

object JCustomer : JAny<Customer>() {

    val id by JField(Customer::id, JInt)
    val name by JField(Customer::name, JString)

    override fun JsonNodeObject.tryDeserialize() =
        Customer(
            id = +id,
            name = +name
        )
}
```

Each field (id,name) need to be associated to a decoder and a field in the mapped object. Then we need to explicitely call the function to create the object from the fields.

## Do We Need another Json Parser?

We wrote this library to solve a specific problem.
It was useful for us there could be other people that would find this beneficial.

To describe the problem, let's say you need to map a Json like this:
```
 {
  "id": "1001",
  "vat-to-pay": true,
  "customer": {
    "id": 1,
    "name": "ann"
  },
  "items": [
    {
      "id": 1001,
      "short_desc": "toothpaste",
      "long_description": "toothpaste \"whiter than white\"",
      "price": 125
    },
    {
      "id": 10001,
      "short_desc": "special offer",
      "long_description": "bla bla"
    }
  ],
  "total": 123.45
}
```

to your domain objects:
```
data class Customer(val id: Int, val name: String)

data class Product(val id: Int, val shortDesc: String, val longDesc: String, val price: Double?)

data class InvoiceId(override val raw: String) : StringWrapper

data class Invoice(
    val id: InvoiceId,
    val vat: Boolean,
    val customer: Customer,
    val items: List<Product>,
    val total: Double
)
```

The Json format is quite similar to the domain objects but there are some differences:
- `InvoiceId` is a custom type wrapping a string, but in the Json there is only the string.
- field names follow a different conventions (snake instead of camel) or totally different (vat-to-pay).
- nullable fields are optional in Json.

We also used the same domain classes inside other Json format, with slightly different field mappings, moreover we have to handle different versions of the Json format.
Another big requirement for us was avoid reflection on domain classes, we all got bad experiences with refactors that broke Json api.
Finally we general prefer to avoid annotating domain classes with serialization details.

The possibile solution we examined were:

- Libraries based on reflection like Jackson or Gson: to meet our requirements we would have to create DTO for all our types with fields heavily annotated.

- KotlinSerializer: even if it's based on compile-time reflection, it has the same problems of the other libraries based on reflection. 
  
So we did several progressive improvements over the idea of defining bidirectional converter explicitly using a simple DSL.

The idea is inspired by functional adjuctions, which are a couple of functors that work in opposite direction. So instead of trying to explain to the Json mapper how to serialize/deserialize our class using annotations we define the adjuntion--that is the converter--for each class. Thanks to Kotlin DSL capabilities, it doesn't require much code.

This is the result:

```
object JProduct : JAny<Product>() {

    val id by JField(Product::id, JInt)
    val long_description by JField(Product::longDesc, JString)
    val short_desc by JField(Product::shortDesc, JString)
    val price by JFieldMaybe(Product::price, JDouble)

    override fun JsonNodeObject.tryDeserialize() =
        Product(
            id = +id,
            shortDesc = +short_desc,
            longDesc = +long_description,
            price = +price
        )
}

object JInvoice : JAny<Invoice>() {
    val id by JField(Invoice::id, JStringWrapper(::InvoiceId))
    val vat by JField(Invoice::vat, JBoolean, jsonFieldName = "vat-to-pay")
    val customer by JField(Invoice::customer, JCustomer)
    val items by JField(Invoice::items, JArray(JProduct))
    val total by JField(Invoice::total, JDouble)

    override fun JsonNodeObject.tryDeserialize(): Invoice =
        Invoice(
            id = +id,
            vat = +vat,
            customer = +customer,
            items = +items,
            total = +total
        )
}
```
Comparing with a solution involving writing DTOs, you need to write less code using converters. Even without considering DTOs, the time needed to write the converters is roughly the same than to annotate the classes one by one, but it's easier and more IDE friendly to create the converter. For example if you attach `JField` to a nullable field of your domain class, it will not compile. 

No need to browse StackOverflow to find the right annotation, IDE can suggest the possible converters or you can write new ones.

It's very easy to define different converters for same class in different api.

Finally in case of errors, the messages are very friendly and precise:
```
error at parsing: Expected a Double at position 55 but found '"' while parsing </items/0/price>
```

## Custom Converters

It's very easy to create new converters to follow your team conventions.

Converters are defined using `JsonNode`, so you don't have to handle the parsing and the serializing separately (which can be a source of bugs). They are easier to write than other libraries custom serialisers.

There are some converters in Kondor ready-to-use:

- Sealed classes: automatically converting your sealed classes in polymorphic json
- Maps: converting a `Map<String, *>` into a Json object
- Instant: both using epoch or date format
- BigDecimal: you can use numbers of any lenght
- String wrappers: simplify json for IDs and other types that wrap over a string

and so on

## Other Advantages

It's easy to write generic converters for special types, much easier and safer than defining custom serializer.

Faster than reflection based parsers.

Immutable node objects are convenient if you want to manipulate Json trees.

No external dependencies.

Doesn't throw any Exception.

## Comparison

- Jackson
- KotlinSerializer
- Moshi

TBA: examples of annotations vs Kondor converters
TBA: comparison of handling errors
TBA: comparison of performance

## Future Ideas

- Generate Json schema and automatically validate

- A DSL for Java
