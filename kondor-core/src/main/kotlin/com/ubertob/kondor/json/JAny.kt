package com.ubertob.kondor.json

import com.ubertob.kondor.outcome.Outcome
import com.ubertob.kondor.outcome.asFailure
import com.ubertob.kondor.outcome.asSuccess
import com.ubertob.kondor.outcome.failIfNull
import java.util.concurrent.atomic.AtomicReference


typealias NodeWriter<T> = (JsonNodeObject, T) -> JsonNodeObject

interface ObjectNodeConverter<T : Any> : JsonAdjunction<T, JsonNodeObject> {

    fun JsonNodeObject.deserializeOrThrow(): T?

    override fun fromJsonNode(node: JsonNodeObject): JsonOutcome<T> =
        tryFromNode(node) {
            node.deserializeOrThrow() ?: throw JsonParsingException(
                JsonError(node.path, "deserializeOrThrow returned null!")
            )
        }

    fun getWriters(value: T): List<NodeWriter<T>> //todo: simplify to a single NodeWriter

    override fun toJsonNode(value: T, path: NodePath): JsonNodeObject =
        getWriters(value)
            .fold(JsonNodeObject(emptyMap(), path)) { acc, writer ->
                writer(acc, value)
            }

    override val nodeType get() = ObjectNode

}


abstract class JAny<T : Any> : ObjectNodeConverter<T> {

    private val nodeWriters: AtomicReference<List<NodeWriter<T>>> = AtomicReference(emptyList())

    override fun getWriters(value: T): List<NodeWriter<T>> = nodeWriters.get()

    private fun registerSetter(nodeWriter: NodeWriter<T>) {
        nodeWriters.getAndUpdate { list -> list + nodeWriter }
    }

    internal fun <FT> registerProperty(jsonProperty: JsonProperty<FT>, binder: (T) -> FT) {
        registerSetter { jno, obj -> jsonProperty.setter(binder(obj))(jno) }
    }

}

//todo refactor better getters and setters
sealed class JsonProperty<T> {
    abstract val propName: String
    abstract fun setter(value: T): (JsonNodeObject) -> JsonNodeObject
    abstract fun getter(wrapped: JsonNodeObject): JsonOutcome<T>
}

data class JsonParsingException(val error: JsonError) : RuntimeException()

data class JsonPropMandatory<T : Any, JN : JsonNode>(
    override val propName: String,
    val converter: JsonAdjunction<T, JN>
) :
    JsonProperty<T>() {

    override fun getter(wrapped: JsonNodeObject): Outcome<JsonError, T> =
        wrapped.fieldMap[propName]
            ?.let(converter::fromJsonNodeBase)
            ?.failIfNull(JsonError(wrapped.path, "Found null for non-nullable '$propName'"))
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
    val converter: JsonAdjunction<T, JN>
) :
    JsonProperty<T?>() {

    override fun getter(wrapped: JsonNodeObject): Outcome<JsonError, T?> =
        wrapped.fieldMap[propName]
            ?.let(converter::fromJsonNodeBase)
            ?: null.asSuccess()


    override fun setter(value: T?): (JsonNodeObject) -> JsonNodeObject = { wrapped ->
        wrapped.copy(
            fieldMap = wrapped.fieldMap + (propName to toJsonNode(value, wrapped))
        )
    }

    private fun toJsonNode(value: T?, wrapped: JsonNodeObject) =
        value?.let { converter.toJsonNode(it, NodePathSegment(propName, wrapped.path)) }
            ?: JsonNodeNull(wrapped.path)

}

data class JsonPropMandatoryFlatten<T : Any>(override val propName: String, val converter: ObjectNodeConverter<T>) :
    JsonProperty<T>() {

    override fun getter(wrapped: JsonNodeObject): Outcome<JsonError, T> =
        wrapped
            .let(converter::fromJsonNodeBase)
            .failIfNull(JsonError(wrapped.path, "Found null for non-nullable '$propName'"))

    override fun setter(value: T): (JsonNodeObject) -> JsonNodeObject =
        { wrapped ->
            wrapped.copy(fieldMap = wrapped.fieldMap + (converter.toJsonNode(value, wrapped.path).fieldMap))
        }

}

