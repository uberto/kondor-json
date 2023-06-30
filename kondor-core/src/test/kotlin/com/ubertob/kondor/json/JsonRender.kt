package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.JsonNode

fun JsonNode.render(): String = JsonStyle.singleLine.render(this) //TODO compact

//TODO obsolete
fun JsonNode.compact(stringBuilder: StringBuilder, explicitNull: Boolean = false): StringBuilder =
    stringBuilder.append(
        (if (explicitNull) JsonStyle.compactIncludeNulls else JsonStyle.compact)
            .render(this)
    )

//TODO obsolete
fun JsonNode.pretty(explicitNull: Boolean = false, indent: Int = 2): String =
    JsonStyle.pretty.copy(indent = indent, includeNulls = explicitNull).render(this)