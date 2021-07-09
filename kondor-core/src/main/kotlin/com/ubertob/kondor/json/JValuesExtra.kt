package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.JsonNodeObject
import com.ubertob.kondor.json.jsonnode.JsonNodeString
import com.ubertob.kondor.json.jsonnode.NodePathSegment
import com.ubertob.kondor.json.schema.enumSchema
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

//todo get Enum class instead of cons
data class JEnum<E : Enum<E>>(override val cons: (String) -> E, val values: List<E> = emptyList() ) : JStringRepresentable<E>() {
    override val render: (E) -> String = { it.name }
    override fun schema(): JsonNodeObject = enumSchema(values)
}

//for serializing Kotlin object and other single instance types
data class JInstance<T : Any>(val singleton: T) : JAny<T>() {
    override fun JsonNodeObject.deserializeOrThrow() = singleton
}




abstract class JSealed<T : Any> : PolymorphicConverter<T>() {

    open val discriminatorFieldName: String
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
abstract class NestedPolyConverter<T : Any> : PolymorphicConverter<T>() {

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

