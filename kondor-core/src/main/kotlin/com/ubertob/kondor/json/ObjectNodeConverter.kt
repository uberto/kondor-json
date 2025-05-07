package com.ubertob.kondor.json


import com.ubertob.kondor.json.JsonStyle.Companion.appendObjectValue
import com.ubertob.kondor.json.jsonnode.*
import com.ubertob.kondor.json.parser.TokensPath
import com.ubertob.kondor.json.parser.TokensStream
import com.ubertob.kondor.json.parser.sameValueAs
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

    fun fromFieldValues(fieldValues: FieldsValues, path: NodePath): JsonOutcome<T>

    fun fromFieldNodeMap(fieldNodeMap: FieldNodeMap, path: NodePath): JsonOutcome<T> =
        //!!! check if we can remove this and leave only fromFieldValues
        fromFieldValues(fieldNodeMap, path)

    override fun fromJsonNode(node: JsonNodeObject, path: NodePath): JsonOutcome<T> =
        fromFieldNodeMap(node._fieldMap, path)

    override fun fromTokens(tokens: TokensStream, path: NodePath): JsonOutcome<T> =
        _nodeType.parse(TokensPath(tokens, path))
            .bind {
                fromJsonNode(
                    it,
                    path
                )
            } // !!! this will be moved to JAny alone when the rest have moved to new parsing
}

abstract class ObjectNodeConverterBase<T : Any> : ObjectNodeConverter<T> {


    override fun toJsonNode(value: T): JsonNodeObject =
        JsonNodeObject(convertFields(value))

    abstract fun convertFields(valueObject: T): FieldNodeMap


}

abstract class ObjectNodeConverterWriters<T : Any> : ObjectNodeConverterBase<T>() {

    abstract val writers: List<NodeWriter<T>>

    override fun convertFields(valueObject: T): FieldNodeMap =
        FieldNodeMap(
            writers.fold(mutableMapOf()) { acc, writer ->
                writer(acc, valueObject)
            }
        )

    abstract fun resolveConverter(
        fieldName: String,
        nodePath: NodePath
    ): JsonOutcome<JsonConverter<*, *>>
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

    protected fun parseField(fieldName: String, tokensStream: TokensStream, nodePath: NodePath): JsonOutcome<Any?> {
        return resolveConverter(fieldName, nodePath)
            .bind { conv ->
                //!!! a better solution to the null is to delegate it to the converter should also consider null in resolveConverter!!!

                if (tokensStream.peek().sameValueAs("null")) {
                    tokensStream.next()
                    null.asSuccess()
                } else {
                    val fieldPath = NodePathSegment(fieldName, nodePath)
                    conv.fromTokens(tokensStream, fieldPath)
                }
            }

    }

    override fun resolveConverter(
        fieldName: String,
        nodePath: NodePath
    ): JsonOutcome<JsonConverter<*, *>> {
        val properties = getProperties()
        val property = properties.find { it.propName == fieldName }

        return if (property == null) {
            JsonPropertyError(
                nodePath.parent(),
                fieldName,
                "Not found a property for the Json field '$fieldName'. Defined properties: ${getProperties().map { it.propName }}"
            )
                .asFailure()
        } else {

            val converter = when (property) {
                is JsonPropMandatory<*, *> -> property.converter
                is JsonPropOptional<*, *> -> property.converter
                is JsonPropMandatoryFlatten<*> -> property.converter
            }

            converter.asSuccess()
        }
    }
}
