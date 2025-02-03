package com.ubertob.kondor.json

import com.ubertob.kondor.json.JsonStyle.Companion.appendObjectValue
import com.ubertob.kondor.json.jsonnode.*
import com.ubertob.kondor.json.parser.KondorSeparator
import com.ubertob.kondor.json.parser.TokensStream
import com.ubertob.kondor.json.parser.parseFields
import com.ubertob.kondor.json.parser.surrounded
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

    fun fromFieldNodeMap(fieldMap: FieldNodeMap, path: NodePath): JsonOutcome<T> //!!! implement it using fromFieldMap

    override fun fromJsonNode(node: JsonNodeObject, path: NodePath): JsonOutcome<T> =
        fromFieldNodeMap(node._fieldMap, path)

    private fun converterByKey(fieldName: String, tokens: TokensStream, path: NodePath): JsonOutcome<Any> {
        TODO()
    }
    override fun fromTokens(tokens: TokensStream, path: NodePath): JsonOutcome<T> =
        surrounded(
            KondorSeparator.OpeningCurly,
            { t, p -> parseFields(t, p, ::converterByKey) },
            KondorSeparator.ClosingCurly,
        )(tokens, path)
            .bind { fromFieldMap(it, path) }

//        _nodeType.parse(TokensPath(tokens, path))
//            .bind { fromJsonNode(it, path) }
}

abstract class ObjectNodeConverterBase<T : Any> : ObjectNodeConverter<T> {

    abstract fun JsonNodeObject.deserializeOrThrow(): T? //we need the receiver for the unaryPlus operator scope

    override fun fromFieldNodeMap(fieldMap: FieldNodeMap, path: NodePath): Outcome<JsonError, T> =
        tryFromNode(path) {
            JsonNodeObject.buildForParsing(fieldMap, path).deserializeOrThrow() ?: throw JsonParsingException(
                ConverterJsonError(path, "deserializeOrThrow returned null!")
            )
        }

    override fun fromFieldMap(fieldMap: FieldMap, path: NodePath): Outcome<JsonError, T> =
        tryFromNode(path) {
            JsonNodeObject.buildForParsing(fieldMap, path)
                .deserializeOrThrow()
        }

    to make it work it will require either changing deserializeOrThrow to use map of values instead of map of jsonnodes,
    or to get rid of deserializeOrThrow and just use test time reflection. Then I need to use it for fromJsonNode.
    Better to leave JAny as it is and do it on JDataClass instead??


    override fun toJsonNode(value: T): JsonNodeObject =
        JsonNodeObject(convertFields(value))

    abstract fun convertFields(valueObject: T): Map<String, JsonNode>

}

sealed class ObjectNodeConverterWriters<T : Any> : ObjectNodeConverterBase<T>() {

    abstract val writers: List<NodeWriter<T>>

    override fun convertFields(valueObject: T): FieldNodeMap =
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
    internal fun <FT> registerPropertyHack(jsonProperty: JsonProperty<FT>, binder: (T) -> Any) =
        registerProperty(jsonProperty, binder as (T) -> FT)


    override fun fieldAppenders(valueObject: T): List<NamedAppender> =
        appenders.flatMap { it(valueObject) }

    override fun schema(): JsonNodeObject = objectSchema(properties.get())
}
