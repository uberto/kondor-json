package com.ubertob.kondor.json

import com.ubertob.kondor.outcome.Outcome
import com.ubertob.kondor.outcome.Outcome.Companion.tryOrFail
import com.ubertob.kondor.outcome.asFailure
import com.ubertob.kondor.outcome.onFailure
import java.math.BigDecimal

inline fun <T> tryParse(
    expected: String,
    actual: KondorToken,
    position: Int,
    path: NodePath,
    f: () -> T
): Outcome<JsonError, T> =
    tryOrFail(f)
        .transformFailure {
            when (it.throwable) {
                is NumberFormatException ->
                    parsingError(expected, "$actual", position, path, it.msg)
                else ->
                    parsingError(expected, "${it.msg} after $actual", position, path, "Invalid Json")
            }
        }

sealed class KondorToken
object OpeningQuotes : KondorToken() {
    override fun toString(): String = "opening quotes"
}

object ClosingQuotes : KondorToken() {
    override fun toString(): String = "closing quotes"
}

object OpeningBracket : KondorToken() {
    override fun toString(): String = "["
}

object ClosingBracket : KondorToken() {
    override fun toString(): String = "]"
}

object OpeningCurly : KondorToken() {
    override fun toString(): String = "{"
}

object ClosingCurly : KondorToken() {
    override fun toString(): String = "}"
}

object Colon : KondorToken() {
    override fun toString(): String = ":"
}

object Comma : KondorToken() {
    override fun toString(): String = ","
}

data class Value(val text: String) : KondorToken() {
    override fun toString(): String = text
}

data class TokensStream(private val tracer: () -> Int, private val iterator: PeekingIterator<KondorToken>) :
    PeekingIterator<KondorToken> by iterator {
    fun position(): Int = tracer()
}

fun parsingError(expected: String, actual: String, position: Int, path: NodePath, details: String) = JsonError(
    path, "at position $position: expected $expected but found '$actual' - $details"
)

fun parsingFailure(expected: String, actual: String, position: Int, path: NodePath, details: String) =
    parsingError(expected, actual, position, path, details).asFailure()

fun parsingFailure(expected: String, actual: KondorToken, position: Int, path: NodePath, details: String) =
    parsingError(expected, actual.toString(), position, path, details).asFailure()

fun parseJsonNodeBoolean(
    tokens: TokensStream,
    path: NodePath
): Outcome<JsonError, JsonNodeBoolean> =
    tryParse("Boolean", tokens.peek(), tokens.position(), path) {
        tokens.next().let {
            when (it) {
                Value("true") -> true
                Value("false") -> false
                else -> return parsingFailure("a Boolean", it, tokens.position(), path, "valid values: false, true")
            }.let { JsonNodeBoolean(it, path) }
        }
    }

fun parseJsonNodeNum(
    tokens: TokensStream,
    path: NodePath
): Outcome<JsonError, JsonNodeNumber> =
    tryParse("a Number", tokens.peek(), tokens.position(), path) {
        tokens.next().let {
            when (it) {
                is Value -> JsonNodeNumber(BigDecimal(it.text), path)
                else -> return parsingFailure("a Number", it, tokens.position(), path, "not a valid number")
            }
        }
    }


fun parseJsonNodeNull(
    tokens: TokensStream,
    path: NodePath
): Outcome<JsonError, JsonNodeNull> =
    tryParse("Null", tokens.peek(), tokens.position(), path) {
        tokens.next().let {
            when (it) {
                Value("null") -> JsonNodeNull(path)
                else -> return parsingFailure("null", it, tokens.position(), path, "valid values: null")
            }
        }
    }

fun parseJsonNodeString(
    tokens: TokensStream,
    path: NodePath
): Outcome<JsonError, JsonNodeString> =
    tryParse("a String", tokens.peek(), tokens.position(), path) {
        val openQuote = tokens.next()
        if (openQuote != OpeningQuotes) return parsingFailure(
            "'\"'", openQuote, tokens.position(), path,
            "missing opening double quotes"
        )
        val text = if (tokens.peek() == ClosingQuotes) {
            ""
        } else {
            tokens.next().let {
                when (it) {
                    is Value -> it.text
                    else -> return parsingFailure("null", it, tokens.position(), path, "valid values: null")
                }
            }
        }
        val endQuote = tokens.next()
        if (endQuote != ClosingQuotes) return parsingFailure(
            "'\"'", endQuote, tokens.position(), path,
            "missing closing double quotes"
        )
        JsonNodeString(text, path)
    }

