package com.ubertob.kondor.jsonSimplified

import java.math.BigDecimal

sealed class JsonNode

object JsonNodeNull : JsonNode()
data class JsonNodeBoolean(val value: Boolean) : JsonNode()
data class JsonNodeNumber(val num: BigDecimal) : JsonNode()
data class JsonNodeString(val text: String) : JsonNode()
data class JsonNodeArray(val values: Iterable<JsonNode>) : JsonNode()
data class JsonNodeObject(val fieldMap: Map<String, JsonNode>) : JsonNode()
