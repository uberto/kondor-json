package com.ubertob.kondor.json

import com.ubertob.kondor.json.JsonStyle.Companion.appendObjectValue
import com.ubertob.kondor.json.jsonnode.*
import com.ubertob.kondor.json.schema.objectSchema
import java.util.concurrent.atomic.AtomicReference

typealias NamedNode = Pair<String, JsonNode>

typealias NodeWriter<T> = (MutableFieldMap, T) -> MutableFieldMap


interface ObjectNodeConverter<T : Any> : JsonConverter<T, JsonObjectNode> {
    override val _nodeType get() = ObjectNode

    fun fieldAppenders(valueObject: T): List<NamedAppender>

    override fun appendValue(app: CharWriter, style: JsonStyle, offset: Int, value: T): CharWriter =
        app.appendObjectValue(style, offset, fieldAppenders(value))
}

abstract class ObjectNodeConverterBase<T : Any> : ObjectNodeConverter<T> {

    abstract fun JsonNodeObject.deserializeOrThrow(): T?

    override fun fromJsonNode(node: JsonObjectNode, path: NodePath): JsonOutcome<T> =
        tryFromNode(path) {
            JsonNodeObject(node._fieldMap, path).deserializeOrThrow() ?: throw JsonParsingException(
                ConverterJsonError(path, "deserializeOrThrow returned null!")
            )
        }

    override fun toJsonNode(value: T): JsonObjectNode =
        JsonObjectNode(convertFields(value))

    abstract fun convertFields(valueObject: T): Map<String, JsonNode>

}

sealed class ObjectNodeConverterWriters<T : Any> : ObjectNodeConverterBase<T>() {

    abstract val writers: List<NodeWriter<T>>

    override fun convertFields(valueObject: T): FieldMap =
        writers.fold(mutableMapOf()) { acc, writer ->
            writer(acc, valueObject)
        }

}

typealias ObjectAppender<T> = (T) -> PropertyAppender? //null appender if the prop is null

abstract class JAny<T : Any> : ObjectNodeConverterWriters<T>() {

    private val nodeWriters: AtomicReference<List<NodeWriter<T>>> = AtomicReference(emptyList())
    private val properties: AtomicReference<List<JsonProperty<*>>> = AtomicReference(emptyList())

    override val writers: List<NodeWriter<T>> by lazy { nodeWriters.get() }

    private val appenders: List<Pair<String, ObjectAppender<T>>> = mutableListOf()
    fun getProperties(): List<JsonProperty<*>> = properties.get()

    private fun registerWriter(writer: NodeWriter<T>) {
        nodeWriters.getAndUpdate { list -> list + writer }
    }

    internal fun <FT> registerProperty(jsonProperty: JsonProperty<FT>, binder: (T) -> FT) {
        properties.getAndUpdate { list -> list + jsonProperty }
        registerWriter { mfm, obj -> jsonProperty.setter(binder(obj))(mfm) }
        (appenders as MutableList).add(jsonProperty.propName to { obj ->
            val field = binder(obj)
            if (field == null)
                null
            else
                jsonProperty.appender(field)
        })
    }

    override fun fieldAppenders(valueObject: T): List<NamedAppender> =
        appenders.map { it.first to it.second(valueObject) }

    override fun schema(): JsonObjectNode = objectSchema(properties.get())
}


abstract class JDataClass<T : Any> : JAny<T>() {

    abstract val clazz: Class<T>

    val constructor by lazy { clazz.constructors.first() }
    override fun JsonNodeObject.deserializeOrThrow(): T? {

        //can we check that is a Kotlin data class? we can also compare constructors args and Json fields


//        val map: Map<String, Any?> = getProperties().associate {
//            it.propName to it.getter(this).orThrow()
//        }
//
//        println("properties map ${map.keys}") //json names
//        val args = mutableListOf<Any?>() //TODO
////        first translate all props in objects values, then pass to the constructor
//        val consParams = constructor.parameters
//        println("found ${consParams.size} cons params")
//
//        val consParamNames = consParams.map { it.annotatedType } //just arg1 arg2...
//        println("consParamNames $consParamNames")
//
//        for (param in consParams) {
//            val field = map[param.name]
//            println("cons param ${param.name}  $field")
//            args.add(field)
//        }

        //this work assuming the JConverter has fields in the same exact order then the data class constructor
        val args: List<Any?> = getProperties().map { it.getter(this).orThrow() } //!!!

        @Suppress("UNCHECKED_CAST")
        return constructor.newInstance(*args.toTypedArray()) as T
    }

//
//    override fun fromJson(json: String): JsonOutcome<T> =
//
//        JsonLexerEager(json).tokenize().bind {
//
//            val tp = TokensPath(it, NodePathRoot) //root??
// //asm generated method handler that know the tokens to expect and what to extract
//
//            tp.toObjectFields(getProperties())
//        }.transform {
//            val args = it.values
//            @Suppress("UNCHECKED_CAST")
//            constructor.newInstance(*args.toTypedArray()) as T
//
//        }


}



