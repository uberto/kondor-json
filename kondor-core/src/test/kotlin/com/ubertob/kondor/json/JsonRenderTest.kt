package com.ubertob.kondor.json

import com.ubertob.kondor.json.JsonStyle.Companion.compact
import com.ubertob.kondor.json.JsonStyle.Companion.compactWithNulls
import com.ubertob.kondor.json.jsonnode.*
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import kotlin.random.Random

class JsonRenderTest {

    @Test
    fun `render null`() {
        val jsonString = JsonNodeNull(NodePathRoot).render()

        expectThat(jsonString).isEqualTo("null")
    }

    @Test
    fun `render Boolean`() {
        val value = true

        val jsonString = JsonNodeBoolean(value, NodePathRoot).render()

        expectThat(jsonString).isEqualTo("true")
    }

    @Test
    fun `render exp Num`() {
        val value = Double.MIN_VALUE

        val jsonString = JsonNodeNumber(value.toBigDecimal(), NodePathRoot).render()

        expectThat(jsonString).isEqualTo("4.9E-324")
    }

    @Test
    fun `render integer Num`() {
        val value = Int.MAX_VALUE.toDouble()

        val jsonString = JsonNodeNumber(value.toBigDecimal(), NodePathRoot).render()

        expectThat(jsonString).isEqualTo("2147483647")
    }

    @Test
    fun `render escaped String`() {
        val value = "abc {} \\ , : [] \" \n \t \r 123"

        val jsonString = JsonNodeString(value, NodePathRoot).render()

        expectThat(jsonString).isEqualTo(""""abc {} \\ , : [] \" \n \t \r 123"""")
    }


    @Test
    fun `render array`() {
        val jsonString =
            JsonNodeArray(
                listOf(JsonNodeString("abc", NodePathRoot), JsonNodeString("def", NodePathRoot)),
                NodePathRoot
            ).render()

        expectThat(jsonString).isEqualTo("""["abc","def"]""")
    }

    @Test
    fun `render array with nulls`() {
        val jsonString =
            JsonNodeArray(
                listOf(
                    JsonNodeString("abc", NodePathRoot),
                    JsonNodeNull(NodePathRoot),
                    JsonNodeString("def", NodePathRoot)
                ), NodePathRoot
            ).render()

        expectThat(jsonString).isEqualTo("""["abc","def"]""")
    }

    @Test
    fun `pretty render array`() {
        val nodeArray = JsonNodeArray(
            listOf(
                JsonNodeString("abc", NodePathRoot),
                JsonNodeNull(NodePathRoot),
                JsonNodeString("def", NodePathRoot)
            ),
            NodePathRoot
        )
        val jsonString = nodeArray.pretty(false, 2)

        expectThat(jsonString).isEqualTo(
            """[
            |  "abc",
            |  "def"
            |]""".trimMargin()
        )

        val jsonStringNN = nodeArray.pretty(true, 2)

        expectThat(jsonStringNN).isEqualTo(
            """[
            |  "abc",
            |  null,
            |  "def"
            |]""".trimMargin()
        )

    }

    @Test
    fun `render object`() {
        val jsonString = JsonNodeObject(
            mapOf(
                "id" to JsonNodeNumber(123.toBigDecimal(), NodePathRoot),
                "name" to JsonNodeString("Ann", NodePathRoot)
            ),
            NodePathRoot
        ).render()

        val expected = """{"id":123,"name":"Ann"}"""
        expectThat(jsonString).isEqualTo(expected)
    }


    @Test
    fun `render object with nulls`() {
        val jsonString = JsonNodeObject(
            mapOf(
                "id" to JsonNodeNumber(123.toBigDecimal(), NodePathRoot),
                "name" to JsonNodeString("Ann", NodePathRoot),
                "nullable" to JsonNodeNull(NodePathRoot)
            ),
            NodePathRoot
        ).render()

        val expected = """{"id":123,"name":"Ann"}"""
        expectThat(jsonString).isEqualTo(expected)
    }

    @Test
    fun `pretty render object`() {

        repeat(5) {
            val indent = Random.nextInt(8)
            val jsonString = JsonNodeObject(
                mapOf(
                    "id" to JsonNodeNumber(123.toBigDecimal(), NodePathRoot),
                    "name" to JsonNodeString("Ann", NodePathRoot)
                ),
                NodePathRoot
            ).pretty(false, indent)

            val internal = " ".repeat(indent)
            val expected = """{
                |$internal"id": 123,
                |$internal"name": "Ann"
                |}""".trimMargin()
            expectThat(jsonString).isEqualTo(expected)
        }
    }

    @Test
    fun `render object with null explicit`() {
        val path = NodePathRoot
        val nodeObject = JsonNodeObject(
            mapOf(
                "id" to JsonNodeNumber(123.toBigDecimal(), path),
                "name" to JsonNodeString("Ann", path),
                "somethingelse" to JsonNodeNull(path)
            ),
            NodePathRoot
        )
        val jsonString = nodeObject.pretty(true)

        val expected = """{
              |  "id": 123,
              |  "name": "Ann",
              |  "somethingelse": null
              |}""".trimMargin()
        expectThat(jsonString).isEqualTo(expected)

        val jsonStringNN = nodeObject.pretty(false)

        val expectedNN = """{
              |  "id": 123,
              |  "name": "Ann"
              |}""".trimMargin()
        expectThat(jsonStringNN).isEqualTo(expectedNN)
    }

    @Test
    fun `compact render object`() {
        val jsonString = JsonNodeObject(
            mapOf(
                "id" to JsonNodeNumber(123.toBigDecimal(), NodePathRoot),
                "name" to JsonNodeString("Ann", NodePathRoot),
                "nullable" to JsonNodeNull(NodePathRoot)
            ),
            NodePathRoot
        ).render(compact)

        expectThat(jsonString).isEqualTo("""{"id":123,"name":"Ann"}""")
    }

    @Test
    fun `compact render object with null explicit`() {
        val jsonString = JsonNodeObject(
            mapOf(
                "id" to JsonNodeNumber(123.toBigDecimal(), NodePathRoot),
                "name" to JsonNodeString("Ann", NodePathRoot),
                "nullable" to JsonNodeNull(NodePathRoot),
                "arrayNullable" to JsonNodeArray(
                    listOf(
                        JsonNodeString("Bob", NodePathRoot),
                        JsonNodeNull(NodePathRoot)
                    ), NodePathRoot
                ),
                "objectNullable" to JsonNodeObject(
                    mapOf(
                        "one" to JsonNodeString("two", NodePathRoot),
                        "three" to JsonNodeNull(NodePathRoot)
                    ),
                    NodePathRoot
                )
            ),
            NodePathRoot
        ).render(compactWithNulls)

        expectThat(jsonString).isEqualTo("""{"id":123,"name":"Ann","nullable":null,"arrayNullable":["Bob",null],"objectNullable":{"one":"two","three":null}}""")
    }

    @Test
    fun `using converter with default style`() {
        val addr = OptionalAddress("Jack", null, "London")
        val jsonPretty = JOptionalAddressPretty.toJson(addr)

        expectThat(jsonPretty).isEqualTo(
            """{
              |  "city": "London",
              |  "name": "Jack",
              |  "street": null
              |}""".trimMargin()
        )
    }
}