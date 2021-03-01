package com.ubertob.kondor.json

import com.ubertob.kondor.json.*
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.time.LocalDate
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

//instant as date string
object JInstantD : JStringRepresentable<Instant>() {
    override val cons: (String) -> Instant = Instant::parse
    override val render: (Instant) -> String = Instant::toString
}

//instant as epoch millis
object JInstant : JNumRepresentable<Instant>() {
    override val cons: (BigDecimal) -> Instant = { Instant.ofEpochMilli(it.toLong()) }
    override val render: (Instant) -> BigDecimal = { it.toEpochMilli().toBigDecimal() }
}

object JLocalDate : JStringRepresentable<LocalDate>() {
    override val cons: (String) -> LocalDate = LocalDate::parse
    override val render: (LocalDate) -> String = LocalDate::toString
}

//for serializing Kotlin object and other single instance types
data class JInstance<T : Any>(val singleton: T) : JAny<T>() {
    override fun JsonNodeObject.deserializeOrThrow() = singleton
}


interface JSealed<T : Any> : JObject<T> {

    val typeFieldName: String
        get() = "_type"


    fun extractTypeName(obj: T): String

    val subtypesJObject: Map<String, JObject<out T>>

    fun typeWriter(jno: JsonNodeObject, obj: T): JsonNodeObject =
        jno.copy(
            fieldMap = jno.fieldMap + (typeFieldName to JsonNodeString(
                extractTypeName(obj),
                Node(typeFieldName, jno.path)
            ))
        )

    override fun JsonNodeObject.deserializeOrThrow(): T? {
        val typeName = JString.fromJsonNodeBase(
            fieldMap[typeFieldName]
                ?: error("expected field $typeFieldName not found!")
        ).orThrow()
        val bidiJson = subtypesJObject[typeName] ?: error("subtype not known $typeName")
        return bidiJson.fromJsonNode(this).orThrow()
    }


    override fun getWriters(value: T): Set<NodeWriter<T>> =
        extractTypeName(value).let { typeName ->
            findSubTypeBiDi(typeName)
                ?.getWriters(value)
                ?.plus(::typeWriter)
                ?: error("subtype not known $typeName")
        }

    @Suppress("UNCHECKED_CAST")
    fun findSubTypeBiDi(typeName: String): JObject<T>? = subtypesJObject[typeName] as? JObject<T>

}

class JMap<T : Any>(private val valueConverter: JsonAdjunction<T, *>) : JObject<Map<String, T>> {
    override fun JsonNodeObject.deserializeOrThrow() =
        fieldMap.mapValues { entry ->
            valueConverter.fromJsonNodeBase(entry.value).orThrow()
        }

    override fun getWriters(value: Map<String, T>): Set<NodeWriter<Map<String, T>>> =
        value.entries.map { (key, value) ->
            { jno: JsonNodeObject, _: Map<String, T> ->
                jno.copy(
                    fieldMap = jno.fieldMap +
                            (key to valueConverter.toJsonNode(value, Node(key, jno.path)))
                )
            }
        }.toSet()

}

