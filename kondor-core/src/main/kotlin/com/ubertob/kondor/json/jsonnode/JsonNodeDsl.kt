package com.ubertob.kondor.json.jsonnode

import java.math.BigDecimal

infix fun String.toNode(fieldValuesMap: Map<String, String?>): Pair<String, JsonNode> =
    this to JsonNodeObject(fieldValuesMap.toNodes(NodePathRoot + this), NodePathRoot + this)

fun Map<String, String?>.toNodes(basePath: NodePath): Map<String, JsonNode> =
    mapValues { (_, str) -> str?.let { JsonNodeString(str, basePath) } ?: JsonNodeNull(basePath) }

infix fun String.toNode(fieldValue: String): Pair<String, JsonNode> =
    this to JsonNodeString(fieldValue, NodePathRoot + this)

infix fun String.toNode(fieldValue: Long): Pair<String, JsonNode> =
    this to JsonNodeNumber(fieldValue.toBigDecimal(), NodePathRoot + this)

infix fun String.toNode(fieldValue: Int): Pair<String, JsonNode> =
    this to JsonNodeNumber(fieldValue.toBigDecimal(), NodePathRoot + this)

infix fun String.toNode(fieldValue: Double): Pair<String, JsonNode> =
    this to JsonNodeNumber(fieldValue.toBigDecimal(), NodePathRoot + this)

infix fun String.toNode(fieldValue: Boolean): Pair<String, JsonNode> =
    this to JsonNodeBoolean(fieldValue, NodePathRoot + this)

private fun String.nullNode() = this to JsonNodeNull(NodePathRoot + this)

@JvmName("toNodeNullable")
infix fun String.toNode(fieldValue: String?): Pair<String, JsonNode> =
    fieldValue?.let(::toNode) ?: nullNode()

@JvmName("toNodeNullable")
infix fun String.toNode(fieldValue: Long?): Pair<String, JsonNode> =
    fieldValue?.let(::toNode) ?: nullNode()

@JvmName("toNodeNullable")
infix fun String.toNode(fieldValue: Int?): Pair<String, JsonNode> =
    fieldValue?.let(::toNode) ?: nullNode()

@JvmName("toNodeNullable")
infix fun String.toNode(fieldValue: Double?): Pair<String, JsonNode> =
    fieldValue?.let(::toNode) ?: nullNode()

@JvmName("toNodeNullable")
infix fun String.toNode(fieldValue: Boolean?): Pair<String, JsonNode> =
    fieldValue?.let(::toNode) ?: nullNode()

fun nodeObject(vararg nodes: Pair<String, JsonNode>): JsonNodeObject = JsonNodeObject(
    _fieldMap = nodes.toMap(), NodePathRoot
)

fun JsonNode?.asStringValue(): String? = (this as? JsonNodeString)?.text

fun JsonNode?.asBooleanValue(): Boolean? = (this as? JsonNodeBoolean)?.value

fun JsonNode?.asNumValue(): BigDecimal? = (this as? JsonNodeNumber)?.num

fun JsonNode?.asObjFieldMap(): Map<String, JsonNode>? = (this as? JsonNodeObject)?._fieldMap


