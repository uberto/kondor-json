# kondor-mongo

This library provides a simple and robust way to interact with MongoDB in Kotlin, by using the functional programming
construct Kleisli for composing MongoDB operations. It is built on top of the official MongoDB Java Driver and the
Kondor Json library.

## Key Features

- Strongly typed MongoDB collection handling
- Composition of MongoDB operations using Kleisli arrows
- Execution of composed operations in a monadic context, yielding a functional pipeline for data handling
- Simple and straightforward API for CRUD operations
- Full control over error handling

## Usage

### Defining Documents

Documents are defined as Kotlin data classes. Here is an example `SimpleFlatDoc` document:

```kotlin
data class SimpleFlatDoc(
    val index: Int,
    val name: String,
    val date: LocalDate,
    val bool: Boolean
)
```

### Defining Collection

Collections are defined as objects extending the `TypedTable<T>` class and passing the specific Kondor converter in the
constructor:

```kotlin
object JSimpleFlatDoc : JAny<SimpleFlatDoc>() {

    val index by num(SimpleFlatDoc::index)
    val name by str(SimpleFlatDoc::name)
    val localDate by str(SimpleFlatDoc::date)
    val isEven by bool(SimpleFlatDoc::bool)

    override fun JsonNodeObject.deserializeOrThrow() = SimpleFlatDoc(
        index = +index,
        name = +name,
        date = +localDate,
        bool = +isEven
    )
}

object FlatDocs : TypedTable<SimpleFlatDoc>(JSimpleFlatDoc) {
    override val collectionName: String = "FlatDocs"
}
```

### Querying Documents

To query a document from a collection, use the `find` method provided by the `TypedTable` class:

```kotlin
fun docQuery(index: Int): MongoReader<SimpleFlatDoc> =
    mongoOperation {
        FlatDocs.find(JSimpleFlatDoc.index eq index)
            .firstOrNull()
    }
```

MongoDB filters operations are translated in infix operation over the Kondor converter properties.

### Writing Documents

To write a document into a collection, use the `insertOne` method provided by the `TypedTable` class:

```kotlin
fun docWriter(doc: SimpleFlatDoc): MongoReader<Unit> =
    mongoOperation {
        FlatDocs.insertOne(doc)
    }
```

### Updating Documents

To update a document based on an attribute, use the `updateMany` method and provide a filter and an update method:

```kotlin
fun updateMany(indexes: List<Int>): MongoReader<Long> =
    mongoOperation {
        FlatDocs.updateMany(
            JSimpleFlatDoc.index `in` indexes,
            Updates.set("name", "updated doc")
        )
    } //returns the number of doc updated
```

### Dropping a collection

To delete all elements in a collection, use the `drop` method on the `TypedTable` without any parameter:

```kotlin
val cleanUp: MongoReader<Unit> = mongoOperation {
    FlatDocs.drop()
}
```

### Kleisli Composition

The Kleisli arrows allow you to compose your operations easily:

```kotlin
@Test
fun `add and query doc safely`() {
    val myDoc = onMongo(
        cleanUp +
                docWriter(doc) +
                docQuery(doc.index)
    ).expectSuccess()
    expectThat(doc).isEqualTo(myDoc)
}
```

In the above example, we compose the `cleanUp`, `docWriter`, and `docQuery` operations into a single operation using
the `+` operator.

You can also use the `bind` function to chain operations, and execute different operations based on the result of the
previous operation:

```kotlin
@Test
fun `operate on results`() {
    val docIndex = Random.nextInt(100)

    val myDoc = onMongo(
        hundredDocWriter
            .bind { docQuery(docIndex) }
            .transformIfNotNull(::extractInfo)
    ).failIfNull { NotFoundError("The doc $docIndex is not present!") }
        .expectSuccess()
    expectThat(myDoc).contains(docIndex.toString())
}
```

## ToDo

- migration hook (update to latest version in the find)
