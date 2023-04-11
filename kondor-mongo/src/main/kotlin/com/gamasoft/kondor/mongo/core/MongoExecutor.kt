package com.gamasoft.kondor.mongo.core

import com.mongodb.client.ChangeStreamIterable
import com.mongodb.client.ListDatabasesIterable
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.connection.ClusterDescription
import com.ubertob.kondor.outcome.Outcome
import com.ubertob.kondor.outcome.asFailure
import com.ubertob.kondor.outcome.asSuccess
import org.bson.Document


class MongoExecutor(private val connection: MongoConnection, val databaseName: String): ContextProvider<MongoSession> {

    private val mongoClient: MongoClient by lazy { MongoClients.create(connection.toMongoClientSettings()) }

    override operator fun <T> invoke(context: ContextReader<MongoSession, T>): Outcome<MongoError, T> =
        try {
            val sess = MongoDbSession(mongoClient.getDatabase(databaseName))
//            println("Connected to ${connection.connString} found dbs: ${mongoClient.listDatabases()}") //TODO add an audit function to constructor (MongoAudit) -> Unit
            context.runWith(sess).asSuccess()
        } catch (e: Exception) {
            MongoErrorException(connection, databaseName, e).asFailure()
        }

    fun listDatabases(): ListDatabasesIterable<Document> = mongoClient.listDatabases()
    fun clusterDescription(): ClusterDescription = mongoClient.clusterDescription
    fun watch(): ChangeStreamIterable<Document> = mongoClient.watch()

}