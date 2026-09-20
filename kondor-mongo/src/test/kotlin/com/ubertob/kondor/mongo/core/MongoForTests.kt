package com.ubertob.kondor.mongo.core

import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.utility.DockerImageName
import java.time.Duration

const val DB_NAME = "MongoKondorTest"

// multi-arch image (amd64/arm64), can be overridden with MONGO_TEST_IMAGE, e.g. to use a mirror registry
private val mongoTestImage: DockerImageName =
    DockerImageName.parse(System.getenv("MONGO_TEST_IMAGE")?.takeUnless { it.isBlank() } ?: "mongo:8.0.32")
        .asCompatibleSubstituteFor("mongo")

fun mongoForTests() =
    MongoDBContainer(mongoTestImage)
        .apply {
            start()
        }


val MongoDBContainer.connection
    get() = MongoConnection(
        connString = getReplicaSetUrl(DB_NAME),
        timeout = Duration.ofMillis(500)
    )
