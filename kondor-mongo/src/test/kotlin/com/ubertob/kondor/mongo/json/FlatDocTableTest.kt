package com.ubertob.kondor.mongo.json

import com.mongodb.client.model.Filters
import com.ubertob.kondor.mongo.core.*
import com.ubertob.kondor.outcome.OutcomeError
import com.ubertob.kondor.outcome.failIfNull
import com.ubertob.kondortools.expectSuccess
import org.junit.jupiter.api.Test
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import java.time.LocalDate
import kotlin.random.Random

@Testcontainers
class FlatDocTableTest {
    companion object {
        @Container
        private val mongoContainer = mongoForTests()

        val mongoConnection = mongoContainer.connection
    }

    private object FlatDocs : TypedTable<SimpleFlatDoc>(JSimpleFlatDoc) {
        override val collectionName: String = "FlatDocs"
        //retention... policy.. index
    }

    val onMongo = MongoExecutorDbClient(mongoConnection, DB_NAME)

    private fun createDoc(i: Int): SimpleFlatDoc =
        SimpleFlatDoc(
            index = i,
            name = "mydoc $i",
            date = LocalDate.now().minusDays(i.toLong()),
            bool = i % 2 == 0
        )

    private val doc = createDoc(0)

    val cleanup: ContextReader<MongoSession, Unit> =
        mongoOperation {
            FlatDocs.drop()
        }

    fun reader(id: Int) = mongoOperation {
        FlatDocs.find(Filters.eq("index"))
    }

    fun docWriter(doc: SimpleFlatDoc): ContextReader<MongoSession, Unit> =
        mongoOperation {
            FlatDocs.addDocument(doc)
        }

    fun docQuery(index: Int): ContextReader<MongoSession, SimpleFlatDoc?> =
        mongoOperation {
            FlatDocs.find("{ index: $index }").firstOrNull()
        }

    val hundredDocWriter: ContextReader<MongoSession, Long> =
        mongoOperation {
            (1..100).forEach {
                FlatDocs.addDocument(createDoc(it))
            }
            FlatDocs.countDocuments()
        }


    @Test
    fun `add and query doc safely`() {

        val myDoc = onMongo(
            cleanup +
                    docWriter(doc) +
                    docQuery(doc.index)
        ).expectSuccess()
        expectThat(doc).isEqualTo(myDoc)

    }

    fun extractInfo(doc: SimpleFlatDoc): String = "${doc.date}-${doc.name}#${doc.index}"

    class NotFoundError(override val msg: String) : OutcomeError


    @Test
    fun `operate on results`() {
        val docIndex = Random.nextInt(100)

        val myDoc = onMongo(
            hundredDocWriter
                .bind { docQuery(docIndex) }
                .transformIfNotNull(::extractInfo)
        ).failIfNull { NotFoundError("The doc $docIndex is not present!") }
            .expectSuccess()
        expectThat(myDoc).contains(docIndex.toString())

    }

//    @Test
//    fun `findOnexxx methods work correctly`() {
//
//        val myDocs = onMongo(cleanUp + write100Audits).expectSuccess()
//
//        val doc42 = onMongo{ mongoOperation {  }}
//    }


}


