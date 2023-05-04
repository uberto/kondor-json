package com.ubertob.kondor.mongo.core

import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.ReturnDocument
import org.bson.BsonDocument
import org.bson.BsonValue
import org.bson.Document
import org.bson.conversions.Bson

interface MongoSession {

    //General Methods
    fun listCollections(filter: Bson? = null): List<Document>

    fun listCollectionNames(filter: Bson? = null): List<String>

    //Edit Methods
    fun <T : Any> MongoTable<T>.addDocument(doc: T): BsonValue?

    //    fun <T : Any> MongoCollection<T>.replaceDocument(doc: BsonDocument): BsonObjectId
    fun <T : Any> MongoTable<T>.addDocuments(docs: Iterable<T>): List<BsonValue>

    fun <T : Any> MongoTable<T>.removeDocuments(queryString: String): Long
    fun <T : Any> MongoTable<T>.removeDocuments(bsonFilters: Bson): Long
    fun <T : Any> MongoTable<T>.updateSingle(bsonFilters: Bson, bsonSetter: Bson): T?

    //Query Methods
    fun <T : Any> MongoTable<T>.findById(id: Any): T?
    fun <T : Any> MongoTable<T>.find(queryString: String): Sequence<T>
    fun <T : Any> MongoTable<T>.find(bsonFilters: Bson): Sequence<T>

    fun <T : Any> MongoTable<T>.all(): Sequence<T> = find("")
    fun <T : Any> MongoTable<T>.aggregate(vararg pipeline: Bson): Sequence<BsonDocument>

    fun MongoTable<*>.countDocuments(): Long

    //Table Methods
    fun <T : Any> MongoTable<T>.drop()
    fun <T : Any> MongoTable<T>.listIndexes(): Sequence<BsonDocument>

}

typealias CollectionCache = MutableMap<String, MongoCollection<BsonDocument>>

class MongoDbSession(val database: MongoDatabase, val collections: CollectionCache) : MongoSession {

    private fun withCollection(mongoTable: MongoTable<*>): MongoCollection<BsonDocument> =
        collections.getOrPut(mongoTable.collectionName) {
            database.getCollection(
                mongoTable.collectionName, BsonDocument::class.java
            ).also {
                mongoTable.onConnection(it)
            }
        }

    fun <T> MongoTable<*>.internalRun(block: (MongoCollection<BsonDocument>) -> T): T =
        block(
            withCollection(this)
        )

    override fun listCollections(filter: Bson?): List<Document> =
        database.listCollections().filter(filter).toList()

    override fun listCollectionNames(filter: Bson?): List<String> =
        listCollections(filter).map { it["name"].toString() }

    override fun <T : Any> MongoTable<T>.addDocument(doc: T): BsonValue? =
        internalRun {
            it.insertOne(toBsonDoc(doc)).insertedId
        }

    override fun <T : Any> MongoTable<T>.addDocuments(docs: Iterable<T>): List<BsonValue> =
        internalRun {
            it.insertMany(docs.map(this::toBsonDoc))
                .insertedIds.values.toList()
        }

    override fun <T : Any> MongoTable<T>.removeDocuments(bsonFilters: Bson): Long =
        internalRun {
            it.deleteMany(bsonFilters)
                .deletedCount
        }

    override fun <T : Any> MongoTable<T>.removeDocuments(queryString: String): Long =
        removeDocuments(BsonDocument.parse(queryString))

    private val RETURN_UPDATED = FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
    override fun <T : Any> MongoTable<T>.updateSingle(bsonFilters: Bson, bsonSetter: Bson): T? =
        internalRun { coll ->
            coll.findOneAndUpdate(bsonFilters, bsonSetter, RETURN_UPDATED)
                ?.let ( ::fromBsonDoc )
        }

    override fun <T : Any> MongoTable<T>.findById(id: Any): T? =
        internalRun { coll ->
            coll.find(Filters.eq("_id", id))
                .firstOrNull()
                ?.let(::fromBsonDoc)
        }

    override fun <T : Any> MongoTable<T>.find(queryString: String): Sequence<T> =
        internalRun {
            when (queryString) {
                "" -> it.find()
                else ->
                    it.find(BsonDocument.parse(queryString))
            }.asSequence().map(::fromBsonDoc)
        }

    override fun <T : Any> MongoTable<T>.find(bsonFilters: Bson): Sequence<T> =
        internalRun {
            it.find(bsonFilters)
            .asSequence().map(::fromBsonDoc)
        }

    override fun <T : Any> MongoTable<T>.aggregate(vararg pipeline: Bson): Sequence<BsonDocument> =
        internalRun {
            it.aggregate(pipeline.toList())
        }.asSequence()


    override fun MongoTable<*>.countDocuments(): Long =
        internalRun {
            it.countDocuments()
        }

    override fun <T : Any> MongoTable<T>.drop() =
        internalRun {
            it.drop()
        }

    override fun <T : Any> MongoTable<T>.listIndexes(): Sequence<BsonDocument> =
        internalRun {
            it.listIndexes().map { it.toBsonDocument() }.iterator().asSequence()
        }
}
