package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.*
import java.io.*

data class JsonStyle(
    val fieldSeparator: String,
    val valueSeparator: String,
    val indent: Int?,
    val sortedObjectFields: Boolean,
    val includeNulls: Boolean
) {

    val writer = ChunkedStringWriter()
    fun render(node: JsonNode): String = render(node, writer.reset())
    fun render(node: JsonNode, writer: StrAppendable): String =
        writer.appendNode(node, this).toString()

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

        fun StrAppendable.appendNode(node: JsonNode, style: JsonStyle, offset: Int = 0): StrAppendable {
            when (node) {
                is JsonNodeNull -> appendNull()
                is JsonNodeString -> appendText(node.text)
                is JsonNodeBoolean -> appendBoolean(node.boolean)
                is JsonNodeNumber -> appendNumber(node.num)
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

                is JsonNodeObject -> {
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
            return this
        }


        private fun StrAppendable.appendNewlineIfNeeded(indent: Int?, offset: Int): StrAppendable =
            also {
                if (indent != null) {
                    append('\n')
                    repeat(indent * offset) {
                        append(" ")
                    }
                }
            }

        private fun StrAppendable.appendQuoted(string: String) = append('\"')
            .appendEscaped(string)
            .append('\"')

        private fun StrAppendable.appendEscaped(string: String): StrAppendable =
            apply {
                string.onEach { char ->
                    when (char) {
                        '\"' -> append("\\\"")
                        '\\' -> append("\\\\")
                        '\b' -> append("\\b") //backspace
                        '\u000C' -> append("\\f") // Form feed
                        '\n' -> append("\\n")
                        '\r' -> append("\\r")
                        '\t' -> append("\\t")
                        else -> append(char)
                    }
                }
            }

        private fun JsonNodeObject.fields(includeNulls: Boolean, sorted: Boolean) =
            (if (includeNulls) _fieldMap.entries else notNullFields).let {
                if (sorted) it.sortedBy(Map.Entry<String, JsonNode>::key) else it
            }

        private fun JsonNodeArray.values(includeNulls: Boolean) =
            if (includeNulls) elements else notNullValues


        fun <T> StrAppendable.appendArrayValues(
            style: JsonStyle,
            offset: Int,
            values: Iterable<T>,
            appender: StrAppendable.(JsonStyle, Int, T) -> StrAppendable //TODO remove T and make a list of appenders
        ): StrAppendable {
            append('[')
                .appendNewlineIfNeeded(style.indent, offset + 1)
            values.nullFilter(style.includeNulls).forEachIndexed { index, each ->
                if (index > 0) {
                    append(style.fieldSeparator)
                        .appendNewlineIfNeeded(style.indent, offset + 1)
                }
                appender(style, offset + 2, each)
            }
            appendNewlineIfNeeded(style.indent, offset)
                .append(']')
            return this
        }

        fun StrAppendable.appendObjectValue(
            style: JsonStyle,
            offset: Int,
            fields: Map<String, StrAppendable.(JsonStyle, Int) -> Boolean>
        ): StrAppendable {
            var first = true //TODO better way?
            append('{')
            appendNewlineIfNeeded(style.indent, offset + 1)
            fields.entries.sort(style.sortedObjectFields)
                .forEach { entry ->
                    if (!first) {
                        append(style.fieldSeparator)
                        appendNewlineIfNeeded(style.indent, offset + 1)
                    }
                    if (entry.value(this, style, offset + 2) && first)
                        first = false
                }
            appendNewlineIfNeeded(style.indent, offset)
            append('}')

            return this
        }


        fun StrAppendable.appendNull() =
            append("null")


        fun StrAppendable.appendNumber(num: Number) = append(num.toString())


        fun StrAppendable.appendBoolean(bool: Boolean) = append(bool.toString())


        fun StrAppendable.appendText(text: String) = appendQuoted(text)

    }
}

private fun <V> Set<Map.Entry<String, V>>.sort(
    sortedObjectFields: Boolean
): Collection<Map.Entry<String, V>> =
    if (sortedObjectFields) sortedBy { it.key } else this

private fun <V> Collection<Map.Entry<String, V>>.filterNulls(includeNulls: Boolean) =
    if (includeNulls) this else filter { it.value != null }

private fun <T> Iterable<T>.nullFilter(includeNulls: Boolean): Iterable<T> =
    if (includeNulls) this else filterNotNull()


