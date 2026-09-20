package com.ubertob.kondor.json.parser

import com.ubertob.kondor.json.InvalidJsonError
import com.ubertob.kondor.json.JsonError
import com.ubertob.kondor.json.JsonOutcome
import com.ubertob.kondor.json.jsonnode.*
import com.ubertob.kondor.json.parser.KondorSeparator.*
import com.ubertob.kondor.outcome.*
import java.math.BigDecimal

fun KondorToken.sameValueAs(text: String): Boolean = when (this) {
    is Separator -> false
    is Value -> this.text == text
}

data class TokensPath(val tokens: TokensStream, val path: NodePath)

// the token iterators throw at the end of the input, so a truncated Json must be checked before peeking or reading
private fun TokensStream.peekOrNull(): KondorToken? = if (hasNext()) peek() else null

private fun TokensStream.nextOrNull(): KondorToken? = if (hasNext()) next() else null

internal fun TokensStream.isNextNull(): Boolean = peekOrNull()?.sameValueAs("null") == true

private fun endOfFileFailure(expected: String, tokens: TokensStream, path: NodePath) =
    parsingFailure(expected, "end of file", tokens.lastPosRead(), path, "invalid Json")

fun TokensStream.lastToken(): KondorToken = this.last() ?: Value("Nothing", 0)


internal fun parsingError(expected: String, actual: String, position: Int, path: NodePath, details: String) =
    InvalidJsonError(
        path, "at position $position: expected $expected but found $actual - $details"
    )

fun parsingError(
    expected: String, actual: KondorToken, lastPosRead: Int, path: NodePath, details: String
): JsonError = parsingError(expected, actual.desc, tokenPos(actual, lastPosRead), path, details)

private fun tokenPos(token: KondorToken, lastPosRead: Int) = if (token is Value) token.pos else lastPosRead

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
        OpeningBracket, TokensPath::array, ClosingBracket
    )()

fun TokensPath.parseJsonNodeObject(): JsonOutcome<JsonNodeObject> =
    surroundedForNodes(
        OpeningCurly, TokensPath::jsonObject, ClosingCurly
    )()

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
): JsonParserFromTokens<T> = { tokens, path ->
    take(openingToken, tokens, path)
        .bind { takeContent(tokens, path) }
        .bindAndIgnore {
            take(closingToken, tokens, path)
        }
}

fun <T> parseValues(
    tokens: TokensStream, path: NodePath,
    parseFun: (TokensStream, NodePath, Int) -> JsonOutcome<Pair<T?, Boolean>>
): JsonOutcome<List<T>> {
    val values = ArrayList<T>(128)
    var index = 0
    var shouldContinue = true

    while (shouldContinue) {
        shouldContinue = parseFun(tokens, path, index++)
            .transform { (value, continueParsing) ->
                value?.let { values.add(it) }
                continueParsing
            }
            .onFailure { return it.asFailure() }
    }

    return values.asSuccess()
}

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

fun TokensPath.jsonObject(): JsonOutcome<JsonNodeObject> = commaSeparated({
    keyValue { tokensPath -> tokensPath.parseFieldValue() }
}).transform { JsonNodeObject(FieldNodeMap(sortKeys(it).toMap())) }

private fun sortKeys(pairs: List<Pair<String, JsonNode>>): List<Pair<String, JsonNode>> =
    pairs.sortedBy { it.first }


fun <T> TokensPath.keyValue(contentParser: (TokensPath) -> JsonOutcome<T>): JsonOutcome<Pair<String, T>>? =
    parseOptionalKeyNode()?.bind { key ->
        val newPath = NodePathSegment(key, path)
        take(Colon, tokens, newPath)
            .bind {
                val tk: TokensPath = copy(path = newPath)
                contentParser(tk)
            }
            .transform { value -> key to value }
    }

private fun TokensPath.parseOptionalKeyNode(): JsonOutcome<String>? = parseNewNode()?.transformFailure {
    parsingError(
        "a valid key", tokens.lastToken(), tokens.lastPosRead(), path, "key missing in object field"
    )
}?.bind { takeKey(it) }

private fun TokensPath.takeKey(keyNode: JsonNode): JsonOutcome<String> = when (keyNode) {
    is JsonNodeString -> keyNode.text.asSuccess()
    else -> parsingFailure("not a key", keyNode.toString(), tokens.lastPosRead(), path, "invalid Json")
}

fun <T> TokensPath.commaSeparated(contentParser: TokensPath.() -> JsonOutcome<T>?): JsonOutcome<List<T>> =
    commaSeparated(tokens, path) { t, p, i -> TokensPath(t, p).contentParser() }

fun <T> commaSeparated(
    tokens: TokensStream, path: NodePath, contentParser: (TokensStream, NodePath, Int) -> JsonOutcome<T>?
): JsonOutcome<List<T>> = parseValues(tokens, path) { t, p, i ->
    val parsedValue = contentParser(t, p, i)
        ?: return@parseValues (null to false).asSuccess()

    parsedValue.transform { value ->
        value to (takeOrNull(Comma, t, p) != null)
    }
}

