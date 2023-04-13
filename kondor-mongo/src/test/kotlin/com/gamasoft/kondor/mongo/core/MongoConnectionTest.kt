package com.gamasoft.kondor.mongo.core

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.eq
import com.ubertob.kondor.mongo.core.MongoConnection
import org.bson.BsonDocument
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.junit.jupiter.Container
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.time.Duration


@TestInstance(Lifecycle.PER_CLASS)
class MongoConnectionTest {

    companion object{
        val dbName = "MongoKondorTest"

        @Container
        private val mongoContainer = MongoDBContainer("mongo:4.4.4")
            .apply {
                start()
            }

        val mongoConnection = MongoConnection(
            connString = mongoContainer.getReplicaSetUrl(dbName),
            timeout = Duration.ofMillis(50)
        )

        @JvmStatic
        @AfterAll
        fun stopContainer() {
            mongoContainer.stop()
        }
    }


    private fun connectToMongo(): MongoDatabase {
        val mongoClient: MongoClient = MongoClients.create(mongoConnection.connString)

        val dbs = mongoClient.listDatabases().map { it.keys }

        println("Databases: $dbs")
        return mongoClient.getDatabase(dbName)
    }

    private val collName = "mycoll"

    fun addADoc(database: MongoDatabase, doc: BsonDocument): String {
        val collection: MongoCollection<BsonDocument> = database.getCollection(collName, BsonDocument::class.java)

        return collection.insertOne(doc).insertedId!!.asObjectId().value.toHexString()
    }


    fun allDocs(database: MongoDatabase): Sequence<BsonDocument> {
        val collection: MongoCollection<BsonDocument> = database.getCollection(collName, BsonDocument::class.java)
        return collection.find().asSequence()
    }

    fun findFizzBuzzEq(database: MongoDatabase): Sequence<BsonDocument> {
        val collection: MongoCollection<BsonDocument> = database.getCollection(collName, BsonDocument::class.java)
        return collection.find(
            eq("otherdata.nested.fizzbuzz", "FizzBuzz")
        ).asSequence()
    }

    fun findFizzBuzzParse(database: MongoDatabase): Sequence<BsonDocument> {
        val collection: MongoCollection<BsonDocument> = database.getCollection(collName, BsonDocument::class.java)
        val filter = BsonDocument.parse("""{ "otherdata.nested.fizzbuzz": "FizzBuzz" }""")
        return collection.find(filter).asSequence()
    }

    fun fizzbuzz(i: Int): String =
        when {
            i % 15 == 0 -> "FizzBuzz"
            i % 3 == 0 -> "Fizz"
            i % 5 == 0 -> "Buzz"
            else -> i.toString()
        }

    @Test
    fun `add and query doc`() {
        val db = connectToMongo()
        dropCollection(db, collName)

        val documents = (1..100).map {
            BsonDocument.parse("""
            {
            prog: $it, 
            name: "document $it"
            otherdata: {
                       y: ${it * 2},
                       bool: true
                       nested: {
                               fizzbuzz: "${fizzbuzz(it)}"
                                }
                       }
            array: [1,2,3]           
            }
        """.trimIndent()
            )
        }

        expectThat(db.name).isEqualTo( dbName)
        val ids = documents.map { addADoc(db, it) }
        expectThat(ids.size).isEqualTo(documents.size)

        val docs = allDocs(db)
        expectThat(ids.size).isEqualTo(docs.count())

        val res = findFizzBuzzParse(db)
        expectThat(res.count()).isEqualTo(6)

        val res2 = findFizzBuzzEq(db)
        expectThat(res2.joinToString()).isEqualTo(res.joinToString())
    }

    @Test
    fun `drop collection`() {
        val db = connectToMongo()

        dropCollection(db, collName)
        val docs = allDocs(db)

        expectThat(docs.count()).isEqualTo(0)
    }

    private fun dropCollection(db: MongoDatabase, collName: String) {
        db.getCollection(collName).drop()
    }

}