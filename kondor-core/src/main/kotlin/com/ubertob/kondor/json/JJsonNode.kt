package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.JsonNodeObject
import com.ubertob.kondor.json.jsonnode.NodePath
import com.ubertob.kondor.json.jsonnode.ObjectNode
import com.ubertob.kondor.outcome.asSuccess

object JJsonNode : ObjectNodeConverter<JsonNodeObject> {
    override val _nodeType = ObjectNode

    override fun toJsonNode(value: JsonNodeObject): JsonNodeObject =
        value

    override fun fromJsonNode(node: JsonNodeObject, path: NodePath): JsonOutcome<JsonNodeObject> =
        node.asSuccess()

}