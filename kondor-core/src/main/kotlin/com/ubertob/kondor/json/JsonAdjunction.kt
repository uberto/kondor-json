package com.ubertob.kondor.json


import JsonLexer
import com.ubertob.kondor.outcome.*
import com.ubertob.kondor.json.JsonNode


data class JsonError(val path: NodePath, val reason: String) : OutcomeError {
    override val msg = "error on <${path.getPath()}> $reason"
}

typealias JsonOutcome<T> = Outcome<JsonError, T>

/*
a couple parser/printer form an adjunction

The laws are (no id because we cannot reconstruct a wrong json from the error):

render `.` parse `.` render = render
parse `.` render `.` parse = parse

where:
f `.` g: (x) -> g(f(x))
render : JsonOutcome<T> -> JSON
parse : JSON -> JsonOutcome<T>

JSON here can be either the Json string or the JsonNode
 */

typealias JConverter<T> = JsonAdjunction<T, *>


interface JsonAdjunction<T, JN : JsonNode> {

    val nodeType: NodeKind<JN>

    @Suppress("UNCHECKED_CAST")
    fun safeCast(node: JsonNode): JsonOutcome<JN> =
        if (node.nodeKind() == nodeType)
            (node as JN).asSuccess()
        else
            JsonError(
                node.path,
                "expected a ${nodeType.desc} but found ${node.nodeKind().desc}"
            ).asFailure()

    fun fromJsonNodeBase(node: JsonNode): JsonOutcome<T> = safeCast(node).bind(::fromJsonNode)
    fun fromJsonNode(node: JN): JsonOutcome<T>
    fun toJsonNode(value: T, path: NodePath): JN

    fun parseToNode(tokensStream: TokensStream, path: NodePath): JsonOutcome<JN> =
        nodeType.parse(tokensStream, path)

    fun toJson(value: T): String = toJsonNode(value, NodeRoot).render()
    fun fromJson(jsonString: String): JsonOutcome<T> {
        val tokensStream = JsonLexer(jsonString).tokenize()
        return parseToNode(tokensStream, NodeRoot)
            .bind { fromJsonNode(it) }
            .bind {
                if (tokensStream.hasNext())
                    parsingFailure("EOF", tokensStream.next(), tokensStream.position(), NodeRoot)
                else
                    it.asSuccess()
            }
    }
}




