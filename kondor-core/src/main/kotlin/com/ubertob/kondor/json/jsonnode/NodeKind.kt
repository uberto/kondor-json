package com.ubertob.kondor.json.jsonnode

import com.ubertob.kondor.json.JsonOutcome
import com.ubertob.kondor.json.catchingStackOverflow
import com.ubertob.kondor.json.parser.*
import com.ubertob.kondor.outcome.bind

sealed class NodeKind<JN : JsonNode>(
    val desc: String,
    parseNode: TokensPath.() -> JsonOutcome<JN>
) {
    //a Json nested too deeply for the stack is an error here too, since this is called directly as well
    val parse: TokensPath.() -> JsonOutcome<JN> = { catchingStackOverflow { parseNode() } }

    fun fromJsonString(json: String): JsonOutcome<JN> =
        KondorTokenizer.tokenize(json)
            .bind { parse(TokensPath(it, NodePathRoot)) }
}

object ArrayNode : NodeKind<JsonNodeArray>("Array", TokensPath::parseJsonNodeArray)
object BooleanNode : NodeKind<JsonNodeBoolean>("Boolean", TokensPath::parseJsonNodeBoolean)
object NullNode : NodeKind<JsonNodeNull>("Null", TokensPath::parseJsonNodeNull)
object NumberNode : NodeKind<JsonNodeNumber>("Number", TokensPath::parseJsonNodeNum)
object ObjectNode : NodeKind<JsonNodeObject>("Object", TokensPath::parseJsonNodeObject)
object StringNode : NodeKind<JsonNodeString>("String", TokensPath::parseJsonNodeString)