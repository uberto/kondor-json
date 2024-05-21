package com.ubertob.kondor.json

import com.ubertob.kondor.json.JsonStyle.Companion.appendBoolean
import com.ubertob.kondor.json.JsonStyle.Companion.appendNumber
import com.ubertob.kondor.json.JsonStyle.Companion.appendText
import com.ubertob.kondor.json.jsonnode.*
import com.ubertob.kondor.outcome.Outcome
import com.ubertob.kondor.outcome.OutcomeException
import com.ubertob.kondor.outcome.asFailure
import com.ubertob.kondor.outcome.asSuccess
import java.math.BigDecimal


abstract class JBooleanRepresentable<T : Any>() : JsonConverter<T, JsonNodeBoolean> {
    abstract val cons: (Boolean) -> T
    abstract val render: (T) -> Boolean

    override fun fromJsonNode(node: JsonNodeBoolean, path: NodePath): JsonOutcome<T> = tryFromNode(path) { cons(node.boolean) }
    override fun toJsonNode(value: T): JsonNodeBoolean =
        JsonNodeBoolean(render(value))

    override val _nodeType = BooleanNode
    override fun appendValue(app: CharWriter, style: JsonStyle, offset: Int, value: T): CharWriter =
        app.appendBoolean(render(value))
}

object JBoolean : JBooleanRepresentable<Boolean>() {
    override val cons: (Boolean) -> Boolean = { it }
    override val render: (Boolean) -> Boolean = { it }
}

object JString : JStringRepresentable<String>() {
    override val cons: (String) -> String = { it }
    override val render: (String) -> String = { it }
    override fun appendValue(app: CharWriter, style: JsonStyle, offset: Int, value: String): CharWriter =
        app.appendText(value)
}

object JDouble : JNumRepresentable<Double>() {

    override val cons: (Number) -> Double = Number::toDouble
    override val render: (Double) -> Number = Double::toBigDecimal
    override fun appendValue(app: CharWriter, style: JsonStyle, offset: Int, value: Double): CharWriter =
        if (value.isFinite())
            app.appendNumber(value)
        else
            app.appendText(value.toString())
}


object JInt : JNumRepresentable<Int>() {
    override val cons: (Number) -> Int = { num ->
        when (num) {
            is BigDecimal -> num.intValueExact()
            else -> num.toInt()
        }
    }
    override val render: (Int) -> Number = { it }
    override fun appendValue(app: CharWriter, style: JsonStyle, offset: Int, value: Int): CharWriter =
        app.appendNumber(value)
}

object JLong : JNumRepresentable<Long>() {
    override val cons: (Number) -> Long = { num ->
        when (num) {
            is BigDecimal -> num.longValueExact()
            else -> num.toLong()
        }
    }
    override val render: (Long) -> Number = { it }
    override fun appendValue(app: CharWriter, style: JsonStyle, offset: Int, value: Long): CharWriter =
        app.appendNumber(value)
}

fun <T> tryFromNode(path: NodePath, f: () -> T): JsonOutcome<T> =
    Outcome.tryOrFail { f() }
        .transformFailure { throwableError ->
            when (val throwable = throwableError.throwable) {
                is JsonParsingException -> throwable.error // add path info ?
                is IllegalStateException -> ConverterJsonError(path, throwableError.msg)
                is OutcomeException -> jsonError(throwable, path)
                else -> ConverterJsonError(path, "Caught exception: $throwable")
            }
        }

private fun jsonError(exception: OutcomeException, path: NodePath): JsonError =
    when (val err = exception.error) {
        is JsonError -> err
        else -> ConverterJsonError(path, "Caught exception: $exception")
    }


abstract class JNumRepresentable<T : Any>() : JsonConverter<T, JsonNodeNumber> {
    abstract val cons: (Number) -> T
    abstract val render: (T) -> Number



    override fun fromJsonNodeBase(node: JsonNode, path: NodePath): JsonOutcome<T?> =
        when (node) {
            is JsonNodeNumber -> fromJsonNode(node)
            is JsonNodeString -> tryNanNode(node, path)
            is JsonNodeNull -> null.asSuccess()
            else -> ConverterJsonError(
                path,
                "expected a Number or NaN but found ${node.nodeKind.desc}"
            ).asFailure()
        }

    private fun tryNanNode(node: JsonNodeString, path: NodePath): Outcome<JsonError, T?> =
        when (node.text) {
            "NaN" -> cons(Double.NaN).asSuccess()
            "+Infinity" -> cons(Double.POSITIVE_INFINITY).asSuccess()
            "-Infinity" -> cons(Double.NEGATIVE_INFINITY).asSuccess()
            else -> ConverterJsonError(
                path,
                "expected a Number or NaN but found '${node.text}'"
            ).asFailure()
        }

    override fun fromJsonNode(node: JsonNodeNumber, path: NodePath): JsonOutcome<T> =
        tryFromNode(path) { cons(node.num) }

    override fun toJsonNode(value: T): JsonNodeNumber =
        JsonNodeNumber(render(value))

    override val _nodeType = NumberNode
    override fun appendValue(app: CharWriter, style: JsonStyle, offset: Int, value: T): CharWriter =
        app.appendNumber(render(value))
}

abstract class JStringRepresentable<T>() : JsonConverter<T, JsonNodeString> {
    abstract val cons: (String) -> T
    abstract val render: (T) -> String

    override fun fromJsonNode(node: JsonNodeString, path: NodePath): JsonOutcome<T> =
        tryFromNode(path) { cons(node.text) }

    override fun toJsonNode(value: T): JsonNodeString =
        JsonNodeString(render(value))

    override val _nodeType = StringNode

    override fun appendValue(app: CharWriter, style: JsonStyle, offset: Int, value: T): CharWriter =
        app.appendText(render(value))
}