private fun TokensPath.explicitNull(): JsonOutcome<JsonNodeNull> = tokens.nextOrNull().let { token ->
    when {
        token == null -> endOfFileFailure("a Null", tokens, path)
        token.sameValueAs("null") -> JsonNodeNull.asSuccess()
        else -> parsingFailure("a Null", token, tokens.lastPosRead(), path, "valid values: null")
    }
}

fun take(separator: KondorSeparator, tokens: TokensStream, path: NodePath): JsonOutcome<KondorToken> =
    if (tokens.hasNext()) {
        tokens.next().let { token ->
            if ((token as? Separator)?.sep == separator)
                token.asSuccess()
            else
                parsingFailure(separator.name, token, tokens.lastPosRead(), path, "invalid Json")
        }
    } else {
        endOfFileFailure(separator.name, tokens, path)
    }


private fun isNext(separator: KondorSeparator, tokens: TokensStream): Boolean =
    tokens.hasNext() && (tokens.peek() as? Separator)?.sep == separator

private fun takeOrNull(separator: KondorSeparator, tokens: TokensStream, path: NodePath): JsonOutcome<KondorToken>? =
    tokens.peekOrNull().let { currToken ->
        if ((currToken as? Separator)?.sep == separator)
            take(separator, tokens, path)
        else
            null
    }

// the value of an object field is mandatory, so the end of the input or a closing separator is an error
internal fun TokensPath.parseFieldValue(): JsonOutcome<JsonNode> =
    parseNewNode() ?: when (val token = tokens.peekOrNull()) {
        null -> endOfFileFailure("a valid node", tokens, path)
        else -> parsingFailure("a valid node", token, tokens.lastPosRead(), path, "invalid Json")
    }

fun TokensPath.parseNewNode(): JsonOutcome<JsonNode>? =
    if (!tokens.hasNext())
        null
    else
        when (val t = tokens.peek()) {
            is Value -> when (t.text) {
                "null" -> parseJsonNodeNull()
                "false", "true" -> parseJsonNodeBoolean()
                else -> parseJsonNodeNum()
            }

            is Separator -> when (t.sep) {
                OpeningQuotes -> parseJsonNodeString()
                OpeningBracket -> parseJsonNodeArray()
                OpeningCurly -> parseJsonNodeObject()
                ClosingBracket, ClosingCurly -> null //no more nodes
                ClosingQuotes, Comma, Colon -> parsingError(
                    "a new node", tokens.lastToken(), tokens.lastPosRead(), path, "${t.desc} in wrong position"
                ).asFailure()
            }
        }


fun parseBoolean(tokens: TokensStream, path: NodePath): JsonOutcome<Boolean> = when (val token = tokens.nextOrNull()) {
    null -> endOfFileFailure("a Boolean", tokens, path)
    is Value -> when (token.text) {
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
    return when (val token = tokens.peekOrNull()) {
        null -> endOfFileFailure("a Number", tokens, path)
        is Value ->
            try {
                tokens.next() //commit on the peek NOOP
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
    when (val token = tokens.peekOrNull()) {
        null -> endOfFileFailure("a String", tokens, path)
        is Value -> token.text.asSuccess().also { tokens.next() }
        else -> if (allowEmpty) "".asSuccess() else parsingFailure(
            "a non empty String", token, tokens.lastPosRead(), path, "invalid Json"
        )
    }

fun <T> parseArray(
    tokens: TokensStream,
    path: NodePath,
    converter: (TokensStream, NodePath) -> JsonOutcome<T>
): JsonOutcome<List<T>> =
    commaSeparated(tokens, path) { t, p, i -> parseNewValue(t, newSegment(p, i), converter) }


private fun newSegment(path: NodePath, nodeNumber: Int): NodePath = NodePathSegment("[$nodeNumber]", path)

fun parseFields(
    tokens: TokensStream,
    path: NodePath,
    fieldParser: (String, TokensStream, NodePath) -> JsonOutcome<Any?>
): JsonOutcome<FieldsValuesMap> =
    commaSeparated(tokens, path) { t, p, i ->
        //empty object: checked only before the first field, so a trailing comma is still an error
        if (i == 0 && isNext(ClosingCurly, t))
            null
        else parseString(t, p)
            .bindAndIgnore {
                take(Colon, t, p)
            }.bind { key ->
                val fieldPath = NodePathSegment(key, path)
                fieldParser(key, t, fieldPath)
                    .transform { key to it }
            }
    }.bind {
        FieldsValuesMap(it.toMap()).asSuccess()
    }

fun <T> parseNewValue(
    tokens: TokensStream, path: NodePath, converter: (TokensStream, NodePath) -> JsonOutcome<T>
): JsonOutcome<T>? = if (!tokens.hasNext()) null
else when (val t = tokens.peek()) {
    is Value -> converter(tokens, path)
    is Separator -> when (t.sep) {
        OpeningQuotes, OpeningBracket, OpeningCurly -> converter(tokens, path)
        ClosingBracket, ClosingCurly -> null //no more nodes
        ClosingQuotes, Comma, Colon -> parsingError(
            "a new json value", tokens.lastToken(), tokens.lastPosRead(), path, "${t.desc} in wrong position"
        ).asFailure()
    }
}
