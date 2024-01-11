package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.JsonObjectNode
import com.ubertob.kondor.json.jsonnode.NodePath
import com.ubertob.kondor.json.jsonnode.ObjectNode
import com.ubertob.kondor.outcome.asSuccess

object JJsonNode : ObjectNodeConverter<JsonObjectNode> {
    override val _nodeType = ObjectNode

    override fun toJsonNode(value: JsonObjectNode): JsonObjectNode =
        value

    override fun fromJsonNode(node: JsonObjectNode, path: NodePath): JsonOutcome<JsonObjectNode> =
        node.asSuccess()

}