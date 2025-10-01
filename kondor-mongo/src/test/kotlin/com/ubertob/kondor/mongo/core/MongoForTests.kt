package com.ubertob.kondor.mongo.core

import org.testcontainers.containers.MongoDBContainer
import java.time.Duration

const val DB_NAME = "MongoKondorTest"

private fun resolveMongoTestImage(): String {
    // Allow override from environment for edge cases (e.g., corporate registries)
    val override = System.getenv("MONGO_TEST_IMAGE")
    if (!override.isNullOrBlank()) return override

    // Default to a multi-arch image tag that works on Linux/Windows/macOS (Apple Silicon and Intel)
    // Testcontainers will pull the correct platform variant via Docker's manifest list.
    return "mongo:6.0.14"
}

fun mongoForTests() =
    MongoDBContainer(resolveMongoTestImage())
        .apply {
            start()
        }


val MongoDBContainer.connection
    get() = MongoConnection(
        connString = getReplicaSetUrl(DB_NAME),
        timeout = Duration.ofMillis(500)
    )
