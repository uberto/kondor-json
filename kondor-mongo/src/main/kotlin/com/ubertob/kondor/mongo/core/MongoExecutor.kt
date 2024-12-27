package com.ubertob.kondor.mongo.core

import com.mongodb.MongoClientSettings
import com.mongodb.MongoDriverInformation
import com.mongodb.client.ChangeStreamIterable
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.connection.ClusterDescription
import com.ubertob.kondor.outcome.*
import org.bson.BsonDocument
import org.bson.Document
import java.util.concurrent.ConcurrentHashMap

interface MongoExecutor : ContextProvider<MongoSession> {
    val databaseName: String
    fun listDatabaseNames(): List<String>

    override operator fun <T> invoke(context: MongoOperation<T>): Outcome<MongoError, T>
    fun <T> bindOutcome(context: MongoOperation<Outcome<OutcomeError, T>>): Outcome<OutcomeError, T>
}

class MongoExecutorDbClient(override val databaseName: String, clientProvider: () -> MongoClient) :
    MongoExecutor {

    private val mongoClient: MongoClient by lazy(clientProvider)

    private val collectionsCache: CollectionCache = ConcurrentHashMap<String, MongoCollection<BsonDocument>>()

    override operator fun <T> invoke(context: MongoOperation<T>): Outcome<MongoError, T> =
        try {
            val sess = MongoDbSession(mongoClient.getDatabase(databaseName), collectionsCache)
            sess._logger("Connected to ${databaseName}")
            context.runWith(sess).asSuccess()
        } catch (e: Exception) {
            MongoErrorException(mongoClient.clusterDescription.shortDescription, databaseName, e).asFailure()
        }

    override fun <T> bindOutcome(context: MongoOperation<Outcome<OutcomeError, T>>): Outcome<OutcomeError, T> =
        invoke(context).join()

    fun listDatabases(): List<Document> = mongoClient.listDatabases().toList()

    override fun listDatabaseNames(): List<String> = listDatabases().map { it["name"].toString() }

    fun clusterDescription(): ClusterDescription = mongoClient.clusterDescription

    fun watch(): ChangeStreamIterable<Document> = mongoClient.watch()


    companion object {
        fun fromConnectionString(connection: MongoConnection, databaseName: String): MongoExecutorDbClient =
            fromClientSettings(databaseName, connection.toMongoClientSettings())

        fun fromClientSettings(
            databaseName: String,
            clientSettings: MongoClientSettings,
            mongoDriverInformation: MongoDriverInformation? = null
        ): MongoExecutorDbClient =
            MongoExecutorDbClient(databaseName)
            { MongoClients.create(clientSettings, mongoDriverInformation) }

    }
}