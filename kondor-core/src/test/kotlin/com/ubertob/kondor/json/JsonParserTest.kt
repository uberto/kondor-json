package com.ubertob.kondor.json

import com.ubertob.kondor.*
import com.ubertob.kondor.json.jsonnode.*
import com.ubertob.kondor.json.parser.*
import com.ubertob.kondortools.expectSuccess
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import java.math.BigDecimal
import kotlin.random.Random

class JsonParserTest {

    private fun tokenize(jsonStr: String) = KondorTokenizer.tokenize(jsonStr)

    @Test
    fun `parse Boolean`() {

        repeat(3) {

            val value = Random.nextBoolean()

            val jsonString = JsonNodeBoolean(value).render()

            val tokens = tokenize(jsonString).expectSuccess()

            val node = tokens.onRoot().parseJsonNodeBoolean().expectSuccess()

            expectThat(node.boolean).isEqualTo(value)
            expectThat(lastPosRead(tokens)).isEqualTo(jsonString.length)
        }
    }


    @Test
    fun `render decimal Num`() {
        val num = "123456789123456789.01234567890123456789"
        val value = BigDecimal(num)

        val jsonString = JsonNodeNumber(value).render()

        expectThat(jsonString).isEqualTo(num)
    }


    @Test
    fun `parse Num`() {

        repeat(10) {

            val value = Random.nextDouble().toBigDecimal()

            val jsonString = JsonNodeNumber(value).render()

            val tokens = tokenize(jsonString).expectSuccess()

            val node = tokens.onRoot().parseJsonNodeNum().expectSuccess()

            expectThat(node.num).isEqualTo(value)
            expectThat(lastPosRead(tokens)).isEqualTo(jsonString.length)
        }

        repeat(10) {

            val value = Random.nextLong().toBigDecimal()

            val jsonString = JsonNodeNumber(value).render()

            val tokens = tokenize(jsonString).expectSuccess()

            val node = tokens.onRoot().parseJsonNodeNum().expectSuccess()

            expectThat(node.num).isEqualTo(value)
            expectThat(lastPosRead(tokens)).isEqualTo(jsonString.length)
        }

        repeat(10) {

            val value = Random.nextLong().toBigDecimal().pow(10)

            val jsonString = JsonNodeNumber(value).render()

//            println("$value -> $jsonString")

            val tokens = tokenize(jsonString).expectSuccess()

            val node = tokens.onRoot().parseJsonNodeNum().expectSuccess()

            expectThat(node.num).isEqualTo(value)
            expectThat(lastPosRead(tokens)).isEqualTo(jsonString.length)
        }

        repeat(10) {

            val value = Random.nextDouble().toBigDecimal().pow(10)

            val jsonString = JsonNodeNumber(value).render()

//            println("$value -> $jsonString")

            val tokens = tokenize(jsonString).expectSuccess()

            val node = tokens.onRoot().parseJsonNodeNum().expectSuccess()

            expectThat(node.num).isEqualTo(value)
            expectThat(lastPosRead(tokens)).isEqualTo(jsonString.length)
        }

    }

    @Test
    fun `parse NaN Double`() {

        val value = Double.NaN

        val jsonString = JsonNodeNumber(value).render()

        val tokens = tokenize(jsonString).expectSuccess()

        val node = tokens.onRoot().parseJsonNodeNum().expectSuccess()

        expectThat(node.num).isEqualTo(value)
        expectThat(lastPosRead(tokens)).isEqualTo(jsonString.length)
    }


    @Test
    fun `parse empty String`() {

        val value = ""

        val jsonString = JsonNodeString(value).render()

        val tokens = tokenize(jsonString).expectSuccess()

        val node = tokens.onRoot().parseJsonNodeString().expectSuccess()

        expectThat(node.text).isEqualTo(value)
        expectThat(lastPosRead(tokens)).isEqualTo(jsonString.length)
    }

    @Test
    fun `parse quote String`() {

        val value = "\""

        val jsonString = JsonNodeString(value).render()

        val tokens = tokenize(jsonString).expectSuccess()

        val node = tokens.onRoot().parseJsonNodeString().expectSuccess()

        expectThat(node.text).isEqualTo(value)
        expectThat(lastPosRead(tokens)).isEqualTo(jsonString.length)
    }

    @Test
    fun `parse String`() {

        repeat(1000) {
            val value = randomString(text, 0, 10)
            val jsonString = JsonNodeString(value).render()

//            println("$value -> $jsonString")

            val tokens = tokenize(jsonString).expectSuccess()

            val node = tokens.onRoot().parseJsonNodeString().expectSuccess()

//            println("-> ${node.text}")

            expectThat(node.text).isEqualTo(value)
            expectThat(lastPosRead(tokens)).isEqualTo(jsonString.length)
        }
    }

