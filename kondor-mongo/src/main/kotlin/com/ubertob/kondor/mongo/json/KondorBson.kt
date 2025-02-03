package com.ubertob.kondor.mongo.json

import com.ubertob.kondor.json.jsonnode.*
import org.bson.BsonDocument
import org.bson.BsonDocumentWriter
import org.bson.types.ObjectId

object KondorBson {

//
//    fun <T : Any> toBsonDoc(conv: JAny<T>, value: T): BsonDocument =
//        convertJsonNodeToBson(conv.toJsonNode(value))
//
//     fun convertBsonToJsonNode(bsonDocument: BsonDocument): JsonNode {
//        val br = bsonDocument.asBsonReader()
//        val t = br.readBsonType()
//       return when (t) {
//            BsonType.END_OF_DOCUMENT -> TODO()
//            BsonType.DOUBLE -> TODO()
//            BsonType.STRING -> TODO()
//            BsonType.DOCUMENT -> TODO()
//            BsonType.ARRAY -> TODO()
//            BsonType.BINARY -> TODO()
//            BsonType.UNDEFINED -> TODO()
//            BsonType.OBJECT_ID -> TODO()
//            BsonType.BOOLEAN -> TODO()
//            BsonType.DATE_TIME -> TODO()
//            BsonType.NULL -> TODO()
//            BsonType.REGULAR_EXPRESSION -> TODO()
//            BsonType.DB_POINTER -> TODO()
//            BsonType.JAVASCRIPT -> TODO()
//            BsonType.SYMBOL -> TODO()
//            BsonType.JAVASCRIPT_WITH_SCOPE -> TODO()
//            BsonType.INT32 -> TODO()
//            BsonType.TIMESTAMP -> TODO()
//            BsonType.INT64 -> TODO()
//            BsonType.DECIMAL128 -> TODO()
//            BsonType.MIN_KEY -> TODO()
//            BsonType.MAX_KEY -> TODO()
//            null -> JsonNodeNull
//        }
//
//    }
//
//    fun fromBsonDoc(jn: JsonNodeObject): BsonDocument {
//        val writer = BsonDocumentWriter(BsonDocument())
//
//        encodeValue(writer, jn)
//
//        return writer.document
//    }

    fun convertJsonNodeToBson(jn: JsonNodeObject): BsonDocument {
        val writer = BsonDocumentWriter(BsonDocument())

        encodeValue(writer, jn)

        return writer.document
    }

    private fun encodeValue(writer: BsonDocumentWriter, value: JsonNode) {
        when (value) {
            is JsonNodeNull -> writer.writeNull()
            is JsonNodeArray -> {
                writer.writeStartArray()
                value.elements.forEach {
                    encodeValue(writer, it)
                }
                writer.writeEndArray()
            }

            is JsonNodeBoolean -> writer.writeBoolean(value.boolean)
            is JsonNodeNumber -> writer.writeDouble(value.num.toDouble())
            is JsonNodeObject -> {
                if (value._fieldMap.keys.contains("\$oid"))
                    writer.writeObjectId(ObjectId(value._fieldMap["\$oid"].asStringValue()))
                else
                    encodeNormalObject(writer, value)
            }

            is JsonNodeString -> writer.writeString(value.text)
        }
    }

    private fun encodeNormalObject(writer: BsonDocumentWriter, value: JsonNodeObject) {
        writer.writeStartDocument()
        value._fieldMap.forEach { (fieldName, node) ->
            writer.writeName(fieldName)
            encodeValue(writer, node)
        }
        writer.writeEndDocument()
    }

}


fun JsonNodeObject.toBsonDocument(): BsonDocument = KondorBson.convertJsonNodeToBson(this)

