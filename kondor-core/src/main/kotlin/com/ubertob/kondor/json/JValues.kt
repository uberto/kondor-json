package com.ubertob.kondor.json

import com.ubertob.kondor.json.JsonStyle.Companion.appendBoolean
import com.ubertob.kondor.json.JsonStyle.Companion.appendNumber
import com.ubertob.kondor.json.JsonStyle.Companion.appendText
import com.ubertob.kondor.json.jsonnode.*
import com.ubertob.kondor.json.parser.TokensStream
import com.ubertob.kondor.json.parser.parseBoolean
import com.ubertob.kondor.json.parser.parseNumber
import com.ubertob.kondor.json.parser.parseString
import com.ubertob.kondor.outcome.Outcome
import com.ubertob.kondor.outcome.OutcomeException
import com.ubertob.kondor.outcome.asFailure
import com.ubertob.kondor.outcome.asSuccess
import java.math.BigDecimal
import java.math.BigInteger


abstract class JBooleanRepresentable<T : Any>() : JsonConverter<T, JsonNodeBoolean> {
    abstract val cons: (Boolean) -> T
    abstract val render: (T) -> Boolean

    override fun fromJsonNode(node: JsonNodeBoolean, path: NodePath): JsonOutcome<T> =
        tryFromNode(path) { cons(node.boolean) }

    override fun toJsonNode(value: T): JsonNodeBoolean =
        JsonNodeBoolean(render(value))

    override val _nodeType = BooleanNode
    override fun appendValue(app: CharWriter, style: JsonStyle, offset: Int, value: T): CharWriter =
        app.appendBoolean(render(value))

    override fun fromTokens(tokens: TokensStream, path: NodePath): JsonOutcome<T> =
        parseBoolean(tokens, path)
            .transform { cons(it) }
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

abstract class JIntRepresentable<T : Any> : JNumRepresentable<Int, T>() {
    override fun parser(value: String): JsonOutcome<Int> = value.toInt().asSuccess()

    override fun toNumberSubtype(number: Number): Int =
        when (number) {
            is BigDecimal -> number.intValueExact()
            else -> number.toInt()
        }

}

object JInt : JIntRepresentable<Int>() {
    override val cons: (Int) -> Int = { it }
    override val render: (Int) -> Int = { it }
}

abstract class JLongRepresentable<T : Any> : JNumRepresentable<Long, T>() {
    override fun parser(value: String): JsonOutcome<Long> = value.toLong().asSuccess()

    override fun toNumberSubtype(number: Number): Long =
        when (number) {
            is BigDecimal -> number.longValueExact()
            else -> number.toLong()
        }
}

object JLong : JLongRepresentable<Long>() {
    override val cons: (Long) -> Long = { it }
    override val render: (Long) -> Long = { it }
}

abstract class JFloatRepresentable<T : Any> : JNumRepresentable<Float, T>() {
    override fun parser(value: String): JsonOutcome<Float> = value.toFloat().asSuccess()
    override fun toNumberSubtype(number: Number): Float = number.toFloat()

    override fun appendValue(app: CharWriter, style: JsonStyle, offset: Int, value: T): CharWriter =
        render(value).let { float ->
            if (float.isFinite())
                app.appendNumber(float)
            else
                app.appendText(float.toString())
        }
}

object JFloat : JFloatRepresentable<Float>() {
    override val cons: (Float) -> Float = { it }
    override val render: (Float) -> Float = { it }
}


abstract class JDoubleRepresentable<T : Any> : JNumRepresentable<Double, T>() {
    override fun parser(value: String): JsonOutcome<Double> = value.toDouble().asSuccess()
    override fun toNumberSubtype(number: Number): Double = number.toDouble()

    override fun appendValue(app: CharWriter, style: JsonStyle, offset: Int, value: T): CharWriter =
        render(value).let { double ->
            if (double.isFinite())
                app.appendNumber(double)
            else
                app.appendText(double.toString())
        }