fun parseJsonNodeArray(
    tokens: TokensStream,
    path: NodePath
): JsonOutcome<JsonNodeArray> =
    tryParse("an Array", tokens.peek(), tokens.position(), path) {
        val openBraket = tokens.next()
        if (openBraket != OpeningBracket)
            return parsingFailure("'['", openBraket, tokens.position(), path, "missing opening bracket")
        else {
            var currToken = tokens.peek()
            if (currToken == ClosingBracket)
                tokens.next()//consume it
            val nodes = mutableListOf<JsonNode>()
            var currNode = 0
            while (currToken != ClosingBracket) { //todo: use fold/recursion here
                nodes.add(
                    parseNewNode(tokens, NodePathSegment("[${currNode++}]", path))
                        .onFailure { return it.asFailure() })
                currToken = tokens.peek()
                if (currToken != Comma && currToken != ClosingBracket)
                    return parsingFailure("',' or ':'", currToken, tokens.position(), path, "missing closing bracket")
                tokens.next()
            }
            JsonNodeArray(nodes, path)
        }
    }

fun parseJsonNodeObject(
    tokens: TokensStream,
    path: NodePath   //todo add more context to NodePath? like the field type, expected values...
): Outcome<JsonError, JsonNodeObject> =
    tryParse("an Object", tokens.peek(), tokens.position(), path) {
        val openCurly = tokens.next()
        if (openCurly != OpeningCurly) return parsingFailure(
            "'{'",
            openCurly,
            tokens.position(),
            path,
            "missing opening curly"
        )
        else {
            var currToken = tokens.peek()
            if (currToken == ClosingCurly)
                tokens.next()//consume it

            val keys = mutableMapOf<String, JsonNode>()
            while (currToken != ClosingCurly) { //todo: use fold/recursion here
                val keyName = parseJsonNodeString(tokens, path).onFailure { return it.asFailure() }.text
                if (keyName in keys)
                    return parsingFailure("a unique key", keyName, tokens.position(), path, "duplicated key")

                val colon = tokens.next()
                if (colon != Colon)
                    return parsingFailure(
                        "':'",
                        colon,
                        tokens.position(),
                        path,
                        "missing colon between key and value in object"
                    )
                val value = parseNewNode(tokens, NodePathSegment(keyName, path)).onFailure { return it.asFailure() }
                keys.put(keyName, value)

                currToken = tokens.peek()
                if (currToken != Comma && currToken != ClosingCurly)
                    return parsingFailure("'}' or ','", currToken, tokens.position(), path, "missing closing curly")
                tokens.next()
            }
            JsonNodeObject(keys, path)
        }
    }

fun parseNewNode(tokens: TokensStream, path: NodePath): JsonOutcome<JsonNode> =
    when (val first = tokens.peek()) {
        Value("null") -> parseJsonNodeNull(tokens, path)
        Value("false"), Value("true") -> parseJsonNodeBoolean(tokens, path)
        OpeningQuotes -> parseJsonNodeString(tokens, path)
        OpeningBracket -> parseJsonNodeArray(tokens, path)
        OpeningCurly -> parseJsonNodeObject(tokens, path)
        is Value ->
            when (first.text.get(0)) { //regex -?(?:0|[1-9]\d*)(?:\.\d+)?(?:[eE][+-]?\d+)?
                in '0'..'9', '-' -> parseJsonNodeNum(tokens, path)
                else -> parsingFailure("a valid json value", first, tokens.position(), path, "invalid json")
            }
        else -> parsingFailure("a valid json value", first, tokens.position(), path, "invalid json")
    }


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
    //todo: try returning StringBuilder for perf?
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
