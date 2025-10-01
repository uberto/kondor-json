# kondor-mongo

This library provides a simple and robust way to interact with MongoDB in Kotlin, by using the functional programming
construct Kleisli for composing MongoDB operations. It is built on top of the official MongoDB Java Driver and the
Kondor Json library.

## Key Features

- Strongly typed MongoDB collection handling
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

### Database Operation Composition

Since each operation on database is function returning a context, it's possible to easily compose them to create complex
operations:

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

Technically speaking, if you really want to know, each database operation returns a `Reader` monad and we are composing
them using Kleisli arrows.

But names are not important, what's nice with this approach is that it allows to easily reuse and test operation on
database without mixing the business logic with the database infrastructure details.

## Running tests locally with Docker (macOS)

The kondor-mongo tests use Testcontainers to spin up a real MongoDB in Docker.
On macOS you just need a working Docker runtime; Testcontainers will pull and run the container automatically.

Steps:
- Install a Docker runtime (choose one):
  - Docker Desktop for Mac: https://www.docker.com/products/docker-desktop/
  - Orbstack: https://orbstack.dev/ (fast, lightweight Docker replacement)
  - Colima (Homebrew): `brew install colima docker` then start with `colima start`.
- Start your Docker runtime so the Docker daemon is running.
- From the project root, run: `./gradlew :kondor-mongo:test`

Notes:
- The tests use the image `mongo:6.0.14`, which has native ARM64 support and works on Apple Silicon and Intel Macs.
- If using Colima and you prefer x86_64 images (slower), you can start with: `colima start --arch x86_64`.
- Testcontainers needs to start helper containers (Ryuk). If you see permission errors, ensure your user can access the Docker socket and that the runtime is running.
- To enable container reuse across test runs (optional), create a file `~/.testcontainers.properties` with:
  `testcontainers.reuse.enable=true`
  Then, start your Docker runtime once and keep it running between test runs.

Troubleshooting:
- "Cannot connect to Docker": make sure Docker Desktop/Orbstack/Colima is running.
- "Image not found or platform mismatch": ensure you are on a recent Docker and that the image tag supports your architecture (we pin 6.0.14 for ARM64 support).
- Network issues when pulling images: try `docker pull mongo:6.0.14` manually to verify connectivity.

## Running tests locally with Docker (Linux)

- Install Docker Engine: https://docs.docker.com/engine/install/
- Optional post-install steps to run docker as non-root: https://docs.docker.com/engine/install/linux-postinstall/
- Ensure the Docker daemon is running: `sudo systemctl status docker` (or your distro's init system).
- Run tests: `./gradlew :kondor-mongo:test`

## Running tests locally with Docker (Windows)

- Install Docker Desktop for Windows: https://www.docker.com/products/docker-desktop/
- Enable WSL2 backend during setup and ensure a Linux distro is installed/enabled in Docker Desktop Settings > Resources > WSL Integration.
- Start Docker Desktop and verify `docker run hello-world` works in a PowerShell or WSL shell.
- Run tests from your project directory: `./gradlew :kondor-mongo:test`

## Image/platform notes

- Tests default to `mongo:6.0.14`, a multi-arch image. Docker will pull the correct variant for your platform (amd64/arm64) automatically.
- You can override the image via environment variable if needed (e.g., to use a corporate mirror):
  - macOS/Linux: `MONGO_TEST_IMAGE=my-registry.example.com/mongo:6.0.14 ./gradlew :kondor-mongo:test`
  - Windows PowerShell: `$env:MONGO_TEST_IMAGE="my-registry.example.com/mongo:6.0.14"; ./gradlew :kondor-mongo:test`

## ToDo

- migration hook (update to latest version in the find)
