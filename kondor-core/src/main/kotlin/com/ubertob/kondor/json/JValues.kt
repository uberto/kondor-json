package com.ubertob.kondor.json

import com.ubertob.kondor.outcome.Outcome
import com.ubertob.kondor.outcome.asSuccess
import com.ubertob.kondor.outcome.extract
import com.ubertob.kondor.json.*
import java.math.BigDecimal


object JBoolean : JsonAdjunction<Boolean, JsonNodeBoolean> {

    override fun fromJsonNode(node: JsonNodeBoolean): JsonOutcome<Boolean> = node.value.asSuccess()
    override fun toJsonNode(value: Boolean, path: NodePath): JsonNodeBoolean = JsonNodeBoolean(value, path)

    override val nodeType = BooleanNode

}

object JString : JsonAdjunction<String, JsonNodeString> {

    override fun fromJsonNode(node: JsonNodeString): JsonOutcome<String> = node.text.asSuccess()
    override fun toJsonNode(value: String, path: NodePath): JsonNodeString = JsonNodeString(value, path)

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
    Outcome.tryThis {
        f()
    }.transformFailure { throwableError ->
        when (throwableError.t) {
            is JsonParsingException -> throwableError.t.error // keep path info
            else -> JsonError(node.path, "Caught exception: ${throwableError.t}")
        }
    }

abstract class JNumRepresentable<T : Any>() : JsonAdjunction<T, JsonNodeNumber> {
    abstract val cons: (BigDecimal) -> T
    abstract val render: (T) -> BigDecimal

    override fun fromJsonNode(node: JsonNodeNumber): JsonOutcome<T> = tryFromNode(node) { cons(node.num) }
    override fun toJsonNode(value: T, path: NodePath): JsonNodeNumber = JsonNodeNumber(render(value), path)

    override val nodeType = NumberNode
}

abstract class JStringRepresentable<T : Any>() : JsonAdjunction<T, JsonNodeString> {
    abstract val cons: (String) -> T
    abstract val render: (T) -> String

    override fun fromJsonNode(node: JsonNodeString): JsonOutcome<T> = tryFromNode(node) { cons(node.text) }
    override fun toJsonNode(value: T, path: NodePath): JsonNodeString = JsonNodeString(render(value), path)

    override val nodeType = StringNode

}


abstract class JArray<T : Any, CT: Iterable<T>>() : JsonAdjunction<CT, JsonNodeArray> {

    abstract val helper: JConverter<T>

    abstract fun convertToCollection(from: Iterable<T>): CT

    override fun fromJsonNode(node: JsonNodeArray): Outcome<JsonError, CT> =
        mapFromArray(node) { jn -> helper.fromJsonNodeBase(jn) }.transform { convertToCollection(it) }

    override fun toJsonNode(value: CT, path: NodePath): JsonNodeArray =
        mapToJson(value, helper::toJsonNode, path)

    private fun <T : Any> mapToJson(objs: Iterable<T>, f: (T, NodePath) -> JsonNode, path: NodePath): JsonNodeArray =
        JsonNodeArray(objs.map { f(it, path) }, path)

    private fun <T : Any> mapFromArray(
        node: JsonNodeArray,
        f: (JsonNode) -> JsonOutcome<T>
    ): JsonOutcome<Iterable<T>> = node.values.map(f).extract()

    override val nodeType = ArrayNode
}

data class JList<T: Any>(override val helper: JConverter<T>): JArray<T, List<T>>(){
    override fun convertToCollection(from: Iterable<T>): List<T> = from.toList()
}

data class JSet<T: Any>(override val helper: JConverter<T>): JArray<T, Set<T>>(){
    override fun convertToCollection(from: Iterable<T>): Set<T>  = from.toSet()
}