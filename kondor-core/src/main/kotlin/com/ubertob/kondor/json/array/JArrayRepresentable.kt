package com.ubertob.kondor.json.array

import com.ubertob.kondor.json.*
import com.ubertob.kondor.json.JsonStyle.Companion.appendArrayValues
import com.ubertob.kondor.json.jsonnode.*
import com.ubertob.kondor.json.parser.KondorSeparator
import com.ubertob.kondor.json.parser.TokensStream
import com.ubertob.kondor.json.parser.parseArray
import com.ubertob.kondor.json.parser.surrounded
import com.ubertob.kondor.json.schema.arraySchema
import com.ubertob.kondor.outcome.Outcome
import com.ubertob.kondor.outcome.traverseIndexed

abstract class JArrayRepresentable<T: Any, IT : Any, IterIT : Iterable<IT?>> : JArrayConverter<T> {
    override val _nodeType = ArrayNode

    abstract val converter: JConverter<IT>
    abstract val cons : (IterIT) -> T
    abstract val binder: T.() -> IterIT

    abstract fun convertToAny(from: Iterable<IT?>): T

    override fun fromJsonNode(node: JsonNodeArray, path: NodePath): Outcome<JsonError, T> =
        mapFromArray(node) { i, e -> converter.fromJsonNodeBase(e, NodePathSegment("[$i]", path)) }
            .transform { convertToAny(it) }

    override fun toJsonNode(value: T): JsonNodeArray =
        mapToJson(value.binder()) { converter.toJsonNode(it) }

    override fun appendValue(app: CharWriter, style: JsonStyle, offset: Int, value: T): CharWriter =
        app.appendArrayValues(style, offset, value.binder(), converter::appendValue)

    private fun mapFromArray(
        node: JsonNodeArray,
        f: (Int, JsonNode) -> JsonOutcome<IT?>
    ): JsonOutcome<Iterable<IT?>> = node.elements
        .traverseIndexed(f)

    private fun <T : Any> mapToJson(objs: Iterable<T?>, f: (T) -> JsonNode): JsonNodeArray =
        JsonNodeArray(objs.map { if (it == null) JsonNodeNull else f(it) })

    @Suppress("UNCHECKED_CAST")
    override fun fromTokens(tokens: TokensStream, path: NodePath): JsonOutcome<T> =
        surrounded(
            KondorSeparator.OpeningBracket,
            { t, p -> parseArray(t, p, converter::fromTokens).transform { cons(it as IterIT) } },
            KondorSeparator.ClosingBracket
        )(tokens, path)

    override fun schema(): JsonNodeObject = arraySchema(converter)
}
