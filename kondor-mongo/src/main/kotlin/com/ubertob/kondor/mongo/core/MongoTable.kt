package com.ubertob.kondor.mongo.core

import com.mongodb.MongoCommandException
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.IndexOptions
import com.ubertob.kondor.json.ObjectNodeConverter
import com.ubertob.kondor.mongo.json.toBsonDocument
import com.ubertob.kondor.outcome.onFailure
import org.bson.BsonDocument
import org.bson.conversions.Bson

sealed interface MongoTable<T : Any> { //actual collections are objects
    val collectionName: String
    val onConnection: (MongoCollection<BsonDocument>) -> Unit

    fun fromBsonDoc(doc: BsonDocument): T
    fun toBsonDoc(obj: T): BsonDocument

}

abstract class BsonTable : MongoTable<BsonDocument> {

    override fun fromBsonDoc(doc: BsonDocument): BsonDocument = doc
    override fun toBsonDoc(obj: BsonDocument): BsonDocument = obj

    override val onConnection: (MongoCollection<BsonDocument>) -> Unit = {}

}

//abstract class TypedTable<T : Any, CONV: ObjectNodeConverter<T>>(val converter: CONV) : MongoTable<T> {
abstract class TypedTable<T : Any>(val converter: ObjectNodeConverter<T>) : MongoTable<T> {
    override fun fromBsonDoc(doc: BsonDocument): T = converter.fromJson(doc.toJson())
        .onFailure {
            error("Conversion failed in TypedTable \n--- $it \n--- with JSON ${doc.toJson()}")
        }

    override fun toBsonDoc(obj: T): BsonDocument = converter.toJsonNode(obj).toBsonDocument()

    override val onConnection: (MongoCollection<BsonDocument>) -> Unit = {}

}

fun MongoCollection<*>.ensureIndex(keys: Bson, indexOptions: IndexOptions) {
    try {
        createIndex(keys, indexOptions)
    } catch (e: MongoCommandException) {
        //if a previous index with same name existed it will fail. so try to delete it and recreate it
        try {
            dropIndex(keys)
        } catch (e: MongoCommandException) {
            //ignore it
        }

        createIndex(keys, indexOptions)
    }
}