package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.JsonNodeObject
import com.ubertob.kondor.json.jsonnode.NodePath
import com.ubertob.kondor.json.jsonnode.NodePathSegment
import com.ubertob.kondor.json.jsonnode.ObjectNode
import com.ubertob.kondor.outcome.failIfNull
import java.util.concurrent.atomic.AtomicReference


typealias NodeWriter<T> = (JsonNodeObject, T) -> JsonNodeObject


sealed class ObjectNodeConverter<T : Any> : JsonConverter<T, JsonNodeObject> {

    abstract fun JsonNodeObject.deserializeOrThrow(): T?

    override fun fromJsonNode(node: JsonNodeObject): JsonOutcome<T> =
        tryFromNode(node) {
            node.deserializeOrThrow() ?: throw JsonParsingException(
                JsonError(node.path, "deserializeOrThrow returned null!")
            )
        }

    abstract fun getWriters(value: T): List<NodeWriter<T>>
    override fun toJsonNode(value: T, path: NodePath): JsonNodeObject =
        getWriters(value)
            .fold(JsonNodeObject(emptyMap(), path)) { acc, writer ->
                writer(acc, value)
            }

    override val nodeType get() = ObjectNode

}


abstract class JAny<T : Any> : ObjectNodeConverter<T>() {

    private val nodeWriters: AtomicReference<List<NodeWriter<T>>> = AtomicReference(emptyList())
    private val properties: AtomicReference<List<JsonProperty<*>>> = AtomicReference(emptyList())

    override fun getWriters(value: T): List<NodeWriter<T>> = nodeWriters.get()

    fun getProperties(): List<JsonProperty<*>> = properties.get()

    private fun registerSetter(nodeWriter: NodeWriter<T>) {
        nodeWriters.getAndUpdate { list -> list + nodeWriter }
    }


    internal fun <FT> registerProperty(jsonProperty: JsonProperty<FT>, binder: (T) -> FT) {
        properties.getAndUpdate { list -> list + jsonProperty }

        registerSetter { jno, obj -> jsonProperty.setter(binder(obj))(jno) }
    }

}


abstract class PolymorphicConverter<T : Any> : ObjectNodeConverter<T>() {

    abstract fun extractTypeName(obj: T): String
    abstract val subConverters: Map<String, ObjectNodeConverter<out T>>


    @Suppress("UNCHECKED_CAST") //todo: add tests for this
    fun findSubTypeConverter(typeName: String): ObjectNodeConverter<T>? =
        subConverters[typeName] as? ObjectNodeConverter<T>

}

class JMap<T : Any>(private val valueConverter: JConverter<T>) : ObjectNodeConverter<Map<String, T>>() {
    override fun JsonNodeObject.deserializeOrThrow() =
        fieldMap.mapValues { entry ->
            valueConverter.fromJsonNodeBase(entry.value)
                .failIfNull(JsonError(path, "Found null node in map!"))
                .orThrow()
        }

    override fun getWriters(value: Map<String, T>): List<NodeWriter<Map<String, T>>> =
        value.entries.toList().sortedBy { it.key }.map { (key, value) ->
            { jno: JsonNodeObject, _: Map<String, T> ->
                jno.copy(
                    fieldMap = jno.fieldMap +
                        (key to valueConverter.toJsonNode(value, NodePathSegment(key, jno.path)))
                )
            }
        }

}



