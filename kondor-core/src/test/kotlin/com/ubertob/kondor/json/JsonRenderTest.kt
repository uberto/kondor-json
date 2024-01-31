package com.ubertob.kondor.json

import com.ubertob.kondor.json.JsonStyle.Companion.compact
import com.ubertob.kondor.json.JsonStyle.Companion.compactWithNulls
import com.ubertob.kondor.json.JsonStyle.Companion.pretty
import com.ubertob.kondor.json.JsonStyle.Companion.prettyWithNulls
import com.ubertob.kondor.json.jsonnode.*
import com.ubertob.kondortools.expectSuccess
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

        val jsonString = JsonNodeNumber(value, NodePathRoot).render()

        expectThat(jsonString).isEqualTo("4.9E-324")
    }

    @Test
    fun `render integer Num`() {
        val value = Int.MAX_VALUE

        val jsonString = JsonNodeNumber(value, NodePathRoot).render()

        expectThat(jsonString).isEqualTo("2147483647")
    }

    @Test
    fun `render Nan Num`() {
        val value = Double.NaN

        val jsonString = JsonNodeNumber(value, NodePathRoot).render()

        expectThat(jsonString).isEqualTo("NaN")
    }

    @Test
    fun `render directly non numeric values`() {
        expectThat(JDouble.toJson(Double.NaN)).isEqualTo(""""NaN"""")
        expectThat(JDouble.toJson(Double.NEGATIVE_INFINITY)).isEqualTo(""""-Infinity"""")
        expectThat(JDouble.toJson(Double.POSITIVE_INFINITY)).isEqualTo(""""Infinity"""")
    }

    @Test
    fun `render String field honoring Json escaping rules`() {
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
    fun `render double array with NaNs`() {
        val doubles = listOf(0.0, 1.0, -252.0, 3.14159, Double.NaN, Double.NEGATIVE_INFINITY, 16E+100)

        val jDoubles = JList(JDouble)

        val jsonString = jDoubles.toJson(doubles)

        expectThat(jsonString).isEqualTo("""[0.0, 1.0, -252.0, 3.14159, "NaN", "-Infinity", 1.6E101]""")

        val parsedDoubles = jDoubles.fromJson(jsonString).expectSuccess()

        expectThat(parsedDoubles).isEqualTo(doubles)

        val parsedDoubleNoDecimal = jDoubles.fromJson("""[0, 1, -252, 3.14159, "NaN", "-Infinity",  1.6E101]""").expectSuccess()

        expectThat(parsedDoubleNoDecimal).isEqualTo(doubles)
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
    fun `render object from node`() {
        val jsonString = JsonNodeObject(
            mapOf(
                "id" to JsonNodeNumber(123, NodePathRoot),
                "name" to JsonNodeString("Ann", NodePathRoot)
            ),
            NodePathRoot
        ).render()

        val expected = """{"id":123,"name":"Ann"}"""
        expectThat(jsonString).isEqualTo(expected)
    }

    @Test
    fun `render object with nulls`() {

        val nullOnlyObj = OptionalAddress(null, null, null)

        expectThat(JOptionalAddress.toJson(nullOnlyObj, compact))
            .isEqualTo("""{}""")

        val streetOnlyObj = OptionalAddress(null, "42 Adams Road", null)

        expectThat(JOptionalAddress.toJson(streetOnlyObj, compact))
            .isEqualTo("""{"street":"42 Adams Road"}""")

        val nameAndstreetObj = OptionalAddress("Marvin", "42 Adams Road", null)

        expectThat(JOptionalAddress.toJson(nameAndstreetObj, compact))
            .isEqualTo("""{"name":"Marvin","street":"42 Adams Road"}""")
    }

    @Test
    fun `render object with nulls from node`() {
        val jsonString = JsonNodeObject(
            mapOf(
                "firstNullable" to JsonNodeNull(NodePathRoot),
                "id" to JsonNodeNumber(123, NodePathRoot),
                "name" to JsonNodeString("Ann", NodePathRoot),
                "lastNullable" to JsonNodeNull(NodePathRoot)
            ),
            NodePathRoot
        ).render()

        val expected = """{"id":123,"name":"Ann"}"""
        expectThat(jsonString).isEqualTo(expected)
    }


    @Test
    fun `custom pretty render object`() {
        repeat(5) {
            val indent = Random.nextInt(8)

            fun customNewLine(app: CharWriter, offset: Int): CharWriter =
                app.apply {
                    app.write('\n')
                    repeat(offset * indent) {
                        app.write(' ')
                    }
                }

            val style = pretty.copy(appendNewline = ::customNewLine)
            val jsonString = JsonNodeObject(
                mapOf(
                    "id" to JsonNodeNumber(123, NodePathRoot),
                    "name" to JsonNodeString("Ann", NodePathRoot)
                ),
                NodePathRoot
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
        val nodeObject = JsonNodeObject(
            mapOf(
                "id" to JsonNodeNumber(123, path),
                "name" to JsonNodeString("Ann", path),
                "somethingelse" to JsonNodeNull(path)
            ),
            NodePathRoot
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
        val jsonString = JsonNodeObject(
            mapOf(
                "id" to JsonNodeNumber(123, NodePathRoot),
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
                "id" to JsonNodeNumber(123, NodePathRoot),
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
    fun `using converter with different default style`() {
        val addr = OptionalAddress("Jack", null, "London")
        val jsonPretty = JOptionalAddress.toJson(addr)

        expectThat(jsonPretty).isEqualTo(
            """{
              |  "city": "London",
              |  "name": "Jack",
              |  "street": null
              |}""".trimMargin()
        )
    }
}