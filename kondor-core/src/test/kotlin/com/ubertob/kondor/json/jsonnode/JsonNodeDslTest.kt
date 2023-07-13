package com.ubertob.kondor.json.jsonnode

import com.ubertob.kondor.json.JsonStyle.Companion.prettyWithNulls
import com.ubertob.kondortools.expectSuccess
import com.ubertob.kondortools.isEquivalentJson
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import java.math.BigDecimal
import kotlin.random.Random

class JsonNodeDslTest {
    @Test
    fun `test String toNode with fieldValuesMap`() {
        val node = "test" toNode mapOf("field1" to "value1", "field2" to null)
        expectThat(node) {
            get { first }.isEqualTo("test")
            get { second.asObjFieldMap()!!.size }.isEqualTo(2)
            get { second.asObjFieldMap()!!["field1"]?.asStringValue() }.isEqualTo("value1")
            get { second.asObjFieldMap()!!["field2"]?.asStringValue() }.isNull()
        }
    }

    @Test
    fun `test String toNode with String`() {
        val node = "test" toNode "value"
        expectThat(node) {
            get { first }.isEqualTo("test")
            get { second.asStringValue() }.isEqualTo("value")
        }
    }

    @Test
    fun `test String toNode with Long`() {
        val node = "test" toNode 123L
        expectThat(node) {
            get { first }.isEqualTo("test")
            get { second.asNumValue() }.isEqualTo(BigDecimal(123))
        }
    }

    @Test
    fun `test String toNode with Int`() {
        val nextInt = Random.nextInt()
        val node = "test" toNode nextInt
        expectThat(node) {
            get { first }.isEqualTo("test")
            get { second.asNumValue() }.isEqualTo(BigDecimal(nextInt))
        }
    }

    @Test
    fun `test String toNode with Double`() {
        val nextDouble = Random.nextDouble()
        val node = "test" toNode nextDouble
        expectThat(node) {
            get { first }.isEqualTo("test")
            get { second.asNumValue() }.isEqualTo(BigDecimal.valueOf(nextDouble))
        }
    }

    @Test
    fun `test String toNode with Boolean`() {
        val node = "test" toNode true
        expectThat(node) {
            get { first }.isEqualTo("test")
            get { second.asBooleanValue() }.isEqualTo(true)
        }
    }

    @Test
    fun `test String toNode with null String`() {
        val node = "test" toNode null as String?
        expectThat(node) {
            get { first }.isEqualTo("test")
            get { second.asStringValue() }.isNull()
        }
    }

    @Test
    fun `test String toNode with null Long`() {
        val node = "test" toNode null as Long?
        expectThat(node) {
            get { first }.isEqualTo("test")
            get { second.asNumValue() }.isNull()
        }
    }

    @Test
    fun `test String toNode with null Int`() {
        val node = "test" toNode null as Int?
        expectThat(node) {
            get { first }.isEqualTo("test")
            get { second.asNumValue() }.isNull()
        }
    }

    @Test
    fun `test String toNode with null Double`() {
        val node = "test" toNode null as Double?
        expectThat(node) {
            get { first }.isEqualTo("test")
            get { second.asNumValue() }.isNull()
        }
    }

    @Test
    fun `test String toNode with null Boolean`() {
        val node = "test" toNode null as Boolean?
        expectThat(node) {
            get { first }.isEqualTo("test")
            get { second.asBooleanValue() }.isNull()
        }
    }

    @Test
    fun `test nodeObject`() {
        val node = nodeObject("test" toNode "value")
        expectThat(node) {
            get { _fieldMap.size }.isEqualTo(1)
            get { _fieldMap["test"]?.asStringValue() }.isEqualTo("value")
        }
    }

    @Test
    fun `test asStringValue with non-null value`() {
        val node = JsonNodeString("value", NodePathRoot + "test")
        expectThat(node.asStringValue()).isEqualTo("value")
    }

    @Test
    fun `test asStringValue with null value`() {
        val node = null as JsonNode?
        expectThat(node.asStringValue()).isNull()
    }

    @Test
    fun `test asBooleanValue with non-null value`() {
        val node = JsonNodeBoolean(true, NodePathRoot + "test")
        expectThat(node.asBooleanValue()).isEqualTo(true)
    }

    @Test
    fun `test asBooleanValue with null value`() {
        val node = null as JsonNode?
        expectThat(node.asBooleanValue()).isNull()
    }

    @Test
    fun `test asNumValue with non-null value`() {
        val node = JsonNodeNumber(BigDecimal(123), NodePathRoot + "test")
        expectThat(node.asNumValue()).isEqualTo(BigDecimal(123))
    }

    @Test
    fun `test asNumValue with null value`() {
        val node = null as JsonNode?
        expectThat(node.asNumValue()).isNull()
    }

    @Test
    fun `test building complex JsonNodeObject`() {
        val node = nodeObject(
            "stringField" toNode "stringValue",
            "nullStringField" toNode null as String?,
            "intField" toNode 123.toInt(),
            "nullIntField" toNode null as Int?,
            "doubleField" toNode 123.456,
            "nullDoubleField" toNode null as Double?,
            "longField" toNode 123L,
            "nullLongField" toNode null as Long?,
            "booleanField" toNode true,
            "nullBooleanField" toNode null as Boolean?,
            "objectField" toNode mapOf(
                "nestedStringField" to "nestedStringValue",
                "nestedNullField" to null
            )
        )

        prettyWithNulls.render(node).isEquivalentJson(
            """
           {
              "booleanField": true,
              "doubleField": 123.456,
              "intField": 123,
              "longField": 123,
              "nullBooleanField": null,
              "nullDoubleField": null,
              "nullIntField": null,
              "nullLongField": null,
              "nullStringField": null,
              "objectField": {
                  "nestedNullField": null,
                  "nestedStringField": "nestedStringValue"
                },
              "stringField": "stringValue"
            }
        """.trimIndent()
        ).expectSuccess()

    }

}