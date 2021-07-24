package com.ubertob.kondor.json.jsonnode

import com.ubertob.kondor.json.JsonError
import com.ubertob.kondor.json.JsonParsingException
import com.ubertob.kondor.json.JsonProperty
import com.ubertob.kondor.json.parser.JsonLexerLazy
import com.ubertob.kondor.json.parser.parseNewNode
import com.ubertob.kondor.outcome.Outcome
import com.ubertob.kondor.outcome.asSuccess
import com.ubertob.kondor.outcome.onFailure
import java.math.BigDecimal

typealias EntryJsonNode = Map.Entry<String, JsonNode>

sealed class JsonNode(val nodeKind: NodeKind<*>) {
    abstract val path: NodePath
}

data class JsonNodeNull(override val path: NodePath) : JsonNode(NullNode)

data class JsonNodeBoolean(val value: Boolean, override val path: NodePath) : JsonNode(BooleanNode)
data class JsonNodeNumber(val num: BigDecimal, override val path: NodePath) : JsonNode(NumberNode)
data class JsonNodeString(val text: String, override val path: NodePath) : JsonNode(StringNode)
data class JsonNodeArray(val values: Iterable<JsonNode>, override val path: NodePath) : JsonNode(ArrayNode) {
    val notNullValues: List<JsonNode> = values.filter { it.nodeKind != NullNode }
}

data class JsonNodeObject(val fieldMap: Map<String, JsonNode>, override val path: NodePath) : JsonNode(ObjectNode) {

    val notNullFields: List<EntryJsonNode> = fieldMap.entries.filter { it.value.nodeKind != NullNode }

    operator fun <T> JsonProperty<T>.unaryPlus(): T =
        getter(this@JsonNodeObject)
            .onFailure { throw JsonParsingException(it) }

}


fun parseJsonNode(jsonString: CharSequence): Outcome<JsonError, JsonNode> =
    JsonLexerLazy(jsonString).tokenize().onRoot().parseNewNode() ?: JsonNodeNull(NodePathRoot).asSuccess()
