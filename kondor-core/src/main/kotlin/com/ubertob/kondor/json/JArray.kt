package com.ubertob.kondor.json

import com.ubertob.kondor.json.JsonStyle.Companion.appendArrayValues
import com.ubertob.kondor.json.jsonnode.*
import com.ubertob.kondor.json.parser.TokensPath
import com.ubertob.kondor.json.parser.TokensStream
import com.ubertob.kondor.json.schema.arraySchema
import com.ubertob.kondor.outcome.Outcome
import com.ubertob.kondor.outcome.bind
import com.ubertob.kondor.outcome.traverseIndexed

interface JArray<T : Any, CT : Iterable<T?>> : JArrayConverter<CT> {

    val converter: JConverter<T>

    fun convertToCollection(from: Iterable<T?>): CT

    override fun fromJsonNode(node: JsonNodeArray, path: NodePath): Outcome<JsonError, CT> =
        mapFromArray(node) { i, e -> converter.fromJsonNodeBase(e, NodePathSegment("[$i]", path)) }
            .transform { convertToCollection(it) }

    override fun toJsonNode(value: CT): JsonNodeArray =
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
        .traverseIndexed(f)

    override fun schema(): JsonNodeObject = arraySchema(converter)

    override fun appendValue(app: CharWriter, style: JsonStyle, offset: Int, value: CT): CharWriter =
        app.appendArrayValues(style, offset, value, converter::appendValue)

    override fun fromTokens(tokens: TokensStream, path: NodePath): JsonOutcome<CT> =
        _nodeType.parse(TokensPath(tokens, path))
            .bind { fromJsonNode(it, path) }
            .bind { it.checkForJsonTail(tokens) } //!!!

}


data class JList<T : Any>(override val converter: JConverter<T>) : JArray<T, List<T>> {
    override fun convertToCollection(from: Iterable<T?>): List<T> = from.filterNotNull().toList()
    override val _nodeType = ArrayNode
}

data class JNullableList<T : Any>(override val converter: JConverter<T>) : JArray<T, List<T?>> {

    override val jsonStyle = JsonStyle.singleLineWithNulls
    override fun convertToCollection(from: Iterable<T?>): List<T?> = from.toList()
    override val _nodeType = ArrayNode
}

data class JSet<T : Any>(override val converter: JConverter<T>) : JArray<T, Set<T>> {
    override fun convertToCollection(from: Iterable<T?>): Set<T> = from.filterNotNull().toSet()
    override val _nodeType = ArrayNode
}