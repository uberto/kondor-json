package com.gamasoft.kondor.mongo.core

import com.ubertob.kondor.mongo.core.MongoConnection
import org.testcontainers.containers.MongoDBContainer
import java.time.Duration

const val DB_NAME = "MongoKondorTest"

fun mongoForTests() =
    MongoDBContainer("mongo:4.4.4")
        .apply {
            start()
        }


val MongoDBContainer.connection
    get() = MongoConnection(
        connString = getReplicaSetUrl(DB_NAME),
        timeout = Duration.ofMillis(500)
    )
