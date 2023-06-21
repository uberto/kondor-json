package com.ubertob.kondor.json.parser

import com.ubertob.kondor.json.jsonnode.*


fun JsonNode.render(): String =
    when (this) {
        is JsonNodeNull -> "null"
        is JsonNodeString -> text.putInQuotes()
        is JsonNodeBoolean -> value.toString()
        is JsonNodeNumber -> num.toString()
        is JsonNodeArray -> notNullValues.map { it.render() }.joinToString(prefix = "[", postfix = "]")
        is JsonNodeObject -> notNullFields.map { it.key.putInQuotes() + ": " + it.value.render() }
            .joinToString(prefix = "{", postfix = "}")
    }

fun JsonNode.compact(stringBuilder: StringBuilder, explicitNull: Boolean = false): StringBuilder =
    stringBuilder.also { sb ->
        when (this) {
            is JsonNodeNull -> sb.append("null")
            is JsonNodeString -> sb.append(text.putInQuotes())
            is JsonNodeBoolean -> sb.append(value.toString())
            is JsonNodeNumber -> sb.append(num.toString())
            is JsonNodeArray -> valuesFiltered(explicitNull)
                .appendAndJoin(builder = sb, prefix = "[", postfix = "]", separator = ",") {
                    it.compact(this, explicitNull)
                }

            is JsonNodeObject -> fieldsFiltered(explicitNull)
                .appendAndJoin(builder = sb, prefix = "{", postfix = "}", separator = ",") {
                    append(it.key.putInQuotes())
                    append(':')
                    it.value.compact(this, explicitNull)
                }
        }
    }


fun JsonNode.pretty(explicitNull: Boolean = false, indent: Int = 2, offset: Int = 0): String =
    when (this) {
        is JsonNodeNull -> render()
        is JsonNodeString -> render()
        is JsonNodeBoolean -> render()
        is JsonNodeNumber -> render()
        is JsonNodeArray -> valuesFiltered(explicitNull).map {
            it.pretty(
                explicitNull,
                indent,
                offset + indent + indent
            )
        }
            .joinToString(
                prefix = "[${br(offset + indent)}",
                postfix = "${br(offset)}]",
                separator = ",${br(offset + indent)}"
            )

        is JsonNodeObject -> fieldsFiltered(explicitNull).map {
            it.key.putInQuotes() + ": " + it.value.pretty(
                explicitNull, indent,
                offset + indent + indent
            )
        }
            .sorted()
            .joinToString(
                prefix = "{${br(offset + indent)}",
                postfix = "${br(offset)}}",
                separator = ",${br(offset + indent)}"
            )
    }

private fun JsonNodeObject.fieldsFiltered(explicitNull: Boolean) =
    if (explicitNull) _fieldMap.entries else notNullFields

private fun JsonNodeArray.valuesFiltered(explicitNull: Boolean) =
    if (explicitNull) values else notNullValues

private fun br(offset: Int): String = "\n" + " ".repeat(offset)


val regex = """[\\"\n\r\t]""".toRegex()
private fun String.putInQuotes(): String =
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


fun <T> Iterable<T>.appendAndJoin(
    builder: StringBuilder,
    separator: CharSequence,
    prefix: CharSequence,
    postfix: CharSequence,
    onEach: StringBuilder.(T) -> Unit
): StringBuilder {
    builder.append(prefix)
    var count = 0
    for (element in this) {
        if (++count > 1) builder.append(separator)
        onEach(builder, element)
    }
    builder.append(postfix)
    return builder
}