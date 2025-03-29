package com.ubertob.kondor.jackson

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.BaseJsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.ubertob.kondor.json.*
import com.ubertob.kondor.json.JsonStyle.Companion.prettyWithNulls
import com.ubertob.kondor.json.jsonnode.*
import com.ubertob.kondortools.expectSuccess
import com.ubertob.kondortools.isEquivalentJson
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.math.BigDecimal
import java.util.stream.Stream
import com.fasterxml.jackson.databind.JsonNode as JJsonNode
import com.ubertob.kondor.json.jsonnode.JsonNode as KJsonNode


class KondorAdaptorsTests {
    @ParameterizedTest
    @MethodSource("kondorToJackson")
    fun `kondor JsonNode to jackson JsonNode`(kondorNode: KJsonNode, expectedJacksonNode: JJsonNode) {
        val actualJacksonNode = kondorNode.toJacksonJsonNode()
        expectThat(actualJacksonNode).equalJsonTo(expectedJacksonNode)
        expectThat(actualJacksonNode).isEqualTo(expectedJacksonNode)
    }

    @ParameterizedTest
    @MethodSource("kondorToJackson")
    fun `jackson JsonNode to kondor JsonNode`(expectedKondorNode: KJsonNode, jacksonNode: JJsonNode) {
        val actualKondorNode = jacksonNode.toKondorJsonNode()
        expectThat(actualKondorNode).isEqualTo(expectedKondorNode)
    }

    @ParameterizedTest
    @MethodSource("jsons")
    fun `jackson JsonNode to kondor JsonNode - from JSON string`(json: String) {
        val j: JJsonNode = mapper.readTree(json)

        val kJson = j.toKondorJsonNode().render(prettyWithNulls)
        val jJson = j.toPrettyString()

        jJson isEquivalentJson json
        kJson isEquivalentJson json
        kJson isEquivalentJson jJson
    }

    @ParameterizedTest
    @MethodSource("jsons")
    fun `kondor JsonNode to jackson JsonNode - from JSON string`(json: String) {
        val k: KJsonNode = parseJsonNode(json).expectSuccess()

        val jJson = k.toJacksonJsonNode().toPrettyString()
        val kJson = k.render(prettyWithNulls)

        jJson isEquivalentJson json
        kJson isEquivalentJson json
        kJson isEquivalentJson jJson
    }

    @Test
    fun `extension function to aid interop`() {
        val exampleToJacksonToKondor = JExample.toJacksonJsonNode(example).toKondorJsonNode()
        val result = JExample.fromJsonNode(exampleToJacksonToKondor, NodePathRoot).expectSuccess()
        expectThat(result).isEqualTo(example)
    }

    @Test
    fun `test complex json going to Jackson and back`() {
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

        val expectedJson = """
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
        """

        prettyWithNulls.render(node.toJacksonJsonNode().toKondorJsonNode()) isEquivalentJson expectedJson
    }


    companion object {
        @JvmStatic
        private fun kondorToJackson(): Stream<Arguments> = k2j().toPairOfArguments()

        @JvmStatic
        private fun jsons(): Stream<Arguments> = jsons.toStringArgument()
    }

    val mapper = ObjectMapper()
    val example = Example(
        name = "Example",
        age = 20,
        salary = 1.4,
        children = listOf(Example("child1", 1, 0.0, emptyList()), Example("child2", -2, -10.1, emptyList())),
        favoriteChild = Example("child3", -2, -10.1, emptyList())
    )
}

val jsons = listOf(
    """
        {
            "first": "first"
        }
    """,
    """
        {
             "array": [ "one" ]           
        }
    """,
    """
        1
    """,
    """
        [ 1 ]
    """,
    """
        {
             "array": [ 1 ]
        }
    """,
    """
        0.1
    """,
    """
        {
            "first": {
                "second": {
                    "third": {
                        "array": [ "text", 1 ],
                        "string": "string",
                        "boolean": true,
                        "number": 1,
                        "decimal": 0.1
                    }
                }
            }            
        }
    """,
)

fun k2j(): List<Pair<com.ubertob.kondor.json.jsonnode.JsonNode, BaseJsonNode>> {
    val json = JsonNodeFactory.instance
    return listOf(
        JsonNodeString("a string")
            to
            json.textNode("a string"),

        JsonNodeNumber(BigDecimal(13))
            to
            json.numberNode(BigDecimal(13)),

        JsonNodeBoolean(true)
            to
            json.booleanNode(true),

        JsonNodeNull
            to
            json.nullNode(),

        JsonNodeArray(listOf())
            to
            json.arrayNode(),

        JsonNodeArray(listOf(JsonNodeString("a string")))
            to
            json.arrayNode().add("a string"),

        JsonNodeArray(listOf(JsonNodeNumber(BigDecimal(1))))
            to
            json.arrayNode().add(BigDecimal(1)),

        JsonNodeArray(listOf(JsonNodeArray(listOf(JsonNodeString("a string")))))
            to
            json.arrayNode().add(json.arrayNode().add("a string")),

        JsonNodeObject(mapOf())
            to
            json.objectNode(),

        JsonNodeObject(mapOf("one" to JsonNodeNumber(BigDecimal(1))))
            to
            json.objectNode().put("one", BigDecimal(1))
    )
}

fun List<Pair<com.ubertob.kondor.json.jsonnode.JsonNode, BaseJsonNode>>.toPairOfArguments(): Stream<Arguments> = map { (a, b) -> Arguments.of(a, b) }.stream()
fun List<String>.toStringArgument(): Stream<Arguments> = map { Arguments.of(it) }.stream()

fun Assertion.Builder<JJsonNode>.equalJsonTo(expected: JJsonNode): Assertion.Builder<JJsonNode> {
    val mapper = ObjectMapper()
    return assertThat("have the same JSON") {
        mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this.subject) == mapper.writerWithDefaultPrettyPrinter().writeValueAsString(expected)
    }
}

data class Example(
    val name: String,
    val age: Int,
    val salary: Double,
    val children: List<Example>,
    val favoriteChild: Example? = null,
)

object JExample : JAny<Example>() {
    private val name by str(Example::name)
    private val age by num(Example::age)
    private val salary by num(Example::salary)
    private val children by array(JExample, Example::children)
    private val favoriteChild by obj(JExample, Example::favoriteChild)

    override fun JsonNodeObject.deserializeOrThrow(): Example =
        Example(
            name = +name,
            age = +age,
            salary = +salary,
            children = +children,
            favoriteChild = +favoriteChild
        )
}