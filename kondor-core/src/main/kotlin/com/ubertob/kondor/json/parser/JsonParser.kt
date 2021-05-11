package com.ubertob.kondor.json.parser

import com.ubertob.kondor.json.JsonError
import com.ubertob.kondor.json.JsonOutcome
import com.ubertob.kondor.json.jsonnode.*
import com.ubertob.kondor.outcome.Outcome
import com.ubertob.kondor.outcome.Outcome.Companion.tryOrFail
import com.ubertob.kondor.outcome.asFailure
import com.ubertob.kondor.outcome.asSuccess
import com.ubertob.kondor.outcome.onFailure
import java.math.BigDecimal

private inline fun <T> tryParse(
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

fun <T> tryParseBind(
    expected: String,
    actual: KondorToken,
    position: Int,
    path: NodePath,
    f: () -> Outcome<JsonError, T>
): Outcome<JsonError, T> =
    try {
        f()
    } catch (t: NumberFormatException) {
        parsingError(expected, "$actual", position, path, t.message.orEmpty()).asFailure()
    } catch (t: Throwable) {
        parsingError(expected, "${t.message.orEmpty()} after $actual", position, path, "Invalid Json").asFailure()
    }


fun parsingError(expected: String, actual: String, position: Int, path: NodePath, details: String) = JsonError(
    path, "at position $position: expected $expected but found '$actual' - $details"
)

fun parsingFailure(expected: String, actual: String, position: Int, path: NodePath, details: String) =
    parsingError(expected, actual, position, path, details).asFailure()

fun parsingFailure(expected: String, actual: KondorToken, position: Int, path: NodePath, details: String) =
    parsingError(expected, actual.toString(), position, path, details).asFailure()


//todo delete these and just use inner boolean in NodeKind.
fun parseJsonNodeBoolean(
    tokens: TokensStream,
    path: NodePath
): JsonOutcome<JsonNodeBoolean> =
    tryParseBind("a Boolean", tokens.peek(), tokens.position(), path) {
        boolean(tokens, path)
    }

fun parseJsonNodeNum(
    tokens: TokensStream,
    path: NodePath
): Outcome<JsonError, JsonNodeNumber> =
    tryParseBind("a Number", tokens.peek(), tokens.position(), path) {
        number(tokens, path)
    }

fun parseJsonNodeNull(
    tokens: TokensStream,
    path: NodePath
): Outcome<JsonError, JsonNodeNull> =
    tryParseBind("a Null", tokens.peek(), tokens.position(), path) {
        explicitNull(tokens, path)
    }


private fun boolean(
    tokens: TokensStream,
    path: NodePath
): JsonOutcome<JsonNodeBoolean> =
    when (val token = tokens.next()) {
        Value("true") -> true.asSuccess()
        Value("false") -> false.asSuccess()
        else -> parsingFailure("a Boolean", token, tokens.position(), path, "valid values: false, true")
    }.transform { JsonNodeBoolean(it, path) }


private fun number(
    tokens: TokensStream,
    path: NodePath
): JsonOutcome<JsonNodeNumber> =
    when (val token = tokens.next()) {
        is Value -> BigDecimal(token.text).asSuccess()
        else -> parsingFailure("a Number", token, tokens.position(), path, "not a valid number")
    }.transform { JsonNodeNumber(it, path) }


private fun explicitNull(
    tokens: TokensStream,
    path: NodePath
): JsonOutcome<JsonNodeNull> =
    when (val token = tokens.next()) {
        Value("null") -> Unit.asSuccess()
        else -> parsingFailure("null", token, tokens.position(), path, "valid values: null")
    }.transform { JsonNodeNull(path) }

//---


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
            var currToken = tokens.peek() //add a tokens foldOutcome
            if (currToken == ClosingBracket)
                tokens.next()//consume it for the case of []
            val nodes = mutableListOf<JsonNode>()
            var currNode = 0
            while (currToken != ClosingBracket) {
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
    path: NodePath
): Outcome<JsonError, JsonNodeObject> =  //TODO remove non local returns...
    tryParse("an Object", tokens.peek(), tokens.position(), path) {
        val openCurly = tokens.next()
        if (openCurly != OpeningCurly)
            return parsingFailure("'{'", openCurly, tokens.position(), path, "missing opening curly")
        else {
            var currToken = tokens.peek()
            if (currToken == ClosingCurly)
                tokens.next()//consume it

            val keys = mutableMapOf<String, JsonNode>()
            while (currToken != ClosingCurly) { //add a tokens foldOutcome
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


