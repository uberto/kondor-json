package com.ubertob.kondor.json

import com.ubertob.kondor.outcome.failIfNull
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*

interface StringWrapper {
    val raw: String
}

data class JStringWrapper<T : StringWrapper>(override val cons: (String) -> T) : JStringRepresentable<T>() {
    override val render: (T) -> String = { it.raw }
}

object JBigDecimal : JNumRepresentable<BigDecimal>() {
    override val cons: (BigDecimal) -> BigDecimal = { it }
    override val render: (BigDecimal) -> BigDecimal = { it }
}

object JBigInteger : JNumRepresentable<BigInteger>() {
    override val cons: (BigDecimal) -> BigInteger = BigDecimal::toBigInteger
    override val render: (BigInteger) -> BigDecimal = BigInteger::toBigDecimal
}


object JCurrency : JStringRepresentable<Currency>() {
    override val cons: (String) -> Currency = Currency::getInstance
    override val render: (Currency) -> String = Currency::getCurrencyCode
}

data class JEnum<E : Enum<E>>(override val cons: (String) -> E) : JStringRepresentable<E>() {
    override val render: (E) -> String = { it.name }
}

//for serializing Kotlin object and other single instance types
data class JInstance<T : Any>(val singleton: T) : JAny<T>() {
    override fun JsonNodeObject.deserializeOrThrow() = singleton
}


interface PolymorphicConverter<T : Any> : ObjectNodeConverter<T> {

    fun extractTypeName(obj: T): String
    val subConverters: Map<String, ObjectNodeConverter<out T>>


    @Suppress("UNCHECKED_CAST") //todo: add tests for this
    fun findSubTypeConverter(typeName: String): ObjectNodeConverter<T>? =
        subConverters[typeName] as? ObjectNodeConverter<T>

}

interface JSealed<T : Any> : PolymorphicConverter<T> {

    val discriminatorFieldName: String
        get() = "_type"

    fun typeWriter(jno: JsonNodeObject, obj: T): JsonNodeObject =
        jno.copy(
            fieldMap = jno.fieldMap + (discriminatorFieldName to JsonNodeString(
                extractTypeName(obj),
                NodePathSegment(discriminatorFieldName, jno.path)
            ))
        )

    override fun JsonNodeObject.deserializeOrThrow(): T? {
        val typeName = JString.fromJsonNodeBase(
            fieldMap[discriminatorFieldName]
                ?: error("expected discriminator field \"$discriminatorFieldName\" not found")
        ).orThrow()
        val converter = subConverters[typeName] ?: error("subtype not known $typeName")
        return converter.fromJsonNode(this).orThrow()
    }

    override fun getWriters(value: T): List<NodeWriter<T>> =
        extractTypeName(value).let { typeName ->
            findSubTypeConverter(typeName)
                ?.getWriters(value)
                ?.plus(::typeWriter)
                ?: error("subtype not known $typeName")
        }

}

//to map polimorphic object with xml->json standard convention
interface NestedPolyConverter<T : Any> : PolymorphicConverter<T> {

    override fun JsonNodeObject.deserializeOrThrow(): T {
        val typeName = fieldMap.keys.first()
        val converter = subConverters[typeName] ?: error("subtype not known $typeName")
        return converter.fromJsonNode(this).orThrow()
    }

    override fun getWriters(value: T): List<NodeWriter<T>> =
        extractTypeName(value).let { typeName ->
            findSubTypeConverter(typeName)
                ?.getWriters(value)
                ?: error("subtype not known $typeName")
        }
}

class JMap<T : Any>(private val valueConverter: JConverter<T>) : ObjectNodeConverter<Map<String, T>> {
    override fun JsonNodeObject.deserializeOrThrow() =
        fieldMap.mapValues { entry ->
            valueConverter.fromJsonNodeBase(entry.value)
                .failIfNull(JsonError(path, "Found null node in map!"))
                .orThrow()
        }

    override fun getWriters(value: Map<String, T>): List<NodeWriter<Map<String, T>>> =
        value.entries.toList().sortedBy { it.key }.map { (key, value) ->
            { jno: JsonNodeObject, _: Map<String, T> ->
                jno.copy(
                    fieldMap = jno.fieldMap +
                            (key to valueConverter.toJsonNode(value, NodePathSegment(key, jno.path)))
                )
            }
        }

}

