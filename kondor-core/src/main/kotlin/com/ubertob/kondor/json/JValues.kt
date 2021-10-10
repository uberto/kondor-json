package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.*
import com.ubertob.kondor.outcome.Outcome
import com.ubertob.kondor.outcome.asSuccess
import java.math.BigDecimal


object JBoolean : JsonConverter<Boolean, JsonNodeBoolean> {

    override fun fromJsonNode(node: JsonNodeBoolean): JsonOutcome<Boolean> = node.value.asSuccess()
    override fun toJsonNode(value: Boolean, path: NodePath): JsonNodeBoolean =
        JsonNodeBoolean(value, path)

    override val nodeType = BooleanNode

}

object JString : JsonConverter<String, JsonNodeString> {

    override fun fromJsonNode(node: JsonNodeString): JsonOutcome<String> = node.text.asSuccess()
    override fun toJsonNode(value: String, path: NodePath): JsonNodeString =
        JsonNodeString(value, path)

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

fun <T> tryFromNode(node: JsonNode, f: () -> T): JsonOutcome<T> =
    Outcome.tryOrFail { f() }
        .transformFailure { throwableError ->
            when (val throwable = throwableError.throwable) {
                is JsonParsingException -> throwable.error // keep path info
                is IllegalStateException -> JsonError(node.path, throwableError.msg)
                else -> JsonError(node.path, "Caught exception: $throwable")
            }
        }

abstract class JNumRepresentable<T : Any>() : JsonConverter<T, JsonNodeNumber> {
    abstract val cons: (BigDecimal) -> T
    abstract val render: (T) -> BigDecimal

    override fun fromJsonNode(node: JsonNodeNumber): JsonOutcome<T> = tryFromNode(node) { cons(node.num) }
    override fun toJsonNode(value: T, path: NodePath): JsonNodeNumber =
        JsonNodeNumber(render(value), path)

    override val nodeType = NumberNode
}

abstract class JStringRepresentable<T>() : JsonConverter<T, JsonNodeString> {
    abstract val cons: (String) -> T
    abstract val render: (T) -> String

    override fun fromJsonNode(node: JsonNodeString): JsonOutcome<T> = tryFromNode(node) { cons(node.text) }
    override fun toJsonNode(value: T, path: NodePath): JsonNodeString =
        JsonNodeString(render(value), path)

    override val nodeType = StringNode
}

