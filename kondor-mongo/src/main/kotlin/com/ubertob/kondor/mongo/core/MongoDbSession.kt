package com.ubertob.kondor.mongo.core

import com.mongodb.bulk.BulkWriteResult
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.*
import org.bson.BsonDocument
import org.bson.BsonValue
import org.bson.Document
import org.bson.conversions.Bson
import org.bson.types.ObjectId


class MongoDbSession(
    val database: MongoDatabase,
    val collections: CollectionCache,
    override val _logger: (String) -> Unit = {}
) : MongoSession {

    private fun withCollection(mongoTable: MongoTable<*>): MongoCollection<BsonDocument> =
        collections.getOrPut(mongoTable.collectionName) {
            database.getCollection(
                mongoTable.collectionName, BsonDocument::class.java
            ).also {
                mongoTable.onConnection(it)
            }
        }

    override fun <T> MongoTable<*>.internalRun(block: (MongoCollection<BsonDocument>) -> T): T =
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

    override fun <T : Any> MongoTable<T>.deleteOne(bsonFilters: Bson): Long =
        internalRun {
            it.deleteOne(bsonFilters)
                .deletedCount
        }

    override fun <T : Any> MongoTable<T>.deleteMany(bsonFilters: Bson): Long =
        internalRun {
            it.deleteMany(bsonFilters)
                .deletedCount
        }

    override fun <T : Any> MongoTable<T>.replaceOne(filter: Bson, doc: T, options: ReplaceOptions): BsonValue? =
        internalRun {
            it.replaceOne(filter, toBsonDoc(doc), options).upsertedId
        }

    override fun <T : Any> MongoTable<T>.updateOne(
        filter: Bson,
        updateModel: Bson,
        options: UpdateOptions
    ): BsonValue? =
        internalRun {
            it.updateOne(filter, updateModel, options).upsertedId
        }

    override fun <T : Any> MongoTable<T>.updateMany(filter: Bson, updateModel: Bson, options: UpdateOptions): Long =
        internalRun {
            it.updateMany(filter, updateModel, options).modifiedCount
        }

    override fun <T : Any> MongoTable<T>.findOneAndUpdate(
        bsonFilters: Bson,
        bsonSetter: Bson,
        options: FindOneAndUpdateOptions
    ): T? = internalRun { coll ->
        coll.findOneAndUpdate(bsonFilters, bsonSetter, options)
            ?.let(::fromBsonDoc)
            ?.orThrow()
    }

    override fun <T : Any> MongoTable<T>.findOneAndReplace(
        bsonFilters: Bson,
        doc: T,
        options: FindOneAndReplaceOptions
    ): T? = internalRun { coll ->
        coll.findOneAndReplace(bsonFilters, toBsonDoc(doc), options)
            ?.let(::fromBsonDoc)
            ?.orThrow()
    }

    override fun <T : Any> MongoTable<T>.findOneAndDelete(bsonFilters: Bson, options: FindOneAndDeleteOptions): T? =
        internalRun { coll ->
            coll.findOneAndDelete(bsonFilters, options)
                ?.let(::fromBsonDoc)
                ?.orThrow()
        }

    override fun <T : Any, U : Any> MongoTable<T>.bulkWrite(
        collection: List<U>,
        options: BulkWriteOptions,
        operation: (U) -> MongoBulkOperation<T>
    ): BulkWriteResult =
        bulkWrite(collection.map(operation), options)


    override fun <T : Any> MongoTable<T>.bulkWrite(
        operations: Iterable<MongoBulkOperation<T>>,
        options: BulkWriteOptions
    ): BulkWriteResult =

        internalRun { coll ->
            val bulkOperations = operations.map { operation ->
                when (operation) {
                    is MongoBulkOperation.Insert -> InsertOneModel(toBsonDoc(operation.document))
                    is MongoBulkOperation.Update -> UpdateOneModel(operation.filter, operation.update)
                    is MongoBulkOperation.Delete -> DeleteOneModel(operation.filter)
                }
            }
            coll.bulkWrite(bulkOperations)
        }

    override fun <T : Any> MongoTable<T>.findById(id: Any): T? =
        internalRun { coll ->
            coll.find(Filters.eq("_id", id))
                .singleOrNull()
                ?.let(::fromBsonDoc)
                ?.orThrow()
        }

    override fun <T : Any> MongoTable<T>.findByOid(id: ObjectId): T? =
        internalRun { coll ->
            coll.find(Filters.eq("_id", id))
                .singleOrNull()
                ?.let(::fromBsonDoc)
                ?.orThrow()
        }

    override fun <T : Any> MongoTable<T>.find(queryString: String): Sequence<T> =
        internalRun { collection ->
            when (queryString) {
                "" -> collection.find()
                else ->
                    collection.find(BsonDocument.parse(queryString))
            }.asSequence()
                .map { fromBsonDoc(it).orThrow() }
        }

    override fun <T : Any> MongoTable<T>.find(bsonFilters: Bson): Sequence<T> =
        internalRun {
            it.find(bsonFilters)
                .asSequence()
                .map { fromBsonDoc(it).orThrow() }
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