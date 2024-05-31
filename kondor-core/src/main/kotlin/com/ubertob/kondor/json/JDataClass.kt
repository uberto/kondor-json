package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.FieldMap
import com.ubertob.kondor.json.jsonnode.JsonNodeObject
import com.ubertob.kondor.json.jsonnode.NodePath
import com.ubertob.kondor.json.jsonnode.NodePathRoot
import com.ubertob.kondor.json.parser.ObjectFields
import com.ubertob.kondor.outcome.asFailure
import com.ubertob.kondor.outcome.asSuccess
import com.ubertob.kondor.outcome.onFailure
import java.lang.reflect.Constructor
import kotlin.reflect.KClass

abstract class JAnyAuto<T : Any>() : JAny<T>() {

    protected val _jsonProperties by lazy { getProperties() }

    override fun JsonNodeObject.deserializeOrThrow(): T? =
        error("Deprecated method! Override fromFieldMap if necessary.")

    override fun fromFieldMap(fieldMap: FieldMap, path: NodePath): JsonOutcome<T> {
        val args = _jsonProperties.associate { prop ->
            val propValue = prop.getter(fieldMap, path)
                .onFailure { return it.asFailure() }
            prop.propName to propValue
        }

        return buildInstance(args, path)
    }

    abstract fun buildInstance(args: ObjectFields, path: NodePath): JsonOutcome<T>
}

abstract class JDataClass<T : Any>(klazz: KClass<T>) : JAnyAuto<T>() {

    val clazz: Class<T> = klazz.java

    private val constructor: Constructor<T> by lazy { clazz.constructors.first() as Constructor<T> }

    override fun buildInstance(args: ObjectFields, path: NodePath) =
        try {
            constructor.newInstance(*(args.values).toTypedArray())
                .asSuccess()
        } catch (t: Exception) {
            ConverterJsonError(
                path,
                "Error calling constructor with signature ${constructor.description()} using params $args"
            )
                .asFailure()
        }
}

private fun <T> Constructor<T>.description(): String =
    parameterTypes
        .map { it.simpleName }
        .joinToString(prefix = "[", postfix = "]")

fun <T : Any> JDataClass<T>.testParserAndRender(times: Int = 100, generator: (index: Int) -> T) {
//add clear error on the fields that are not matching!!!!
    repeat(times) { index ->
        val value = generator(index)

        val jn = toJsonNode(value)

        val valFromNode = fromJsonNode(jn, NodePathRoot)
            .onFailure { throw AssertionError(it) }

        assert(value == valFromNode)

        val json = toJson(value)
        val valFromJson = fromJson(json)
            .onFailure { throw AssertionError(it) }

        assert(value == valFromJson)
    }
}