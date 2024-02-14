package com.ubertob.kondor.json

import com.ubertob.kondor.json.JsonStyle.Companion.appendObjectFields
import com.ubertob.kondor.json.JsonStyle.Companion.appendText
import com.ubertob.kondor.json.jsonnode.*
import com.ubertob.kondor.json.parser.TokensStream
import com.ubertob.kondor.outcome.Outcome
import com.ubertob.kondor.outcome.asFailure
import com.ubertob.kondor.outcome.asSuccess
import com.ubertob.kondor.outcome.failIfNull

typealias MutableFieldMap = MutableMap<String, JsonNode>
typealias PropertySetter = (MutableFieldMap) -> MutableFieldMap
typealias PropertyAppender = CharWriter.(JsonStyle, Int) -> CharWriter
typealias NamedAppender = Pair<String, PropertyAppender?>

sealed class JsonProperty<T> {
    abstract val propName: String
    abstract fun setter(value: T): PropertySetter
    abstract fun appender(value: T): PropertyAppender?
    abstract fun getter(fieldMap: FieldMap, path: NodePath): JsonOutcome<T>
}

data class JsonParsingException(val error: JsonError) : RuntimeException()

data class JsonPropMandatory<T : Any, JN : JsonNode>(
    override val propName: String,
    val converter: JsonConverter<T, JN>
) : JsonProperty<T>() {

    override fun getter(fieldMap: FieldMap, path: NodePath): Outcome<JsonError, T> =
        fieldMap[propName]
            ?.let { converter.fromJsonNodeBase(it, NodePathSegment(propName, path)) }
            ?.failIfNull { JsonPropertyError(NodePathSegment(propName, path), propName, "Found null for non-nullable") }
            ?: JsonPropertyError(
                path,
                propName,
                "Not found key '$propName'. Keys found: [${fieldMap.keys.joinToString()}]"
            ).asFailure()


    override fun setter(value: T): PropertySetter = { fm ->
        fm.apply {
            put(propName, converter.toJsonNode(value))
        }
    }

    override fun appender(value: T): PropertyAppender = { style, off ->
        appendText(propName)
        style.appendValueSeparator(this)
            .appendValue(style, off, value)
    }

    fun CharWriter.appendValue(
        style: JsonStyle,
        offset: Int,
        value: T
    ): CharWriter = converter.appendValue(this, style, offset, value)
}


data class JsonPropOptional<T, JN : JsonNode>(
    override val propName: String,
    val converter: JsonConverter<T, JN>
) : JsonProperty<T?>() {

    override fun getter(fieldMap: FieldMap, path: NodePath): Outcome<JsonError, T?> =
        fieldMap[propName]
            ?.let { converter.fromJsonNodeBase(it, NodePathSegment(propName, path)) }
            ?: null.asSuccess()


    override fun setter(value: T?): PropertySetter = { fm ->
        fm.apply {
            put(propName,
                value?.let { converter.toJsonNode(it) }
                    ?: JsonNodeNull
            )
        }
    }

    override fun appender(value: T?): PropertyAppender? =
        if (value == null)
            null
        else { style, off ->
            appendText(propName)
            style.appendValueSeparator(this)
                .appendValue(style, off, value)
        }

    fun CharWriter.appendValue(
        style: JsonStyle,
        offset: Int,
        value: T
    ): CharWriter = converter.appendValue(this, style, offset, value)
}

data class JsonPropMandatoryFlatten<T : Any>(
    override val propName: String,
    val converter: ObjectNodeConverter<T>,
    val parent: JAny<*>
) : JsonProperty<T>() {

    private val parentProperties = parent.getProperties().map { it.propName }

    override fun appender(value: T): PropertyAppender = { js, off ->
        appendObjectFields(js, off - 1, converter.fieldAppenders(value))
    }

    override fun getter(fieldMap: FieldMap, path: NodePath): Outcome<JsonError, T> =
        JsonObjectNode(fieldMap.removeFieldsFromParent())
            .let(converter::fromJsonNode)
            .failIfNull { JsonPropertyError(path, propName, "Found null for non-nullable") }

    private fun FieldMap.removeFieldsFromParent() =
        filterKeys { key -> !parentProperties.contains(key) }

    override fun setter(value: T): PropertySetter = { fm ->
        fm.apply {
            fm.putAll(converter.toJsonNode(value)._fieldMap)
        }
    }

}