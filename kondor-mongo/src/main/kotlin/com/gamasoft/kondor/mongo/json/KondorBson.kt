package com.gamasoft.kondor.mongo.json

import com.ubertob.kondor.json.JAny
import com.ubertob.kondor.json.jsonnode.*
import org.bson.BsonDocument
import org.bson.BsonDocumentWriter
import org.bson.BsonType

object KondorBson {

    fun <T : Any> toBsonDoc(conv: JAny<T>, value: T): BsonDocument {
        val jn: JsonNodeObject = conv.toJsonNode(value, NodePathRoot)

        return convertJsonNodeToBson(jn)
    }

    private fun convertBsonToJsonNode(bsonDocument: BsonDocument): JsonNode {
        val br = bsonDocument.asBsonReader()
        val t = br.readBsonType()
        when (t) {
            BsonType.END_OF_DOCUMENT -> TODO()
            BsonType.DOUBLE -> TODO()
            BsonType.STRING -> TODO()
            BsonType.DOCUMENT -> TODO()
            BsonType.ARRAY -> TODO()
            BsonType.BINARY -> TODO()
            BsonType.UNDEFINED -> TODO()
            BsonType.OBJECT_ID -> TODO()
            BsonType.BOOLEAN -> TODO()
            BsonType.DATE_TIME -> TODO()
            BsonType.NULL -> TODO()
            BsonType.REGULAR_EXPRESSION -> TODO()
            BsonType.DB_POINTER -> TODO()
            BsonType.JAVASCRIPT -> TODO()
            BsonType.SYMBOL -> TODO()
            BsonType.JAVASCRIPT_WITH_SCOPE -> TODO()
            BsonType.INT32 -> TODO()
            BsonType.TIMESTAMP -> TODO()
            BsonType.INT64 -> TODO()
            BsonType.DECIMAL128 -> TODO()
            BsonType.MIN_KEY -> TODO()
            BsonType.MAX_KEY -> TODO()
            null -> TODO()
        }

        return JsonNodeNull(NodePathRoot)
    }

    fun convertJsonNodeToBson(jn: JsonNodeObject): BsonDocument {

        val writer = BsonDocumentWriter(BsonDocument())

        encodeValue(writer, jn)

        return writer.document
    }

    fun encodeValue(writer: BsonDocumentWriter, value: JsonNode) {
        when (value) {
            is JsonNodeNull -> writer.writeNull()
            is JsonNodeArray -> {
                writer.writeStartArray()
                value.values.forEach {
                    encodeValue(writer, it)
                }
                writer.writeEndArray()
            }

            is JsonNodeBoolean -> writer.writeBoolean(value.value)
            is JsonNodeNumber -> writer.writeDouble(value.num.toDouble())
            is JsonNodeObject -> {
                writer.writeStartDocument()

                value._fieldMap.forEach { (fieldName, node) ->
                    writer.writeName(fieldName)
                    encodeValue(writer, node)
                }

                writer.writeEndDocument()
            }

            is JsonNodeString -> writer.writeString(value.text)
        }
    }

}


fun JsonNodeObject.toBsonDocument(): BsonDocument = KondorBson.convertJsonNodeToBson(this)

