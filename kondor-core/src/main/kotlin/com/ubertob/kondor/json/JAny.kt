package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.JsonNode
import com.ubertob.kondor.json.jsonnode.JsonNodeObject
import com.ubertob.kondor.json.jsonnode.NodePath
import com.ubertob.kondor.json.jsonnode.ObjectNode
import com.ubertob.kondor.json.schema.objectSchema
import java.util.concurrent.atomic.AtomicReference

typealias NamedNode = Pair<String, JsonNode>

typealias NodeWriter<T> = (JsonNodeObject, T) -> JsonNodeObject


interface ObjectNodeConverter<T : Any> : JsonConverter<T, JsonNodeObject> {
    override val _nodeType get() = ObjectNode
}


sealed class ObjectNodeConverterWriters<T : Any> : ObjectNodeConverter<T> {

    abstract fun JsonNodeObject.deserializeOrThrow(): T?

    override fun fromJsonNode(node: JsonNodeObject): JsonOutcome<T> =
        tryFromNode(node) {
            node.deserializeOrThrow() ?: throw JsonParsingException(
                ConverterJsonError(node._path, "deserializeOrThrow returned null!")
            )
        }

    abstract fun getWriters(value: T): List<NodeWriter<T>> //usually getWriters doesn't need value, but sometime does, like in JMap
    override fun toJsonNode(value: T, path: NodePath): JsonNodeObject =
        getWriters(value) //this can be made faster since we know how many prop there are...
            .fold(JsonNodeObject(mutableMapOf(), path)) { acc, writer ->
                writer(acc, value)
            }


}


abstract class JAny<T : Any> : ObjectNodeConverterWriters<T>() {

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

    override fun schema(): JsonNodeObject = objectSchema(properties.get())
}


abstract class PolymorphicConverter<T : Any> : ObjectNodeConverterWriters<T>() {
    abstract fun extractTypeName(obj: T): String
    abstract val subConverters: Map<String, ObjectNodeConverterWriters<out T>>

    @Suppress("UNCHECKED_CAST")
    fun findSubTypeConverter(typeName: String): ObjectNodeConverterWriters<T>? =
        subConverters[typeName] as? ObjectNodeConverterWriters<T>

}


