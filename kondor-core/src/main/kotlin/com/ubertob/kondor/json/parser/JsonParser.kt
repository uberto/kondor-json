package com.ubertob.kondor.json.parser

import com.ubertob.kondor.json.InvalidJsonError
import com.ubertob.kondor.json.JsonError
import com.ubertob.kondor.json.JsonOutcome
import com.ubertob.kondor.json.jsonnode.*
import com.ubertob.kondor.json.parser.KondorSeparator.*
import com.ubertob.kondor.outcome.*
import java.math.BigDecimal


data class TokensPath(val tokens: TokensStream, val path: NodePath)

fun TokensStream.lastToken(): KondorToken = this.last() ?: ValueTokenEager("Nothing", 0)


private fun parsingError(expected: String, actual: String, position: Int, path: NodePath, details: String) =
    InvalidJsonError(
        path, "at position $position: expected $expected but found $actual - $details"
    )

fun parsingError(
    expected: String, actual: KondorToken, lastPosRead: Int, path: NodePath, details: String
): JsonError = parsingError(expected, actual.desc, tokenPos(actual, lastPosRead), path, details)

private fun tokenPos(token: KondorToken, lastPosRead: Int) = if (token.pos > 0) token.pos else lastPosRead

fun parsingFailure(expected: String, actual: String, position: Int, path: NodePath, details: String) =
    parsingError(expected, actual, position, path, details).asFailure()

fun parsingFailure(expected: String, actual: KondorToken, lastPosRead: Int, path: NodePath, details: String) =
    parsingError(expected, actual, lastPosRead, path, details).asFailure()


fun TokensPath.parseJsonNodeBoolean(): JsonOutcome<JsonNodeBoolean> = TokensPath::boolean.invoke(this)

fun TokensPath.parseJsonNodeNum(): Outcome<JsonError, JsonNodeNumber> = TokensPath::number.invoke(this)

fun TokensPath.parseJsonNodeNull(): Outcome<JsonError, JsonNodeNull> = TokensPath::explicitNull.invoke(this)


fun TokensPath.parseJsonNodeString(): JsonOutcome<JsonNodeString> =
    parseString(tokens, path).transform { JsonNodeString(it) }


fun TokensPath.parseJsonNodeArray(): JsonOutcome<JsonNodeArray> =
    surroundedForNodes(
        OpeningSquare, TokensPath::array, ClosingSquare
    )() //!!! switch to parseArray


fun TokensPath.parseJsonNodeObject(): JsonOutcome<JsonNodeObject> =
    surroundedForNodes(
        OpeningCurly, TokensPath::jsonObject, ClosingCurly
    )()  //!!! switch to parseObject


typealias JsonParser<T> = TokensPath.() -> JsonOutcome<T>
typealias JsonParserFromTokens<T> = (TokensStream, NodePath) -> JsonOutcome<T>

fun <T> surroundedForNodes(
    openingToken: KondorSeparator, takeContent: JsonParser<T>, closingToken: KondorSeparator
): JsonParser<T> = {
    val middle = { _: KondorToken, middle: T, _: KondorToken -> middle }

    middle `!` take(openingToken, tokens, path) `*` takeContent() `*` take(closingToken, tokens, path)
}

fun <T> surrounded(
    openingToken: KondorSeparator, takeContent: JsonParserFromTokens<T>, closingToken: KondorSeparator
): JsonParserFromTokens<T> = { tokens, path ->  //!!! add test for null object {}
    take(openingToken, tokens, path)
        .bind { takeContent(tokens, path) }
        .bindAndIgnore {
            take(closingToken, tokens, path)
        }
}

fun <T> parseValues(
    tokens: TokensStream, path: NodePath, parseFun: (TokensStream, NodePath) -> JsonOutcome<T>?
): JsonOutcome<List<T>> {
    var arrayIndex = 0
    val values = ArrayList<T>(128)

    while (true) {
        parseFun(tokens, newSegment(path, arrayIndex++))?.let { outcome ->
            val value = outcome.onFailure { return it.asFailure() }
            values.add(value)
        } ?: break
    }
    return values.asSuccess()
}

private fun newSegment(path: NodePath, nodeNumber: Int): NodePath = NodePathSegment("[$nodeNumber]", path)

fun TokensPath.boolean(): JsonOutcome<JsonNodeBoolean> =
    parseBoolean(tokens, path)
        .transform { JsonNodeBoolean(it) }


