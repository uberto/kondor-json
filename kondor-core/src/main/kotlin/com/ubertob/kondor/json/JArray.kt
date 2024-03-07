package com.ubertob.kondor.json

import com.ubertob.kondor.json.JsonStyle.Companion.appendArrayValues
import com.ubertob.kondor.json.jsonnode.*
import com.ubertob.kondor.json.schema.arraySchema
import com.ubertob.kondor.outcome.Outcome
import com.ubertob.kondor.outcome.traverse

interface JArray<T : Any, CT : Iterable<T>> : JArrayConverter<CT> {

    val converter: JConverter<T>

    fun convertToCollection(from: Iterable<T>): CT

    override fun fromJsonNode(node: JsonNodeArray): Outcome<JsonError, CT> =
        mapFromArray(node, converter::fromJsonNodeBase)
            .transform { convertToCollection(it) }

    override fun toJsonNode(value: CT, path: NodePath): JsonNodeArray =
        mapToJson(value, converter::toJsonNode, path)

    private fun <T : Any> mapToJson(objs: Iterable<T>, f: (T, NodePath) -> JsonNode, path: NodePath): JsonNodeArray =
        JsonNodeArray(objs.map { f(it, path) }, path)

    private fun <T : Any> mapFromArray(
        node: JsonNodeArray,
        f: (JsonNode) -> JsonOutcome<T?>
    ): JsonOutcome<Iterable<T>> = node.elements
        .traverse(f)
        .transform { it.filterNotNull() }

    override fun schema(): JsonNodeObject = arraySchema(converter)

    override fun appendValue(app: CharWriter, style: JsonStyle, offset: Int, value: CT): CharWriter =
        app.appendArrayValues(jsonStyle, 0, value, converter::appendValue)
}


data class JList<T : Any>(override val converter: JConverter<T>) : JArray<T, List<T>> {
    override fun convertToCollection(from: Iterable<T>): List<T> = from.toList()
    override val _nodeType = ArrayNode
}

data class JSet<T : Any>(override val converter: JConverter<T>) : JArray<T, Set<T>> {
    override fun convertToCollection(from: Iterable<T>): Set<T> = from.toSet()
    override val _nodeType = ArrayNode
}