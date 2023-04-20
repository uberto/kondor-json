package com.ubertob.kondor.mongo.core

import com.mongodb.client.*
import com.mongodb.connection.ClusterDescription
import com.ubertob.kondor.outcome.Outcome
import com.ubertob.kondor.outcome.asFailure
import com.ubertob.kondor.outcome.asSuccess
import org.bson.BsonDocument
import org.bson.Document
import java.util.concurrent.ConcurrentHashMap


class MongoExecutor(private val connection: MongoConnection, val databaseName: String): ContextProvider<MongoSession> {

    private val mongoClient: MongoClient by lazy { MongoClients.create(connection.toMongoClientSettings()) }

    private val collectionsCache: CollectionCache = ConcurrentHashMap<String, MongoCollection<BsonDocument>>()

    override operator fun <T> invoke(context: ContextReader<MongoSession, T>): Outcome<MongoError, T> =
        try {
            val sess = MongoDbSession(mongoClient.getDatabase(databaseName), collectionsCache)
//            println("Connected to ${connection.connString} found dbs: ${mongoClient.listDatabases()}") //TODO add an audit function to constructor (MongoAudit) -> Unit
            context.runWith(sess).asSuccess()
        } catch (e: Exception) {
            MongoErrorException(connection, databaseName, e).asFailure()
        }

    fun listDatabases(): ListDatabasesIterable<Document> = mongoClient.listDatabases()
    fun clusterDescription(): ClusterDescription = mongoClient.clusterDescription
    fun watch(): ChangeStreamIterable<Document> = mongoClient.watch()

}