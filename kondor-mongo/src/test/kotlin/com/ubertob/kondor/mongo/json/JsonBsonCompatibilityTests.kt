package com.ubertob.kondor.mongo.json

import com.mongodb.client.model.Filters
import com.ubertob.kondor.mongo.core.*
import com.ubertob.kondortools.expectSuccess
import org.bson.*
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import strikt.api.expectThat
import strikt.assertions.isEqualTo
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

    private fun readDocById(id: String) = mongoOperation {
        ABsonTable.findByOid(ObjectId(id))
    }

    @Test
    fun `write using Bson`() {
        val now = LocalDateTime.now()
        val id = Random.nextInt()
        onMongo(cleanUp + writeDocWithDateTime(id, now)).expectSuccess()

        onMongo(readDoc(id)).expectSuccess() //printIt()

    }

    @Test
    fun `write using Bson special fields`() {
        val docId = """64630bdf1874ed7632114f11"""
        val bsonJson =
            """{"_id": {"${MongoSpecialFields.oid}": "$docId"}, "index": 12345, "name": "myname 12345", "localDate": {"${MongoSpecialFields.date}": "1970-01-20T11:50:45.103Z"}, "isEven": false}"""

        val bsonDoc = BsonDocument.parse(bsonJson)
        val writeCustomDoc = mongoOperation {
            ABsonTable.insertOne(bsonDoc)
        }
        onMongo(cleanUp + writeCustomDoc).expectSuccess()

        val doc = onMongo(readDocById(docId)).expectSuccess() ?: fail("No doc found!")

        expectThat(doc["name"]?.asString()?.value).isEqualTo("myname 12345")

    }

}