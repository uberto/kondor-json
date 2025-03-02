package com.ubertob.kondor.json


import com.ubertob.kondor.json.JsonStyle.Companion.appendObjectValue
import com.ubertob.kondor.json.jsonnode.*
import com.ubertob.kondor.json.parser.TokensPath
import com.ubertob.kondor.json.parser.TokensStream
import com.ubertob.kondor.json.schema.objectSchema
import com.ubertob.kondor.outcome.asFailure
import com.ubertob.kondor.outcome.asSuccess
import com.ubertob.kondor.outcome.bind
import java.util.concurrent.atomic.AtomicReference

typealias NamedNode = Pair<String, JsonNode>

typealias NodeWriter<T> = (MutableFieldMap, T) -> MutableFieldMap


interface ObjectNodeConverter<T : Any> : JsonConverter<T, JsonNodeObject> {
    override val _nodeType get() = ObjectNode

    fun fieldAppenders(valueObject: T): List<NamedAppender>

    override fun appendValue(app: CharWriter, style: JsonStyle, offset: Int, value: T): CharWriter =
        app.appendObjectValue(style, offset, fieldAppenders(value))

    fun fromFieldMap(fieldMap: FieldMap, path: NodePath): JsonOutcome<T>

    fun fromFieldNodeMap(fieldMap: FieldNodeMap, path: NodePath): JsonOutcome<T> =
        fromFieldMap(fieldMap, path)

    override fun fromJsonNode(node: JsonNodeObject, path: NodePath): JsonOutcome<T> =
        fromFieldNodeMap(node._fieldMap, path)

    override fun fromTokens(tokens: TokensStream, path: NodePath): JsonOutcome<T> =
        _nodeType.parse(TokensPath(tokens, path))
            .bind { fromJsonNode(it, path) } // this will be moved to JAny alone when the rest have moved to new parsing
}

abstract class ObjectNodeConverterBase<T : Any> : ObjectNodeConverter<T> {


    override fun toJsonNode(value: T): JsonNodeObject =
        JsonNodeObject(convertFields(value))

    abstract fun convertFields(valueObject: T): Map<String, JsonNode>

}

abstract class ObjectNodeConverterWriters<T : Any> : ObjectNodeConverterBase<T>() {

    abstract val writers: List<NodeWriter<T>>

    override fun convertFields(valueObject: T): FieldNodeMap =
        writers.fold(mutableMapOf()) { acc, writer ->
            writer(acc, valueObject)
        }

}

abstract class ObjectNodeConverterProperties<T : Any> : ObjectNodeConverterWriters<T>() {

    private val nodeWriters: AtomicReference<List<NodeWriter<T>>> = AtomicReference(emptyList())
    private val properties: AtomicReference<List<JsonProperty<*>>> = AtomicReference(emptyList())

    override val writers: List<NodeWriter<T>> by lazy { nodeWriters.get() }

    private val appenders: List<(T) -> List<NamedAppender>> = mutableListOf()
    fun getProperties(): List<JsonProperty<*>> = properties.get()

    private fun registerWriter(writer: NodeWriter<T>) {
        nodeWriters.getAndUpdate { list -> list + writer }
    }

    fun <FT> registerProperty(jsonProperty: JsonProperty<FT>, binder: (T) -> FT) {
        properties.getAndUpdate { list -> list + jsonProperty }
        registerWriter { mfm, obj -> jsonProperty.setter(binder(obj))(mfm) }
        (appenders as MutableList).add { obj ->
            jsonProperty.appender(binder(obj))
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <FT> registerPropertyHack(jsonProperty: JsonProperty<FT>, binder: (T) -> Any) =
        registerProperty(jsonProperty, binder as (T) -> FT) //!!! do we really need it?


    override fun fieldAppenders(valueObject: T): List<NamedAppender> =
        appenders.flatMap { it(valueObject) }

    override fun schema(): JsonNodeObject = objectSchema(properties.get())

    @Suppress("UNCHECKED_CAST")
    protected fun parseField(fieldName: String, tokensStream: TokensStream, nodePath: NodePath): JsonOutcome<Any> {
        val property = getProperties().find { it.propName == fieldName }
            ?: return JsonPropertyError(nodePath, fieldName, "Unknown field").asFailure()
//!!! clean this better
        val fieldPath = NodePathSegment(fieldName, nodePath)
        return when (property) {
            is JsonPropMandatory<*, *> -> property.converter.fromTokens(tokensStream, fieldPath) as JsonOutcome<Any>
            is JsonPropOptional<*, *> -> property.converter.fromTokens(tokensStream, fieldPath).bind {
                (it as Any).asSuccess()
            }

            is JsonPropMandatoryFlatten<*> -> property.converter.fromTokens(tokensStream, fieldPath) as JsonOutcome<Any>
        }
    }
}
