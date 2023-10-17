package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.*
import com.ubertob.kondor.json.schema.objectSchema
import java.util.concurrent.atomic.AtomicReference

typealias NamedNode = Pair<String, JsonNode>

typealias NodeWriter<T> = (MutableFieldMap, T, NodePath) -> MutableFieldMap


interface ObjectNodeConverter<T : Any> : JsonConverter<T, JsonNodeObject> {
    override val _nodeType get() = ObjectNode
}

abstract class ObjectNodeConverterBase<T : Any> : ObjectNodeConverter<T> {

    abstract fun JsonNodeObject.deserializeOrThrow(): T?

    override fun fromJsonNode(node: JsonNodeObject): JsonOutcome<T> =
        tryFromNode(node) {
            node.deserializeOrThrow() ?: throw JsonParsingException(
                ConverterJsonError(node._path, "deserializeOrThrow returned null!")
            )
        }

    override fun toJsonNode(value: T, path: NodePath): JsonNodeObject =
        JsonNodeObject(convertFields(value, path), path)

    abstract fun convertFields(valueObject: T, path: NodePath): Map<String, JsonNode>

}


sealed class ObjectNodeConverterWriters<T : Any> : ObjectNodeConverterBase<T>() {

    abstract val writers: List<NodeWriter<T>>
    override fun convertFields(valueObject: T, path: NodePath): FieldMap =
        writers.fold(mutableMapOf()) { acc, writer ->
            writer(acc, valueObject, path)
        }

}

abstract class JAny<T : Any> : ObjectNodeConverterWriters<T>() {

    private val nodeWriters: AtomicReference<List<NodeWriter<T>>> = AtomicReference(emptyList())
    private val properties: AtomicReference<List<JsonProperty<*>>> = AtomicReference(emptyList())

    override val writers: List<NodeWriter<T>> by lazy { nodeWriters.get() }
    fun getProperties(): List<JsonProperty<*>> = properties.get()

    private fun registerWriter(writer: NodeWriter<T>) {
        nodeWriters.getAndUpdate { list -> list + writer }
    }

    internal fun <FT> registerProperty(jsonProperty: JsonProperty<FT>, binder: (T) -> FT) {
        properties.getAndUpdate { list -> list + jsonProperty }
        registerWriter { mfm, obj, path -> jsonProperty.setter(binder(obj))(mfm, path) }
    }

    override fun schema(): JsonNodeObject = objectSchema(properties.get())
}

private fun <T> ((MutableFieldMap, NodePath, T) -> MutableFieldMap).toWriter(): NodeWriter<T> =
    { fm: MutableFieldMap, obj: T, path: NodePath ->
        fm.also { it.putAll(this(fm, path, obj)) }
    }


abstract class PolymorphicConverter<T : Any> : ObjectNodeConverterBase<T>() {
    abstract fun extractTypeName(obj: T): String
    abstract val subConverters: Map<String, ObjectNodeConverterBase<out T>>

    @Suppress("UNCHECKED_CAST")
    fun findSubTypeConverter(typeName: String): ObjectNodeConverterBase<T>? =
        subConverters[typeName] as? ObjectNodeConverterBase<T>

}


