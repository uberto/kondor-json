package com.ubertob.kondor.json

import JsonLexer
import com.ubertob.kondor.expectSuccess
import com.ubertob.kondor.lowercase
import com.ubertob.kondor.randomString
import com.ubertob.kondor.text
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.math.BigDecimal
import kotlin.random.Random

class JsonParserTest {


    @Test
    fun `render Boolean`() {
        val value = true

        val jsonString = JsonNodeBoolean(value, NodeRoot).render()

        expectThat(jsonString).isEqualTo("true")
    }

    @Test
    fun `parse Boolean`() {

        repeat(3) {

            val value = Random.nextBoolean()

            val jsonString = JsonNodeBoolean(value, NodeRoot).render()

            val tokens = JsonLexer(jsonString).tokenize()

            val node = parseJsonNodeBoolean(tokens, NodeRoot).expectSuccess()

            expectThat(node.value).isEqualTo(value)
            expectThat(tokens.position()).isEqualTo(jsonString.length)
        }
    }

    @Test
    fun `render exp Num`() {
        val value = Double.MIN_VALUE

        val jsonString = JsonNodeNumber(value.toBigDecimal(), NodeRoot).render()

        expectThat(jsonString).isEqualTo("4.9E-324")
    }

    @Test
    fun `render decimal Num`() {
        val num = "123456789123456789.01234567890123456789"
        val value = BigDecimal(num)

        val jsonString = JsonNodeNumber(value, NodeRoot).render()

        expectThat(jsonString).isEqualTo(num)
    }

    @Test
    fun `render integer Num`() {
        val value = Int.MAX_VALUE.toDouble()

        val jsonString = JsonNodeNumber(value.toBigDecimal(), NodeRoot).render()

        expectThat(jsonString).isEqualTo("2147483647")
    }

    @Test
    fun `parse Num`() {

        repeat(10) {

            val value = Random.nextDouble().toBigDecimal()

            val jsonString = JsonNodeNumber(value, NodeRoot).render()

            val tokens = JsonLexer(jsonString).tokenize()

            val node = parseJsonNodeNum(tokens, NodeRoot).expectSuccess()

            expectThat(node.num).isEqualTo(value)
            expectThat(tokens.position()).isEqualTo(jsonString.length)
        }

        repeat(10) {

            val value = Random.nextLong().toBigDecimal()

            val jsonString = JsonNodeNumber(value, NodeRoot).render()

            val tokens = JsonLexer(jsonString).tokenize()

            val node = parseJsonNodeNum(tokens, NodeRoot).expectSuccess()

            expectThat(node.num).isEqualTo(value)
            expectThat(tokens.position()).isEqualTo(jsonString.length)
        }

        repeat(10) {

            val value = Random.nextLong().toBigDecimal().pow(10)

            val jsonString = JsonNodeNumber(value, NodeRoot).render()

//            println("$value -> $jsonString")

            val tokens = JsonLexer(jsonString).tokenize()

            val node = parseJsonNodeNum(tokens, NodeRoot).expectSuccess()

            expectThat(node.num).isEqualTo(value)
            expectThat(tokens.position()).isEqualTo(jsonString.length)
        }

        repeat(10) {

            val value = Random.nextDouble().toBigDecimal().pow(10)

            val jsonString = JsonNodeNumber(value, NodeRoot).render()

//            println("$value -> $jsonString")

            val tokens = JsonLexer(jsonString).tokenize()

            val node = parseJsonNodeNum(tokens, NodeRoot).expectSuccess()

            expectThat(node.num).isEqualTo(value)
            expectThat(tokens.position()).isEqualTo(jsonString.length)
        }

    }

