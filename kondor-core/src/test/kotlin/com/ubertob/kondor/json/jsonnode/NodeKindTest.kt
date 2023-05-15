package com.ubertob.kondor.json.jsonnode

import com.ubertob.kondor.json.parser.pretty
import com.ubertob.kondortools.expectSuccess
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo

class NodeKindTest {

    @Test
    fun `from Json to JsonNode`() {
        val jsonString = """
          {
            "id": 123,
            "name": "Ann",
            "somethingelse": null
          }
        """.trimIndent()

        val jsonNode: JsonNodeObject = ObjectNode.fromJsonString(jsonString).expectSuccess()

        expectThat(jsonNode._fieldMap.keys).containsExactly(setOf("id", "name", "somethingelse"))
        expectThat(jsonNode.pretty(true)).isEqualTo(jsonString)
    }
}