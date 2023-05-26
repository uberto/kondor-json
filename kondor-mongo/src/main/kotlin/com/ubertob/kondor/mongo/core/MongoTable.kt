package com.ubertob.kondor.mongo.core

import com.mongodb.client.MongoCollection
import com.ubertob.kondor.json.ObjectNodeConverterWriters
import com.ubertob.kondor.json.jsonnode.NodePathRoot
import com.ubertob.kondor.mongo.json.toBsonDocument
import com.ubertob.kondor.outcome.onFailure
import org.bson.BsonDocument

interface MongoTable<T : Any> { //actual collections are objects
    val collectionName: String
    val onConnection: (MongoCollection<BsonDocument>) -> Unit

    //todo retention policy etc.
    fun fromBsonDoc(doc: BsonDocument): T
    fun toBsonDoc(obj: T): BsonDocument

}

abstract class BsonTable : MongoTable<BsonDocument> {
    override fun fromBsonDoc(doc: BsonDocument): BsonDocument = doc
    override fun toBsonDoc(obj: BsonDocument): BsonDocument = obj

    override val onConnection: (MongoCollection<BsonDocument>) -> Unit = {} //todo something better
}

abstract class TypedTable<T : Any>(private val converter: ObjectNodeConverterWriters<T>) : MongoTable<T> {
    override fun fromBsonDoc(doc: BsonDocument): T = converter.fromJson(doc.toJson())
        .onFailure {
            error("Conversion failed in TypedTable \n--- $it \n--- with JSON ${doc.toJson()}")
        }

    override fun toBsonDoc(obj: T): BsonDocument = converter.toJsonNode(obj, NodePathRoot).toBsonDocument()
    //BsonDocument.parse(converter.toJson(obj))


    override val onConnection: (MongoCollection<BsonDocument>) -> Unit = {}

}

