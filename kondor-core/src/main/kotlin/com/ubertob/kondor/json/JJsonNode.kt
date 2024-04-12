package com.ubertob.kondor.json

import com.ubertob.kondor.json.JsonStyle.Companion.appendNode
import com.ubertob.kondor.json.jsonnode.*
import com.ubertob.kondor.outcome.asSuccess

object JJsonNode : ObjectNodeConverter<JsonNodeObject> {
    override val _nodeType = ObjectNode
    override fun toJsonNode(value: JsonNodeObject, path: NodePath): JsonNodeObject =
        value

    override fun fromJsonNode(node: JsonNodeObject): JsonOutcome<JsonNodeObject> =
        node.asSuccess()

    override fun fieldAppenders(valueObject: JsonNodeObject): List<NamedAppender> =
        valueObject._fieldMap
            .map { (key, value) ->
                key to valueAppender(key, value)
            }
            .sortedBy { it.first }

    private fun valueAppender(propName: String, node: JsonNode): ValueAppender? =
        if (node is JsonNodeNull)
            null
        else
            { style, off -> appendNode(node, style, off) }
}