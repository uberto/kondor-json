package com.ubertob.kondor.json

import com.ubertob.kondor.json.JsonStyle.Companion.appendNode
import com.ubertob.kondor.json.jsonnode.JsonNode
import com.ubertob.kondor.json.jsonnode.JsonNodeNull
import com.ubertob.kondor.json.jsonnode.JsonNodeObject
import com.ubertob.kondor.json.jsonnode.ObjectNode
import com.ubertob.kondor.outcome.asSuccess

object JJsonNode : ObjectNodeConverter<JsonNodeObject> {
    override val _nodeType = ObjectNode
    override fun toJsonNode(value: JsonNodeObject): JsonNodeObject =
        value

    override fun fromJsonNode(node: JsonNodeObject): JsonOutcome<JsonNodeObject> =
        node.asSuccess()

    override fun fieldAppenders(valueObject: JsonNodeObject): List<NamedAppender> =
        valueObject._fieldMap
            .map { (key, value) ->
                key to valueAppender(value)
            }
            .sortedBy { it.first }

    private fun valueAppender(node: JsonNode): ValueAppender? =
        if (node is JsonNodeNull) null
        else { style, off ->
            style.appendValueSeparator(this)
                .appendNode(node, style, off)
        }
}