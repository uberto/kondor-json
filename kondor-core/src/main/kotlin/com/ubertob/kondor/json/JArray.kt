package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.*
import com.ubertob.kondor.json.schema.arraySchema
import com.ubertob.kondor.outcome.Outcome
import com.ubertob.kondor.outcome.extractList

interface JArray<T : Any, CT : Iterable<T>> : JArrayConverter<CT> {

    val converter: JConverter<T>

    fun convertToCollection(from: Iterable<T>): CT

    override fun fromJsonNode(node: JsonNodeArray, path: NodePath): Outcome<JsonError, CT> =
        mapFromArray(node) { converter.fromJsonNodeBase(it, path) }
            .transform { convertToCollection(it) }

    override fun toJsonNode(value: CT): JsonNodeArray =
        mapToJson(value, converter::toJsonNode)

    private fun <T : Any> mapToJson(objs: Iterable<T>, f: (T) -> JsonNode): JsonNodeArray =
        JsonNodeArray(objs.map { f(it) })

    private fun <T : Any> mapFromArray(
        node: JsonNodeArray,
        f: (JsonNode) -> JsonOutcome<T?>
    ): JsonOutcome<Iterable<T>> = node.elements.map(f)
        .extractList()
        .transform { it.filterNotNull() }

    override fun schema(): JsonNodeObject = arraySchema(converter)

}


data class JList<T : Any>(override val converter: JConverter<T>) : JArray<T, List<T>> {
    override fun convertToCollection(from: Iterable<T>): List<T> = from.toList()
    override val _nodeType = ArrayNode
}

data class JSet<T : Any>(override val converter: JConverter<T>) : JArray<T, Set<T>> {
    override fun convertToCollection(from: Iterable<T>): Set<T> = from.toSet()
    override val _nodeType = ArrayNode
}