package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.JsonNode
import java.io.OutputStream

fun JsonNode.render(style: JsonStyle = JsonStyle.compact): String = style.render(this)

fun <APP : CharWriter> JsonNode.render(appendable: APP, style: JsonStyle = JsonStyle.compact): APP =
    style.render(this, appendable).let { appendable }

fun JsonNode.render(outputStream: OutputStream, style: JsonStyle = JsonStyle.compact) = style.render(this, OutputStreamCharWriter(outputStream))



