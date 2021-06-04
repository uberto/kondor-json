package com.ubertob.kondor.jsonSimplified

import com.ubertob.kondor.outcome.Outcome
import com.ubertob.kondor.outcome.asSuccess
import com.ubertob.kondor.outcome.extractList
import java.math.BigDecimal


object JBoolean : JsonConverter<Boolean, JsonNodeBoolean> {

    override fun fromJsonNode(node: JsonNodeBoolean): JsonOutcome<Boolean> = node.value.asSuccess()
    override fun toJsonNode(value: Boolean): JsonNodeBoolean =
        JsonNodeBoolean(value)

    override val nodeType = BooleanNode

}

object JString : JsonConverter<String, JsonNodeString> {

    override fun fromJsonNode(node: JsonNodeString): JsonOutcome<String> = node.text.asSuccess()
    override fun toJsonNode(value: String): JsonNodeString =
        JsonNodeString(value)

    override val nodeType = StringNode
}

object JDouble : JNumRepresentable<Double>() {

    override val cons: (BigDecimal) -> Double = BigDecimal::toDouble
    override val render: (Double) -> BigDecimal = Double::toBigDecimal
}


object JInt : JNumRepresentable<Int>() {
    override val cons: (BigDecimal) -> Int = BigDecimal::toInt
    override val render: (Int) -> BigDecimal = Int::toBigDecimal
}

object JLong : JNumRepresentable<Long>() {
    override val cons: (BigDecimal) -> Long = BigDecimal::toLong
    override val render: (Long) -> BigDecimal = Long::toBigDecimal
}

fun <T : Any> tryFromNode(node: JsonNode, f: () -> T): JsonOutcome<T> =
    Outcome.tryOrFail {
        f()
    }.transformFailure { throwableError ->
        when (val throwable = throwableError.throwable) {
            is JsonParsingException -> throwable.error // keep path info
            is IllegalStateException -> JsonError(throwableError.msg)
            else -> JsonError("Caught exception: $throwable")
        }
    }

abstract class JNumRepresentable<T : Any>() : JsonConverter<T, JsonNodeNumber> {
    abstract val cons: (BigDecimal) -> T
    abstract val render: (T) -> BigDecimal

    override fun fromJsonNode(node: JsonNodeNumber): JsonOutcome<T> = tryFromNode(node) { cons(node.num) }
    override fun toJsonNode(value: T): JsonNodeNumber =
        JsonNodeNumber(render(value))

    override val nodeType = NumberNode
}

abstract class JStringRepresentable<T : Any>() : JsonConverter<T, JsonNodeString> {
    abstract val cons: (String) -> T
    abstract val render: (T) -> String

    override fun fromJsonNode(node: JsonNodeString): JsonOutcome<T> = tryFromNode(node) { cons(node.text) }
    override fun toJsonNode(value: T): JsonNodeString =
        JsonNodeString(render(value))

    override val nodeType = StringNode

}

interface JArray<T : Any, CT : Iterable<T>> : JArrayConverter<CT> {

    val converter: JConverter<T>

    fun convertToCollection(from: Iterable<T>): CT

    override fun fromJsonNode(node: JsonNodeArray): Outcome<JsonError, CT> =
        mapFromArray(node, converter::fromJsonNodeBase)
            .transform { convertToCollection(it) }

    override fun toJsonNode(value: CT): JsonNodeArray =
        mapToJson(value, converter::toJsonNode)

    private fun <T : Any> mapToJson(objs: Iterable<T>, f: (T) -> JsonNode): JsonNodeArray =
        JsonNodeArray(objs.map { f(it) })

    private fun <T : Any> mapFromArray(
        node: JsonNodeArray,
        f: (JsonNode) -> JsonOutcome<T?>
    ): JsonOutcome<Iterable<T>> = node.values.map(f)
        .extractList()
        .transform { it.filterNotNull() }

}

data class JList<T : Any>(override val converter: JConverter<T>) : JArray<T, List<T>> {
    override fun convertToCollection(from: Iterable<T>): List<T> = from.toList()
    override val nodeType = ArrayNode
}

data class JSet<T : Any>(override val converter: JConverter<T>) : JArray<T, Set<T>> {
    override fun convertToCollection(from: Iterable<T>): Set<T> = from.toSet()
    override val nodeType = ArrayNode
}