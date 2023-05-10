package com.ubertob.kondor.mongo.json

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
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


    private fun createDoc(i: Int): SimpleFlatDoc {

        return SimpleFlatDoc(
            index = i,
            name = "mydoc $i",
            date = calcDocDate(i),
            bool = i % 2 == 0
        )
    }

    private fun calcDocDate(i: Int): LocalDate = LocalDate.now().minusDays(i.toLong())

    private val doc = createDoc(0)

    private val cleanUp = mongoOperation {
        FlatDocs.drop()
    }

    fun reader(index: Int) = mongoOperation {
        FlatDocs.find(Filters.eq("index", index))
            .firstOrNull()
    }

    fun docWriter(doc: SimpleFlatDoc): ContextReader<MongoSession, Unit> =
        mongoOperation {
            FlatDocs.addDocument(doc)
        }

    fun docQuery(index: Int): ContextReader<MongoSession, SimpleFlatDoc?> =
        mongoOperation {
            FlatDocs.find("{ index: $index }").firstOrNull()
        }

    private val hundredDocWriter: ContextReader<MongoSession, Long> =
        mongoOperation {
            (1..100).forEach {
                FlatDocs.addDocument(createDoc(it))
            }
            FlatDocs.countDocuments()
        }


    @Test
    fun `add and query doc safely`() {

        val myDoc = onMongo(
            cleanUp +
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


    @Test
    fun `findOneAndUpdate methods work correctly`() {

        onMongo(cleanUp + hundredDocWriter).expectSuccess()

        onMongo(mongoOperation {
            FlatDocs.findOneAndUpdate(
                Filters.eq("index", 42),
                Updates.combine(
                    Updates.set("name", "updated 42"),
                    Updates.set("isEven", false)
                )
            )
        }).expectSuccess()

        val newDoc42 = onMongo(reader(42)).expectSuccess()

        val expected = SimpleFlatDoc(42, "updated 42", calcDocDate(42), false)
        expectThat(newDoc42).isEqualTo(expected)
    }

    @Test
    fun `findOneAndReplace methods work correctly`() {

        onMongo(cleanUp + hundredDocWriter).expectSuccess()

        val updatedDoc = SimpleFlatDoc(43, "updated doc", calcDocDate(43), true)
        onMongo(mongoOperation {
            FlatDocs.findOneAndReplace(
                Filters.eq("index", 42),
                updatedDoc
            )
        }).expectSuccess()

        val newDoc43 = onMongo(reader(43)).expectSuccess()

        expectThat(newDoc43).isEqualTo(updatedDoc)
    }

    @Test
    fun `findOneAndDelete methods work correctly`() {

        onMongo(cleanUp + hundredDocWriter).expectSuccess()

        onMongo(mongoOperation {
            FlatDocs.findOneAndDelete(Filters.eq("index", 44))
        }).expectSuccess()

        val noDoc = onMongo(reader(44)).expectSuccess()

        expectThat(noDoc).isEqualTo(null)
    }

}


