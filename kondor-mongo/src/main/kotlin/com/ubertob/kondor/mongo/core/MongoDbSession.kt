package com.ubertob.kondor.mongo.core

import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.model.FindOneAndDeleteOptions
import com.mongodb.client.model.FindOneAndReplaceOptions
import com.mongodb.client.model.FindOneAndUpdateOptions
import org.bson.BsonDocument
import org.bson.BsonValue
import org.bson.Document
import org.bson.conversions.Bson

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

    override fun <T : Any> MongoTable<T>.insertOne(doc: T): BsonValue? =
        internalRun {
            it.insertOne(toBsonDoc(doc)).insertedId
        }

    override fun <T : Any> MongoTable<T>.insertMany(docs: Iterable<T>): List<BsonValue> =
        internalRun {
            it.insertMany(docs.map(this::toBsonDoc))
                .insertedIds.values.toList()
        }

    override fun <T : Any> MongoTable<T>.deleteMany(bsonFilters: Bson): Long =
        internalRun {
            it.deleteMany(bsonFilters)
                .deletedCount
        }

    override fun <T : Any> MongoTable<T>.findOneAndUpdate(
        bsonFilters: Bson,
        bsonSetter: Bson,
        options: FindOneAndUpdateOptions
    ): T? = internalRun { coll ->
        coll.findOneAndUpdate(bsonFilters, bsonSetter, options)
            ?.let(::fromBsonDoc)
    }

    override fun <T : Any> MongoTable<T>.findOneAndReplace(
        bsonFilters: Bson,
        doc: T,
        options: FindOneAndReplaceOptions
    ): T? = internalRun { coll ->
        coll.findOneAndReplace(bsonFilters, toBsonDoc(doc), options)
            ?.let(::fromBsonDoc)
    }

    override fun <T : Any> MongoTable<T>.findOneAndDelete(bsonFilters: Bson, options: FindOneAndDeleteOptions): T? =
        internalRun { coll ->
            coll.findOneAndDelete(bsonFilters, options)
                ?.let(::fromBsonDoc)
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