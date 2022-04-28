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


interface JsonConverter<T, JN : JsonNode> : Profunctor<T, T>,
    ToJson<T>, FromJson<T> {

    override fun <C, D> dimap(contraMap: (C) -> T, coMap: (T) -> D): ProfunctorConverter<C, D> =
        ProfunctorConverter<T, T>(::fromJson, ::toJson).dimap(contraMap, coMap)

    override fun <C> lmap(f: (C) -> T): ProfunctorConverter<C, T> =
        ProfunctorConverter<T, T>(::fromJson, ::toJson).lmap(f)

    override fun <D> rmap(g: (T) -> D): ProfunctorConverter<T, D> =
        ProfunctorConverter<T, T>(::fromJson, ::toJson).rmap(g)


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

    override fun fromJson(json: String): JsonOutcome<T> =
        safeTokenize(json).bind(::parseAndConvert)

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

