package com.ubertob.kondor.json


import com.ubertob.kondor.json.jsonnode.*
import com.ubertob.kondor.json.parser.*
import com.ubertob.kondor.json.schema.valueSchema
import com.ubertob.kondor.outcome.asFailure
import com.ubertob.kondor.outcome.asSuccess
import com.ubertob.kondor.outcome.bind
import java.io.InputStream


typealias JConverter<T> = JsonConverter<T, *>

typealias JArrayConverter<CT> = JsonConverter<CT, JsonNodeArray>

interface ToJson<T, S> {
    fun toJson(value: T): S
}

interface FromJson<T, S> {
    fun fromJson(json: S): JsonOutcome<T>
}

data class ToJsonF<T, S>(val toJson: (T) -> S) : ToJson<T, S> {
    override fun toJson(value: T): S = toJson(value)
    fun <U> contraTransform(f: (U) -> T): ToJson<U, S> = ToJsonF { toJson(f(it)) }
}

data class FromJsonF<T, S>(val fromJson: (S) -> JsonOutcome<T>) : FromJson<T, S> {
    override fun fromJson(json: S): JsonOutcome<T> = fromJson(json)
    fun <U> transform(f: (T) -> U): FromJson<U, S> = FromJsonF { fromJson(it).transform(f) }
}


interface JsonConverter<T, JN : JsonNode> : Profunctor<T, T>,
    ToJson<T, String>, FromJson<T, String> {

    override fun <C, D> dimap(contraFun: (C) -> T, g: (T) -> D): ProfunctorConverter<String, C, D, JsonError> =
        ProfunctorConverter<String, T, T, JsonError>(::fromJson, ::toJson).dimap(contraFun, g)

    override fun <C> lmap(f: (C) -> T): ProfunctorConverter<String, C, T, JsonError> =
        ProfunctorConverter<String, T, T, JsonError>(::fromJson, ::toJson).lmap(f)

    override fun <D> rmap(g: (T) -> D): ProfunctorConverter<String, T, D, JsonError> =
        ProfunctorConverter<String, T, T, JsonError>(::fromJson, ::toJson).rmap(g)


    val nodeType: NodeKind<JN>

    @Suppress("UNCHECKED_CAST") //but we are confident it's safe
    private fun safeCast(node: JsonNode): JsonOutcome<JN?> =
        if (node.nodeKind == nodeType)
            (node as JN).asSuccess()
        else if (node.nodeKind == NullNode)
            null.asSuccess()
        else
            ConverterJsonError(node.path, "expected a ${nodeType.desc} but found ${node.nodeKind.desc}").asFailure()

    private fun fromJsonNodeNull(node: JN?): JsonOutcome<T?> = node?.let { fromJsonNode(it) } ?: null.asSuccess()

    fun fromJsonNodeBase(node: JsonNode): JsonOutcome<T?> = safeCast(node).bind(::fromJsonNodeNull)

    fun fromJsonNode(node: JN): JsonOutcome<T>

    fun toJsonNode(value: T, path: NodePath): JN

    private fun TokensStream.parseFromRoot(): JsonOutcome<JN> =
        try {
            nodeType.parse(onRoot())
        } catch (e: EndOfCollection) {
            parsingFailure("a valid Json", lastToken(), NodePathRoot, "Unexpected end of file - Invalid Json")
        } catch (t: Throwable) {
            parsingFailure("a valid Json", lastToken(), NodePathRoot, t.message.orEmpty())
        }

    override fun toJson(value: T): String = toJsonNode(value, NodePathRoot).render()

    override fun fromJson(jsonString: String): JsonOutcome<T> =
        safeTokenize(jsonString).bind(::parseAndConvert)

    fun fromJson(jsonStream: InputStream): JsonOutcome<T> =
        safeLazyTokenize(jsonStream).bind(::parseAndConvert)

    fun parseAndConvert(tokens: TokensStream) =
        tokens.parseFromRoot()
            .bind { fromJsonNode(it) }
            .bind {
                if (tokens.hasNext())
                    parsingFailure("EOF", tokens.next(), NodePathRoot, "json continue after end")
                else
                    it.asSuccess()
            }

    fun schema(): JsonNodeObject = valueSchema(nodeType)

}


fun <T, JN : JsonNode> JsonConverter<T, JN>.toPrettyJson(value: T): String =
    toJsonNode(value, NodePathRoot).pretty(false, 2)

fun <T, JN : JsonNode> JsonConverter<T, JN>.toNullJson(value: T): String =
    toJsonNode(value, NodePathRoot).pretty(true, 2)


private fun safeTokenize(jsonString: String): JsonOutcome<TokensStream> =
    try {
        KondorTokenizer.tokenize(jsonString).asSuccess()
    } catch (e: JsonParsingException) {
        e.error.asFailure()
    }

private fun safeLazyTokenize(jsonString: InputStream): JsonOutcome<TokensStream> =
    try {
        KondorTokenizer.tokenize(jsonString).asSuccess()
    } catch (e: JsonParsingException) {
        e.error.asFailure()
    }

