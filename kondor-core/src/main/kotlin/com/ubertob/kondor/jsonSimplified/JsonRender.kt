package com.ubertob.kondor.jsonSimplified



fun JsonNode.render(): String = //todo: try returning StringBuilder for perf?
    when (this) {
        is JsonNodeNull -> "null"
        is JsonNodeString -> text.putInQuotes()
        is JsonNodeBoolean -> value.toString()
        is JsonNodeNumber -> num.toString()
        is JsonNodeArray -> notNullValues.map { it.render() }.joinToString(prefix = "[", postfix = "]")
        is JsonNodeObject -> notNullFields.map { it.key.putInQuotes() + ": " + it.value.render() }
            .joinToString(prefix = "{", postfix = "}")
    }


fun JsonNode.pretty(explicitNull: Boolean, indent: Int, offset: Int = 0): String =
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
    if (explicitNull) fieldMap.entries else notNullFields

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