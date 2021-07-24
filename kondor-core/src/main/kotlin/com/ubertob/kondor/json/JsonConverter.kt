package com.ubertob.kondor.json


import com.ubertob.kondor.json.jsonnode.*
import com.ubertob.kondor.json.parser.*
import com.ubertob.kondor.json.schema.valueSchema
import com.ubertob.kondor.outcome.*


data class JsonError(val path: NodePath, val reason: String) : OutcomeError {
    override val msg = "error on <${path.getPath()}> $reason"

    override fun toString(): String = msg
}

typealias JsonOutcome<T> = Outcome<JsonError, T>

typealias JConverter<T> = JsonConverter<T, *>

typealias JArrayConverter<CT> = JsonConverter<CT, JsonNodeArray>

interface JsonConverter<T, JN : JsonNode>: Profunctor<T, T>  {

    override fun <C, D> dimap(f: (C) -> T, g: (T) -> D): ProfunctorConverter<String, C, D, JsonError> = ProfunctorConverter(::fromJson, ::toJson).dimap(f,g)

    override fun <C> lmap(f: (C) -> T): ProfunctorConverter<String, C, T, JsonError> = ProfunctorConverter(::fromJson, ::toJson).lmap(f)

    override fun <D> rmap(g: (T) -> D): ProfunctorConverter<String, T, D, JsonError> = ProfunctorConverter(::fromJson, ::toJson).rmap(g)


    val nodeType: NodeKind<JN>

    @Suppress("UNCHECKED_CAST") //but we are confident it's safe
    private fun safeCast(node: JsonNode): JsonOutcome<JN?> =
        if (node.nodeKind == nodeType)
            (node as JN).asSuccess()
        else if (node.nodeKind == NullNode)
            null.asSuccess()
        else
            JsonError(node.path, "expected a ${nodeType.desc} but found ${node.nodeKind.desc}").asFailure()

    private fun fromJsonNodeNull(node: JN?): JsonOutcome<T?> = node?.let { fromJsonNode(it) } ?: null.asSuccess()

    fun fromJsonNodeBase(node: JsonNode): JsonOutcome<T?> = safeCast(node).bind(::fromJsonNodeNull)

    fun fromJsonNode(node: JN): JsonOutcome<T>

    fun toJsonNode(value: T, path: NodePath): JN

    private fun TokensStream.parseFromRoot(): JsonOutcome<JN> =
        nodeType.parse(onRoot())

    fun toJson(value: T): String = toJsonNode(value, NodePathRoot).render()

    fun fromJson(jsonString: String): JsonOutcome<T> =
        KondorTokenizer.tokenize(jsonString).run {
            parseFromRoot()
                .bind { fromJsonNode(it) }
                .bind {
                    if (hasNext())
                        parsingFailure("EOF", next(), position(), NodePathRoot, "json continue after end")
                    else
                        it.asSuccess()
                }
        }

    fun schema(): JsonNodeObject = valueSchema(nodeType)

}

fun <T, JN : JsonNode> JsonConverter<T, JN>.toPrettyJson(value: T): String =
    toJsonNode(value, NodePathRoot).pretty(false, 2)

fun <T, JN : JsonNode> JsonConverter<T, JN>.toNullJson(value: T): String =
    toJsonNode(value, NodePathRoot).pretty(true, 2)




