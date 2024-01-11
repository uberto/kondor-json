package com.ubertob.kondor.json

import com.ubertob.kondor.json.JsonStyle.Companion.compact
import com.ubertob.kondor.json.JsonStyle.Companion.compactWithNulls
import com.ubertob.kondor.json.JsonStyle.Companion.pretty
import com.ubertob.kondor.json.JsonStyle.Companion.prettyWithNulls
import com.ubertob.kondor.json.jsonnode.*
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import kotlin.random.Random

class JsonRenderTest {

    @Test
    fun `render null`() {
        val jsonString = JsonNodeNull.render()

        expectThat(jsonString).isEqualTo("null")
    }

    @Test
    fun `render Boolean`() {
        val value = true

        val jsonString = JsonNodeBoolean(value).render()

        expectThat(jsonString).isEqualTo("true")
    }

    @Test
    fun `render exp Num`() {
        val value = Double.MIN_VALUE

        val jsonString = JsonNodeNumber(value.toBigDecimal()).render()

        expectThat(jsonString).isEqualTo("4.9E-324")
    }

    @Test
    fun `render integer Num`() {
        val value = Int.MAX_VALUE.toDouble()

        val jsonString = JsonNodeNumber(value.toBigDecimal()).render()

        expectThat(jsonString).isEqualTo("2147483647")
    }

    @Test
    fun `render escaped String`() {
        val value = "abc {} \\ , : [] \" \n \t \r 123"

        val jsonString = JsonNodeString(value).render()

        expectThat(jsonString).isEqualTo(""""abc {} \\ , : [] \" \n \t \r 123"""")
    }


    @Test
    fun `render array`() {
        val jsonString =
            JsonNodeArray(
                listOf(JsonNodeString("abc"), JsonNodeString("def"))
            ).render()

        expectThat(jsonString).isEqualTo("""["abc","def"]""")
    }

    @Test
    fun `render array with nulls`() {
        val jsonString =
            JsonNodeArray(
                listOf(
                    JsonNodeString("abc"),
                    JsonNodeNull,
                    JsonNodeString("def")
                )
            ).render()

        expectThat(jsonString).isEqualTo("""["abc","def"]""")
    }

    @Test
    fun `pretty render array`() {
        val nodeArray = JsonNodeArray(
            listOf(
                JsonNodeString("abc"),
                JsonNodeNull,
                JsonNodeString("def")
            )
        )
        val jsonString = pretty.render(nodeArray)

        expectThat(jsonString).isEqualTo(
            """[
            |  "abc",
            |  "def"
            |]""".trimMargin()
        )

        val jsonStringNN = prettyWithNulls.render(nodeArray)

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
        val jsonString = JsonObjectNode(
            mapOf(
                "id" to JsonNodeNumber(123.toBigDecimal()),
                "name" to JsonNodeString("Ann")
            )
        ).render()

        val expected = """{"id":123,"name":"Ann"}"""
        expectThat(jsonString).isEqualTo(expected)
    }


    @Test
    fun `render object with nulls`() {
        val jsonString = JsonObjectNode(
            mapOf(
                "id" to JsonNodeNumber(123.toBigDecimal()),
                "name" to JsonNodeString("Ann"),
                "nullable" to JsonNodeNull
            )
        ).render()

        val expected = """{"id":123,"name":"Ann"}"""
        expectThat(jsonString).isEqualTo(expected)
    }

    @Test
    fun `pretty render object`() {

        repeat(5) {
            val indent = Random.nextInt(8)
            val style = pretty.copy(indent = indent)
            val jsonString = JsonObjectNode(
                mapOf(
                    "id" to JsonNodeNumber(123.toBigDecimal()),
                    "name" to JsonNodeString("Ann")
                )
            ).render(style)

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
        val nodeObject = JsonObjectNode(
            mapOf(
                "id" to JsonNodeNumber(123.toBigDecimal()),
                "name" to JsonNodeString("Ann"),
                "somethingelse" to JsonNodeNull
            )
        )
        val jsonString = prettyWithNulls.render(nodeObject)

        val expected = """{
              |  "id": 123,
              |  "name": "Ann",
              |  "somethingelse": null
              |}""".trimMargin()
        expectThat(jsonString).isEqualTo(expected)

        val jsonStringNN = pretty.render(nodeObject)

        val expectedNN = """{
              |  "id": 123,
              |  "name": "Ann"
              |}""".trimMargin()
        expectThat(jsonStringNN).isEqualTo(expectedNN)
    }

    @Test
    fun `compact render object`() {
        val jsonString = JsonObjectNode(
            mapOf(
                "id" to JsonNodeNumber(123.toBigDecimal()),
                "name" to JsonNodeString("Ann"),
                "nullable" to JsonNodeNull
            )
        ).render(compact)

        expectThat(jsonString).isEqualTo("""{"id":123,"name":"Ann"}""")
    }

    @Test
    fun `compact render object with null explicit`() {
        val jsonString = JsonObjectNode(
            mapOf(
                "id" to JsonNodeNumber(123.toBigDecimal()),
                "name" to JsonNodeString("Ann"),
                "nullable" to JsonNodeNull,
                "arrayNullable" to JsonNodeArray(
                    listOf(
                        JsonNodeString("Bob"),
                        JsonNodeNull
                    )
                ),
                "objectNullable" to JsonObjectNode(
                    mapOf(
                        "one" to JsonNodeString("two"),
                        "three" to JsonNodeNull
                    )
                )
            )
        ).render(compactWithNulls)

        expectThat(jsonString).isEqualTo("""{"id":123,"name":"Ann","nullable":null,"arrayNullable":["Bob",null],"objectNullable":{"one":"two","three":null}}""")
    }

    @Test
    fun `using converter with different default style`() {
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