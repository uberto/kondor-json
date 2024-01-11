package com.ubertob.kondor.json

import com.ubertob.kondor.json.JsonStyle.Companion.appendNode
import com.ubertob.kondor.json.JsonStyle.Companion.appendText
import com.ubertob.kondor.json.jsonnode.*
import com.ubertob.kondor.outcome.asSuccess

object JJsonNode : ObjectNodeConverter<JsonObjectNode> {
    override val _nodeType = ObjectNode
    override fun toJsonNode(value: JsonObjectNode): JsonObjectNode =
        value

    override fun fromJsonNode(node: JsonObjectNode, path: NodePath): JsonOutcome<JsonObjectNode> =
        node.asSuccess()

    override fun fieldAppenders(valueObject: JsonNodeObject): List<NamedAppender> =
        valueObject._fieldMap
            .map { (key, value) ->
                key to valueAppender(key, value)
            }
            .sortedBy { it.first }

    private fun valueAppender(propName: String, node: JsonNode): PropertyAppender? =
        if (node is JsonNodeNull) null
        else { style, off ->
            appendText(propName)
            style.appendValueSeparator(this)
                .appendNode(node, style, off)
        }
}