package com.ubertob.kondor.json


import JsonLexer
import com.ubertob.kondor.outcome.*


data class JsonError(val path: NodePath, val reason: String) : OutcomeError {
    override val msg = "error on <${path.getPath()}> $reason"

    override fun toString(): String = msg
}

typealias JsonOutcome<T> = Outcome<JsonError, T>

typealias JConverter<T> = JsonAdjunction<T, *>

typealias JArrayConverter<CT> = JsonAdjunction<CT, JsonNodeArray>

interface JsonAdjunction<T, JN : JsonNode> {

    val nodeType: NodeKind<JN>

    @Suppress("UNCHECKED_CAST") //but we are confident it's safe
    private fun safeCast(node: JsonNode): JsonOutcome<JN?> =
        if (node.nodeKind() == nodeType)
            (node as JN).asSuccess()
        else if (node.nodeKind() == NullNode)
            null.asSuccess()
        else
            JsonError(node.path, "expected a ${nodeType.desc} but found ${node.nodeKind().desc}").asFailure()

    fun fromJsonNodeBase(node: JsonNode): JsonOutcome<T?> = safeCast(node).bind(::fromJsonNodeNull)

    fun fromJsonNode(node: JN): JsonOutcome<T>

    fun fromJsonNodeNull(node: JN?): JsonOutcome<T?> = node?.let { fromJsonNode(it) } ?: null.asSuccess()

    fun toJsonNode(value: T, path: NodePath): JN

    private fun TokensStream.parseFromRoot(): JsonOutcome<JN> =
        nodeType.parse(this, NodePathRoot)

    fun toJson(value: T): String = toJsonNode(value, NodePathRoot).render()

    fun fromJson(jsonString: String): JsonOutcome<T> =
        JsonLexer.tokenize(jsonString).run {
            parseFromRoot()
                .bind { fromJsonNode(it) }
                .bind {
                    if (hasNext())
                        parsingFailure("EOF", next(), position(), NodePathRoot, "json continue after end")
                    else
                        it.asSuccess()
                }
        }

}

fun <T, JN : JsonNode> JsonAdjunction<T, JN>.toPrettyJson(value: T): String =
    toJsonNode(value, NodePathRoot).pretty(false, 2)

fun <T, JN : JsonNode> JsonAdjunction<T, JN>.toNullJson(value: T): String =
    toJsonNode(value, NodePathRoot).pretty(true, 2)




