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

    fun fromFieldNodeMap(fieldMap: FieldNodeMap, path: NodePath): JsonOutcome<T> //!!! implement it using fromFieldMap

    override fun fromJsonNode(node: JsonNodeObject, path: NodePath): JsonOutcome<T> =
        fromFieldNodeMap(node._fieldMap, path)

    override fun fromTokens(tokens: TokensStream, path: NodePath): JsonOutcome<T> =
        _nodeType.parse(TokensPath(tokens, path))
            .bind { fromJsonNode(it, path) } // this will be moved to JAny alone when the rest have moved to new parsing
}

abstract class ObjectNodeConverterBase<T : Any> : ObjectNodeConverter<T> {

    abstract fun JsonNodeObject.deserializeOrThrow(): T? //we need the receiver for the unaryPlus operator scope

    //   abstract fun fromFieldMap(fieldMap: FieldMap, path: NodePath): JsonOutcome<T>
// !!! the plan is to use the above instead of deserializeOrThrow and leave deserializeOrThrow only on JAny as legacy
    //the new JAny will use fromFieldMap directly with new unaryPlus operators


    override fun fromFieldNodeMap(fieldMap: FieldNodeMap, path: NodePath): Outcome<JsonError, T> =
        tryFromNode(path) {
            JsonNodeObject.buildForParsing(fieldMap, path).deserializeOrThrow() ?: throw JsonParsingException(
                ConverterJsonError(path, "deserializeOrThrow returned null!")
            )
        }


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

    protected fun parseField(fieldName: String, tokensStream: TokensStream, nodePath: NodePath): JsonOutcome<Any> {
        TODO("!!! start implement this")
    }
}
