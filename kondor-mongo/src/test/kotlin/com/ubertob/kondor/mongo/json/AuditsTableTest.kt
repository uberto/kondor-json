package com.ubertob.kondor.mongo.json

import com.mongodb.client.MongoClients
import com.ubertob.kondor.mongo.core.*
import com.ubertob.kondortools.chronoAndLog
import com.ubertob.kondortools.expectSuccess
import org.bson.BsonDocument
import org.junit.jupiter.api.Test
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo

@Testcontainers
class AuditsTableTest {
    companion object {
        @Container
        private val mongoContainer = mongoForTests()

        val mongoConnection = mongoContainer.connection

        fun buildMongoClient() =
            MongoClients.create(mongoConnection.toMongoClientSettings())

    }

    private object AuditsTable : TypedTable<AuditMessage>(JAuditMessage) {
        override val collectionName: String = "Audits"
        //retention... policy.. index
    }

    fun randomString(lengthRange: IntRange = (1..10)): String {
        val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz0123456789"
        val length = (lengthRange.first..lengthRange.last).random()
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

    fun <T> randomList(generator: () -> T, sizeRange: IntRange = (1..10)): List<T> {
        val size = sizeRange.random()
        return List(size) { generator() }
    }

    fun randomText() = randomList(::randomString)

    private fun randomAudit(): AuditMessage =
        when (AuditMessage::class.sealedSubclasses.random()) {
            StringAudit::class -> StringAudit(randomString())
            TextAudit::class -> TextAudit(randomText())
            ErrorCodeAudit::class -> ErrorCodeAudit(randomInt(), randomString())
            MultiAudit::class -> MultiAudit(
                randomList({ StringAudit(randomString()) }, (1..5))
            )

            else -> error("Unknown class $this")
        }

    private fun randomInt(range: IntRange = 1..1000): Int = range.random()

    private val audit = randomAudit()

    val oneDocReader = mongoOperation {
        AuditsTable.drop()
        AuditsTable.insertOne(audit)
        val docs = AuditsTable.all()
        expectThat(1).isEqualTo(docs.count())
        docs.first()
    }


    val onMongo = MongoExecutorDbClient(DB_NAME) { buildMongoClient() }

    private val cleanUp = mongoOperation {
        AuditsTable.drop()
    }

    val audits = (1..100).map { randomAudit() }
    val write100Audits = mongoOperation {
        audits.forEach {
            AuditsTable.insertOne(it)
        }
    }

    val readAllAudits = mongoOperation {
        AuditsTable.all()
    }

    @Test
    fun `read and write from db`() {
        val myAudits: List<AuditMessage> = onMongo(cleanUp + write100Audits + readAllAudits)
            .expectSuccess().toList()

        expectThat(myAudits).hasSize(100)
        expectThat(myAudits).isEqualTo(audits)
    }

    @Test
    fun `add and query doc safely`() {

        val myDoc = onMongo(oneDocReader).expectSuccess()
        expectThat(audit).isEqualTo(myDoc)
    }

    @Test
    fun `parsing query safely`() {

        val myDocs = onMongo(cleanUp + write100Audits + readAllAudits).expectSuccess().toList()

        expectThat(myDocs).hasSize(100)
        expectThat(myDocs).isEqualTo(audits)

//        println(myDocs)
    }


    private object AuditsBsonTable : BsonTable() {
        override val collectionName: String = "performanceTests"
        //retention... policy.. index
    }


    @Test
    fun `performance check`() {
// average on 10 runs with 10k random audits
//        toJsonAndParse 37 ms
//        to JsonNode 5 ms
//        to BsonDoc 5 ms
//        to DB 1065 ms
//        Audits Table 1336 ms

        repeat(1) {
            onMongo(mongoOperation {
                AuditsBsonTable.drop()
                AuditsTable.drop()
            })

            val audits = (1..100).map { randomAudit() }

            chronoAndLog("toJsonAndParse") {
                audits.map { BsonDocument.parse(JAuditMessage.toJson(it)) }
            }

            val jsonNodes = chronoAndLog("to JsonNode") {
                audits.map { JAuditMessage.toJsonNode(it) }
            }

            val bsonDocs = chronoAndLog("to BsonDoc") {
                jsonNodes.map { it.toBsonDocument() }
            }



            chronoAndLog("to DB") {
                onMongo(
                    mongoOperation {
                        bsonDocs.forEach {
                            AuditsBsonTable.insertOne(it)
                        }
                    }
                )
            }


            chronoAndLog("Audits Table") {
                onMongo(
                    mongoOperation {
                        audits.forEach {
                            AuditsTable.insertOne(it)
                        }
                    }
                )
            }
        }
    }
}

