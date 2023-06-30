package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.*

data class JsonRenderer(private val style: Style, private val includeNulls: Boolean) {
    fun render(node: JsonNode): String = buildString { render(node, this) }

    fun render(node: JsonNode, appendable: Appendable) = appendable.append(node, style, includeNulls)

    sealed interface Style { //merge with JsonRenderer TODO
        val fieldSeparator: String
        val valueSeparator: String
        val indent: Int?
        val sortedObjectFields: Boolean
    }

    object DefaultStyle : Style {
        override val fieldSeparator: String = ", "
        override val valueSeparator: String = ": "
        override val indent = null
        override val sortedObjectFields: Boolean = false
    }

    object CompactStyle : Style {
        override val fieldSeparator: String = ","
        override val valueSeparator: String = ":"
        override val indent = null
        override val sortedObjectFields: Boolean = false
    }

    data class PrettyStyle(override val indent: Int = 2, override val sortedObjectFields: Boolean = true) : Style {
        override val fieldSeparator: String = ","
        override val valueSeparator: String = ": "
    }

    companion object {
        val default: JsonRenderer = JsonRenderer(DefaultStyle, includeNulls = false)
        val compact: JsonRenderer = JsonRenderer(CompactStyle, includeNulls = false)
        val compactIncludeNulls: JsonRenderer = JsonRenderer(CompactStyle, includeNulls = true)
        val pretty: JsonRenderer = JsonRenderer(PrettyStyle(), includeNulls = false)
        val prettyIncludeNulls: JsonRenderer = JsonRenderer(PrettyStyle(), includeNulls = true)

        private fun Appendable.append(node: JsonNode, style: Style, includeNulls: Boolean, offset: Int = 0) {
            when (node) {
                is JsonNodeNull -> append("null")
                is JsonNodeString -> append(node.text.quoted())
                is JsonNodeBoolean -> append(node.value.toString())
                is JsonNodeNumber -> append(node.num.toString())
                is JsonNodeArray -> {
                    append('[')
                    appendIndentationIfNeeded(style.indent, offset + 1)
                    node.values(includeNulls).forEachIndexed { index, each ->
                        if (index > 0) {
                            append(style.fieldSeparator)
                            appendIndentationIfNeeded(style.indent, offset + 1)
                        }
                        append(each, style, includeNulls, offset + 2)
                    }
                    appendIndentationIfNeeded(style.indent, offset)
                    append(']')
                }

                is JsonNodeObject -> {
                    append('{')
                    appendIndentationIfNeeded(style.indent, offset + 1)
                    node.fields(includeNulls, style.sortedObjectFields)
                        .forEachIndexed { index, entry ->
                            if (index > 0) {
                                append(style.fieldSeparator)
                                appendIndentationIfNeeded(style.indent, offset + 1)
                            }
                            append(entry.key.quoted())
                            append(style.valueSeparator)
                            append(entry.value, style, includeNulls, offset + 2)
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