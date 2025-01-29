package com.ubertob.kondor.json

import com.ubertob.kondor.json.JsonStyle.Companion.appendObjectValue
import com.ubertob.kondor.json.jsonnode.*
import com.ubertob.kondor.json.parser.TokensPath
import com.ubertob.kondor.json.parser.TokensStream
import com.ubertob.kondor.json.schema.objectSchema
import com.ubertob.kondor.outcome.Outcome
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

    override fun fromJsonNode(node: JsonNodeObject, path: NodePath): JsonOutcome<T> =
        fromFieldMap(node._fieldMap, path)

    override fun fromTokens(tokens: TokensStream, path: NodePath): JsonOutcome<T> =
        _nodeType.parse(TokensPath(tokens, path))
            .bind { fromJsonNode(it, path) }
            .bind { it.checkForJsonTail(tokens) } //!!!
}

abstract class ObjectNodeConverterBase<T : Any> : ObjectNodeConverter<T> {

    abstract fun JsonNodeObject.deserializeOrThrow(): T? //we need the receiver for the unaryPlus operator scope

    override fun fromFieldMap(fieldMap: FieldMap, path: NodePath): Outcome<JsonError, T> =
        tryFromNode(path) {
            JsonNodeObject.buildForParsing(fieldMap, path).deserializeOrThrow() ?: throw JsonParsingException(
                ConverterJsonError(path, "deserializeOrThrow returned null!")
            )
        }


    override fun toJsonNode(value: T): JsonNodeObject =
        JsonNodeObject(convertFields(value))

    abstract fun convertFields(valueObject: T): Map<String, JsonNode>

}

sealed class ObjectNodeConverterWriters<T : Any> : ObjectNodeConverterBase<T>() {

    abstract val writers: List<NodeWriter<T>>

    override fun convertFields(valueObject: T): FieldMap =
        writers.fold(mutableMapOf()) { acc, writer ->
            writer(acc, valueObject)
        }

}

abstract class JAny<T : Any> : ObjectNodeConverterWriters<T>() {

    private val nodeWriters: AtomicReference<List<NodeWriter<T>>> = AtomicReference(emptyList())
    private val properties: AtomicReference<List<JsonProperty<*>>> = AtomicReference(emptyList())

    override val writers: List<NodeWriter<T>> by lazy { nodeWriters.get() }

    private val appenders: List<(T) -> List<NamedAppender>> = mutableListOf()
    fun getProperties(): List<JsonProperty<*>> = properties.get()

    private fun registerWriter(writer: NodeWriter<T>) {
        nodeWriters.getAndUpdate { list -> list + writer }
    }

    internal fun <FT> registerProperty(jsonProperty: JsonProperty<FT>, binder: (T) -> FT) {
        properties.getAndUpdate { list -> list + jsonProperty }
        registerWriter { mfm, obj -> jsonProperty.setter(binder(obj))(mfm) }
        (appenders as MutableList).add { obj ->
            jsonProperty.appender(binder(obj))
        }
    }

    @Suppress("UNCHECKED_CAST")
    internal fun <FT> registerPropertyHack(jsonProperty: JsonProperty<FT>, binder: (T) -> Any)  =
        registerProperty(jsonProperty, binder as (T) -> FT)


    override fun fieldAppenders(valueObject: T): List<NamedAppender> =
        appenders.flatMap { it(valueObject) }

    override fun schema(): JsonNodeObject = objectSchema(properties.get())
}
