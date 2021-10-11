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


data class JsonPropOptional<T, JN : JsonNode>(
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
    val converter: ObjectNodeConverter<T>,
    val parent: JAny<*>
) : JsonProperty<T>() {

    private val parentProperties = parent.getProperties().map { it.propName }

    override fun getter(wrapped: JsonNodeObject): Outcome<JsonError, T> =
        wrapped.removeFieldsFromParent()
            .let { JsonNodeObject(it, wrapped.path) }
            .let(converter::fromJsonNode)
            .failIfNull { JsonError(wrapped.path, "Found null for non-nullable '$propName'") }

    private fun JsonNodeObject.removeFieldsFromParent() =
        fieldMap.filterKeys { key -> !parentProperties.contains(key) }

    override fun setter(value: T): (JsonNodeObject) -> JsonNodeObject =
        { wrapped ->
            wrapped.copy(fieldMap = wrapped.fieldMap + (converter.toJsonNode(value, wrapped.path).fieldMap))
        }

}
