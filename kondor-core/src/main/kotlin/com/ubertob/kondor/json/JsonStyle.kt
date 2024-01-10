package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.*
import java.io.*

data class JsonStyle(
    val appendFieldSeparator: (CharWriter) -> CharWriter,
    val appendValueSeparator: (CharWriter) -> CharWriter,
    val appendNewline: (CharWriter, Int) -> CharWriter,
    val sortedObjectFields: Boolean,
    val explicitNulls: Boolean
) {

    fun render(node: JsonNode): String = render(node, ChunkedStringWriter())
    fun render(node: JsonNode, writer: CharWriter): String =
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


        fun comma(app: CharWriter): CharWriter =
            app.write(',')

        fun colon(app: CharWriter): CharWriter =
            app.write(':')

        fun commaSpace(app: CharWriter): CharWriter =
            app.write(',').write(' ')

        fun colonSpace(app: CharWriter): CharWriter =
            app.write(':').write(' ')

        fun appendNewLineOffset(app: CharWriter, offset: Int): CharWriter =
            app.apply {
                app.write('\n')
                repeat(offset) {
                    app.write(' ').write(' ')
                }
            }

        @Suppress("UNUSED_PARAMETER")
        fun noNewLine(app: CharWriter, offset: Int): CharWriter = app

        fun CharWriter.appendNode(node: JsonNode, style: JsonStyle, offset: Int = 0): CharWriter {
            when (node) {
                is JsonNodeNull -> appendNull()
                is JsonNodeString -> appendText(node.text)
                is JsonNodeBoolean -> appendBoolean(node.boolean)
                is JsonNodeNumber -> appendNumber(node.num)
                is JsonNodeArray -> {
                    write('[')
                    style.appendNewline(this, offset + 1)
                    node.values(style.explicitNulls)
                        .forEachIndexed { index, each ->
                            if (index > 0) {
                                style.appendFieldSeparator(this)
                                style.appendNewline(this, offset + 1)
                            }
                            appendNode(each, style, offset + 2)
                        }
                    style.appendNewline(this, offset)
                    write(']')
                }

                is JsonNodeObject -> {
                    write('{')
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
                    write('}')
                }
            }
            return this
        }


        private fun CharWriter.appendQuoted(string: String) = write('\"')
            .appendEscaped(string)
            .write('"')

        private fun CharWriter.appendEscaped(string: String): CharWriter =
            apply {
                string.onEach { char ->
                    when (char) {
                        '\"' -> write("\\\"")
                        '\\' -> write("\\\\")
                        '\b' -> write("\\b") //backspace
                        '\u000C' -> write("\\f") // Form feed
                        '\n' -> write("\\n")
                        '\r' -> write("\\r")
                        '\t' -> write("\\t")
                        else -> write(char)
                    }
                }
            }

        private fun JsonNodeObject.fields(includeNulls: Boolean, sorted: Boolean) =
            (if (includeNulls) _fieldMap.entries else notNullFields).let {
                if (sorted) it.sortedBy(Map.Entry<String, JsonNode>::key) else it
            }

        private fun JsonNodeArray.values(includeNulls: Boolean) =
            if (includeNulls) elements else notNullValues


        fun <T> CharWriter.appendArrayValues(
            style: JsonStyle,
            offset: Int,
            values: Iterable<T>,
            appender: CharWriter.(JsonStyle, Int, T) -> CharWriter
        ): CharWriter {
            write('[')

            style.appendNewline(this, offset + 1)
            values.nullFilter(style.explicitNulls).forEachIndexed { index, each ->
                if (index > 0) {
                    style.appendFieldSeparator(this)
                    style.appendNewline(this, offset + 1)
                }
                appender(style, offset + 2, each)
            }
            style.appendNewline(this, offset)
                .write(']')
            return this
        }

        fun CharWriter.appendObjectValue(
            style: JsonStyle,
            offset: Int,
            fields: List<NamedAppender>
        ): CharWriter =
            apply {
                write('{')
                style.appendNewline(this, offset + 1)
                    .appendObjectFields(style, offset + 1, fields)
                style.appendNewline(this, offset)
                    .write('}')
            }

        fun CharWriter.appendObjectFields(
            style: JsonStyle,
            offset: Int,
            fields: List<NamedAppender>
        ): CharWriter = apply {
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

        fun CharWriter.appendNullField(fieldName: String, style: JsonStyle): CharWriter =
            appendText(fieldName).apply {
                style.appendValueSeparator(this)
                    .appendNull()
            }

        fun CharWriter.appendNull() =
            write("null")


        fun CharWriter.appendNumber(num: Number) = write(num.toString())


        fun CharWriter.appendBoolean(bool: Boolean) = write(bool.toString())


        fun CharWriter.appendText(text: String) = appendQuoted(text)

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


