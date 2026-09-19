package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.FieldsValues
import com.ubertob.kondor.json.jsonnode.JsonNodeObject
import com.ubertob.kondor.json.jsonnode.NodePath
import com.ubertob.kondor.json.schema.enumSchema
import java.util.*
import kotlin.reflect.KClass


interface StringWrapper {
    val raw: String
}

data class JStringWrapper<T : StringWrapper>(override val cons: (String) -> T) : JStringRepresentable<T>() {
    override val render: (T) -> String = { it.raw }
}

object JUUID : JStringRepresentable<UUID>() {
    override val cons = UUID::fromString
    override val render = UUID::toString
}



object JCurrency : JStringRepresentable<Currency>() {
    override val cons: (String) -> Currency = Currency::getInstance
    override val render: (Currency) -> String = Currency::getCurrencyCode
}


data class JEnum<E : Enum<E>>(override val cons: (String) -> E, val values: List<E> = emptyList()) :
    JStringRepresentable<E>() {
    override val render: (E) -> String = { it.name } //see enumValueOf() and enumValues()
    override fun schema(): JsonNodeObject = enumSchema(values)
}

data class JEnumClass<E : Enum<E>>(val clazz: KClass<E>) : JStringRepresentable<E>() {
    private val valuesMap: Map<String, E> by lazy { clazz.java.enumConstants.associateBy { it.name } }
    override val cons: (String) -> E = { name -> valuesMap[name] ?: error("not found $name among ${valuesMap.keys}") }
    override val render: (E) -> String = { it.name }
    override fun schema(): JsonNodeObject = enumSchema(valuesMap.values.toList())
}

//for serializing Kotlin object and other single instance types
data class JInstance<T : Any>(val singleton: T) : JObj<T>() {
    override fun FieldsValues.deserializeOrThrow(path: NodePath) = singleton
}

