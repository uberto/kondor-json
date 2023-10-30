package com.ubertob.kondor.json.jsonnode

import com.ubertob.kondor.json.NamedNode
import java.math.BigDecimal

infix fun String.toNode(fieldValuesMap: Map<String, String?>): NamedNode =
    this to JsonNodeObject(fieldValuesMap.toNodes())

fun Map<String, String?>.toNodes(): FieldMap =
    mapValues { (_, str) -> str?.let { JsonNodeString(str) } ?: JsonNodeNull }

infix fun String.toNode(fieldValue: String): NamedNode =
    this to JsonNodeString(fieldValue)

infix fun String.toNode(fieldValue: Long): NamedNode =
    this to JsonNodeNumber(fieldValue.toBigDecimal())

infix fun String.toNode(fieldValue: Int): NamedNode =
    this to JsonNodeNumber(fieldValue.toBigDecimal())

infix fun String.toNode(fieldValue: Double): NamedNode =
    this to JsonNodeNumber(fieldValue.toBigDecimal())

infix fun String.toNode(fieldValue: Boolean): NamedNode =
    this to JsonNodeBoolean(fieldValue)

private fun String.nullNode() = this to JsonNodeNull

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
    _fieldMap = nodes.toMap()
)

fun JsonNode?.asStringValue(): String? = (this as? JsonNodeString)?.text

fun JsonNode?.asBooleanValue(): Boolean? = (this as? JsonNodeBoolean)?.boolean

fun JsonNode?.asNumValue(): BigDecimal? = (this as? JsonNodeNumber)?.num

fun JsonNode?.asObjFieldMap(): FieldMap? = (this as? JsonNodeObject)?._fieldMap


