package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.JsonNode

fun JsonNode.render(style: JsonStyle = JsonStyle.compact): String = style.render(this)

fun <APP : Appendable> JsonNode.render(appendable: APP, style: JsonStyle = JsonStyle.compact): APP =
    style.render(this, appendable).let { appendable }

@Deprecated("Use JsonStyle specification", replaceWith = ReplaceWith("render(stringBuilder)"))
fun JsonNode.compact(stringBuilder: StringBuilder, explicitNull: Boolean = false): StringBuilder =
    render(stringBuilder, if (explicitNull) JsonStyle.compactWithNulls else JsonStyle.compact)

@Deprecated("Use JsonStyle specification", replaceWith = ReplaceWith(".render(pretty)"))
fun JsonNode.pretty(explicitNull: Boolean = false, indent: Int = 2): String =
    JsonStyle.pretty.copy(indent = indent, includeNulls = explicitNull).render(this)