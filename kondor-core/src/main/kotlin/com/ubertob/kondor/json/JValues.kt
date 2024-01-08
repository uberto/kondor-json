package com.ubertob.kondor.json

import com.ubertob.kondor.json.JsonStyle.Companion.appendBoolean
import com.ubertob.kondor.json.JsonStyle.Companion.appendNumber
import com.ubertob.kondor.json.JsonStyle.Companion.appendText
import com.ubertob.kondor.json.jsonnode.*
import com.ubertob.kondor.outcome.Outcome
import com.ubertob.kondor.outcome.asSuccess
import java.math.BigDecimal


object JBoolean : JsonConverter<Boolean, JsonNodeBoolean> {

    override fun fromJsonNode(node: JsonNodeBoolean): JsonOutcome<Boolean> = node.boolean.asSuccess()
    override fun toJsonNode(value: Boolean, path: NodePath): JsonNodeBoolean =
        JsonNodeBoolean(value, path)

    override val _nodeType = BooleanNode
    override fun appendValue(app: StrAppendable, style: JsonStyle, offset: Int, value: Boolean): StrAppendable =
        app.appendBoolean(value)

}

object JString : JStringRepresentable<String>() {
    override val cons: (String) -> String = { it }
    override val render: (String) -> String = { it }
    override fun appendValue(app: StrAppendable, style: JsonStyle, offset: Int, value: String): StrAppendable =
        app.appendText(value)
}

object JDouble : JNumRepresentable<Double>() {

    override val cons: (BigDecimal) -> Double = BigDecimal::toDouble
    override val render: (Double) -> BigDecimal = Double::toBigDecimal
    override fun appendValue(app: StrAppendable, style: JsonStyle, offset: Int, value: Double): StrAppendable =
        if (value.isFinite())
            app.appendNumber(value)
        else
            app.appendText(value.toString())
}


object JInt : JNumRepresentable<Int>() {
    override val cons: (BigDecimal) -> Int = BigDecimal::intValueExact
    override val render: (Int) -> BigDecimal = Int::toBigDecimal
    override fun appendValue(app: StrAppendable, style: JsonStyle, offset: Int, value: Int): StrAppendable =
        app.appendNumber(value)
}

object JLong : JNumRepresentable<Long>() {
    override val cons: (BigDecimal) -> Long = BigDecimal::longValueExact
    override val render: (Long) -> BigDecimal = Long::toBigDecimal
    override fun appendValue(app: StrAppendable, style: JsonStyle, offset: Int, value: Long): StrAppendable =
        app.appendNumber(value)
}

fun <T> tryFromNode(node: JsonNode, f: () -> T): JsonOutcome<T> =
    Outcome.tryOrFail { f() }
        .transformFailure { throwableError ->
            when (val throwable = throwableError.throwable) {
                is JsonParsingException -> throwable.error // keep path info
                is IllegalStateException -> ConverterJsonError(node._path, throwableError.msg)
                else -> ConverterJsonError(node._path, "Caught exception: $throwable")
            }
        }

//TODO replace with Long/Int/Double represntables
abstract class JNumRepresentable<T : Any>() : JsonConverter<T, JsonNodeNumber> {
    abstract val cons: (BigDecimal) -> T
    abstract val render: (T) -> BigDecimal

    override fun fromJsonNode(node: JsonNodeNumber): JsonOutcome<T> = tryFromNode(node) { cons(node.num) }
    override fun toJsonNode(value: T, path: NodePath): JsonNodeNumber =
        JsonNodeNumber(render(value), path)

    override val _nodeType = NumberNode
    override fun appendValue(app: StrAppendable, style: JsonStyle, offset: Int, value: T): StrAppendable =
        app.appendNumber(render(value))
}

abstract class JStringRepresentable<T>() : JsonConverter<T, JsonNodeString> {
    abstract val cons: (String) -> T
    abstract val render: (T) -> String

    override fun fromJsonNode(node: JsonNodeString): JsonOutcome<T> = tryFromNode(node) { cons(node.text) }
    override fun toJsonNode(value: T, path: NodePath): JsonNodeString =
        JsonNodeString(render(value), path)

    override val _nodeType = StringNode

    override fun appendValue(app: StrAppendable, style: JsonStyle, offset: Int, value: T): StrAppendable =
        app.appendText(render(value))
}

