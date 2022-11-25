package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.JsonNodeObject
import com.ubertob.kondor.json.jsonnode.NodePath
import com.ubertob.kondor.json.jsonnode.ObjectNode
import com.ubertob.kondor.json.schema.objectSchema
import java.util.concurrent.atomic.AtomicReference


typealias NodeWriter<T> = (JsonNodeObject, T) -> JsonNodeObject //todo: convert to (T) -> Pair<String, JsonNode>


sealed class ObjectNodeConverter<T : Any> : JsonConverter<T, JsonNodeObject> {

    abstract fun JsonNodeObject.deserializeOrThrow(): T?

    override fun fromJsonNode(node: JsonNodeObject): JsonOutcome<T> =
        tryFromNode(node) {
            node.deserializeOrThrow() ?: throw JsonParsingException(
                ConverterJsonError(node._path, "deserializeOrThrow returned null!")
            )
        }

    abstract fun getWriters(value: T): List<NodeWriter<T>> //todo getWriters shouldn't need value, but it's a problem on JMap
    override fun toJsonNode(value: T, path: NodePath): JsonNodeObject =
        getWriters(value) //todo: this can be make faster since we know how many prop there are...
            .fold(JsonNodeObject(mutableMapOf(), path)) { acc, writer ->
                writer(acc, value)
            }

    override val _nodeType get() = ObjectNode

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

    override fun schema(): JsonNodeObject = objectSchema(properties.get())
}


abstract class PolymorphicConverter<T : Any> : ObjectNodeConverter<T>() {

    abstract fun extractTypeName(obj: T): String
    abstract val subConverters: Map<String, ObjectNodeConverter<out T>>


    @Suppress("UNCHECKED_CAST") //todo: add tests for this
    fun findSubTypeConverter(typeName: String): ObjectNodeConverter<T>? =
        subConverters[typeName] as? ObjectNodeConverter<T>

}


