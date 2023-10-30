package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.*
import com.ubertob.kondor.outcome.Outcome
import com.ubertob.kondor.outcome.asSuccess
import java.math.BigDecimal


object JBoolean : JsonConverter<Boolean, JsonNodeBoolean> {

    override fun fromJsonNode(node: JsonNodeBoolean, path: NodePath): JsonOutcome<Boolean> = node.boolean.asSuccess()
    override fun toJsonNode(value: Boolean): JsonNodeBoolean =
        JsonNodeBoolean(value)

    override val _nodeType = BooleanNode

}

object JString : JStringRepresentable<String>() {
    override val cons: (String) -> String = {it}
    override val render: (String) -> String = {it}
}

object JDouble : JNumRepresentable<Double>() {

    override val cons: (BigDecimal) -> Double = BigDecimal::toDouble
    override val render: (Double) -> BigDecimal = Double::toBigDecimal
}


object JInt : JNumRepresentable<Int>() {
    override val cons: (BigDecimal) -> Int = BigDecimal::intValueExact
    override val render: (Int) -> BigDecimal = Int::toBigDecimal
}

object JLong : JNumRepresentable<Long>() {
    override val cons: (BigDecimal) -> Long = BigDecimal::longValueExact
    override val render: (Long) -> BigDecimal = Long::toBigDecimal
}

fun <T> tryFromNode(path: NodePath, f: () -> T): JsonOutcome<T> =
    Outcome.tryOrFail { f() }
        .transformFailure { throwableError ->
            when (val throwable = throwableError.throwable) {
                is JsonParsingException -> throwable.error // add!!! path info
                is IllegalStateException -> ConverterJsonError(path, throwableError.msg)
                else -> ConverterJsonError(path, "Caught exception: $throwable")
            }
        }

abstract class JNumRepresentable<T : Any>() : JsonConverter<T, JsonNodeNumber> {
    abstract val cons: (BigDecimal) -> T
    abstract val render: (T) -> BigDecimal

    override fun fromJsonNode(node: JsonNodeNumber, path: NodePath): JsonOutcome<T> =
        tryFromNode(path) { cons(node.num) }

    override fun toJsonNode(value: T): JsonNodeNumber =
        JsonNodeNumber(render(value))

    override val _nodeType = NumberNode
}

abstract class JStringRepresentable<T>() : JsonConverter<T, JsonNodeString> {
    abstract val cons: (String) -> T
    abstract val render: (T) -> String

    override fun fromJsonNode(node: JsonNodeString, path: NodePath): JsonOutcome<T> =
        tryFromNode(path) { cons(node.text) }

    override fun toJsonNode(value: T): JsonNodeString =
        JsonNodeString(render(value))

    override val _nodeType = StringNode
}

