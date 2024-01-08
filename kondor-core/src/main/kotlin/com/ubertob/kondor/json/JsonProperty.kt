package com.ubertob.kondor.json

import com.ubertob.kondor.json.JsonStyle.Companion.appendNull
import com.ubertob.kondor.json.JsonStyle.Companion.appendObjectFields
import com.ubertob.kondor.json.JsonStyle.Companion.appendText
import com.ubertob.kondor.json.jsonnode.*
import com.ubertob.kondor.outcome.Outcome
import com.ubertob.kondor.outcome.asFailure
import com.ubertob.kondor.outcome.asSuccess
import com.ubertob.kondor.outcome.failIfNull

typealias MutableFieldMap = MutableMap<String, JsonNode>
typealias PropertySetter = (MutableFieldMap, NodePath) -> MutableFieldMap
typealias PropertyAppender = StrAppendable.(JsonStyle, Int) -> Boolean

sealed class JsonProperty<T> {
    abstract val propName: String
    abstract fun setter(value: T): PropertySetter
    abstract fun appender(value: T): PropertyAppender
    abstract fun getter(wrapped: JsonNodeObject): JsonOutcome<T>


}

data class JsonParsingException(val error: JsonError) : RuntimeException()

data class JsonPropMandatory<T : Any, JN : JsonNode>(
    override val propName: String,
    val converter: JsonConverter<T, JN>
) : JsonProperty<T>() {

    override fun getter(wrapped: JsonNodeObject): Outcome<JsonError, T> =
        wrapped._fieldMap[propName]
            ?.let(converter::fromJsonNodeBase)
            ?.failIfNull { JsonPropertyError(wrapped._path, propName, "Found null for non-nullable") }
            ?: JsonPropertyError(
                wrapped._path,
                propName,
                "Not found key '$propName'. Keys found: [${wrapped._fieldMap.keys.joinToString()}]"
            ).asFailure()


    override fun setter(value: T): PropertySetter = { fieldMap, path ->
        fieldMap.apply {
            put(propName, converter.toJsonNode(value, NodePathSegment(propName, path)))
        }
    }

    override fun appender(value: T): PropertyAppender = { js, off ->
        appendText(propName)
            .append(js.valueSeparator)
            .appendValue(js, off, value)
        true
    }

    fun StrAppendable.appendValue(
        style: JsonStyle,
        offset: Int,
        value: T
    ): StrAppendable = converter.appendValue(this, style, offset, value)
}


data class JsonPropOptional<T, JN : JsonNode>(
    override val propName: String,
    val converter: JsonConverter<T, JN>
) : JsonProperty<T?>() {

    override fun getter(wrapped: JsonNodeObject): Outcome<JsonError, T?> =
        wrapped._fieldMap[propName]
            ?.let(converter::fromJsonNodeBase)
            ?: null.asSuccess()


    override fun setter(value: T?): PropertySetter = { fm, path ->
        fm.apply {
            put(propName,
                value?.let { converter.toJsonNode(it, NodePathSegment(propName, path)) }
                    ?: JsonNodeNull(path)
            )
        }
    }

    override fun appender(value: T?): PropertyAppender = { js, off ->
        if (value != null) {
            appendText(propName)
                .append(js.valueSeparator)
                .appendValue(js, off, value)
            true
        } else if (js.includeNulls) {
            appendText(propName)
                .append(js.valueSeparator)
                .appendNull()
            true
        } else
            false
    }

    fun StrAppendable.appendValue(
        style: JsonStyle,
        offset: Int,
        value: T
    ): StrAppendable = converter.appendValue(this, style, offset, value)
}

data class JsonPropMandatoryFlatten<T : Any>(
    override val propName: String,
    val converter: ObjectNodeConverter<T>,
    val parent: JAny<*>
) : JsonProperty<T>() {

    private val parentProperties = parent.getProperties().map { it.propName }

    override fun appender(value: T): PropertyAppender = {  js, off ->
        appendObjectFields(js,  off, converter.fieldAppenders(value)) //!!!remove brackets
        true
    }

    override fun getter(wrapped: JsonNodeObject): Outcome<JsonError, T> =
        wrapped.removeFieldsFromParent()
            .let { JsonNodeObject(it, wrapped._path) }
            .let(converter::fromJsonNode)
            .failIfNull { JsonPropertyError(wrapped._path, propName, "Found null for non-nullable") }

    private fun JsonNodeObject.removeFieldsFromParent() =
        _fieldMap.filterKeys { key -> !parentProperties.contains(key) }

    override fun setter(value: T): PropertySetter = { fm, path ->
        fm.apply {
            fm.putAll(converter.toJsonNode(value, path)._fieldMap)
        }
    }

}