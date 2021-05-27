package com.ubertob.kondor.json

import com.ubertob.kondor.expectSuccess
import com.ubertob.kondor.json.jsonnode.ObjectNode
import com.ubertob.kondor.json.jsonnode.onRoot
import com.ubertob.kondor.json.parser.JsonLexer
import com.ubertob.kondor.json.parser.pretty
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class JsonNodeTest {

    @Test
    fun `comparing two equivalent jsons`() {

        val json1 = """
            {
            "name" : "Frank",
            "age" : 32,
            "married" : true,
            "children" : ["Ann", "Bob", "Cathy"] 
            }
        """.trimIndent()

        val json2 = """ {"married": true, "age" : 32,"name" : "Frank", "children": ["Ann", "Bob",  "Cathy"]}"""

        val jn1 = ObjectNode.parse(JsonLexer(json1).tokenize().onRoot()).expectSuccess()
        val jn2 = ObjectNode.parse(JsonLexer(json2).tokenize().onRoot()).expectSuccess()


        expectThat(jn1.pretty(true, 2)).isEqualTo(jn2.pretty(true, 2))

        expectThat(jn1).isEqualTo(jn2)

    }
}