package com.ubertob.kondor.json

import com.ubertob.kondor.outcome.Outcome
import com.ubertob.kondor.outcome.Outcome.Companion.tryThis
import com.ubertob.kondor.outcome.asFailure
import com.ubertob.kondor.outcome.onFailure
import java.math.BigDecimal

inline fun <T> tryParse(expected: String, position: Int, path: NodePath, f: () -> T): Outcome<JsonError, T> =
    tryThis(f).transformFailure {
        parsingError(expected, it.msg, position, path)
    }


data class TokensStream(private val tracer: () -> Int, private val iterator: PeekingIterator<String>) :
    PeekingIterator<String> by iterator {
    fun position(): Int = tracer()
}

fun parsingError(expected: String, actual: String, position: Int, path: NodePath) = JsonError(
    path, "at position $position: expected $expected but found '$actual'"
)

fun parsingFailure(expected: String, actual: String, position: Int, path: NodePath) =
    parsingError(expected, actual, position, path).asFailure()


fun parseJsonNodeBoolean(
    tokens: TokensStream,
    path: NodePath
): Outcome<JsonError, JsonNodeBoolean> =
    tryParse("Boolean", tokens.position(), path) {
        tokens.next().let {
            when (it) {
                "true" -> true
                "false" -> false
                else -> return parsingFailure("a Boolean", it, tokens.position(), path)
            }.let { JsonNodeBoolean(it, path) }
        }
    }

fun parseJsonNodeNum(
    tokens: TokensStream,
    path: NodePath
): Outcome<JsonError, JsonNodeNumber> =
    tryParse("a Number", tokens.position(), path) {
        val t = tokens.next()
        JsonNodeNumber(BigDecimal(t), path)
    }


fun parseJsonNodeNull(
    tokens: TokensStream,
    path: NodePath
): Outcome<JsonError, JsonNodeNull> =
    tryParse("Null", tokens.position(), path) {
        tokens.next()
            .let {
                if (it == "null") JsonNodeNull(path) else
                    return parsingFailure("null", it, tokens.position(), path)
            }
    }

fun parseJsonNodeString(
    tokens: TokensStream,
    path: NodePath
): Outcome<JsonError, JsonNodeString> =
    tryParse("a String", tokens.position(), path) {
        val openQuote = tokens.next()
        val text = tokens.next()
        val endQuote = tokens.next()
        if (openQuote != "\"") return parsingFailure("'\"'", openQuote, tokens.position(), path)
        if (endQuote != "\"") return parsingFailure("'\"'", endQuote, tokens.position(), path)
        JsonNodeString(text, path)
    }


fun parseJsonNodeArray(
    tokens: TokensStream,
    path: NodePath
): JsonOutcome<JsonNodeArray> =
    tryParse("an Array", tokens.position(), path) {
        val openBraket = tokens.next()
        if (openBraket != "[") return parsingFailure("'['", openBraket, tokens.position(), path)
        else {
            var currToken = tokens.peek()
            if (currToken == "]")
                tokens.next()//consume it
            val nodes = mutableListOf<JsonNode>()
            var currNode = 0
            while (currToken != "]") {
                nodes.add(
                    parseNewNode(tokens, Node("${currNode++}", path)).onFailure { return it.asFailure() })
                currToken = tokens.peek()
                if (currToken != "," && currToken != "]") return parsingFailure(
                    "',' or ':'",
                    currToken,
                    tokens.position(),
                    path
                )
                tokens.next()
            }
            JsonNodeArray(nodes, path)
        }
    }

fun parseJsonNodeObject(
    tokens: TokensStream,
    path: NodePath   //todo add more context to NodePath? like the field type, expected values...
): Outcome<JsonError, JsonNodeObject> =
    tryParse("an Object", tokens.position(), path) {
        val openCurly = tokens.next()
        if (openCurly != "{") return parsingFailure("'{'", openCurly, tokens.position(), path)
        else {
            var currToken = tokens.peek()
            if (currToken == "}")
                tokens.next()//consume it

            val fields = mutableMapOf<String, JsonNode>()
            while (currToken != "}") {
                val fieldName = parseJsonNodeString(tokens, path).onFailure { return it.asFailure() }.text

                val colon = tokens.next()
                if (colon != ":") return parsingFailure("':'", colon, tokens.position(), path)
                val value = parseNewNode(tokens, Node(fieldName, path)).onFailure { return it.asFailure() }
                fields.put(fieldName, value)

                currToken = tokens.peek()
                if (currToken != "," && currToken != "}") return parsingFailure("'}' or ','", currToken, tokens.position(), path)
                tokens.next()
            }
            JsonNodeObject(fields, path)
        }
    }

fun parseNewNode(tokens: TokensStream, path: NodePath): JsonOutcome<JsonNode> =
    when (val first = tokens.peek()) {
        "null" -> parseJsonNodeNull(tokens, path)
        "false", "true" -> parseJsonNodeBoolean(tokens, path)
        "\"" -> parseJsonNodeString(tokens, path)
        "[" -> parseJsonNodeArray(tokens, path)
        "{" -> parseJsonNodeObject(tokens, path)
        else ->
            when (first.get(0)) { //regex -?(?:0|[1-9]\d*)(?:\.\d+)?(?:[eE][+-]?\d+)?
                in '0'..'9', '-' -> parseJsonNodeNum(tokens, path)
                else -> parsingFailure("a valid json value", first, tokens.position(), path)
            }
    }


fun JsonNode.render(): String = //todo: try returning StringBuilder for perf?
    when (this) {
        is JsonNodeNull -> "null"
        is JsonNodeString -> text.putInQuotes()
        is JsonNodeBoolean -> value.toString()
        is JsonNodeNumber -> num.toString()
        is JsonNodeArray -> values.map { it.render() }.joinToString(prefix = "[", postfix = "]")
        is JsonNodeObject -> fieldMap.entries.map { it.key.putInQuotes() + ": " + it.value.render() }
            .joinToString(prefix = "{", postfix = "}")
    }

private fun String.putInQuotes(): String = replace("\"", "\\\"").let { "\"${it}\"" }
