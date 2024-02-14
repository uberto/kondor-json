package com.ubertob.kondor.json.jsonnode

import com.ubertob.kondor.json.JsonError
import com.ubertob.kondor.json.JsonParsingException
import com.ubertob.kondor.json.JsonProperty
import com.ubertob.kondor.json.parser.JsonLexerEager
import com.ubertob.kondor.json.parser.parseNewNode
import com.ubertob.kondor.json.parser.parsingFailure
import com.ubertob.kondor.outcome.Outcome
import com.ubertob.kondor.outcome.asSuccess
import com.ubertob.kondor.outcome.bind
import com.ubertob.kondor.outcome.onFailure

typealias EntryJsonNode = Map.Entry<String, JsonNode>
typealias FieldMap = Map<String, JsonNode>

sealed class JsonNode(val nodeKind: NodeKind<*>) {
    abstract val _path: NodePath
}

data class JsonNodeNull(override val _path: NodePath) : JsonNode(NullNode)

data class JsonNodeBoolean(val boolean: Boolean, override val _path: NodePath) : JsonNode(BooleanNode)

data class JsonNodeNumber(val num: Number, override val _path: NodePath) : JsonNode(NumberNode)
data class JsonNodeString(val text: String, override val _path: NodePath) : JsonNode(StringNode)
data class JsonNodeArray(val elements: Iterable<JsonNode>, override val _path: NodePath) : JsonNode(ArrayNode) {
    val notNullValues: List<JsonNode> = elements.filter { it.nodeKind != NullNode }
}

data class JsonNodeObject(val _fieldMap: FieldMap, override val _path: NodePath) : JsonNode(ObjectNode) {

    val notNullFields: List<EntryJsonNode> by lazy { _fieldMap.entries.filter { it.value.nodeKind != NullNode } }

    operator fun <T> JsonProperty<T>.unaryPlus(): T =
        getter(this@JsonNodeObject)
            .onFailure { throw JsonParsingException(it) }

}

fun parseJsonNode(jsonString: String): Outcome<JsonError, JsonNode> =
    if (jsonString.isEmpty())
        parsingFailure("some valid Json", "end of file", 0, NodePathRoot, "invalid Json")
    else
        JsonLexerEager(jsonString).tokenize()
//    JsonLexerLazy(ByteArrayInputStream(jsonString.toByteArray())).tokenize()
            .bind { it.onRoot().parseNewNode() ?: JsonNodeNull(NodePathRoot).asSuccess() }
