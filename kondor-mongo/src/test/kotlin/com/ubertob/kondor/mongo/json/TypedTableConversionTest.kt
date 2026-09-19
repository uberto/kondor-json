package com.ubertob.kondor.mongo.json

import com.ubertob.kondor.json.JObj
import com.ubertob.kondor.json.bool
import com.ubertob.kondor.json.datetime.str
import com.ubertob.kondor.json.jsonnode.FieldsValues
import com.ubertob.kondor.json.jsonnode.NodePath
import com.ubertob.kondor.json.num
import com.ubertob.kondor.json.str
import com.ubertob.kondor.mongo.core.TypedTable
import com.ubertob.kondortools.expectSuccess
import org.bson.BsonDocument
import org.bson.BsonObjectId
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.time.LocalDate

// no MongoDB needed: these tests check only the conversion of the documents read from a collection
class TypedTableConversionTest {

    object JSimpleFlatDocObj : JObj<SimpleFlatDoc>() {
        val index by num(SimpleFlatDoc::index)
        val name by str(SimpleFlatDoc::name)
        val localDate by str(SimpleFlatDoc::date)
        val isEven by bool(SimpleFlatDoc::bool)

        override fun FieldsValues.deserializeOrThrow(path: NodePath) = SimpleFlatDoc(
            index = +index,
            name = +name,
            date = +localDate,
            bool = +isEven
        )
    }

    object JObjTable : TypedTable<SimpleFlatDoc>(JSimpleFlatDocObj) {
        override val collectionName: String = "JObjDocs"
    }

    object JAnyTable : TypedTable<SimpleFlatDoc>(JSimpleFlatDoc) {
        override val collectionName: String = "JAnyDocs"
    }

    private val doc = SimpleFlatDoc(7, "seven", LocalDate.of(2026, 9, 19), false)

    private val fields = """"index": 7, "name": "seven", "localDate": "2026-09-19", "isEven": false"""

    @Test
    fun `JObj and JAny tables ignore the _id added by MongoDB`() {
        val bsonDoc = BsonDocument.parse("{$fields}").append("_id", BsonObjectId(ObjectId()))

        expectThat(JObjTable.fromBsonDoc(bsonDoc).expectSuccess()).isEqualTo(doc)
        expectThat(JAnyTable.fromBsonDoc(bsonDoc).expectSuccess()).isEqualTo(doc)
    }

    @Test
    fun `a JObj table reads documents with the _id first`() {
        val bsonDoc = BsonDocument("_id", BsonObjectId(ObjectId())).apply { putAll(BsonDocument.parse("{$fields}")) }

        expectThat(JObjTable.fromBsonDoc(bsonDoc).expectSuccess()).isEqualTo(doc)
    }
}
