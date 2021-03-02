package com.ubertob.kondor.json

import com.ubertob.kondor.outcome.Outcome
import com.ubertob.kondor.outcome.asFailure
import com.ubertob.kondor.outcome.asSuccess
import java.util.concurrent.atomic.AtomicReference
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty


typealias NodeWriter<T> = (JsonNodeObject, T) -> JsonNodeObject

interface JObject<T : Any> : JsonAdjunction<T, JsonNodeObject> {

    fun JsonNodeObject.deserializeOrThrow(): T?

    override fun fromJsonNode(node: JsonNodeObject): JsonOutcome<T> =
        tryFromNode(node) {
            node.deserializeOrThrow() ?: throw JsonParsingException(
                JsonError(node.path, "tryDeserialize returned null!")
            )
        }

    fun getWriters(value: T): List<NodeWriter<T>>

    override fun toJsonNode(value: T, path: NodePath): JsonNodeObject =
        getWriters(value)
            .fold(JsonNodeObject(emptyMap(), path)) { acc, writer ->
                writer(acc, value)
            }

    override val nodeType get() = ObjectNode

}


abstract class JAny<T : Any> : JObject<T> {

    private val nodeWriters: AtomicReference<List<NodeWriter<T>>> = AtomicReference(emptyList())

    override fun getWriters(value: T): List<NodeWriter<T>> = nodeWriters.get()

    private fun registerSetter(nodeWriter: NodeWriter<T>) {
        nodeWriters.getAndUpdate { list -> list + nodeWriter }
    }

    internal fun <FT> registerProperty(jsonProperty: JsonProperty<FT>, binder: (T) -> FT) {
        registerSetter { jno, obj -> jsonProperty.setter(binder(obj))(jno) }
    }

}


sealed class JsonProperty<T> {
    abstract val propName: String
    abstract fun setter(value: T): (JsonNodeObject) -> JsonNodeObject
    abstract fun getter(wrapped: JsonNodeObject): JsonOutcome<T>
    abstract fun parser(tokensStream: TokensStream, path: NodePath): JsonOutcome<JsonNode>
}

data class JsonParsingException(val error: JsonError) : RuntimeException()

data class JsonPropMandatory<T : Any, JN : JsonNode>(override val propName: String, val jf: JsonAdjunction<T, JN>) :
    JsonProperty<T>() {

    override fun getter(wrapped: JsonNodeObject): Outcome<JsonError, T> =
        wrapped.fieldMap[propName]
            ?.let(jf::fromJsonNodeBase)
            ?: JsonError(wrapped.path, "Not found $propName").asFailure()


    override fun setter(value: T): (JsonNodeObject) -> JsonNodeObject =
        { wrapped ->
            wrapped.copy(fieldMap = wrapped.fieldMap + (propName to jf.toJsonNode(value, Node(propName, wrapped.path))))
        }

    override fun parser(tokensStream: TokensStream, path: NodePath): JsonOutcome<JsonNode> =
        jf.parseToNode(tokensStream, path)

}


data class JsonPropOptional<T : Any, JN : JsonNode>(override val propName: String, val jf: JsonAdjunction<T, JN>) :
    JsonProperty<T?>() {


    override fun getter(wrapped: JsonNodeObject): Outcome<JsonError, T?> =
        wrapped.fieldMap[propName]
            ?.let(jf::fromJsonNodeBase)
            ?: null.asSuccess()


    override fun setter(value: T?): (JsonNodeObject) -> JsonNodeObject =
        { wrapped ->
            value?.let {
                wrapped.copy(
                    fieldMap = wrapped.fieldMap + (propName to jf.toJsonNode(
                        it,
                        Node(propName, wrapped.path)
                    ))
                )
            } ?: wrapped
        }

    override fun parser(tokensStream: TokensStream, path: NodePath): JsonOutcome<JsonNode> =
        tokensStream.run {
            if (peek() == "null") parseJsonNodeNull(tokensStream, path)
            else
                jf.parseToNode(tokensStream, path)
        }


}

sealed class JFieldBase<T, PT : Any>
    : ReadOnlyProperty<JAny<PT>, JsonProperty<T>> {

    protected abstract val binder: (PT) -> T

    protected abstract fun buildJsonProperty(property: KProperty<*>): JsonProperty<T>

    operator fun provideDelegate(thisRef: JAny<PT>, prop: KProperty<*>): JFieldBase<T, PT> {
        val jp = buildJsonProperty(prop)
        thisRef.registerProperty(jp, binder)
        return this
    }

    override fun getValue(thisRef: JAny<PT>, property: KProperty<*>): JsonProperty<T> =
        buildJsonProperty(property)
}

class JField<T : Any, PT : Any>(
    override val binder: (PT) -> T,
    private val jsonAdjunction: JsonAdjunction<T, *>,
    private val jsonFieldName: String? = null
) : JFieldBase<T, PT>() {

    override fun buildJsonProperty(property: KProperty<*>): JsonProperty<T> =
        JsonPropMandatory(jsonFieldName ?: property.name, jsonAdjunction)

}

class JFieldMaybe<T : Any, PT : Any>(
    override val binder: (PT) -> T?,
    private val jsonAdjunction: JsonAdjunction<T, *>,
    private val jsonFieldName: String? = null
) : JFieldBase<T?, PT>() {

    override fun buildJsonProperty(property: KProperty<*>): JsonProperty<T?> =
        JsonPropOptional(jsonFieldName ?: property.name, jsonAdjunction)

}

