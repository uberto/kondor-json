package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.*
import com.ubertob.kondor.json.schema.objectSchema
import com.ubertob.kondor.outcome.failIfNull
import java.util.concurrent.atomic.AtomicReference


typealias NodeWriter<T> = (JsonNodeObject, T) -> JsonNodeObject //todo: convert to (T) -> Pair<String, JsonNode>


sealed class ObjectNodeConverter<T : Any> : JsonConverter<T, JsonNodeObject> {

    abstract fun JsonNodeObject.deserializeOrThrow(): T?

    override fun fromJsonNode(node: JsonNodeObject): JsonOutcome<T> =
        tryFromNode(node) {
            node.deserializeOrThrow() ?: throw JsonParsingException(
                JsonError(node.path, "deserializeOrThrow returned null!")
            )
        }

    abstract fun getWriters(value: T): List<NodeWriter<T>> //todo getWriters shouldn't need value, but it's a problem on JMap
    override fun toJsonNode(value: T, path: NodePath): JsonNodeObject =
        getWriters(value) //todo: this can be make faster since we know how many prop there are...
            .fold(JsonNodeObject(mutableMapOf(), path)) { acc, writer ->
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

    override fun schema(): JsonNodeObject = objectSchema(properties.get())
}


abstract class PolymorphicConverter<T : Any> : ObjectNodeConverter<T>() {

    abstract fun extractTypeName(obj: T): String
    abstract val subConverters: Map<String, ObjectNodeConverter<out T>>


    @Suppress("UNCHECKED_CAST") //todo: add tests for this
    fun findSubTypeConverter(typeName: String): ObjectNodeConverter<T>? =
        subConverters[typeName] as? ObjectNodeConverter<T>

}

class JMap<K : Any, V : Any>(
    private val keyConverter: JStringRepresentable<K>,
    private val valueConverter: JConverter<V>
) : ObjectNodeConverter<Map<K, V>>() {

    companion object {
        operator fun <V: Any> invoke(valueConverter: JConverter<V>): JMap<String, V> =
            JMap(
                object: JStringRepresentable<String>() {
                    override val cons: (String) -> String = { it }
                    override val render: (String) -> String = { it }
                },
                valueConverter
            )
    }

    override fun JsonNodeObject.deserializeOrThrow() =
        fieldMap.entries.associate { (key, value) ->
            keyConverter.cons(key) to
                    valueConverter.fromJsonNodeBase(value)
                        .failIfNull { JsonError(path, "Found null node in map!") }
                        .orThrow()
        }

    override fun getWriters(value: Map<K, V>): List<NodeWriter<Map<K, V>>> =
        value
            .map { (key, value) -> keyConverter.render(key) to value }
            .sortedBy { it.first }
            .map { (key, value) ->
                { jno: JsonNodeObject, _: Map<K, V> ->
                    jno.copy(
                        fieldMap = jno.fieldMap +
                                (key to valueConverter.toJsonNode(value, NodePathSegment(key, jno.path)))
                    )
                }
            }

}
