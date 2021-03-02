[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.ubertob.kondor/kondor-core/badge.svg?style=plastic)](https://maven-badges.herokuapp.com/maven-central/com.ubertob.kondor/kondor-core)


# KondorJson
A library to serialize/deserialize Json fast and safely without reflection or generators.

Loosely inspired by the concept of functional adjunctions,

## Dependency declaration
Maven
```xml
<dependency>
  <groupId>com.ubertob.kondor</groupId>
  <artifactId>kondor-core</artifactId>
  <version>1.1.0</version>
</dependency>
```

Gradle
```
implementation 'com.ubertob.kondor:kondor-core:1.1.0'
```

## Quick Start

To transform a value (string in this case) into and from Json:
```kotlin
val jsonString = JString.toJson("my string is here")
val value = JString.fromJson(jsonStr).orThrow()  //"my string is here"
```

`JString` is the Json decoder, there are others for all primitive types

To transform an object we need to write the decoder first with a simple DSL:
```kotlin
data class Customer(val id: Int, val name: String)

object JCustomer : JAny<Customer>() {

    val id by JField(Customer::id, JInt)
    val name by JField(Customer::name, JString)

    override fun JsonNodeObject.deserializeOrThrow() =
        Customer(
            id = +id,
            name = +name
        )
}
```

Each field (id,name) need to be associated to a decoder and a field in the mapped object. Then we need to explicitely call the function to create the object from the fields.

## Do We Need another Json Parser?

We wrote this library to solve a specific problem.
It was useful for us, so there could be other people that could find this beneficial.

To describe the problem, let's say you need to map a Json like this:
```json
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

To your own domain objects:
```kotlin
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

Another big requirement for us was not having reflection on our domain classes, we all got bad experiences with refactors that broke Json api.

Finally, we prefer to avoid annotating domain classes with serialization details.

The possible solutions we examined were:

- Libraries based on reflection like Jackson or Gson: to meet our requirements we would have to create DTO for all our types with fields heavily annotated.

- KotlinSerializer: even if it's based on compile-time reflection, it has the same problems of the other libraries based on reflection. 
  
So we did several progressive improvements over the idea of defining bidirectional converter explicitly using a simple DSL.

The idea is inspired by functional adjuctions, which are a couple of functors that work in opposite direction. So instead of trying to explain to the Json mapper how to serialize/deserialize our class using annotations we define the adjuntion (aka the converter) for each class. Thanks to Kotlin DSL capabilities, it doesn't require much code.

This is the result:

```kotlin
object JProduct : JAny<Product>() {

    val id by JField(Product::id, JInt)
    val long_description by JField(Product::longDesc, JString)
    val short_desc by JField(Product::shortDesc, JString)
    val price by JFieldMaybe(Product::price, JDouble)

    override fun JsonNodeObject.deserializeOrThrow() =
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

    override fun JsonNodeObject.deserializeOrThrow(): Invoice =
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
## How It Works

To you Kondor you need to define a Converter for each type (or class of types).

Let's analyze an example in details:

```kotlin
data class Product(val id: Int, val shortDesc: String, val longDesc: String, val price: Double?) // 1

object JProduct: JAny<Product>() { // 2

    val id by JField(Product::id, JInt) // 3
    val long_description by JField(Product::longDesc, JString) // 4
    val `short-desc` by JField(Product::shortDesc, JString) // 5
    val price by JFieldMaybe(Product::price, JDouble) // 6

    override fun JsonNodeObject.deserializeOrThrow() = // 7
        Product( // 8
            id = +id, //9 
            shortDesc = +`short-desc`,
            longDesc = +long_description,
            price = +price
        )
}
```

1. This is the class we want to serialize/deserialize
2. Here we define the converter, inheriting from a `JAny<T>` where `T` is our type. If we want to serialize a collection we can start from `JList` or `JSet` and so on, we can also create new abstract converters. 
3. Inside the converter we need to define the fields as they will be saved in Json. For each field we need to specify the getter for the serialization and the specific converter needed for its type. If the converter or the getter is not correct it won't compile.
4. The name of the field is taken from the variable name, `long_description` in this case
5. Using ticks we can also use names illegal for variables in Kotlin
6. For nullable/optional fields we use `JFieldMaybe`, otherwise it won't compile.
7. We then need to define the method to create our objects from Json fields. If we are only interested in serialization we can leave the method empty.
8. Here we use the class constructor, but we could have used any function that return a `Product`
9. To get the value from the fields we use the `unaryplus` operator. Since we match the name of parameter with the fields it will be easy to spot any mistake.


## Custom Converters

It's very easy to create new converters to follow your team conventions.

Converters are defined using `JsonNode`, so you don't have to handle the parsing, and the serializing separately (which can be a source of bugs). They are easier to write than other libraries custom serialisers.

There are some converters in Kondor ready-to-use:

- Sealed classes: automatically converting your sealed classes in polymorphic json
- Maps: converting a `Map<String, *>` into a Json object
- Instant: both using epoch or date format
- BigDecimal: you can use numbers of any lenght
- String wrappers: simplify json for IDs and other types that wrap over a string

and so on

You can choose which fields to serialize or even use functions, and for deserialization you don't have to use the constructor.

TODO: example of class with private constructor and custom serializer/deserializer


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

TODO: examples of annotations vs Kondor converters

TODO: comparison of handling errors

TODO: comparison of performance

## Future Ideas

- Generate Json schema and automatically validate

- A DSL for Java
