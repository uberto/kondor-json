package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.*

data class JsonStyle(
    val fieldSeparator: String,
    val valueSeparator: String,
    val indent: Int?,
    val sortedObjectFields: Boolean,
    val includeNulls: Boolean
) {
    fun render(node: JsonNode): String = buildString { render(node, this) }

    fun render(node: JsonNode, appendable: Appendable) = appendable.append(node, this)

    companion object {
        val singleLine = JsonStyle(
            fieldSeparator = ", ",
            valueSeparator = ": ",
            indent = null,
            sortedObjectFields = false,
            includeNulls = false
        )

        val compact = JsonStyle(
            fieldSeparator = ",",
            valueSeparator = ":",
            indent = null,
            sortedObjectFields = false,
            includeNulls = false
        )

        val compactWithNulls = JsonStyle(
            fieldSeparator = ",",
            valueSeparator = ":",
            indent = null,
            sortedObjectFields = false,
            includeNulls = true
        )

        val pretty = JsonStyle(
            fieldSeparator = ",",
            valueSeparator = ": ",
            indent = 2,
            sortedObjectFields = true,
            includeNulls = false
        )

        val prettyIncludeNulls: JsonStyle = JsonStyle(
            fieldSeparator = ",",
            valueSeparator = ": ",
            indent = 2,
            sortedObjectFields = true,
            includeNulls = true
        )

        private fun Appendable.append(node: JsonNode, style: JsonStyle, offset: Int = 0) {
            when (node) {
                is JsonNodeNull -> append("null")
                is JsonNodeString -> append(node.text.quoted())
                is JsonNodeBoolean -> append(node.value.toString())
                is JsonNodeNumber -> append(node.num.toString())
                is JsonNodeArray -> {
                    append('[')
                    appendIndentationIfNeeded(style.indent, offset + 1)
                    node.values(style.includeNulls).forEachIndexed { index, each ->
                        if (index > 0) {
                            append(style.fieldSeparator)
                            appendIndentationIfNeeded(style.indent, offset + 1)
                        }
                        append(each, style, offset + 2)
                    }
                    appendIndentationIfNeeded(style.indent, offset)
                    append(']')
                }

                is JsonNodeObject -> {
                    append('{')
                    appendIndentationIfNeeded(style.indent, offset + 1)
                    node.fields(style.includeNulls, style.sortedObjectFields)
                        .forEachIndexed { index, entry ->
                            if (index > 0) {
                                append(style.fieldSeparator)
                                appendIndentationIfNeeded(style.indent, offset + 1)
                            }
                            append(entry.key.quoted())
                            append(style.valueSeparator)
                            append(entry.value, style, offset + 2)
                        }
                    appendIndentationIfNeeded(style.indent, offset)
                    append('}')
                }
            }
        }

        private fun Appendable.appendIndentationIfNeeded(indent: Int?, offset: Int) =
            indent?.also {
                append('\n')
                append(" ".repeat(indent * offset))
            }


        private val regex = """[\\"\n\r\t]""".toRegex()
        private fun String.quoted(): String =
            replace(regex) { m ->
                when (m.value) {
                    "\\" -> "\\\\"
                    "\"" -> "\\\""
                    "\n" -> "\\n"
                    "\b" -> "\\b"
                    "\r" -> "\\r"
                    "\t" -> "\\t"
                    else -> ""
                }
            }.let { "\"${it}\"" }

        private fun JsonNodeObject.fields(includeNulls: Boolean, sorted: Boolean) =
            (if (includeNulls) _fieldMap.entries else notNullFields).let {
                if (sorted) it.sortedBy(Map.Entry<String, JsonNode>::key) else it
            }

        private fun JsonNodeArray.values(includeNulls: Boolean) =
            if (includeNulls) values else notNullValues
    }
}