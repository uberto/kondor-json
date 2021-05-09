package com.ubertob.kondor.json.jsonnode

import com.ubertob.kondor.json.JsonOutcome
import com.ubertob.kondor.json.parser.*

sealed class NodeKind<JN : JsonNode>(
    val desc: String,
    val parse: (tokensStream: TokensStream, path: NodePath) -> JsonOutcome<JN>
)

object ArrayNode : NodeKind<JsonNodeArray>("Array", ::parseJsonNodeArray)
object BooleanNode : NodeKind<JsonNodeBoolean>("Boolean", ::parseJsonNodeBoolean)
object NullNode : NodeKind<JsonNodeNull>("Null", ::parseJsonNodeNull)
object NumberNode : NodeKind<JsonNodeNumber>("Number", ::parseJsonNodeNum)
object ObjectNode : NodeKind<JsonNodeObject>("Object", ::parseJsonNodeObject)
object StringNode : NodeKind<JsonNodeString>("String", ::parseJsonNodeString)