    override fun fromJsonNodeBase(node: JsonNode, path: NodePath): JsonOutcome<T?> =
        when (node) {
            is JsonNodeNumber -> fromJsonNode(node, path)
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

}

object JDouble : JDoubleRepresentable<Double>() {
    override val cons: (Double) -> Double = { it }
    override val render: (Double) -> Double = { it }
}

abstract class JBigDecimalRepresentable<T : Any> : JNumRepresentable<BigDecimal, T>() {
    override fun parser(value: String): JsonOutcome<BigDecimal> = BigDecimal(value).asSuccess()
    override fun toNumberSubtype(number: Number): BigDecimal = number.toString().toBigDecimal()
}

object JBigDecimal : JBigDecimalRepresentable<BigDecimal>() {
    override val cons: (BigDecimal) -> BigDecimal = { it }
    override val render: (BigDecimal) -> BigDecimal = { it }
}

abstract class JBigIntegerRepresentable<T : Any> : JNumRepresentable<BigInteger, T>() {
    override fun parser(value: String): JsonOutcome<BigInteger> = BigInteger(value).asSuccess()
    override fun toNumberSubtype(number: Number): BigInteger = number.toString().toBigInteger()
}

object JBigInteger : JBigIntegerRepresentable<BigInteger>() {
    override val cons: (BigInteger) -> BigInteger = { it }
    override val render: (BigInteger) -> BigInteger = { it }
}


fun <T> tryWithPath(path: NodePath, f: () -> JsonOutcome<T>): JsonOutcome<T> =
    try {
        f()
    } catch (exception: Throwable) {
        when (exception) {
            is NumberFormatException -> ConverterJsonError(path, "Wrong number format ${exception.message}")
            is ArithmeticException -> ConverterJsonError(path, "Wrong number format ${exception.message}")
            is JsonParsingException -> exception.error
            is IllegalStateException -> ConverterJsonError(path, "IllegalStateException: ${exception.message}")
            is OutcomeException -> exception.toJsonError(path)
            else -> ConverterJsonError(path, "Caught exception: $exception")
        }.asFailure()
    }

fun <T> tryFromNode(path: NodePath, f: () -> T): JsonOutcome<T> =
    Outcome.tryOrFail { f() }
        .transformFailure { throwableError ->
            when (val exception = throwableError.throwable) {
                is ArithmeticException -> ConverterJsonError(path, "Wrong number format: ${exception.message}")
                is JsonParsingException -> exception.error
                is IllegalStateException -> ConverterJsonError(path, throwableError.msg)
                is OutcomeException -> exception.toJsonError(path)
                else -> ConverterJsonError(path, "Caught exception: $exception")
            }
        }

private fun OutcomeException.toJsonError(path: NodePath): JsonError =
    when (val err = error) {
        is JsonError -> err
        else -> ConverterJsonError(path, "Nested exception: ${this}")
    }


abstract class JNumRepresentable<NUM : Number, T : Any>() : JsonConverter<T, JsonNodeNumber> {
    abstract val cons: (NUM) -> T
    abstract val render: (T) -> NUM

    abstract fun parser(value: String): JsonOutcome<NUM>
    abstract fun toNumberSubtype(number: Number): NUM

    override fun fromTokens(tokens: TokensStream, path: NodePath): JsonOutcome<T> =
        parseNumber(tokens, path, safelyParse(path))
            .transform { cons(it) }

    private fun safelyParse(path: NodePath): (String) -> JsonOutcome<NUM> = { numAsStr ->
        tryWithPath(path) {
            parser(numAsStr)
        }
    }

    override fun appendValue(app: CharWriter, style: JsonStyle, offset: Int, value: T): CharWriter =
        app.appendNumber(render(value))

    override fun fromJsonNode(node: JsonNodeNumber, path: NodePath): JsonOutcome<T> =
        tryFromNode(path) { cons(toNumberSubtype(node.num)) }

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

    override fun appendValue(app: CharWriter, style: JsonStyle, offset: Int, value: T): CharWriter =
        app.appendText(render(value))

    override fun fromTokens(tokens: TokensStream, path: NodePath): JsonOutcome<T> =
        parseString(tokens, path, true)
            .transform { cons(it) }
}