fun bigDecimalParser(value: String): JsonOutcome<Number> =
    try {
        BigDecimal(value).asSuccess()
    } catch (e: NumberFormatException) {
        value.toDouble().asSuccess() //for NaN, Infinity etc that are valid Double but not BigDecimals
    }

fun TokensPath.number(): JsonOutcome<JsonNodeNumber> =
    parseNumber(tokens, path, ::bigDecimalParser).transform { JsonNodeNumber(it) }


fun TokensPath.array(): JsonOutcome<JsonNodeArray> = commaSeparated { parseNewNode() }.transform { JsonNodeArray(it) }

fun TokensPath.jsonObject(): JsonOutcome<JsonNodeObject> = commaSeparated(withParentNode {
    keyValue {
        parseNewNode() ?: parsingFailure("a valid node", "nothing", tokens.last()?.pos ?: 0, path, "invalid Json")
    }
}).transform(::checkForDuplicateKeys).transform { JsonNodeObject(it.toMap()) }

private fun checkForDuplicateKeys(pairs: List<Pair<String, JsonNode>>): List<Pair<String, JsonNode>> =
    pairs.sortedBy { it.first }


fun <T> withParentNode(f: TokensPath.() -> JsonOutcome<T>?): TokensPath.() -> JsonOutcome<T>? =
    { f(copy(path = path.parent())) }


fun <T> TokensPath.keyValue(contentParser: TokensPath.() -> JsonOutcome<T>): JsonOutcome<Pair<String, T>>? =
    parseOptionalKeyNode()?.bind { key ->
        take(Colon, tokens, path)
            .bind { contentParser(copy(path = NodePathSegment(key, path))) }
            .transform { value -> key to value }
    }

private fun TokensPath.parseOptionalKeyNode(): JsonOutcome<String>? = parseNewNode()?.transformFailure {
    parsingError(
        "a valid key", tokens.lastToken(), tokens.lastPosRead(), path, "key missing in object field"
    )
}?.bind { takeKey(it) }

private fun TokensPath.takeKey(keyNode: JsonNode): JsonOutcome<String> = when (keyNode) {
    is JsonNodeString -> keyNode.text.asSuccess()
    else -> parsingFailure("not a key", keyNode.toString(), tokens.last()?.pos ?: 0, path, "invalid Json")
}

fun <T> TokensPath.commaSeparated(contentParser: TokensPath.() -> JsonOutcome<T>?): JsonOutcome<List<T>> =
    commaSeparated(tokens, path) { t, p -> TokensPath(t, p).contentParser() }

fun <T> commaSeparated(
    tokens: TokensStream, path: NodePath, contentParser: (TokensStream, NodePath) -> JsonOutcome<T>?
): JsonOutcome<List<T>> = parseValues(tokens, path) { t, p ->
    contentParser(t, p)?.bindAndIgnore {
        takeOrNull(Comma, t, p) ?: null.asSuccess()
    }
}

private fun TokensPath.explicitNull(): JsonOutcome<JsonNodeNull> = tokens.next().let { token ->
    if (token.sameValueAs("null")) JsonNodeNull.asSuccess()
    else parsingFailure("a Null", token, tokens.lastPosRead(), path, "valid values: null")
}

fun take(separator: KondorSeparator, tokens: TokensStream, path: NodePath): JsonOutcome<KondorToken> =
    if (tokens.hasNext()) {
        tokens.next().let { token ->
            if (token.sameAs(separator)) token.asSuccess()
            else parsingFailure(separator.name, token, tokens.lastPosRead(), path, "invalid Json")
        }
    } else {
        parsingFailure(separator.name, "end of file", tokens.last()?.pos ?: 0, path, "invalid Json")
    }


private fun takeOrNull(separator: KondorSeparator, tokens: TokensStream, path: NodePath): JsonOutcome<KondorToken>? =
    tokens.peek().let { currToken ->
        if (currToken.sameAs(separator))
            take(separator, tokens, path)
        else
            null
    }

fun TokensPath.parseNewNode(): JsonOutcome<JsonNode>? = if (!tokens.hasNext()) null
else when (val t = tokens.peek()) {
    is ValueTokenEager -> when (t.text) {
        "null" -> parseJsonNodeNull()
        "false", "true" -> parseJsonNodeBoolean()
        else -> parseJsonNodeNum()
    }

    is SeparatorToken -> when (t.sep) {
        OpeningQuotes -> parseJsonNodeString()
        OpeningSquare -> parseJsonNodeArray()
        OpeningCurly -> parseJsonNodeObject()
        ClosingSquare, ClosingCurly -> null //no more nodes
        ClosingQuotes, Comma, Colon -> parsingError(
            "a new node", tokens.lastToken(), tokens.lastPosRead(), path, "${t.desc} in wrong position"
        ).asFailure()
    }

    else -> parsingError(
        "a valid token", t.toString(), tokens.lastPosRead(), path, "unexpected token type"
    ).asFailure()
}


