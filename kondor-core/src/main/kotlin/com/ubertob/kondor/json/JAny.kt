package com.ubertob.kondor.json

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


