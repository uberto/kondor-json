package com.ubertob.kondor.jsonSimplified


sealed class NodeKind<JN : JsonNode>(
    val desc: String,
    val parse: TokensStream.() -> JsonOutcome<JN>
)

object ArrayNode : NodeKind<JsonNodeArray>("Array", TokensStream::array)
object BooleanNode : NodeKind<JsonNodeBoolean>("Boolean", TokensStream::boolean)
object NullNode : NodeKind<JsonNodeNull>("Null", TokensStream::explicitNull)
object NumberNode : NodeKind<JsonNodeNumber>("Number", TokensStream::number)
object ObjectNode : NodeKind<JsonNodeObject>("Object", TokensStream::jsonObject)
object StringNode : NodeKind<JsonNodeString>("String", TokensStream::string)