fun <T, U, E : OutcomeError> Outcome<E, T>.bindAndIgnore(f: (T) -> Outcome<E, U>): Outcome<E, T> = when (this) {
    is Failure -> this
    is Success -> f(value).transform { value }
} //!!! this should go to Outcome core if not already there

fun parseBoolean(tokens: TokensStream, path: NodePath): JsonOutcome<Boolean> = when (val token = tokens.next()) {
    is ValueTokenEager -> when (token.text) {
        "true" -> true.asSuccess()
        "false" -> false.asSuccess()
        else -> parsingFailure("a Boolean", token, tokens.lastPosRead(), path, "valid values: false, true")
    }

    else -> parsingFailure("a Boolean", token, tokens.lastPosRead(), path, "valid values: false, true")
}

fun <T> parseNumber(
    tokens: TokensStream,
    path: NodePath,
    converter: (String) -> JsonOutcome<T>
): JsonOutcome<T> {
    val position = tokens.lastPosRead()
    return when (val token = tokens.peek()) {
        is ValueTokenEager ->
            try {
                tokens.next()
                converter(token.text)
            } catch (nfe: NumberFormatException) {
                parsingFailure("a valid Number", token.desc, position, path, "NumberFormatException ${nfe.message}")
            } catch (e: Exception) {
                parsingFailure("a Number or NaN", token.desc, position, path, e.message.orEmpty())
            }

        is OpeningQuotesSep -> //case NaN Infinity -> letting the converter try
            parseString(tokens, path)
                .bind { converter(it) }

        else -> parsingFailure("a Number value", token, position, path, "not a valid number")
    }
}

fun parseString(tokens: TokensStream, path: NodePath, allowEmpty: Boolean = true): JsonOutcome<String> =
    surrounded(
        OpeningQuotes,
        { t, p -> stringOrEmpty(allowEmpty, t, p) }, ClosingQuotes
    )(tokens, path)

private fun stringOrEmpty(
    allowEmpty: Boolean,
    tokens: TokensStream,
    path: NodePath
): JsonOutcome<String> =
    when (val token = tokens.peek()) {
        is ValueTokenEager -> token.text.asSuccess().also { tokens.next() }
        else -> if (allowEmpty) "".asSuccess() else parsingFailure(
            "a non empty String", token, tokens.lastPosRead(), path, "invalid Json"
        )
    }

fun <T> parseArray(tokens: TokensStream, path: NodePath, converter: (TokensStream, NodePath) -> JsonOutcome<T>) =
    commaSeparated(tokens, path) { t, p -> parseNewValue(t, p, converter) }

fun parseFields(
    tokens: TokensStream,
    path: NodePath,
    fieldParser: (String, TokensStream, NodePath) -> JsonOutcome<Any>
): JsonOutcome<Map<String, Any>> =
    commaSeparated(tokens, path) { t, p ->
        parseString(t, p)
            .bindAndIgnore {
                take(Comma, t, p)
            }.bind { key ->
                fieldParser(key, t, p)
                    .transform { key to it }
            }
    }.bind {
        it.toMap().asSuccess()
    }

fun <T> parseNewValue(
    tokens: TokensStream, path: NodePath, converter: (TokensStream, NodePath) -> JsonOutcome<T>
): JsonOutcome<T>? = if (!tokens.hasNext()) null
else when (val t = tokens.peek()) {
    is ValueTokenEager -> converter(tokens, path)
    is SeparatorToken -> when (t.sep) {
        OpeningQuotes, OpeningSquare, OpeningCurly -> converter(tokens, path)
        ClosingSquare, ClosingCurly -> null //no more nodes
        ClosingQuotes, Comma, Colon -> parsingError(
            "a new json value", tokens.lastToken(), tokens.lastPosRead(), path, "${t.desc} in wrong position"
        ).asFailure()
    }

    else -> parsingError(
        "a valid token", t.toString(), tokens.lastPosRead(), path, "unexpected token type"
    ).asFailure()
}