    @Test
    fun `parse unicode String`() {

        repeat(100) {
            val value = randomString(digits + latin1 + japanese, 0, 10)
            val jsonString = JsonNodeString(value).render()

//            println("$value -> $jsonString")

            val tokens = tokenize(jsonString).expectSuccess()

            val node = tokens.onRoot().parseJsonNodeString().expectSuccess()

//            println("-> ${node.text}")

            expectThat(node.text).isEqualTo(value)
            expectThat(lastPosRead(tokens)).isEqualTo(jsonString.length)
        }
    }

    @Test
    fun `parse Null`() {

        val jsonString = JsonNodeNull.render()

        val tokens = tokenize(jsonString).expectSuccess()

        tokens.onRoot().parseJsonNodeNull().expectSuccess()

        expectThat(lastPosRead(tokens)).isEqualTo(jsonString.length)
    }


    @Test
    fun `parse array`() {

        val jsonString = """
            ["abc", null, "def"]
        """.trimIndent()

        val tokens = tokenize(jsonString).expectSuccess()

        val nodes = tokens.onRoot().parseJsonNodeArray().expectSuccess()

        expectThat(nodes.elements.count()).isEqualTo(3)
        expectThat(nodes.render()).isEqualTo("""["abc","def"]""")
        expectThat(lastPosRead(tokens)).isEqualTo(jsonString.length)
    }

    @Test
    fun `parse empty array nested`() {

        val jsonString = "[[],[]]".trimIndent()

        val tokens = tokenize(jsonString).expectSuccess()

        val nodes = tokens.onRoot().parseJsonNodeArray().expectSuccess()

        expectThat(nodes.render()).isEqualTo("[[],[]]")
        expectThat(lastPosRead(tokens)).isEqualTo(jsonString.length)
    }

    @Test
    fun `parse an object`() {

        val jsonString = """{"id": 123, "name": "Ann"}"""

        val tokens = tokenize(jsonString).expectSuccess()

        val nodes = tokens.onRoot().parseJsonNodeObject().expectSuccess()

        val expected = """{"id":123,"name":"Ann"}"""
        expectThat(nodes.render()).isEqualTo(expected)
        expectThat(lastPosRead(tokens)).isEqualTo(jsonString.length)
    }

    @Test
    fun `parse empty object`() {

        val jsonString = "{}".trimIndent()

        val tokens = tokenize(jsonString).expectSuccess()

        val nodes = tokens.onRoot().parseJsonNodeObject().expectSuccess()

        expectThat(nodes.render()).isEqualTo("{}")
        expectThat(lastPosRead(tokens)).isEqualTo(jsonString.length)
    }

    @Test
    fun `parse an object with nulls`() {

        val jsonString = """{"id": 123, "name": "Ann", "somethingelse": null}"""

        val tokens = tokenize(jsonString).expectSuccess()

        val nodes = tokens.onRoot().parseJsonNodeObject().expectSuccess()

        expectThat(nodes._fieldMap.map.size).isEqualTo(3)

        val expected = """{"id":123,"name":"Ann"}"""
        expectThat(nodes.render()).isEqualTo(expected)
        expectThat(lastPosRead(tokens)).isEqualTo(jsonString.length)
    }

    private fun lastPosRead(tokens: TokensStream): Int =
        tokens.lastPosRead()

    @Test
    fun `parse an object with JsonNode field`() {
        val jsonString = """
          {
            "id": 123,
            "name": "Ann",
            "aString": "String",
            "aObj": {
              "aNestedString": "NestedString",
              "aNestedNum": 123123
            }
          }
        """.trimIndent()

        val objWithDynamicAttr = JDynamicAttr.fromJson(jsonString).expectSuccess()

        expectThat(objWithDynamicAttr) {
            get { id }.isEqualTo(123)
            get { name }.isEqualTo("Ann")
            get { attributes._fieldMap.map.get("aString") }.isEqualTo(
                JsonNodeString(
                    "String"
                )
            )
            get { attributes._fieldMap.map.get("aObj") }.isA<JsonNodeObject>()
        }

        expectThat(objWithDynamicAttr.attributes._fieldMap.map.get("aObj") as? JsonNodeObject).isNotNull()
            .and {
                get { (_fieldMap.map.get("aNestedString") as? JsonNodeString)?.text }.isEqualTo("NestedString")
                get { (_fieldMap.map.get("aNestedNum") as JsonNodeNumber).num.toInt() }.isEqualTo(123123)
            }
    }

    @Test
    fun `parse huge object`() {
        repeat(10) {
            val bigMetadata = (1..100).associate {
                it.toString() to randomString(all, it * 1000, it * 1000)
            }
            val expected = MetadataFile(
                filename = randomText(100),
                metadata = bigMetadata
            )

            val json = JMetadataFile.toJson(expected)
            val actual = JMetadataFile.fromJson(json).expectSuccess()

            expectThat(actual).isEqualTo(expected)
        }
    }
}