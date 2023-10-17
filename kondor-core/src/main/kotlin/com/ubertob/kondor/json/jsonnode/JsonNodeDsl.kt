package com.ubertob.kondor.json.jsonnode

import com.ubertob.kondor.json.NamedNode
import java.math.BigDecimal

infix fun String.toNode(fieldValuesMap: Map<String, String?>): NamedNode =
    this to JsonNodeObject(fieldValuesMap.toNodes(NodePathRoot + this), NodePathRoot + this)

fun Map<String, String?>.toNodes(basePath: NodePath): FieldMap =
    mapValues { (_, str) -> str?.let { JsonNodeString(str, basePath) } ?: JsonNodeNull(basePath) }

infix fun String.toNode(fieldValue: String): NamedNode =
    this to JsonNodeString(fieldValue, NodePathRoot + this)

infix fun String.toNode(fieldValue: Long): NamedNode =
    this to JsonNodeNumber(fieldValue.toBigDecimal(), NodePathRoot + this)

infix fun String.toNode(fieldValue: Int): NamedNode =
    this to JsonNodeNumber(fieldValue.toBigDecimal(), NodePathRoot + this)

infix fun String.toNode(fieldValue: Double): NamedNode =
    this to JsonNodeNumber(fieldValue.toBigDecimal(), NodePathRoot + this)

infix fun String.toNode(fieldValue: Boolean): NamedNode =
    this to JsonNodeBoolean(fieldValue, NodePathRoot + this)

private fun String.nullNode() = this to JsonNodeNull(NodePathRoot + this)

@JvmName("toNodeNullable")
infix fun String.toNode(fieldValue: String?): NamedNode =
    fieldValue?.let(::toNode) ?: nullNode()

@JvmName("toNodeNullable")
infix fun String.toNode(fieldValue: Long?): NamedNode =
    fieldValue?.let(::toNode) ?: nullNode()

@JvmName("toNodeNullable")
infix fun String.toNode(fieldValue: Int?): NamedNode =
    fieldValue?.let(::toNode) ?: nullNode()

@JvmName("toNodeNullable")
infix fun String.toNode(fieldValue: Double?): NamedNode =
    fieldValue?.let(::toNode) ?: nullNode()

@JvmName("toNodeNullable")
infix fun String.toNode(fieldValue: Boolean?): NamedNode =
    fieldValue?.let(::toNode) ?: nullNode()

fun nodeObject(vararg nodes: NamedNode): JsonNodeObject = JsonNodeObject(
    _fieldMap = nodes.toMap(), NodePathRoot
)

fun JsonNode?.asStringValue(): String? = (this as? JsonNodeString)?.text

fun JsonNode?.asBooleanValue(): Boolean? = (this as? JsonNodeBoolean)?.boolean

fun JsonNode?.asNumValue(): BigDecimal? = (this as? JsonNodeNumber)?.num

fun JsonNode?.asObjFieldMap(): FieldMap? = (this as? JsonNodeObject)?._fieldMap


