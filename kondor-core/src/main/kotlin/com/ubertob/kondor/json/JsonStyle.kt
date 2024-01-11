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

    fun render(node: JsonNode, appendable: Appendable) = appendable.appendNode(node, this)

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

        val prettyWithNulls: JsonStyle = JsonStyle(
            fieldSeparator = ",",
            valueSeparator = ": ",
            indent = 2,
            sortedObjectFields = true,
            includeNulls = true
        )

        private fun Appendable.appendNode(node: JsonNode, style: JsonStyle, offset: Int = 0) {
            when (node) {
                is JsonNodeNull -> append("null")
                is JsonNodeString -> appendQuoted(node.text)
                is JsonNodeBoolean -> append(node.boolean.toString())
                is JsonNodeNumber -> append(node.num.toString())
                is JsonNodeArray -> {
                    append('[')
                    appendNewlineIfNeeded(style.indent, offset + 1)
                    node.values(style.includeNulls).forEachIndexed { index, each ->
                        if (index > 0) {
                            append(style.fieldSeparator)
                            appendNewlineIfNeeded(style.indent, offset + 1)
                        }
                        appendNode(each, style, offset + 2)
                    }
                    appendNewlineIfNeeded(style.indent, offset)
                    append(']')
                }

                is JsonObjectNode -> {
                    append('{')
                    appendNewlineIfNeeded(style.indent, offset + 1)
                    node.fields(style.includeNulls, style.sortedObjectFields)
                        .forEachIndexed { index, entry ->
                            if (index > 0) {
                                append(style.fieldSeparator)
                                appendNewlineIfNeeded(style.indent, offset + 1)
                            }
                            appendQuoted(entry.key)
                            append(style.valueSeparator)
                            appendNode(entry.value, style, offset + 2)
                        }
                    appendNewlineIfNeeded(style.indent, offset)
                    append('}')
                }
            }
        }

        private fun Appendable.appendNewlineIfNeeded(indent: Int?, offset: Int) =
            indent?.also {
                append('\n')
                repeat(indent * offset) {
                    append(" ")
                }
            }


        private val regex = """[\\"\n\r\t]""".toRegex()
        private fun String.escaped(): String =
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
            }

        private fun Appendable.appendQuoted(string: String) {
            append("\"")
            append(string.escaped())
            append("\"")
        }

        private fun JsonObjectNode.fields(includeNulls: Boolean, sorted: Boolean) =
            (if (includeNulls) _fieldMap.entries else notNullFields).let {
                if (sorted) it.sortedBy(Map.Entry<String, JsonNode>::key) else it
            }

        private fun JsonNodeArray.values(includeNulls: Boolean) =
            if (includeNulls) elements else notNullValues
    }
}
