package com.ubertob.kondor.json.jsonnode

import com.ubertob.kondor.json.JsonError
import com.ubertob.kondor.json.JsonParsingException
import com.ubertob.kondor.json.JsonProperty
import com.ubertob.kondor.json.parser.JsonLexerEager
import com.ubertob.kondor.json.parser.parseNewNode
import com.ubertob.kondor.outcome.Outcome
import com.ubertob.kondor.outcome.asSuccess
import com.ubertob.kondor.outcome.bind
import com.ubertob.kondor.outcome.onFailure
import java.math.BigDecimal

typealias EntryJsonNode = Map.Entry<String, JsonNode>

sealed class JsonNode(val nodeKind: NodeKind<*>) {
    abstract val _path: NodePath
}

data class JsonNodeNull(override val _path: NodePath) : JsonNode(NullNode)

data class JsonNodeBoolean(val boolean: Boolean, override val _path: NodePath) : JsonNode(BooleanNode)
data class JsonNodeNumber(val num: BigDecimal, override val _path: NodePath) : JsonNode(NumberNode)
data class JsonNodeString(val text: String, override val _path: NodePath) : JsonNode(StringNode)
data class JsonNodeArray(val elements: Iterable<JsonNode>, override val _path: NodePath) : JsonNode(ArrayNode) {
    val notNullValues: List<JsonNode> = elements.filter { it.nodeKind != NullNode }
}

data class JsonNodeObject(val _fieldMap: Map<String, JsonNode>, override val _path: NodePath) : JsonNode(ObjectNode) {

    val notNullFields: List<EntryJsonNode> by lazy { _fieldMap.entries.filter { it.value.nodeKind != NullNode } }

    operator fun <T> JsonProperty<T>.unaryPlus(): T =
        getter(this@JsonNodeObject)
            .onFailure { throw JsonParsingException(it) }

}

fun parseJsonNode(jsonString: String): Outcome<JsonError, JsonNode> =
    JsonLexerEager(jsonString).tokenize()
//    JsonLexerLazy(ByteArrayInputStream(jsonString.toByteArray())).tokenize()
        .bind { it.onRoot().parseNewNode() ?: JsonNodeNull(NodePathRoot).asSuccess() }
