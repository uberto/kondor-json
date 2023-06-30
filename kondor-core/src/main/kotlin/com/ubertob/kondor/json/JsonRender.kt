package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.JsonNode

fun JsonNode.render(style: JsonStyle = JsonStyle.compact): String = style.render(this)

fun JsonNode.render(stringBuilder: StringBuilder, style: JsonStyle = JsonStyle.compact): StringBuilder =
    stringBuilder.append(style.render(this, stringBuilder))

@Deprecated("Use JsonStyle specification", replaceWith = ReplaceWith("render(stringBuilder)"))
fun JsonNode.compact(stringBuilder: StringBuilder, explicitNull: Boolean = false): StringBuilder =
    render(stringBuilder, if (explicitNull) JsonStyle.compactWithNulls else JsonStyle.compact)

@Deprecated("Use JsonStyle specification", replaceWith = ReplaceWith("render(pretty)"))
fun JsonNode.pretty(explicitNull: Boolean = false, indent: Int = 2): String =
    JsonStyle.pretty.copy(indent = indent, includeNulls = explicitNull).render(this)