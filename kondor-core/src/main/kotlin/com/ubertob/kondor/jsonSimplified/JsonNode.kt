package com.ubertob.kondor.jsonSimplified

import com.ubertob.kondor.outcome.*
import java.math.BigDecimal

sealed class JsonNode {

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

object JsonNodeNull : JsonNode()
data class JsonNodeBoolean(val value: Boolean) : JsonNode()
data class JsonNodeNumber(val num: BigDecimal) : JsonNode()
data class JsonNodeString(val text: String) : JsonNode()
data class JsonNodeArray(val values: Iterable<JsonNode>) : JsonNode() {
    val notNullValues: List<JsonNode> = values.filter { it.nodeKind() != NullNode }
}

data class JsonNodeObject(val fieldMap: Map<String, JsonNode>) : JsonNode() {

    val notNullFields: List<EntryJsonNode> = fieldMap.entries.filter { it.value.nodeKind() != NullNode }

    operator fun <T> JsonProperty<T>.unaryPlus(): T =
        getter(this@JsonNodeObject)
            .onFailure { throw JsonParsingException(it) }

}
