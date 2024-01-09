package com.ubertob.kondor.json

import com.ubertob.kondor.json.JsonStyle.Companion.compact
import com.ubertob.kondor.json.JsonStyle.Companion.pretty
import com.ubertob.kondor.json.JsonStyle.Companion.prettyWithNulls
import com.ubertob.kondor.json.jsonnode.*
import com.ubertob.kondor.json.parser.KondorTokenizer
import com.ubertob.kondor.json.parser.TokensStream
import com.ubertob.kondor.json.parser.parsingFailure
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

    val _nodeType: NodeKind<JN>

    @Suppress("UNCHECKED_CAST") //but we are confident it's safe
    private fun safeCast(node: JsonNode): JsonOutcome<JN?> =
        when (node.nodeKind) {
            _nodeType -> (node as JN).asSuccess()
            NullNode -> null.asSuccess()
            else -> ConverterJsonError(
                node._path,
                "expected a ${_nodeType.desc} but found ${node.nodeKind.desc}"
            ).asFailure()
        }

    private fun fromJsonNodeNull(node: JN?): JsonOutcome<T?> = node?.let { fromJsonNode(it) } ?: null.asSuccess()

    fun fromJsonNodeBase(node: JsonNode): JsonOutcome<T?> = safeCast(node).bind(::fromJsonNodeNull)

    fun fromJsonNode(node: JN): JsonOutcome<T>

    fun toJsonNode(value: T, path: NodePath): JN

    private fun TokensStream.parseFromRoot(): JsonOutcome<JN> =
        _nodeType.parse(onRoot())

    val jsonStyle: JsonStyle
        get() = JsonStyle.singleLine

    override fun toJson(value: T): String =
        toJson(value, jsonStyle)

    override fun fromJson(json: String): JsonOutcome<T> =
        KondorTokenizer.tokenize(json)
            .bind(::parseAndConvert)

    fun fromJson(jsonStream: InputStream): JsonOutcome<T> =
        KondorTokenizer.tokenize(jsonStream)
            .bind(::parseAndConvert)

    fun parseAndConvert(tokens: TokensStream) =
        tokens.parseFromRoot()
            .bind { fromJsonNode(it) }
            .bind {
                if (tokens.hasNext())
                    parsingFailure("EOF", tokens.next(), NodePathRoot, "json continue after end")
                else
                    it.asSuccess()
            }

    fun appendValue(app: StrAppendable, style: JsonStyle, offset: Int, value: T): StrAppendable
    fun schema(): JsonNodeObject = valueSchema(_nodeType)
}

fun <T, JN : JsonNode> JsonConverter<T, JN>.toJson(value: T, renderer: JsonStyle): String =
    appendValue(ChunkedStringWriter(), renderer, 0, value).toString()


//deprecated methods
@Deprecated("Use JsonStyle specification", replaceWith = ReplaceWith("toJson(value, pretty)"))
fun <T, JN : JsonNode> JsonConverter<T, JN>.toPrettyJson(value: T): String =
    toJson(value, pretty)

@Deprecated("Use JsonStyle specification", replaceWith = ReplaceWith("toJson(value, prettyWithNulls)"))
fun <T, JN : JsonNode> JsonConverter<T, JN>.toNullJson(value: T): String =
    toJson(value, prettyWithNulls)

@Deprecated("Use JsonStyle specification", replaceWith = ReplaceWith("toJson(value, compact)"))
fun <T, JN : JsonNode> JsonConverter<T, JN>.toCompactJson(value: T): String =
    toJson(value, compact)
