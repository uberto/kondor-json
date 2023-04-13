package com.gamasoft.kondor.mongo.json

import com.gamasoft.kondor.mongo.core.MongoConnectionTest.Companion.dbName
import com.gamasoft.kondor.mongo.core.MongoConnectionTest.Companion.mongoConnection
import com.ubertob.kondor.mongo.core.MongoConnection
import com.ubertob.kondor.mongo.core.MongoExecutor
import com.ubertob.kondor.mongo.core.TypedTable
import com.ubertob.kondor.mongo.core.mongoOperation
import com.ubertob.kondortools.expectSuccess
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.junit.jupiter.Container
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.time.Duration
import java.time.LocalDate

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FlatDocTableTest {

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

    private object FlatDocs: TypedTable<SimpleFlatDoc>(JSimpleFlatDoc) {
        override val collectionName: String = "FlatDocs"
        //retention... policy.. index
    }

    val provider = MongoExecutor(mongoConnection, dbName)

    private fun createDoc(i: Int): SimpleFlatDoc =
        SimpleFlatDoc(
            index = i,
            name = "mydoc $i",
            date = LocalDate.now().minusDays(i.toLong()),
            bool = i % 2 == 0
        )

    private val doc = createDoc(0)

    val oneDocReader = mongoOperation {
        FlatDocs.drop()
        FlatDocs.addDocument(doc)
        val docs = FlatDocs.all()
        expectThat(1).isEqualTo( docs.count())
        docs.first()
    }

    val docQueryReader = mongoOperation {
        (1..100).forEach {
            FlatDocs.addDocument(createDoc(it))
        }
        FlatDocs.find("{ index: 42 }").first()
    }


    @Test
    fun `add and query doc safely`() {

        val myDoc = provider(oneDocReader).expectSuccess()
        expectThat(doc).isEqualTo( myDoc)

    }


    @Test
    fun `parsing query safely`() {

        val myDoc = provider(docQueryReader).expectSuccess()
        expectThat(42).isEqualTo( myDoc.index)

    }
}


