package com.ubertob.kondor.json.jsonnode

import com.ubertob.kondor.json.JsonError
import com.ubertob.kondor.json.JsonParsingException
import com.ubertob.kondor.json.JsonProperty
import com.ubertob.kondor.json.catchingStackOverflow
import com.ubertob.kondor.json.parser.JsonLexerEager
import com.ubertob.kondor.json.parser.parseNewNode
import com.ubertob.kondor.json.parser.parsingFailure
import com.ubertob.kondor.outcome.Outcome
import com.ubertob.kondor.outcome.asSuccess
import com.ubertob.kondor.outcome.bind
import com.ubertob.kondor.outcome.onFailure
import java.math.BigDecimal

typealias EntryJsonNode = Map.Entry<String, JsonNode>

data class FieldNodeMap(val map: Map<String, JsonNode>)

data class FieldsValuesMap(private val map: Map<String, Any?>) : FieldsValues {
    override fun getValue(fieldName: String): Any? = map[fieldName]
    override fun getMap(): Map<String, Any?> = map
}

sealed class JsonNode(val nodeKind: NodeKind<*>)

object JsonNodeNull : JsonNode(NullNode)

data class JsonNodeBoolean(val boolean: Boolean) : JsonNode(BooleanNode)

data class JsonNodeNumber(val num: Number) : JsonNode(NumberNode)

/**
 * The number of a [JsonNodeNumber] read from a zero written with a minus: a `BigDecimal` cannot hold the sign of a
 * zero, so it is kept here, together with the parsed number.
 *
 * `JDouble` and `JFloat` read it as `-0.0`, the other converters read [zero] as they would read any number, and
 * rendering it writes the zero with its sign and its scale.
 */
data class NegativeZero(val zero: BigDecimal) : Number() {
    init {
        require(zero.signum() == 0) { "a NegativeZero holds a zero, not $zero" }
    }

    override fun toDouble(): Double = -0.0
    override fun toFloat(): Float = -0.0f
    override fun toInt(): Int = 0
    override fun toLong(): Long = 0L
    override fun toByte(): Byte = 0
    override fun toShort(): Short = 0
    override fun toString(): String = "-$zero"
}
data class JsonNodeString(val text: String) : JsonNode(StringNode)
data class JsonNodeArray(val elements: Iterable<JsonNode>) : JsonNode(ArrayNode) {
    val notNullValues: List<JsonNode> = elements.filter { it.nodeKind != NullNode }
}

data class JsonNodeObject(val _fieldMap: FieldNodeMap) : JsonNode(ObjectNode) {

    constructor(map: Map<String, JsonNode>) : this(FieldNodeMap(map))

    companion object {
        @Suppress("DEPRECATION")
        internal fun buildForParsing(fieldMap: Map<String, JsonNode>, path: NodePath): JsonNodeObject =
            JsonNodeObject(FieldNodeMap(fieldMap), path) //we are forced to use the deprecated constructor
    }

    internal var _path: NodePath = NodePathRoot //hack to get the current path during parsing without breaking changes.

    @Suppress("LocalVariableName")
    @Deprecated("Use the primary constructor without path instead")
    constructor(_fieldMap: FieldNodeMap, _path: NodePath) : this(_fieldMap) {
        this._path = _path
    }

    val notNullFields: List<EntryJsonNode> by lazy { _fieldMap.map.entries.filter { it.value.nodeKind != NullNode } }

    operator fun <T> JsonProperty<T>.unaryPlus(): T =
        getter(_fieldMap, path = _path)
            .onFailure { throw JsonParsingException(it) }
}

interface FieldsValues {

    fun getValue(fieldName: String): Any?

    fun getMap(): Map<String, Any?>

    @Suppress("UNCHECKED_CAST")
    operator fun <T> JsonProperty<T>.unaryPlus(): T = getValue(propName) as T

}


fun parseJsonNode(jsonString: String): Outcome<JsonError, JsonNode> =
    if (jsonString.isEmpty())
        parsingFailure("some valid Json", "end of file", 0, NodePathRoot, "invalid Json")
    else
        catchingStackOverflow {
            JsonLexerEager(jsonString).tokenize()
                .bind { it.onRoot().parseNewNode() ?: JsonNodeNull.asSuccess() }
        }
