package com.ubertob.kondor.json.jsonnode

import com.ubertob.kondor.json.JsonOutcome
import com.ubertob.kondor.json.parser.*

sealed class NodeKind<JN : JsonNode>(
    val desc: String,
    val parse: TokensPath.() -> JsonOutcome<JN>
)

object ArrayNode : NodeKind<JsonNodeArray>("Array", TokensPath::parseJsonNodeArray)
object BooleanNode : NodeKind<JsonNodeBoolean>("Boolean", TokensPath::parseJsonNodeBoolean)
object NullNode : NodeKind<JsonNodeNull>("Null", TokensPath::parseJsonNodeNull)
object NumberNode : NodeKind<JsonNodeNumber>("Number", TokensPath::parseJsonNodeNum)
object ObjectNode : NodeKind<JsonNodeObject>("Object", TokensPath::parseJsonNodeObject)
object StringNode : NodeKind<JsonNodeString>("String", TokensPath::parseJsonNodeString)