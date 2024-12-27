package com.ubertob.kondor.mongo.core

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.MongoTimeoutException
import com.mongodb.connection.ClusterSettings
import com.mongodb.connection.ServerConnectionState
import com.ubertob.kondor.outcome.MessageError
import com.ubertob.kondor.outcome.Outcome
import com.ubertob.kondor.outcome.failIfNull
import com.ubertob.kondortools.expectFailure
import com.ubertob.kondortools.expectSuccess
import org.bson.BsonDocument
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThan
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit

private object collForTest : BsonTable() {
    override val collectionName: String = "collForTest"
}

@Testcontainers
class MongoExecutorTest {

    companion object {
        @Container
        private val mongoContainer = mongoForTests()

        val mongoConnection = mongoContainer.connection
    }

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

    val writeAndReadOneDoc = mongoOperation {
        collForTest.drop()
        collForTest.insertOne(doc)
        val docs = collForTest.all()
        expectThat(1).isEqualTo(docs.count())
        docs.first()
    }

    val dropCollReader = mongoOperation {
        collForTest.drop()
        collForTest.all().count()
    }

    val docQueryReader: ContextReader<MongoSession, BsonDocument> = mongoOperation {
        (1..100).forEach {
            collForTest.insertOne(createDoc(it))
        }
        collForTest.find("{ index: 42 }").first()
    }

    fun docName(doc: BsonDocument): Outcome<MessageError, String> = (doc["name"]?.asString()?.value).failIfNull { MessageError("Name not found!") }

    private val dbName = "MongoProvTest"

    @Test
    fun `add and query doc safely`() {
        val executor = buildExecutor()

        val outcome = writeAndReadOneDoc exec executor
        val myDoc = outcome.expectSuccess()
        expectThat(doc).isEqualTo(myDoc)
    }

    @Test
    fun `drop collection safely`() {
        val executor = buildExecutor()

        val tot: Int = executor(dropCollReader).expectSuccess()
        expectThat(0).isEqualTo(tot)
    }

    @Test
    fun `return error in case of wrong connection`() {
        val executor = MongoExecutorDbClient.fromConnectionString(MongoConnection("mongodb://localhost:12345"), dbName)

        val res = executor(dropCollReader)
        assertTrue(res.toString().contains("MongoErrorException"))
    }

    @Test
    fun `parsing query safely`() {
        val executor = buildExecutor()

        val myDoc = executor(docQueryReader).expectSuccess()
        expectThat(42).isEqualTo(myDoc["index"]!!.asInt32().value)
    }

    @Test
    fun `handle outcome operations`() {
        val executor = buildExecutor()

        val outcomeCalculation: ContextReader<MongoSession, Outcome<MessageError, String>> = docQueryReader.transform { docName(it) }
        val name = executor.bindOutcome(outcomeCalculation).expectSuccess()
        expectThat("subdoc42").isEqualTo(name)
    }

    @Test
    fun `list database names`() {
        val executor = buildExecutor()

        val dbNames = executor.listDatabaseNames() //.printIt("db names")
        expectThat(dbNames.size).isGreaterThan(0)
    }

    @Test
    fun `create executor from client settings`() {
        val clientSettings = MongoClientSettings.builder()
            .applyToSocketSettings { builder ->
                builder.applySettings(
                    builder.connectTimeout(200, TimeUnit.MILLISECONDS).build()
                )
            }
            .applyToClusterSettings { builder: ClusterSettings.Builder ->
                builder.serverSelectionTimeout(200, TimeUnit.MILLISECONDS).build()
            }
            .applyConnectionString(ConnectionString(mongoContainer.connection.connString))
            .build()
        val executor = MongoExecutorDbClient.fromClientSettings(dbName, clientSettings)

        val dbNames = executor.listDatabaseNames()
        expectThat(dbNames.size).isGreaterThan(0)
    }

    @Test
    fun `retrieve connection state`() {
        val executor = buildExecutor()
        executor(writeAndReadOneDoc).expectSuccess()

        val clusterDesc = executor.clusterDescription()

        expectThat(clusterDesc.serverDescriptions[0].state).isEqualTo(ServerConnectionState.CONNECTED)
    }

    @Test
    fun `handle wrong connection`() {
        val wrongConn = MongoConnection(
            connString = "mongodb://mynonexistanthost:1234/dbname",
            timeout = Duration.ofMillis(10)
        )
        val executor = MongoExecutorDbClient.fromConnectionString(wrongConn, dbName)

        val error = executor(writeAndReadOneDoc).expectFailure() as MongoErrorException

        expectThat(error.e).isA<MongoTimeoutException>()
        val clusterDesc = executor.clusterDescription()

        expectThat(clusterDesc.serverDescriptions[0].state).isEqualTo(ServerConnectionState.CONNECTING)
    }

    fun buildExecutor() = MongoExecutorDbClient.fromConnectionString(mongoConnection, dbName)
}
