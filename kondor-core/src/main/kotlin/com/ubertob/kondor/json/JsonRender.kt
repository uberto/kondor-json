package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.JsonNode

fun JsonNode.render(style: JsonStyle = JsonStyle.compact): String = style.render(this)

fun <APP : StrAppendable> JsonNode.render(appendable: APP, style: JsonStyle = JsonStyle.compact): APP =
    style.render(this, appendable).let { appendable }
