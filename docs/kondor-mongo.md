# kondor-mongo Module

## Purpose

The `kondor-mongo` module stores and reads domain objects in MongoDB using the same Kondor converters used for Json.
Database operations are plain values (`MongoOperation<T>`) that can be composed and then run by an executor, which
returns an `Outcome` instead of throwing exceptions. It is built on the official MongoDB Java sync driver.

See also [kondor-mongo/README.md](../kondor-mongo/README.md) for a step-by-step introduction.

## Key Components

All in `com.ubertob.kondor.mongo.core` and `com.ubertob.kondor.mongo.json`.

| Component                          | Role                                                                                 |
|------------------------------------|--------------------------------------------------------------------------------------|
| `TypedTable<T>(converter)`         | A collection of `T`, mapped with an object converter (a `JAny`, see below)           |
| `BsonTable`                        | A collection of raw `BsonDocument`, without a converter                              |
| `MongoOperation<T>`                | An operation on the database, not yet executed (a Reader over `MongoSession`)        |
| `mongoOperation { ... }`           | Builds an operation; inside the block you can call the `MongoSession` methods        |
| `mongoCalculation { input -> ...}` | Builds a function from an input to an operation, for composition                     |
| `MongoSession`                     | The operations available on a table: `insertOne`, `find`, `updateMany`, `bulkWrite`… |
| `MongoExecutor`                    | Runs an operation; `MongoExecutorDbClient` is the implementation using `MongoClient` |
| `MongoConnection`                  | Connection string and timeout, used to create a `MongoExecutorDbClient`             |
| `MongoOutcome<T>`                  | `Outcome<MongoError, T>`, the result of running an operation                         |
| Filters (`eq`, `lt`, `gt`, `in`…)  | Infix functions on the converter fields, producing Mongo `Bson` filters              |
| `JObjectId`                        | Converter for MongoDB `ObjectId`, with the `str(...)` field function                 |

## Usage Examples

### Defining a Collection

```kotlin
data class Person(val id: Int, val name: String)

object JPerson : JAny<Person>() {
    val id by num(Person::id)
    val name by str(Person::name)

    override fun JsonNodeObject.deserializeOrThrow() =
        Person(id = +id, name = +name)
}

object People : TypedTable<Person>(JPerson) {
    override val collectionName: String = "People"
}
```

The converter fields are public so they can be used in filters.

MongoDB adds an `_id` field to every document: both `JAny` and `JObj` converters ignore it, unless they declare it.

A `Decimal128` field is read by the driver as `{"$numberDecimal": "9.99"}`, a number between quotes, which kondor
refuses since 4.2.0: read that field with a converter reading a String, such as a `JStringRepresentable<BigDecimal>`.

Documents are written with all numbers as doubles, so an `Int` field is stored as `7.0`. A `JAny` reads it back
correctly, while a `JObj` fails on the integer fields: use `JAny` converters for tables with `Int` or `Long` fields.

### Writing and Querying

```kotlin
fun addPerson(person: Person): MongoOperation<Unit> =
    mongoOperation {
        People.insertOne(person)
    }.ignoreValue()

fun findPerson(id: Int): MongoOperation<Person?> =
    mongoOperation {
        People.find(JPerson.id eq id).firstOrNull()
    }

fun renameAll(ids: List<Int>): MongoOperation<Long> =
    mongoOperation {
        People.updateMany(JPerson.id `in` ids, Updates.set("name", "renamed"))
    }
```

### Composing and Running

```kotlin
val onMongo = MongoExecutorDbClient.fromConnectionString(
    MongoConnection("mongodb://localhost:27017"),
    "MyDatabase"
)

val result: MongoOutcome<Person?> = onMongo(addPerson(Person(1, "Alice")) + findPerson(1))
```

Operations are combined with `+` (or `combineWith` and `bind`), and nothing touches the database until the combined
operation is executed by calling the executor: `onMongo(operation)`, or `operation exec onMongo` when the result is not
nullable.

`find` returns a lazy `Sequence`: consume it inside the operation (`firstOrNull()`, `toList()`...), otherwise the
documents are read, and conversion errors thrown, after the executor has returned.

## Error Handling

Running an operation returns `MongoOutcome<T>`, with an error from the sealed `MongoError`. `MongoErrorException`
wraps any exception thrown while running the operation: driver errors (connection, writes...), and also documents that
the converter could not read inside `find` and the other query methods. `MongoConversionError` is the error returned by
`MongoTable.fromBsonDoc`.

## BSON Conversion

`TypedTable` writes objects with the converter's `toJsonNode`, then converts the node to BSON
(`JsonNodeObject.toBsonDocument()`). It reads documents by rendering them as Mongo extended Json (`BsonDocument.toJson()`)
and parsing that with the converter's `fromJson`, so special types appear as objects like `{"$oid": ...}` and
`{"$date": ...}` (see `MongoSpecialFields` and `JObjectId`).

## Testing

The module tests use Testcontainers, so they need a running Docker daemon: `./gradlew :kondor-mongo:test`.
They run against the multi-arch `mongo:8.0.32` image by default; set `MONGO_TEST_IMAGE` to use another image (for
example from a mirror registry). See `kondor-mongo/README.md` for per-platform Docker setup.
