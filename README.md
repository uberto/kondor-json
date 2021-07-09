[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.ubertob.kondor/kondor-core/badge.svg?style=plastic)](https://maven-badges.herokuapp.com/maven-central/com.ubertob.kondor/kondor-core)

# KondorJson

A functional Kotlin library to serialize/deserialize Json fast and safely without using reflection, annotations or code generation.

With Kondor you need to define how the Json would look like for each of the types you want to persist, using an high level DSL.

No need of custom Data Transfer Objects, and custom serializer, no matter how complex is the Json format you need to use. You can also define more than one converter for each class if you want to have multiple formats for the same types, for example in case of versioned api or different formats for Json in HTTP and persistence.

## Dependency declaration

Maven

```xml

<dependency>
   <groupId>com.ubertob.kondor</groupId>
   <artifactId>kondor-core</artifactId>
   <version>1.6.0</version>
</dependency>
```

Gradle
```groovy
implementation 'com.ubertob.kondor:kondor-core:1.6.0'
```

## The Video Presentation

[![Watch the video](https://secure.meetupstatic.com/photos/event/2/2/c/0/highres_496289655.jpeg)](https://www.youtube.com/watch?v=hIKruBc6aeg&t=3200s)

## Quick Start

To transform a value (string in this case) into and from Json:
```kotlin
val jsonString = JString.toJson("my string is here")
val value = JString.fromJson(jsonString).orThrow()  //"my string is here"
```

`JString` is the Json decoder, there are others for all primitive types

To transform an object we need to write the decoder first with a simple DSL:

```kotlin
data class Customer(val id: Int, val name: String)

object JCustomer : JAny<Customer>() {

   val id by num(Customer::id)
   val name by str(Customer::name)

   override fun JsonNodeObject.deserializeOrThrow() =
      Customer(
         id = +id,
         name = +name
      )
}
```

Each field (id,name) need to be associated to a decoder and a field in the mapped object. Then we need to explicitely call the function to create the object from the fields.

## Do We Need another Json Parser?

We wrote this library to solve a specific problem. It was useful for us, so there could be other people that could find this beneficial.

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
data class Person(val id: Int, val name: String)

data class Product(val id: Int, val shortDesc: String, val longDesc: String, val price: Double?)

data class InvoiceId(override val raw: String) : StringWrapper

data class Invoice(
   val id: InvoiceId,
   val vat: Boolean,
   val customer: Person,
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

So we did several progressive improvements over the idea of defining bidirectional converter explicitly using a simple
DSL.

Theoretically each converter is a profunctor, which is a special kind of bifunctor where one of the functors is covariant and the other is contravariant. 
So instead of trying to explain to the Json mapper how to serialize/deserialize our class using annotations we define the converter (technically a profunctor) for each class. Thanks to Kotlin DSL capabilities, it doesn't require much code.

And here is the result, we need to describe the Json format using a converter object called Jxxx where xxx is the name
of your class:

```kotlin
object JProduct : JAny<Product>() {

   private val id by num(Product::id)
   private val long_description by str(Product::longDesc)
   private val `short-desc` by str(Product::shortDesc)
   private val price by num(Product::price)

   override fun JsonNodeObject.deserializeOrThrow() =
      Product(
         id = +id,
         shortDesc = +`short-desc`,
         longDesc = +long_description,
         price = +price
      )
}

object JInvoice : JAny<Invoice>() {

   private val id by str(::InvoiceId, Invoice::id)
   private val `vat-to-pay` by bool(Invoice::vat)
   private val customer by obj(JPerson, Invoice::customer)
   private val items by array(JProduct, Invoice::items)
   private val total by num(Invoice::total)

   override fun JsonNodeObject.deserializeOrThrow(): Invoice =
      Invoice(
         id = +id,
         vat = +`vat-to-pay`,
         customer = +customer,
         items = +items,
         total = +total,
         created = +created_date,
         paid = +paid_datetime
      )

}
```

Then in your code you only need to invoke the converter:

```kotlin
val invoice: Invoice = JInvoice.fromJson(invoiceJsonString)

val json: String = JInvoice.toPrettyJson(invoice) 
```

The converters itself can be generated from the domain classes, you only have to copy and paste them in your code base,
and adapting them as you need. Note that there is no automatic update if you change the data class, this is the whole
point of Kondor-Json.

To have a play with generators, look at `kondor-tools` module.

Comparing with a solution involving writing DTOs, you need to much less code using converters (especially if you use the
generator). Even without considering DTOs, the time needed to write the converters is roughly the same than to annotate
the classes one by one, but it's easier and more IDE friendly to create the converter. For example if you
attach `JField` to a nullable field of your domain class, it will not compile.

No need to browse StackOverflow to find the right annotation, IDE can suggest the possible converters or you can write
new ones.

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

object JProduct : JAny<Product>() { // 2

   val id by num(Product::id) // 3
   val long_description by str(Product::longDesc) // 4
   val `short-desc` by str(Product::shortDesc) // 5
   val price by num(Product::price) // 6

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
2. Here we define the converter, inheriting from a `JAny<T>` where `T` is our type. If we want to serialize a collection
   we can start from `JList` or `JSet` and so on, we can also create new abstract converters.
3. Inside the converter we need to define the fields as they will be saved in Json. For each field we need to specify
   the getter for the serialization, inside a function that represent the kind of Json node (boolean, number,
   string,array, object) and the specific converter needed for its type. If the converter or the getter is not correct
   it won't compile.
4. The name of the field is taken from the variable name, `long_description` in this case
5. Using ticks we can also use names illegal for variables in Kotlin
6. Nullable/optional fields are handled automatically.
7. We then need to define the method to create our objects from Json fields. If we are only interested in serialization
   we can leave the method empty.
8. Here we use the class constructor, but we could have used any function that return a `Product`
9. To get the value from the fields we use the `unaryplus` operator. It is easy to spot any mistake since we match the
   name of parameter with the fields.

## No Exceptions

When failing to parse a Json, Kondor is not throwing any exception, instead `fromJson` and `fromJsonNode` methods return
an `Outcome<T>` instead of a simple `T`. Why is that?

`Outcome` is an example of the *Either* monad for error handling pattern, if you are not familiar with it, here is how
to use it.

There are 5 ways to handle errors depending on the case:

1. orThrow()

```kotlin
JCustomer.parseJson(jsonString).orThrow()
```

this throw an exception if there is an error.

1. orNull()

```kotlin
JCustomer.parseJson(jsonString).orNull()
   ?.let { customer ->
      //do something only if successful
   }

```

this return null if there is an error, not great because the error is lost but it can be convenient sometime.

1. onFailure{}

```kotlin
val customer = JCustomer.parseJson(jsonString)
   .onFailure { error ->
      log(error)
      return
   }
```

using `onFailure` we can return from the calling function (non-local return) in case of errors.

1. transform{} + recover{}

```kotlin
val htmlPage = JCustomer.parseJson(jsonString)
   .transform { customer ->
      display(customer)
   }.recover { error ->
      display(error)
   }
```

using `transform` we can convert the `Outcome<Customer>` to something else, for example a `Outcome<HtmlPage>`, then
using `recover` we can convert the error result to the same type and remove the `Outcome`.

This is my favorite way to handle errors.

There are many other implementations of the Either monad in Kotlin (Result4k, Arrows, Kotlin-Result etc...) if you
already are using one of these, you can easily convert Kondor `Outcome` to your specific result type. As for me, I
choose a different name to avoid clashing with `Result` in the Kotlin library which work differently but it will always
be imported first by the IDE. I also don't like `map` and `flatmap` be identical to the collections methods because when
using a collection of results it becomes very confusing.

## Special Cases

With Kondor is easy to solve all Json mappings, including not so trivial ones. For example:

### Enums

Enums are automatically transformed in strings. For example with these types:

```kotlin
enum class TaxType { Domestic, Exempt, EU, US, Other }

data class Company(val name: String, val taxType: TaxType)
```

You can create this converter:

```kotlin
object JCompany : JAny<Company>() {

   private val name by str(Company::name)
   private val tax_type by str(Company::taxType)

   override fun JsonNodeObject.deserializeOrThrow() =
      Company(
         name = +name,
         taxType = +tax_type
      )
}
```

And it will be mapped to this Json format:

```json
{
   "name": "Company Name",
   "tax_type": "Domestic"
}
```

### Sealed classes and polymorphic Json

To store in Json a sealed class, or an interface with a number of known implementations you can use the `JSealed` base
converter.

For example assuming `Customer` can be either a `Person` or a `Company`:

```kotlin
sealed class Customer()
data class Person(val id: Int, val name: String) : Customer()
data class Company(val name: String, val taxType: TaxType) : Customer()
```

You just need to map each converter to a string and (optionally) specifiy the name of the discriminator field:

```kotlin
object JCustomer : JSealed<Customer> {

    override val discriminatorFieldName = "type"
   
    override val subtypesJObject: Map<String, JObject<out Customer>> =
        mapOf(
            "private" to JPerson,
            "company" to JCompany
        )

   override fun extractTypeName(obj: Customer): String =
      when (obj) {
         is Person -> "private"
         is Company -> "company"
      }
}
```

It will be mapped in a Json like this:

```json
{
   "type": "private",
   "id": 1,
   "name": "ann"
}
```

Where "type" here is the discriminator field.

### Flatten Fields

Let's say we have a class `FileInfo` that maps to this json format:

```json
{
   "name": "filename",
   "date": 0,
   "size": 123,
   "folderPath": "/"
}
```

Now we need to create a type which is same as `FileInfo` but with a boolean field added:

```kotlin
data class SelectedFile(val selected: Boolean, val file: FileInfo)
```

Writing a converter we will get this json format:

```json
{
   "selected": true,
   "file": {
      "name": "filename",
      "date": 0,
      "size": 123,
      "folderPath": "/"
   }
}
```

But instead we want something simpler:

```json
{
   "selected": true,
   "name": "filename",
   "date": 0,
   "size": 123,
   "folderPath": "/"
}
```

With Kondor is easy to do this using the `flatten` format:

```kotlin
object JSelectedFile : JAny<SelectedFile>() {

   val selected by bool(SelectedFile::selected)
   val file_info by flatten(JFileInfo, SelectedFile::file)

   override fun JsonNodeObject.deserializeOrThrow() =
      SelectedFile(
         +selected,
         +file_info
      )

}
```

Note that it only works with non-nullable fields and it requires that there are no fields with same name on `SelectedFile` and `FileInfo`.

### Storing a Map as Json

Let's say you have a field which is a map and you want to save it as a Json object.

For example a map of things to do, with a short key and a longer description:

```kotlin
data class Notes(val updated: Instant, val thingsToDo: Map<String, String>)
```

You just need to use the `JMap` converter and passing it the converter for the value type of the Map (the keys have to be `String` because of Json syntax):

```kotlin
object JNotes : JAny<Notes>() {
   private val updated by str(Notes::updated, JInstantD)
   private val things_to_do by obj(JMap(JString), Notes::thingsToDo)

   override fun JsonNodeObject.deserializeOrThrow() =
      Notes(
         updated = +updated,
         thingsToDo = +things_to_do
      )
}
```

The result will be a Json like this:

```json
{
   "updated": "2021-03-26T19:19:20.093501Z",
   "things_to_do": {
      "something": "lorem ipsum",
      "something else": "Lorem ipsum dolor sit amet",
      "another thing to do": "Lorem ipsum dolor sit amet, consectetur adipiscing elit",
      "ditto": "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididun"
   }
}
```

### Custom collections

If you have a custom collection:

```kotlin
class Products : ArrayList<Product>() {
   fun total(): Double = sumOf { it.price ?: 0.0 }

   companion object {
        fun fromIterable(from: Iterable<Product>): Products =
            from.fold(Products()) { acc, p -> acc.apply { add(p) } }
    }
}
```

You can easily create a converter for it that will render it as a normal array:

```kotlin
object JProducts : JArray<Product, Products>() {
   override val helper = JProduct

   override fun convertToCollection(from: Iterable<Product>) =
      Products.fromIterable(from)

}
```

And it will be rendered as a standard Json array:

```json
[
   {
      "id": 175,
      "long_description": "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididun",
      "short-desc": "Good Stuff",
      "price": 223.23
   },
   {
      "id": 281,
      "long_description": "Lorem ipsum dolor sit amet, consectetur adipiscing elit",
      "short-desc": "Free Stuff"
   }
]
```

### Xml->Json Format

When it comes to convert an Xml format to json, a common practice is to map the Xml nodes to a Json array of objects with a single field that contains the type of the object as key and the object content as value.

For example an extract of the Liquibase Json format to describe a ChangeSet with two changes, one of type `addColumn`and the other one of type `addLookupTable`:

```json
{
   "changeSet": {
      "id": "1",
      "changes": [
         {
            "addColumn": {
               "tableName": "person",
               "columns": [
                  {
                     "column": {
                        "name": "username",
                        "type": "varchar(8)"
                     }
                  }
               ]
            }
         },
         {
            "addLookupTable": {
               "existingTableName": "person",
               "existingColumnName": "state",
               "newTableName": "state",
               "newColumnName": "id",
               "newColumnDataType": "char(2)"
            }
         }
      ]
   }
}
```

It is quite complicated to parse this format, either using reflection, annotations or generated classes.

Once we have the types to represent that format:

```kotlin
 data class ChangeSet(val id: String, val author: String, val changes: List<Change>) : ChangeLogItem

sealed class Change {
   data class CreateTable(val tableName: String, val columns: List<Column>) : Change()
   data class AddColumn(val tableName: String, val columns: List<Column>) : Change()
   data class AddLookupTable(
      val existingTableName: String,
      val existingColumnName: String,
      val newTableName: String,
      val newColumnName: String,
      val newColumnDataType: String
   ) : Change()
}
```

With Kondor we can abstract on the protocol, and we can create a specific converter for this format, let's call it `NestedPolyConverter`.

We can now easily define our converters using the new format that will parse and output the Json example correctly:

```kotlin
object JChangeSet : JAny<ChangeSet>() {
   val id by str(ChangeSet::id)
   val author by str(ChangeSet::author)
   val changes by array(JChange, ChangeSet::changes)

   override fun JsonNodeObject.deserializeOrThrow() =
      ChangeSet("id", "author", emptyList())
}

object JChange : NestedPolyConverter<Change> {

   override fun extractTypeName(obj: Change): String =
      when (obj) {
         is Change.AddColumn -> "addColumn"
         is Change.AddLookupTable -> "addLookupTable"
         is Change.CreateTable -> "createTable"
      }

   override val subConverters = mapOf(
      "addColumn" to JAddColumn,
      "addLookupTable" to JAddLookupTable,
      "createTable" to JCreateTable
   )
}
```

## Custom Converters

It's very easy to create new converters to follow your team conventions.

Converters are defined using `JsonNode`, so you don't have to handle the parsing, and the serializing separately (which can be a source of bugs). They are easier to write than other libraries custom serialisers.

There are some converters in Kondor ready-to-use:

- Sealed classes: automatically converting your sealed classes in polymorphic json
- Maps: converting a `Map<String, *>` into a Json object
- Instant: both using epoch or date format
- BigDecimal: you can use numbers of any length
- String wrappers: simplify json for IDs and other types that wrap over a string

and so on...

You can choose which fields to serialize or even use functions, and for deserialization you don't have to use the
constructor.

TODO: example of class with private constructor and custom serializer/deserializer

## Integration with Http4k

Using Kondor it's easy to integrate with Http4k since they use the same functional approach.

For example you can create a `BodyLens` directly from a JConverter using an ext function like this:

```kotlin
fun <T : Any> JConverter<T>.toBodyLens(vararg metas: Meta): BiDiBodyLens<T> =
   httpBodyRoot(metas.toList(), APPLICATION_JSON, ContentNegotiation.None)
      .map({ fromJson(it.payload.asString()).orThrow() }, { Body(toJson(it)) })
      .toLens()
```

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

## Ideas for Future Features (PRs welcome)

- Generate Json schema and automatically validate

- A DSL for Java

- Generating random values from the converters

- Add a converter that use Jackson for simplify the migration/adopting

- Add integration with Snodge for fuzzy testing

## Profunctor

I've got the inspiration for Kondor while studying Adjoint functors. But further studies showed me that Kondor two functors don't form an adjunction but instead they form a profunctor.

Profunctor and Adjunctions are a fascinating part of Category Theory, you can find some more materials about them here:

https://en.wikipedia.org/wiki/Profunctor

https://typeclasses.com/profunctors

https://www.schoolofhaskell.com/school/to-infinity-and-beyond/pick-of-the-week/profunctors


https://en.wikipedia.org/wiki/Adjoint_functors

https://bartoszmilewski.com/2016/04/18/adjunctions/

https://www.youtube.com/watch?v=TNtntlVo4LY

https://www.youtube.com/watch?v=TnV9SQGPcLY
