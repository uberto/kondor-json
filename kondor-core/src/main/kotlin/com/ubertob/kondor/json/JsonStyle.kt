package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.*
import java.io.*

data class JsonStyle(
    val appendFieldSeparator: (StrAppendable) -> StrAppendable,
    val appendValueSeparator: (StrAppendable) -> StrAppendable,
    val appendNewline: (StrAppendable, Int) -> StrAppendable,
    val sortedObjectFields: Boolean,
    val explicitNulls: Boolean
) {

    val writer = ChunkedStringWriter()
    fun render(node: JsonNode): String = render(node, writer.reset())
    fun render(node: JsonNode, writer: StrAppendable): String =
        writer.appendNode(node, this).toString()


    companion object {
        val singleLine = JsonStyle(
            appendFieldSeparator = ::commaSpace,
            appendValueSeparator = ::colonSpace,
            appendNewline = JsonStyle::noNewLine,
            sortedObjectFields = false,
            explicitNulls = false
        )

        val compact = JsonStyle(
            appendFieldSeparator = ::comma,
            appendValueSeparator = ::colon,
            appendNewline = JsonStyle::noNewLine,
            sortedObjectFields = false,
            explicitNulls = false
        )

        val compactWithNulls = JsonStyle(
            appendFieldSeparator = ::comma,
            appendValueSeparator = ::colon,
            appendNewline = JsonStyle::noNewLine,
            sortedObjectFields = false,
            explicitNulls = true
        )


        val pretty = JsonStyle(
            appendFieldSeparator = ::comma,
            appendValueSeparator = ::colonSpace,
            appendNewline = JsonStyle::appendNewLineOffset,
            sortedObjectFields = true,
            explicitNulls = false
        )

        val prettyWithNulls: JsonStyle = JsonStyle(
            appendFieldSeparator = ::comma,
            appendValueSeparator = ::colonSpace,
            appendNewline = JsonStyle::appendNewLineOffset,
            sortedObjectFields = true,
            explicitNulls = true
        )


        fun comma(app: StrAppendable): StrAppendable =
            app.append(',')

        fun colon(app: StrAppendable): StrAppendable =
            app.append(':')

        fun commaSpace(app: StrAppendable): StrAppendable =
            app.append(',').append(' ')

        fun colonSpace(app: StrAppendable): StrAppendable =
            app.append(':').append(' ')

        fun appendNewLineOffset(app: StrAppendable, offset: Int): StrAppendable =
            app.apply {
                app.append('\n')
                repeat(offset) {
                    app.append(' ').append(' ')
                }
            }

        fun noNewLine(app: StrAppendable, offset: Int): StrAppendable = app


        fun StrAppendable.appendNode(node: JsonNode, style: JsonStyle, offset: Int = 0): StrAppendable {
            when (node) {
                is JsonNodeNull -> appendNull()
                is JsonNodeString -> appendText(node.text)
                is JsonNodeBoolean -> appendBoolean(node.boolean)
                is JsonNodeNumber -> appendNumber(node.num)
                is JsonNodeArray -> {
                    append('[')
                    style.appendNewline(this, offset + 1)
                    node.values(style.explicitNulls).forEachIndexed { index, each ->
                        if (index > 0) {
                            style.appendFieldSeparator(this)
                            style.appendNewline(this, offset + 1)
                        }
                        appendNode(each, style, offset + 2)
                    }
                    style.appendNewline(this, offset)
                    append(']')
                }

                is JsonNodeObject -> {
                    append('{')
                    style.appendNewline(this, offset + 1)
                    node.fields(style.explicitNulls, style.sortedObjectFields)
                        .forEachIndexed { index, entry ->
                            if (index > 0) {
                                style.appendFieldSeparator(this)
                                style.appendNewline(this, offset + 1)
                            }
                            appendQuoted(entry.key)
                            style.appendValueSeparator(this)
                            appendNode(entry.value, style, offset + 2)
                        }
                    style.appendNewline(this, offset)
                    append('}')
                }
            }
            return this
        }


        private fun StrAppendable.appendQuoted(string: String) = append('\"')
            .appendEscaped(string)
            .append('"')

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
            appender: StrAppendable.(JsonStyle, Int, T) -> StrAppendable
        ): StrAppendable {
            append('[')

            style.appendNewline(this, offset + 1)
            values.nullFilter(style.explicitNulls).forEachIndexed { index, each ->
                if (index > 0) {
                    style.appendFieldSeparator(this)
                    style.appendNewline(this, offset + 1)
                }
                appender(style, offset + 2, each)
            }
            style.appendNewline(this, offset)
                .append(']')
            return this
        }

        fun StrAppendable.appendObjectValue(
            style: JsonStyle,
            offset: Int,
            fields: List<NamedAppender>
        ): StrAppendable =
            apply {
                append('{')
                style.appendNewline(this, offset + 1)
                    .appendObjectFields(style, offset + 1, fields)
                style.appendNewline(this, offset)
                    .append('}')
            }

        fun StrAppendable.appendObjectFields(
            style: JsonStyle,
            offset: Int,
            fields: List<NamedAppender>
        ): StrAppendable = apply {
            fields
                .filterNulls(style.explicitNulls)
                .sort(style.sortedObjectFields)
                .forEachIndexed { i, (fieldName, appender) ->
                    if (i > 0) {
                        style.appendFieldSeparator(this)
                        style.appendNewline(this, offset)
                    }
                    if (appender != null)
                        appender(this, style, offset + 1)
                    else
                        appendNullField(fieldName, style)
                }
        }

        fun StrAppendable.appendNullField(fieldName: String, style: JsonStyle): StrAppendable =
            appendText(fieldName).apply {
                style.appendValueSeparator(this)
                    .appendNull()
            }

        fun StrAppendable.appendNull() =
            append("null")


        fun StrAppendable.appendNumber(num: Number) = append(num.toString())


        fun StrAppendable.appendBoolean(bool: Boolean) = append(bool.toString())


        fun StrAppendable.appendText(text: String) = appendQuoted(text)

    }
}

private fun <V> List<Pair<String, V>>.sort(
    sortedObjectFields: Boolean
): List<Pair<String, V>> =
    if (sortedObjectFields) sortedBy { it.first } else this

private fun <V> List<Pair<String, V>>.filterNulls(includeNulls: Boolean) =
    if (includeNulls) this else filter { it.second != null }

private fun <T> Iterable<T>.nullFilter(includeNulls: Boolean): Iterable<T> =
    if (includeNulls) this else filterNotNull()


