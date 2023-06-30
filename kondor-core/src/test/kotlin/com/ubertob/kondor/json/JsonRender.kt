package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.*

fun JsonNode.render(): String = JsonRenderer.default.render(this)

fun JsonNode.compact(stringBuilder: StringBuilder, explicitNull: Boolean = false): StringBuilder =
    stringBuilder.append(JsonRenderer(JsonRenderer.CompactStyle, explicitNull).render(this))

fun JsonNode.pretty(explicitNull: Boolean = false, indent: Int = 2): String =
    JsonRenderer(JsonRenderer.PrettyStyle(indent), explicitNull).render(this)