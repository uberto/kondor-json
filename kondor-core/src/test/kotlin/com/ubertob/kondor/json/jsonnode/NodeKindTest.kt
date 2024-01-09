package com.ubertob.kondor.json.jsonnode

import com.ubertob.kondor.json.FromJson
import com.ubertob.kondor.json.JsonOutcome
import com.ubertob.kondor.json.JsonPropertyError
import com.ubertob.kondor.json.JsonStyle.Companion.prettyWithNulls
import com.ubertob.kondor.json.render
import com.ubertob.kondor.outcome.failIfNull
import com.ubertob.kondortools.expectSuccess
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo

class NodeKindTest {

    val jsonString = """
          {
            "id": 123,
            "name": "Ann",
            "somethingelse": null
          }
        """.trimIndent()

    @Test
    fun `from Json to JsonNode`() {
        val jsonNode: JsonNodeObject = ObjectNode.fromJsonString(jsonString).expectSuccess()

        expectThat(jsonNode._fieldMap.keys).containsExactly(setOf("id", "name", "somethingelse"))
        expectThat(jsonNode.render(prettyWithNulls)).isEqualTo(jsonString)
    }


    object ExtractId : FromJson<Long> {
        override fun fromJson(json: String): JsonOutcome<Long> =
            ObjectNode.fromJsonString(json)
                .transform { it._fieldMap["id"].asNumValue()?.toLong() }
                .failIfNull { JsonPropertyError(NodePathRoot, "id", "expected Number!") }
    }

    @Test
    fun `extracting only a field`() {
        val id = ExtractId.fromJson(jsonString).expectSuccess()

        expectThat(id).isEqualTo(123)
    }


}