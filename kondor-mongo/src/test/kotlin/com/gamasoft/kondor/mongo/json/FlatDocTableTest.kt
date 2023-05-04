package com.gamasoft.kondor.mongo.json

import com.gamasoft.kondor.mongo.core.DB_NAME
import com.gamasoft.kondor.mongo.core.connection
import com.gamasoft.kondor.mongo.core.mongoForTests
import com.ubertob.kondor.mongo.core.*
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

    val executor = MongoExecutor(mongoConnection, DB_NAME)

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

    fun docWriter(doc: SimpleFlatDoc): ContextReader<MongoSession, Unit> =
        mongoOperation {
            FlatDocs.addDocument(doc)
        }

    fun docQuery(index: Int): ContextReader<MongoSession, SimpleFlatDoc> =
        mongoOperation {
            FlatDocs.find("{ index: $index }").first()
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

        val myDoc = executor(
            cleanup +
                    docWriter(doc) +
                    docQuery(doc.index)
        ).expectSuccess()
        expectThat(doc).isEqualTo(myDoc)

    }

    fun extractInfo(doc: SimpleFlatDoc): String = "${doc.date}-${doc.name}#${doc.index}"

    @Test
    fun `operate on results`() {
        val docIndex = Random.nextInt(100)
        val operation = hundredDocWriter
            .bind { docQuery(docIndex) }
            .transform(::extractInfo)

        val myDoc = executor(
            operation
        ).expectSuccess()
        expectThat(myDoc).contains(docIndex.toString())

    }

}


