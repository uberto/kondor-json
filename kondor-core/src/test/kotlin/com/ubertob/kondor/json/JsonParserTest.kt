package com.ubertob.kondor.json

import JsonLexer
import com.ubertob.kondor.expectSuccess
import com.ubertob.kondor.randomString
import com.ubertob.kondor.text
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.math.BigDecimal
import kotlin.random.Random

class JsonParserTest {




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
    fun `render decimal Num`() {
        val num = "123456789123456789.01234567890123456789"
        val value = BigDecimal(num)

        val jsonString = JsonNodeNumber(value, NodeRoot).render()

        expectThat(jsonString).isEqualTo(num)
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
    fun `parse empty String`() {

        val value = ""

        val jsonString = JsonNodeString(value, NodeRoot).render()

        val tokens = JsonLexer(jsonString).tokenize()

        val node = parseJsonNodeString(tokens, NodeRoot).expectSuccess()

        expectThat(node.text).isEqualTo(value)
        expectThat(tokens.position()).isEqualTo(jsonString.length)
    }

    @Test
    fun `parse quote String`() {

        val value = "\""

        val jsonString = JsonNodeString(value, NodeRoot).render()

        val tokens = JsonLexer(jsonString).tokenize()

        val node = parseJsonNodeString(tokens, NodeRoot).expectSuccess()

        expectThat(node.text).isEqualTo(value)
        expectThat(tokens.position()).isEqualTo(jsonString.length)
    }

    @Test
    fun `parse String`() {

        repeat(1000) {
            val value = randomString(text, 0, 10)
            val jsonString = JsonNodeString(value, NodeRoot).render()

//            println("$value -> $jsonString")

            val tokens = JsonLexer(jsonString).tokenize()

            val node = parseJsonNodeString(tokens, NodeRoot).expectSuccess()

//            println("-> ${node.text}")

            expectThat(node.text).isEqualTo(value)
            expectThat(tokens.position()).isEqualTo(jsonString.length)
        }
    }



    @Test
    fun `parse Null`() {

        val jsonString = JsonNodeNull(NodeRoot).render()

        val tokens = JsonLexer(jsonString).tokenize()

        parseJsonNodeNull(tokens, NodeRoot).expectSuccess()

        expectThat(tokens.position()).isEqualTo(jsonString.length)
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