package com.ubertob.kondor.json.jsonnode

import com.ubertob.kondor.json.*
import com.ubertob.kondor.json.parser.*
import com.ubertob.kondor.outcome.*
import java.math.BigDecimal

sealed class JsonNode {
    abstract val path: NodePath

    fun nodeKind(): NodeKind<*> =
        when (this) {
            is JsonNodeArray -> ArrayNode
            is JsonNodeBoolean -> BooleanNode
            is JsonNodeNull -> NullNode
            is JsonNodeNumber -> NumberNode
            is JsonNodeObject -> ObjectNode
            is JsonNodeString -> StringNode
        }
}

typealias EntryJsonNode = Map.Entry<String, JsonNode>

data class JsonNodeNull(override val path: NodePath) : JsonNode()
data class JsonNodeBoolean(val value: Boolean, override val path: NodePath) : JsonNode()
data class JsonNodeNumber(val num: BigDecimal, override val path: NodePath) : JsonNode()
data class JsonNodeString(val text: String, override val path: NodePath) : JsonNode()
data class JsonNodeArray(val values: Iterable<JsonNode>, override val path: NodePath) : JsonNode() {
    val notNullValues: List<JsonNode> = values.filter { it.nodeKind() != NullNode }
}

data class JsonNodeObject(val fieldMap: Map<String, JsonNode>, override val path: NodePath) : JsonNode() {

    val notNullFields: List<EntryJsonNode> = fieldMap.entries.filter { it.value.nodeKind() != NullNode }

    operator fun <T> JsonProperty<T>.unaryPlus(): T =
        getter(this@JsonNodeObject)
            .onFailure { throw JsonParsingException(it) }

}


fun parseJsonNode(jsonString: CharSequence): Outcome<JsonError, JsonNode> =
    JsonLexer(jsonString).tokenize().onRoot().parseNewNode() ?: JsonNodeNull(NodePathRoot).asSuccess()
