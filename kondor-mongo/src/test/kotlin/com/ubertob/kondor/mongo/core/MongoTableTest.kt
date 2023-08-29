package com.ubertob.kondor.mongo.core

import com.mongodb.client.MongoCollection
import com.mongodb.client.model.*
import com.ubertob.kondortools.expectFailure
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
import kotlin.random.Random

@Testcontainers
class MongoTableTest {

    companion object {
        @Container
        private val mongoContainer = mongoForTests()

        val mongoConnection = mongoContainer.connection
    }

    object simpleDocTable : TypedTable<SmallClass>(JSmallClass) {
        override val collectionName: String = "simpleDocs"

        override val onConnection: (MongoCollection<BsonDocument>) -> Unit = { coll ->
            coll.ensureIndex(
                Indexes.ascending("int", "string"),
                IndexOptions().background(true).name("MyIndex")
            )
        }
    }

    object keyValueStoreTable : TypedTable<KeyValueStore>(JKeyValueStore) {
        override val collectionName: String = "simpleDocs"
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

    private val localMongo = MongoExecutorDbClient.fromConnectionString(
        connection = mongoConnection,
        databaseName = "mongoCollTest"
    )

    @BeforeEach
    fun cleanUp() {
        val cleanUpExecutor = MongoExecutorDbClient.fromConnectionString(
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

        val myDoc = randomSmallClass()

        val doc = mongoOperation {
            simpleDocTable.drop()
            simpleDocTable.insertOne(myDoc)

            val docs = simpleDocTable.all()
            expectThat(1).isEqualTo(docs.count())
            docs.first()
        }.exec(localMongo).expectSuccess()

        expectThat(myDoc).isEqualTo(doc)
    }


    val myDocs = (1..100).map { buildSealedClass(it) }
    val write100Doc = mongoOperation {
        complexDocTable.insertMany(myDocs)
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
        complexDocTable.deleteMany(Filters.eq("string", "SmallClass$id"))
            .expectedOne()
        complexDocTable.deleteMany(Filters.eq("small_class.string", "Nested${id + 1}"))
            .expectedOne()
        complexDocTable.deleteMany(Filters.eq("name", "ClassWithArray${id + 2}"))
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
        val myDoc = randomSmallClass()

        testConnectionTable.counter.set(0)

        mongoOperation {
            testConnectionTable.insertOne(myDoc)
            testConnectionTable.insertOne(myDoc)

            testConnectionTable.countDocuments()

        }.exec(localMongo).expectSuccess()

        expectThat(testConnectionTable.counter.get()).isEqualTo(1)

        mongoOperation {
            testConnectionTable.countDocuments()
        }.exec(localMongo).expectSuccess()

        expectThat(testConnectionTable.counter.get()).isEqualTo(1)

    }

    private fun randomSmallClass() = SmallClass(
        string = Random.nextInt(100000, 999999).let { "String$it" },
        int = Random.nextInt(),
        double = Random.nextDouble(),
        boolean = Random.nextBoolean()
    )

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
    fun `list all collections`() {
        writeASimpleDocAndAComplexDoc.exec(localMongo).expectSuccess()

        val allColls = mongoOperation {
            listCollections()
        }.exec(localMongo).expectSuccess()

//        allColls.printIt("colls")
        expectThat(allColls.count()).isEqualTo(2)
        expectThat(allColls.map { it.toJson() }.joinToString()) {
            contains(complexDocTable.collectionName)
            contains(simpleDocTable.collectionName)
        }
    }

    private val writeASimpleDocAndAComplexDoc = mongoOperation {
        simpleDocTable.insertOne(randomSmallClass()) ?: error("Cannot create simpledoc")
        complexDocTable.insertOne(buildSealedClass(Random.nextInt())) ?: error("Cannot create complexdoc")
    }

    @Test
    fun `list only some collections`() {
        writeASimpleDocAndAComplexDoc.exec(localMongo).expectSuccess()

        val oneColl = mongoOperation {
            listCollectionNames(Filters.eq("name", simpleDocTable.collectionName))
        }.exec(localMongo).expectSuccess()

        expectThat(oneColl.count()).isEqualTo(1)
        expectThat(oneColl.first()).isEqualTo(simpleDocTable.collectionName)
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

    @Test
    fun `typed filters simplify the queries`() {

        val query = mongoOperation {
            val a = complexDocTable.find(JSmallClass.double eq 12.0).firstOrNull()
            val b = complexDocTable.find(JSmallClass.string eq "SmallClass3").firstOrNull()
            val c = complexDocTable.find(JSmallClass.int eq 9).firstOrNull()
            val d = complexDocTable.find(JSmallClass.boolean eq true).firstOrNull()

            "a=${a}, b=${b}, c=${c}, d=${d}"
        }
        val res = write100Doc + query exec localMongo

        expectThat(res.expectSuccess()).isEqualTo("a=SmallClass(string=SmallClass12, int=12, double=12.0, boolean=true), b=SmallClass(string=SmallClass3, int=3, double=3.0, boolean=false), c=SmallClass(string=SmallClass9, int=9, double=9.0, boolean=false), d=SmallClass(string=SmallClass6, int=6, double=6.0, boolean=true)")
    }

    private val storeThreeKV = mongoOperation {
        keyValueStoreTable.insertOne(KeyValueStore("0000", "first", 0.0))
        keyValueStoreTable.insertOne(KeyValueStore("0001", "second", 1.0))
        keyValueStoreTable.insertOne(KeyValueStore("0002", "third", 2.0))
    }.ignoreValue()

    fun queryByKey(id: String) = mongoOperation {
        keyValueStoreTable.findById(id)
    }

    @Test
    fun `use mongo id`() {

        val res = localMongo(storeThreeKV + queryByKey("0001"))

        expectThat(res.expectSuccess()?.description).isEqualTo("second")
    }

    @Test
    fun `mongo id must be unique`() {

        val firstTime = mongoOperation {
            keyValueStoreTable.insertOne(KeyValueStore("0042", "first", 0.0))
        }
        val objId = localMongo(firstTime).expectSuccess()

        expectThat(objId?.asString()?.value).isEqualTo("0042")

        val updateAgain = mongoOperation {
            keyValueStoreTable.insertOne(KeyValueStore("0042", "second", 1.0))
            keyValueStoreTable.insertOne(KeyValueStore("0042", "third", 2.0))
        }

        val fail = localMongo(updateAgain).expectFailure()
        expectThat(fail.msg).contains("duplicate key error")

        val res = localMongo(queryByKey("0042"))

        expectThat(res.expectSuccess()?.description).isEqualTo("first")
    }

    @Test
    fun `mongo id using the pk index`() {

    }

}

