package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.FieldMap
import com.ubertob.kondor.json.jsonnode.JsonNodeObject
import com.ubertob.kondor.json.jsonnode.NodePath
import com.ubertob.kondor.json.parser.ObjectFields
import com.ubertob.kondor.outcome.Outcome
import kotlin.reflect.KClass

abstract class JAnyAuto<T : Any>() : JAny<T>() {

    protected val _jsonProperties by lazy { getProperties() }

    override fun JsonNodeObject.deserializeOrThrow(): T? =
        error("Deprecated method! Override fromFieldMap if necessary.")

    override fun fromFieldMap(fieldMap: FieldMap, path: NodePath): Outcome<JsonError, T> =
        tryFromNode(path) {
            val args: ObjectFields = _jsonProperties.associate { prop ->
                prop.propName to prop.getter(fieldMap, path).orThrow()
            }

            buildInstance(args)
        }

    abstract fun buildInstance(args: ObjectFields): T
}

abstract class JDataClass<T : Any>(klazz: KClass<T>) : JAnyAuto<T>() {

    val clazz: Class<T> = klazz.java
    private val constructor by lazy { clazz.constructors.first() }

    @Suppress("UNCHECKED_CAST")
    override fun buildInstance(args: ObjectFields) =
        constructor.newInstance(*(args.values).toTypedArray()) as T

}