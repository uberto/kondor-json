package com.gamasoft.kondor.mongo.core

import com.mongodb.client.MongoCollection
import com.mongodb.client.model.*
import com.ubertob.kondor.mongo.core.*
import com.ubertob.kondortools.expectSuccess
import org.bson.BsonDocument
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import java.util.concurrent.atomic.AtomicInteger

@Testcontainers
class MongoTableTest {
    //TODO add tests for retention... policy.. index
    companion object {
        @Container
        private val mongoContainer = mongoForTests()

        val mongoConnection = mongoContainer.connection
    }

    object simpleDocTable : TypedTable<SmallClass>(JSmallClass) {
        override val collectionName: String = "simpleDocs"

        override val onConnection: (MongoCollection<BsonDocument>) -> Unit = { coll ->
            coll.createIndex(
                Indexes.ascending("int", "string"),
                IndexOptions().background(true).name("MyIndex")
            )
        }
    }

    object testConnectionTable : TypedTable<SmallClass>(JSmallClass) {
        val counter = AtomicInteger(0)

        override val collectionName: String = "testConnectionTable"

        override val onConnection: (MongoCollection<BsonDocument>) -> Unit = {
            counter.incrementAndGet()
        }
    }

    object complexDocTable : TypedTable<SealedClass>(JSealedClass) {
        override val collectionName: String = "complexDocs"
    }

    private val localMongo = MongoExecutor(
        connection = mongoConnection,
        databaseName = "mongoCollTest"
    )

    @BeforeEach
    fun cleanUp() {
        val cleanUpExecutor = MongoExecutor(
            connection = mongoConnection,
            databaseName = "mongoCollTest"
        )

        mongoOperation {
            testConnectionTable.drop()
            simpleDocTable.drop()
            complexDocTable.drop()
        }.exec(cleanUpExecutor)
    }

    @Test
    fun `add and retrieve single doc`() {

        val myDoc = SmallClass("abc", 123, 3.14, true)

        val doc = mongoOperation {
            simpleDocTable.drop()
            simpleDocTable.addDocument(myDoc)

            val docs = simpleDocTable.all()
            expectThat(1).isEqualTo(docs.count())
            docs.first()
        }.exec(localMongo).expectSuccess()

        expectThat(myDoc).isEqualTo(doc)
    }


    val myDocs = (1..100).map { buildSealedClass(it) }
    val write100Doc = mongoOperation {
        complexDocTable.addDocuments(myDocs)
        complexDocTable.countDocuments()
    }.withAction { expectThat(it).isEqualTo(100) }

    val readAll = mongoOperation {
        complexDocTable.all()
    }


    @Test
    fun `add and retrieve many random docs`() {

        val res = write100Doc exec localMongo

        res.expectSuccess()

        val allDocs = readAll.exec(localMongo).expectSuccess()

        expectThat(allDocs.toList()).isEqualTo(myDocs)
    }

    fun delete3Docs(id: Int) = mongoOperation {
        complexDocTable.removeDocuments("""{ string: "SmallClass$id" }""")
            .expectedOne()
        complexDocTable.removeDocuments("""{ "small_class.string" : "Nested${id + 1}" }""")
            .expectedOne()
        complexDocTable.removeDocuments("""{ "name" : "ClassWithArray${id + 2}" }""")
            .expectedOne()
        Unit
    }

    private fun Long.expectedOne() =
        let { expectThat(it).isEqualTo(1) }


    @Test
    fun `add and delete`() {

        val tot = write100Doc
            .bind { delete3Docs(42) }
            .bind { readAll }
            .transform { it.count() }
            .exec(localMongo).expectSuccess()

        expectThat(97).isEqualTo(tot)
    }

    @Test
    fun `add and delete alternate syntax`() {

        val res = write100Doc + delete3Docs(42) + readAll

        val tot = res.transform { it.count() }
            .exec(localMongo).expectSuccess()

        expectThat(97).isEqualTo(tot)
    }


    @Test
    fun `call onConnection just once for connection`() {
        val myDoc = SmallClass("abc", 123, 3.14, true)

        testConnectionTable.counter.set(0)

        mongoOperation {
            testConnectionTable.addDocument(myDoc)
            testConnectionTable.addDocument(myDoc)

            testConnectionTable.countDocuments()

        }.exec(localMongo).expectSuccess()

        expectThat(testConnectionTable.counter.get()).isEqualTo(1)

        mongoOperation {
            testConnectionTable.countDocuments()
        }.exec(localMongo).expectSuccess()


        expectThat(testConnectionTable.counter.get()).isEqualTo(1)

    }

    @Test
    fun `verify Indexes`() {

        val indexes = mongoOperation {
            expectThat(complexDocTable.listIndexes().count()).isEqualTo(0)
            simpleDocTable.listIndexes()
        }.exec(localMongo).expectSuccess()

        val definitions = indexes.toList().map { it.toJson() }
//        definitions.printIt("Indexes")
        expectThat(definitions.count()).isEqualTo(2)
        expectThat(definitions[1]).contains("MyIndex")
    }

    @Test
    fun `query and aggregate results`() {

        val aggr = write100Doc
            .bindCalculation {
                complexDocTable.aggregate(
                    Aggregates.match(Filters.exists("name")),
                    Aggregates.group("name", Accumulators.sum("count", 1))
                )
            }.exec(localMongo).expectSuccess()

        val count = aggr.single()["count"]!!.asInt32().value
        expectThat(count).isEqualTo(33)

    }

}