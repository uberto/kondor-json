package com.ubertob.kondor.mongo.json

import com.mongodb.client.model.Filters
import com.ubertob.kondor.mongo.core.*
import com.ubertob.kondortools.expectSuccess
import org.bson.*
import org.junit.jupiter.api.Test
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.random.Random

@Testcontainers
class JsonBsonCompatibilityTests {

    companion object {
        @Container
        private val mongoContainer = mongoForTests()

        val mongoConnection = mongoContainer.connection
    }

    val onMongo = MongoExecutorDbClient.fromConnectionString(mongoConnection, DB_NAME)

    object ABsonTable : BsonTable() {
        override val collectionName: String = "CompatibilityDocs"
    }

    object ATypedTable : TypedTable<SimpleFlatDoc>(JSimpleFlatDoc) {
        override val collectionName: String = "CompatibilityDocs"
    }

    private val cleanUp = mongoOperation {
        ABsonTable.drop()
    }

    private fun writeDocWithDateTime(index: Int, localDateTime: LocalDateTime) = mongoOperation {
        val doc = BsonDocument(
            listOf(
                BsonElement("index", BsonInt32(index)),
                BsonElement("name", BsonString("myname $index")),
                BsonElement("localDate", BsonDateTime(localDateTime.toEpochSecond(ZoneOffset.UTC))),
                BsonElement("isEven", BsonBoolean(index % 2 == 0))
            )
        )
        ABsonTable.insertOne(
            doc
        )


    }

    private fun readDoc(index: Int) = mongoOperation {
        ABsonTable.find(Filters.eq("index", index)).single()
    }

    @Test
    fun `write using Bson`() {
        val now = LocalDateTime.now()
        val id = Random.nextInt()
        onMongo(cleanUp + writeDocWithDateTime(id, now)).expectSuccess()


        val docRead = onMongo(readDoc(id)).expectSuccess()

        println(docRead) //todo make sure we can read id and date fields with Kondor

//{"_id": {"$oid": "64630bdf1874ed7632114f11"}, "index": -28859791, "name": "myname -28859791", "localDate": {"$date": "1970-01-20T11:50:45.103Z"}, "isEven": false}

    }

}