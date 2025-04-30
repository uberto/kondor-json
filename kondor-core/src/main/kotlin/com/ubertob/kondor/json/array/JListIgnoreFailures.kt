package com.ubertob.kondor.json.array

import com.ubertob.kondor.json.*
import com.ubertob.kondor.json.JsonStyle.Companion.appendArrayValues
import com.ubertob.kondor.json.jsonnode.*
import com.ubertob.kondor.json.parser.KondorSeparator.ClosingBracket
import com.ubertob.kondor.json.parser.KondorSeparator.OpeningBracket
import com.ubertob.kondor.json.parser.TokensStream
import com.ubertob.kondor.json.parser.parseArray
import com.ubertob.kondor.json.parser.surrounded
import com.ubertob.kondor.outcome.Outcome
import com.ubertob.kondor.outcome.asSuccess
import com.ubertob.kondor.outcome.bindFailure
import com.ubertob.kondor.outcome.foldOutcomeIndexed

/**
 * Array converter that ignorer failures in its sub converter
 */
class JListIgnoreFailures<T : Any, IterT : Iterable<T?>>(val converter: JConverter<T>) : JArrayConverter<IterT> {
    override val _nodeType: NodeKind<JsonNodeArray> = ArrayNode

    @Suppress("UNCHECKED_CAST")
    fun convertToCollection(from: Iterable<T?>): IterT = from.filterNotNull() as IterT

    override fun fromJsonNode(node: JsonNodeArray, path: NodePath): Outcome<JsonError, IterT> =
        mapFromArray(node) { i, e -> converter.fromJsonNodeBase(e, NodePathSegment("[$i]", path)) }
            .transform { convertToCollection(it) }

    override fun toJsonNode(value: IterT): JsonNodeArray =
        mapToJson(value) { converter.toJsonNode(it) }

    private fun <T : Any> mapToJson(objs: Iterable<T?>, f: (T) -> JsonNode): JsonNodeArray =
        JsonNodeArray(objs.map {
            if (it == null)
                JsonNodeNull
            else
                f(it)
        })

    private fun <T : Any> mapFromArray(
        node: JsonNodeArray,
        f: (Int, JsonNode) -> JsonOutcome<T?>
    ): JsonOutcome<Iterable<T?>> = node.elements
        .foldOutcomeIndexed(mutableListOf()) { index, acc, e ->
            f(index, e).transform { acc.add(it); acc }.bindFailure { acc.asSuccess() }
        }

    override fun appendValue(app: CharWriter, style: JsonStyle, offset: Int, value: IterT): CharWriter =
        app.appendArrayValues(style, offset, value, converter::appendValue)

    @Suppress("UNCHECKED_CAST")
    override fun fromTokens(tokens: TokensStream, path: NodePath): JsonOutcome<IterT> =
        surrounded(
            OpeningBracket,
            { t, p -> parseArray(t, p, converter::fromTokens).transform { it as IterT } },
            ClosingBracket
        )(tokens, path)
}
