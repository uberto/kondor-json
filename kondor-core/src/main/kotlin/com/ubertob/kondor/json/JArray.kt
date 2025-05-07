package com.ubertob.kondor.json

import com.ubertob.kondor.json.JsonStyle.Companion.appendArrayValues
import com.ubertob.kondor.json.jsonnode.*
import com.ubertob.kondor.json.parser.KondorSeparator.ClosingBracket
import com.ubertob.kondor.json.parser.KondorSeparator.OpeningBracket
import com.ubertob.kondor.json.parser.TokensStream
import com.ubertob.kondor.json.parser.parseArray
import com.ubertob.kondor.json.parser.surrounded
import com.ubertob.kondor.json.schema.arraySchema
import com.ubertob.kondor.outcome.Outcome
import com.ubertob.kondor.outcome.traverseIndexed

interface JArray<T : Any, COLL> : JArrayConverter<COLL> {

    val converter: JConverter<T>

    fun convertToCollection(iterable: Iterable<T?>): COLL
    fun convertFromCollection(collection: COLL): Iterable<T?>

    override fun fromJsonNode(node: JsonNodeArray, path: NodePath): Outcome<JsonError, COLL> =
        mapFromArray(node) { i, e -> converter.fromJsonNodeBase(e, NodePathSegment("[$i]", path)) }
            .transform { convertToCollection(it) }

    override fun toJsonNode(value: COLL): JsonNodeArray =
        mapToJson(convertFromCollection(value)) { converter.toJsonNode(it) }

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
        .traverseIndexed(f)

    override fun schema(): JsonNodeObject = arraySchema(converter)

    override fun appendValue(app: CharWriter, style: JsonStyle, offset: Int, value: COLL): CharWriter =
        app.appendArrayValues(style, offset, convertFromCollection(value), converter::appendValue)

    @Suppress("UNCHECKED_CAST")
    override fun fromTokens(tokens: TokensStream, path: NodePath): JsonOutcome<COLL> =
        surrounded(
            OpeningBracket,
            { t, p ->
                parseArray(t, p, converter::fromTokens)
                    .transform { convertToCollection(it) }
            },
            ClosingBracket
        )(tokens, path)

}


data class JList<T : Any>(override val converter: JConverter<T>) : JArray<T, List<T>> {
    override fun convertToCollection(from: Iterable<T?>): List<T> = from.filterNotNull().toList()
    override fun convertFromCollection(collection: List<T>): Iterable<T?> = collection
    override val _nodeType = ArrayNode
}

data class JNullableList<T : Any>(override val converter: JConverter<T>) : JArray<T, List<T?>> {

    override val jsonStyle = JsonStyle.singleLineWithNulls
    override fun convertToCollection(from: Iterable<T?>): List<T?> = from.toList()
    override fun convertFromCollection(collection: List<T?>): Iterable<T?> = collection
    override val _nodeType = ArrayNode
}

data class JSet<T : Any>(override val converter: JConverter<T>) : JArray<T, Set<T>> {
    override fun convertToCollection(from: Iterable<T?>): Set<T> = from.filterNotNull().toSet()
    override fun convertFromCollection(collection: Set<T>): Iterable<T?> = collection
    override val _nodeType = ArrayNode
}