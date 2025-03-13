package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.*
import com.ubertob.kondor.outcome.Outcome
import com.ubertob.kondor.outcome.asFailure
import com.ubertob.kondor.outcome.asSuccess
import com.ubertob.kondor.outcome.failIfNull

typealias MutableFieldMap = MutableMap<String, JsonNode>
typealias PropertySetter = (MutableFieldMap) -> MutableFieldMap
typealias ValueAppender = CharWriter.(JsonStyle, Int) -> CharWriter
typealias NamedAppender = Pair<String, ValueAppender?>

sealed class JsonProperty<T> {
    abstract val propName: String
    abstract fun setter(value: T): PropertySetter
    abstract fun appender(value: T): List<NamedAppender>
    abstract fun getter(fieldMap: FieldNodeMap, path: NodePath): JsonOutcome<T>
}

data class JsonParsingException(val error: JsonError) : RuntimeException()

data class JsonPropMandatory<T : Any, JN : JsonNode>(
    override val propName: String,
    val converter: JsonConverter<T, JN>
) : JsonProperty<T>() {

    override fun getter(fieldMap: FieldNodeMap, path: NodePath): Outcome<JsonError, T> =
        fieldMap.map[propName]
            ?.let { converter.fromJsonNodeBase(it, NodePathSegment(propName, path)) }
            ?.failIfNull { JsonPropertyError(NodePathSegment(propName, path), propName, "Found null for non-nullable") }
            ?: JsonPropertyError(
                path, propName, "Not found key '$propName'. Keys found: [${fieldMap.map.keys.joinToString()}]"
            ).asFailure()

    override fun setter(value: T): PropertySetter = { fm ->
        fm.apply {
            put(propName, converter.toJsonNode(value))
        }
    }

    override fun appender(value: T): List<NamedAppender> = listOf(propName to { style, off ->
        appendValue(style, off, value)
    })

    fun CharWriter.appendValue(style: JsonStyle, offset: Int, value: T): CharWriter =
        converter.appendValue(this, style, offset, value)
}


data class JsonPropOptional<T, JN : JsonNode>(
    override val propName: String,
    val converter: JsonConverter<T, JN>
) : JsonProperty<T?>() {

    override fun getter(fieldMap: FieldNodeMap, path: NodePath): Outcome<JsonError, T?> =
        fieldMap.map[propName]
            ?.let { converter.fromJsonNodeBase(it, NodePathSegment(propName, path)) }
            ?: null.asSuccess()


    override fun setter(value: T?): PropertySetter = { fm ->
        fm.apply {
            put(
                propName,
                value?.let { converter.toJsonNode(it) }
                    ?: JsonNodeNull
            )
        }
    }

    override fun appender(value: T?): List<NamedAppender> =
        if (value == null)
            listOf(propName to null)
        else listOf(propName to { style, off ->
            appendValue(style, off, value)
        })

    fun CharWriter.appendValue(style: JsonStyle, offset: Int, value: T): CharWriter =
        converter.appendValue(this, style, offset, value)
}

data class JsonPropMandatoryFlatten<T : Any>(
    override val propName: String,
    val converter: ObjectNodeConverter<T>,
    val parent: ObjectNodeConverterProperties<*>
) : JsonProperty<T>() {

    private val parentProperties = parent.getProperties().map { it.propName }

    override fun appender(value: T): List<NamedAppender> = converter.fieldAppenders(value)

    override fun getter(fieldMap: FieldNodeMap, path: NodePath): Outcome<JsonError, T> =
        converter.fromFieldNodeMap(fieldMap.removeFieldsFromParent(), path)
            .failIfNull { JsonPropertyError(path, propName, "Found null for non-nullable") }

    private fun FieldNodeMap.removeFieldsFromParent(): FieldNodeMap =
        FieldNodeMap(map.filterKeys { key -> !parentProperties.contains(key) })

    override fun setter(value: T): PropertySetter = { fm ->
        fm.apply {
            fm.putAll(converter.toJsonNode(value)._fieldMap.map)
        }
    }

}