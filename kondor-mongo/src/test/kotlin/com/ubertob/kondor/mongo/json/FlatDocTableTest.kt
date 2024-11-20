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

    val onMongo = MongoExecutorDbClient.fromConnectionString(mongoConnection, DB_NAME)

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

    private val cleanUp: MongoOperation<Unit> = mongoOperation {
        FlatDocs.drop()
    }

    fun reader(index: Int) = mongoOperation {
        FlatDocs.find(JSimpleFlatDoc.index eq index)
            .firstOrNull()
    }

    val count = mongoOperation {
        FlatDocs.countDocuments()
    }

    fun docWriter(doc: SimpleFlatDoc): MongoOperation<Unit> =
        mongoOperation {
            FlatDocs.insertOne(doc)
        }

    fun docQuery(index: Int): MongoOperation<SimpleFlatDoc?> =
        mongoOperation {
            FlatDocs.find(JSimpleFlatDoc.index eq index).firstOrNull()
        }

    private val docCounter: MongoOperation<Long> = mongoOperation {
        FlatDocs.countDocuments()
    }

    private val hundredDocWriter: MongoOperation<Long> =
        mongoOperation {
            (1..100).forEach {
                FlatDocs.insertOne(createDoc(it))
            }

        } + docCounter


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
                JSimpleFlatDoc.index eq 42,
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
                JSimpleFlatDoc.index eq 42,
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
            FlatDocs.findOneAndDelete(JSimpleFlatDoc.index eq 44)
        }).expectSuccess()

        val noDoc = onMongo(reader(44)).expectSuccess()

        expectThat(noDoc).isEqualTo(null)
    }

    @Test
    fun `replaceOne methods work correctly`() {

        onMongo(cleanUp + hundredDocWriter).expectSuccess()

        val updatedDoc = SimpleFlatDoc(43, "updated doc", calcDocDate(43), true)
        onMongo(mongoOperation {
            FlatDocs.replaceOne(
                Filters.eq("index", 43),
                updatedDoc
            )
        }).expectSuccess()

        val newDoc43 = onMongo(reader(43)).expectSuccess()

        expectThat(newDoc43).isEqualTo(updatedDoc)
    }

    @Test
    fun `updateOne methods work correctly`() {

        onMongo(cleanUp + hundredDocWriter).expectSuccess()

        onMongo(mongoOperation {
            FlatDocs.updateOne(
                JSimpleFlatDoc.index eq 43,
                Updates.combine(
                    Updates.mul("index", 10),
                    Updates.set("name", "updated doc 43")
                )
            )
        }).expectSuccess()

        val newDoc430 = onMongo(reader(430)).expectSuccess()

        expectThat(newDoc430?.name).isEqualTo("updated doc 43")
    }

    @Test
    fun `updateMany methods work correctly`() {

        onMongo(cleanUp + hundredDocWriter).expectSuccess()

        onMongo(mongoOperation {
            FlatDocs.updateMany(
                JSimpleFlatDoc.index `in` setOf(43, 73),
                Updates.set("name", "updated doc")
            )
        }).expectSuccess()

        val newDoc43 = onMongo(reader(43)).expectSuccess()
        expectThat(newDoc43?.name).isEqualTo("updated doc")
        val newDoc73 = onMongo(reader(73)).expectSuccess()
        expectThat(newDoc73?.name).isEqualTo("updated doc")
    }

    @Test
    fun `deleteOne methods work correctly`() {

        onMongo(cleanUp + hundredDocWriter).expectSuccess()

        onMongo(mongoOperation {
            FlatDocs.deleteOne(JSimpleFlatDoc.index eq 44)
        }).expectSuccess()

        val noDoc = onMongo(reader(44)).expectSuccess()

        expectThat(noDoc).isEqualTo(null)
    }

    @Test
    fun `deleteMulti methods work correctly`() {

        onMongo(cleanUp + hundredDocWriter).expectSuccess()

        onMongo(mongoOperation {
            FlatDocs.deleteMany(Filters.and(JSimpleFlatDoc.index lt 60, JSimpleFlatDoc.index gt 40))
        }).expectSuccess()

        val noDoc = onMongo(count).expectSuccess()
        expectThat(noDoc).isEqualTo(81)

        val missing = onMongo(reader(41)).expectSuccess()
        expectThat(missing).isEqualTo(null)
    }

    @Test
    fun `bulkWrite execute multiple write operations as a single command`() {

        onMongo(cleanUp).expectSuccess()

        val hundredDocs = (1..100).map {
            createDoc(it)
        }

        val tot = onMongo(
            mongoOperation {
                FlatDocs.bulkWrite(hundredDocs) { doc ->
                    WriteOperation.Insert(doc)
                }.insertedCount
            }).expectSuccess()

        expectThat(tot).isEqualTo(100)

        val doc43 = onMongo(reader(43)).expectSuccess()
        expectThat(doc43?.name).isEqualTo("mydoc 43")
        val doc99 = onMongo(reader(99)).expectSuccess()
        expectThat(doc99?.name).isEqualTo("mydoc 99")

    }

    @Test
    fun `bulkWrite performs multiple operations in a single call`() {

        val doc1 = createDoc(1001)
        val doc2 = createDoc(1002)
        val doc3 = createDoc(1003)

        val operations = listOf(
            WriteOperation.Insert(doc1),
            WriteOperation.Insert(doc2),
            WriteOperation.Insert(doc3),
            WriteOperation.Update(Filters.eq("index", 1001), Updates.set("value", 1042)),
            WriteOperation.Delete(Filters.eq("index", 1002))
        )

        val tot = onMongo(
            mongoOperation {
                FlatDocs.bulkWrite(operations)
            }).expectSuccess()


        expectThat(tot.insertedCount).isEqualTo(3)
        expectThat(tot.deletedCount).isEqualTo(1)
        expectThat(tot.modifiedCount).isEqualTo(1)
    }

    @Test
    fun `watch should report the changes`() {

        val watcher = onMongo.watch()
        val cursor = watcher.iterator()

        val docTot = onMongo(
            cleanUp +
                    docWriter(createDoc(1)) +
                    docWriter(createDoc(2)) +
                    docWriter(createDoc(3)) +
                    docCounter
        ).expectSuccess()
        expectThat(docTot).isEqualTo(3)

        val events = cursor.asSequence().take(4).map { it.operationType?.value }.toList()
        expectThat(events).isEqualTo(listOf("drop", "insert", "insert", "insert"))
    }
}

