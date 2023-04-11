package com.gamasoft.kondor.mongo.core

import com.ubertob.kondortools.expectSuccess
import org.bson.BsonDocument
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.util.*

private object collForTest: BsonTable() {
    override val collectionName: String = "collForTest"
    //retention... policy.. index
}

class MongoExecutorTest {

    val uuid = UUID.randomUUID()
    val doc = BsonDocument.parse(
        """{
            docId: "$uuid"
            name: "test document"
            nested: {
                       int: 42,
                       double: 3.14
                       bool: true
                     }
            array: [1,2,3,4]   
            }
        """.trimIndent()
    )

    fun createDoc(index: Int) = BsonDocument.parse(
        """{
            parentId: "$uuid"
            name: "subdoc${index}"
            index: $index
        }""".trimIndent()
    )

    val oneDocReader = mongoOperation {
        collForTest.drop()
        collForTest.addDocument(doc)
        val docs = collForTest.all()
        expectThat(1).isEqualTo( docs.count())
        docs.first()
    }

    val dropCollReader = mongoOperation {
        collForTest.drop()
        collForTest.all().count()
    }

    val docQueryReader = mongoOperation {
        (1..100).forEach {
            collForTest.addDocument(createDoc(it))
        }
        collForTest.find("{ index: 42 }").first()
    }

    private val mongoConnection = MongoConnection("mongodb://localhost:27017")

    private val dbName = "MongoProvTest"

    @Test
    fun `add and query doc safely`() {
        val provider = MongoExecutor(mongoConnection, dbName)

        val outcome = oneDocReader exec provider
        val myDoc = outcome.expectSuccess()
        expectThat(doc).isEqualTo(myDoc)
    }

    @Test
    fun `drop collection safely`() {
        val provider = MongoExecutor(mongoConnection, dbName)

        val tot: Int = provider(dropCollReader).expectSuccess()
        expectThat(0).isEqualTo( tot)
    }

    @Test
    fun `return error in case of wrong connection`() {
        val provider = MongoExecutor(MongoConnection("mongodb://localhost:12345"), dbName)

        val res = provider(dropCollReader)
        assertTrue(res.toString().contains("MongoErrorException"))
    }

    @Test
    fun `parsing query safely`() {
        val provider = MongoExecutor(mongoConnection, dbName)

        val myDoc = provider(docQueryReader).expectSuccess()
        expectThat(42).isEqualTo( myDoc["index"]!!.asInt32().value)
    }
}