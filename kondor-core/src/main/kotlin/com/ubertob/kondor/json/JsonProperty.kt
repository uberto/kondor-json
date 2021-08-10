package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.*
import com.ubertob.kondor.outcome.Outcome
import com.ubertob.kondor.outcome.asFailure
import com.ubertob.kondor.outcome.asSuccess
import com.ubertob.kondor.outcome.failIfNull

sealed class JsonProperty<T> {
    abstract val propName: String
    abstract fun setter(value: T): (JsonNodeObject) -> JsonNodeObject
    abstract fun getter(wrapped: JsonNodeObject): JsonOutcome<T>
}

data class JsonParsingException(val error: JsonError) : RuntimeException()

data class JsonPropMandatory<T : Any, JN : JsonNode>(
    override val propName: String,
    val converter: JsonConverter<T, JN>
) : JsonProperty<T>() {

    override fun getter(wrapped: JsonNodeObject): Outcome<JsonError, T> =
        wrapped.fieldMap[propName]
            ?.let(converter::fromJsonNodeBase)
            ?.failIfNull{JsonError(wrapped.path, "Found null for non-nullable '$propName'")}
            ?: JsonError(wrapped.path, "Not found key '$propName'").asFailure()


    override fun setter(value: T): (JsonNodeObject) -> JsonNodeObject =
        { wrapped ->
            wrapped.copy(
                fieldMap = wrapped.fieldMap + (propName to converter.toJsonNode(
                    value,
                    NodePathSegment(propName, wrapped.path)
                ))
            )
        }

}


data class JsonPropOptional<T : Any, JN : JsonNode>(
    override val propName: String,
    val converter: JsonConverter<T, JN>
) : JsonProperty<T?>() {

    override fun getter(wrapped: JsonNodeObject): Outcome<JsonError, T?> =
        wrapped.fieldMap[propName]
            ?.let(converter::fromJsonNodeBase)
            ?: null.asSuccess()


    override fun setter(value: T?): (JsonNodeObject) -> JsonNodeObject = { wrapped ->
        wrapped.copy(
            fieldMap = wrapped.fieldMap +
                    (propName to toNullableJsonNode(value, wrapped.path))
        )
    }

    private fun toNullableJsonNode(value: T?, nodePath: NodePath) =
        value?.let { converter.toJsonNode(it, NodePathSegment(propName, nodePath)) }
            ?: JsonNodeNull(nodePath)

}

data class JsonPropMandatoryFlatten<T : Any>(
    override val propName: String,
    val converter: ObjectNodeConverter<T>
) : JsonProperty<T>() {

    override fun getter(wrapped: JsonNodeObject): Outcome<JsonError, T> =
        wrapped
            .let(converter::fromJsonNodeBase)
            .failIfNull { JsonError(wrapped.path, "Found null for non-nullable '$propName'") }

    override fun setter(value: T): (JsonNodeObject) -> JsonNodeObject =
        { wrapped ->
            wrapped.copy(fieldMap = wrapped.fieldMap + (converter.toJsonNode(value, wrapped.path).fieldMap))
        }

}
