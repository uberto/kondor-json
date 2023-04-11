package com.gamasoft.kondor.mongo.core

import com.gamasoft.kondor.mongo.json.toBsonDocument
import com.mongodb.client.MongoCollection
import com.ubertob.kondor.json.ObjectNodeConverter
import com.ubertob.kondor.json.jsonnode.NodePathRoot
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

abstract class TypedTable<T : Any>(private val converter: ObjectNodeConverter<T>) : MongoTable<T> {
    override fun fromBsonDoc(doc: BsonDocument): T = converter.fromJson(doc.toJson())
        .onFailure {
            error("Conversion failed in TypedTable \n--- $it \n--- with JSON ${doc.toJson()}")
        }

    override fun toBsonDoc(obj: T): BsonDocument = //BsonDocument.parse(converter.toJson(obj))
        converter.toJsonNode(obj, NodePathRoot).toBsonDocument()

    override val onConnection: (MongoCollection<BsonDocument>) -> Unit = {}

}