    @Test
    fun `render String`() {
        val value = """ abc {} \\ , : [] " \n 123"""

        val jsonString = JsonNodeString(value, NodeRoot).render()

        expectThat(jsonString).isEqualTo("""" abc {} \\ , : [] \" \n 123"""")
    }

    @Test
    fun `parse simple String`() {

        repeat(10) {
            val value = randomString(lowercase, 3, 3)

            val jsonString = JsonNodeString(value, NodeRoot).render()

            val tokens = JsonLexer(jsonString).tokenize()

            val node = parseJsonNodeString(tokens, NodeRoot).expectSuccess()

            expectThat(node.text).isEqualTo(value)
            expectThat(tokens.position()).isEqualTo(jsonString.length)
        }
    }

    @Test
    fun `parse String`() {

        repeat(100) {
            val value = randomString(text, 1, 10)

            val jsonString = JsonNodeString(value, NodeRoot).render()

//            println("$value -> $jsonString")

            val tokens = JsonLexer(jsonString).tokenize()

            val node = parseJsonNodeString(tokens, NodeRoot).expectSuccess()

            expectThat(node.text).isEqualTo(value)
            expectThat(tokens.position()).isEqualTo(jsonString.length)
        }
    }

    @Test
    fun `render null`() {
        val jsonString = JsonNodeNull(NodeRoot).render()

        expectThat(jsonString).isEqualTo("null")
    }

    @Test
    fun `parse Null`() {

        val jsonString = JsonNodeNull(NodeRoot).render()

        val tokens = JsonLexer(jsonString).tokenize()

        parseJsonNodeNull(tokens, NodeRoot).expectSuccess()

        expectThat(tokens.position()).isEqualTo(jsonString.length)
    }

    @Test
    fun `render array`() {
        val jsonString =
            JsonNodeArray(listOf(JsonNodeString("abc", NodeRoot), JsonNodeString("def", NodeRoot)), NodeRoot).render()

        expectThat(jsonString).isEqualTo("""["abc", "def"]""")
    }

    @Test
    fun `parse array`() {

        val jsonString = """
            ["abc", "def"]
        """.trimIndent()

        val tokens = JsonLexer(jsonString).tokenize()

        val nodes = parseJsonNodeArray(tokens, NodeRoot).expectSuccess()

        expectThat(nodes.render()).isEqualTo("""["abc", "def"]""")
        expectThat(tokens.position()).isEqualTo(jsonString.length)
    }

    @Test
    fun `parse empty array nested`() {

        val jsonString = "[[],[]]".trimIndent()

        val tokens = JsonLexer(jsonString).tokenize()

        val nodes = parseJsonNodeArray(tokens, NodeRoot).expectSuccess()

        expectThat(nodes.render()).isEqualTo("[[], []]")
        expectThat(tokens.position()).isEqualTo(jsonString.length)
    }

    @Test
    fun `render object`() {
        val jsonString = JsonNodeObject(
            mapOf("id" to JsonNodeNumber(123.toBigDecimal(), NodeRoot), "name" to JsonNodeString("Ann", NodeRoot)),
            NodeRoot
        ).render()

        val expected = """{"id": 123, "name": "Ann"}"""
        expectThat(jsonString).isEqualTo(expected)
    }

    @Test
    fun `parse an object`() {

        val jsonString = """
          {
            "id": 123,
            "name": "Ann"
          }
        """.trimIndent()

        val tokens = JsonLexer(jsonString).tokenize()

        val nodes = parseJsonNodeObject(
            tokens,
            NodeRoot
        ).expectSuccess()

        val expected = """{"id": 123, "name": "Ann"}"""
        expectThat(nodes.render()).isEqualTo(expected)
        expectThat(tokens.position()).isEqualTo(jsonString.length)
    }

    @Test
    fun `parse empty object`() {

        val jsonString = "{}".trimIndent()

        val tokens = JsonLexer(jsonString).tokenize()

        val nodes = parseJsonNodeObject(tokens, NodeRoot).expectSuccess()

        expectThat(nodes.render()).isEqualTo("{}")
        expectThat(tokens.position()).isEqualTo(jsonString.length)
    }

}