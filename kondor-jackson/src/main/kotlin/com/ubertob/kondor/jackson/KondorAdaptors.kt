package com.ubertob.kondor.jackson

import com.fasterxml.jackson.databind.node.*
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.BooleanNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.ubertob.kondor.json.ObjectNodeConverter
import com.ubertob.kondor.json.jsonnode.*
import java.math.BigDecimal
import java.math.BigInteger
import com.fasterxml.jackson.databind.JsonNode as JJsonNode
import com.ubertob.kondor.json.jsonnode.JsonNode as KJsonNode

fun <T: Any> ObjectNodeConverter<T>.toJacksonJsonNode(t: T): ObjectNode = this.toJsonNode(t).toJacksonJsonNode()

fun KJsonNode.toJacksonJsonNode(): JJsonNode {
    val json: JsonNodeFactory = JsonNodeFactory.instance

    return when (this) {
        is JsonNodeArray -> toJacksonJsonNode(json)
        is JsonNodeBoolean -> toJacksonJsonNode(json)
        is JsonNodeNull -> toJacksonJsonNode(json)
        is JsonNodeNumber -> toJacksonJsonNode(json)
        is JsonNodeObject -> toJacksonJsonNode(json)
        is JsonNodeString -> toJacksonJsonNode(json)
    }
}

fun JsonNodeString.toJacksonJsonNode(json: JsonNodeFactory = JsonNodeFactory.instance) = json.textNode(this.text)
fun JsonNodeNumber.toJacksonJsonNode(json: JsonNodeFactory = JsonNodeFactory.instance) = this.num.toNumericNode(json)
fun JsonNodeBoolean.toJacksonJsonNode(json: JsonNodeFactory = JsonNodeFactory.instance) = json.booleanNode(this.boolean)
fun JsonNodeNull.toJacksonJsonNode(json: JsonNodeFactory = JsonNodeFactory.instance) = json.nullNode()
fun JsonNodeObject.toJacksonJsonNode(json: JsonNodeFactory = JsonNodeFactory.instance) = _fieldMap.map.entries
    .fold(json.objectNode()) { obj, node ->
        obj.set<JJsonNode>(node.key, node.value.toJacksonJsonNode())
        obj
    }

fun JsonNodeArray.toJacksonJsonNode(json: JsonNodeFactory = JsonNodeFactory.instance): ArrayNode =
    elements.fold(json.arrayNode()) { array, node -> array.add(node.toJacksonJsonNode()) }

fun TextNode.toKondorJsonNode() = JsonNodeString(textValue())
fun NumericNode.toKondorJsonNode() = JsonNodeNumber(decimalValue())
fun BooleanNode.toKondorJsonNode() = JsonNodeBoolean(booleanValue())
fun NullNode.toKondorJsonNode() = JsonNodeNull
fun ArrayNode.toKondorJsonNode() =
    JsonNodeArray(mapIndexedNotNull { i, node -> node.toKondorJsonNode() })

fun JJsonNode.toKondorJsonNode(): KJsonNode {
    return when (this) {
        is TextNode -> toKondorJsonNode()
        is NumericNode -> toKondorJsonNode()
        is BooleanNode -> toKondorJsonNode()
        is NullNode -> toKondorJsonNode()
        is ArrayNode -> toKondorJsonNode()
        is ObjectNode -> toKondorJsonNode()
        else -> throw IllegalArgumentException("Unknown Jackson JsonNode: $this")
    }
}

fun ObjectNode.toKondorJsonNode(): JsonNodeObject {
    val map = properties().associate { (key, node) -> key to node.toKondorJsonNode() }

    return JsonNodeObject(map)
}

private fun Number.toNumericNode(json: JsonNodeFactory): ValueNode {
    return when (this) {
        is Byte-> json.numberNode(this)
        is Short-> json.numberNode(this)
        is Int -> json.numberNode(this)
        is Long -> json.numberNode(this)
        is BigInteger -> json.numberNode(this)
        is Float -> json.numberNode(this)
        is Double -> json.numberNode(this)
        is BigDecimal -> json.numberNode(this)
        else -> throw IllegalArgumentException("Unknown number type: $this")
    }
}

fun <T: Any> T.intoJacksonJsonNode(converter: ObjectNodeConverter<T>): ObjectNode = converter.toJacksonJsonNode(this)

