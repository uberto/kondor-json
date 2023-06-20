package com.ubertob.kondor.mongo.core

import com.mongodb.client.ChangeStreamIterable
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.connection.ClusterDescription
import com.ubertob.kondor.outcome.Outcome
import com.ubertob.kondor.outcome.asFailure
import com.ubertob.kondor.outcome.asSuccess
import org.bson.BsonDocument
import org.bson.Document
import java.util.concurrent.ConcurrentHashMap

interface MongoExecutor : ContextProvider<MongoSession> {
    val databaseName: String
    fun listDatabaseNames(): List<String>

    override operator fun <T> invoke(context: ContextReader<MongoSession, T>): Outcome<MongoError, T>
}

class MongoExecutorDbClient(override val databaseName: String, clientProvider: () -> MongoClient) :
    MongoExecutor {

    private val mongoClient: MongoClient by lazy(clientProvider)

    private val collectionsCache: CollectionCache = ConcurrentHashMap<String, MongoCollection<BsonDocument>>()

    override operator fun <T> invoke(context: ContextReader<MongoSession, T>): Outcome<MongoError, T> =
        try {
            val sess = MongoDbSession(mongoClient.getDatabase(databaseName), collectionsCache)
//            println("Connected to ${connection.connString} found dbs: ${mongoClient.listDatabases()}") //TODO add an audit function to constructor (MongoAudit) -> Unit
            context.runWith(sess).asSuccess()
        } catch (e: Exception) {
            MongoErrorException(mongoClient.clusterDescription.shortDescription, databaseName, e).asFailure()
        }

    fun listDatabases(): List<Document> = mongoClient.listDatabases().toList() //TODO add test

    override fun listDatabaseNames(): List<String> = listDatabases().map { it["name"].toString() }

    fun clusterDescription(): ClusterDescription = mongoClient.clusterDescription

    fun watch(): ChangeStreamIterable<Document> = mongoClient.watch() //TODO add test


    companion object {
        fun fromConnectionString(connection: MongoConnection, databaseName: String): MongoExecutorDbClient =
            MongoExecutorDbClient(databaseName)
            { MongoClients.create(connection.toMongoClientSettings()) }
    }
}