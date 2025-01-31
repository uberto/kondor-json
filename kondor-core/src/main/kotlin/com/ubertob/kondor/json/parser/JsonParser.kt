package com.ubertob.kondor.json.parser

import com.ubertob.kondor.json.InvalidJsonError
import com.ubertob.kondor.json.JDouble
import com.ubertob.kondor.json.JsonError
import com.ubertob.kondor.json.JsonOutcome
import com.ubertob.kondor.json.jsonnode.*
import com.ubertob.kondor.json.parser.KondorSeparator.*
import com.ubertob.kondor.outcome.*
import java.math.BigDecimal


data class TokensPath(val tokens: TokensStream, val path: NodePath)

fun TokensStream.lastToken(): KondorToken = this.last() ?: Value("Nothing", 0)


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


fun TokensPath.parseJsonNodeArray(): JsonOutcome<JsonNodeArray> = surrounded(
    OpeningBracket, TokensPath::array, ClosingBracket
)()


fun TokensPath.parseJsonNodeObject(): JsonOutcome<JsonNodeObject> = surrounded(
    OpeningCurly, TokensPath::jsonObject, ClosingCurly
)()


typealias JsonParser<T> = TokensPath.() -> JsonOutcome<T>
typealias JsonParser2<T> = (TokensStream, NodePath) -> JsonOutcome<T> //!!! remove it at the end

fun <T> surrounded(
    openingToken: KondorSeparator, takeContent: JsonParser<T>, closingToken: KondorSeparator
): JsonParser<T> = {
    val middle = { _: KondorToken, middle: T, _: KondorToken -> middle }

    middle `!` take(openingToken) `*` takeContent() `*` take(closingToken)
}

fun <T> surrounded2(
    openingToken: KondorSeparator, takeContent: JsonParser2<T>, closingToken: KondorSeparator
): JsonParser2<T> = { tokens, path ->  //!!!
    take2(openingToken, tokens, path).bind { takeContent(tokens, path) }.bindAndIgnore {
        take2(closingToken, tokens, path)
    }
}

fun <T> extractValues(
    tokens: TokensStream, path: NodePath, extractFun: (TokensStream, NodePath) -> JsonOutcome<T>?
): JsonOutcome<List<T>> {
    var arrayIndex = 0
    val values = ArrayList<T>(128)
    while (true) {
        val value = extractFun(tokens, newSegment(path, arrayIndex++))
            ?.onFailure { return it.asFailure() }
            ?: break //!!! maybe better with a fold or a traverse
        values.add(value)
    }
    return values.asSuccess()
}

private fun TokensPath.subNodePath(nodeNumber: Int): TokensPath = copy(path = newSegment(path, nodeNumber))

private fun newSegment(path: NodePath, nodeNumber: Int): NodePath = NodePathSegment("[$nodeNumber]", path)

fun TokensPath.boolean(): JsonOutcome<JsonNodeBoolean> = when (val token = tokens.next()) {
    is Value -> when (token.text) {
        "true" -> true.asSuccess()
        "false" -> false.asSuccess()
        else -> parsingFailure("a Boolean", token, tokens.lastPosRead(), path, "valid values: false, true")
    }

    else -> parsingFailure("a Boolean", token, tokens.lastPosRead(), path, "valid values: false, true")
}.transform { JsonNodeBoolean(it) }


fun bigDecimalParser(value: String): JsonOutcome<Number> =
    try {
        BigDecimal(value).asSuccess()
    } catch (e: NumberFormatException) {
        value.toDouble().asSuccess() //for NaN, Infinity etc that are valid Double but not BigDecimals
    }

fun TokensPath.number(): JsonOutcome<JsonNodeNumber> =
    parseNumber(tokens, path, ::bigDecimalParser).transform { JsonNodeNumber(it) }


fun TokensPath.array(): JsonOutcome<JsonNodeArray> = commaSepared { parseNewNode() }.transform { JsonNodeArray(it) }

fun TokensPath.jsonObject(): JsonOutcome<JsonNodeObject> = commaSepared(withParentNode {
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
        take(Colon).bind { contentParser(copy(path = NodePathSegment(key, path))) }.transform { value -> key to value }
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

fun <T> TokensPath.commaSepared(contentParser: TokensPath.() -> JsonOutcome<T>?): JsonOutcome<List<T>> =
    commaSeparated(tokens, path) { t, p -> TokensPath(t, p).contentParser() }

fun <T> commaSeparated(
    tokens: TokensStream, path: NodePath, contentParser: (TokensStream, NodePath) -> JsonOutcome<T>?
): JsonOutcome<List<T>> = extractValues(tokens, path) { t, p ->

    contentParser(t, p)?.bindAndIgnore {
        takeOrNull(t, p, Comma) ?: null.asSuccess()
    }
}

private fun TokensPath.explicitNull(): JsonOutcome<JsonNodeNull> = tokens.next().let { token ->
    if (token.sameValueAs("null")) JsonNodeNull.asSuccess()
    else parsingFailure("a Null", token, tokens.lastPosRead(), path, "valid values: null")
}


fun TokensPath.take(separator: KondorSeparator): JsonOutcome<KondorToken> = take2(separator, tokens, path)

fun take2(separator: KondorSeparator, tokens: TokensStream, path: NodePath): JsonOutcome<KondorToken> = //!!!
    if (tokens.hasNext()) {
        tokens.next().let { token ->
            if (token.sameAs(separator)) token.asSuccess()
            else parsingFailure(separator.name, token, tokens.lastPosRead(), path, "invalid Json")
        }
    } else {
        parsingFailure(separator.name, "end of file", tokens.last()?.pos ?: 0, path, "invalid Json")
    }


private fun takeOrNull(tokens: TokensStream, path: NodePath, separator: KondorSeparator): JsonOutcome<KondorToken>? =
    tokens.peek().let { currToken ->
        if (currToken.sameAs(separator)) take2(separator, tokens, path)
        else null
    }

fun TokensPath.parseNewNode(): JsonOutcome<JsonNode>? = if (!tokens.hasNext()) null
else when (val t = tokens.peek()) {
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


fun <T, U, E : OutcomeError> Outcome<E, T>.bindAndIgnore(f: (T) -> Outcome<E, U>): Outcome<E, T> = when (this) {
    is Failure -> this
    is Success -> f(value).transform { value }
} //!!! this should go to Outcome core if not already there

fun parseBoolean(tokens: TokensStream, path: NodePath): JsonOutcome<Boolean> = when (val token = tokens.next()) {
    is Value -> when (token.text) {
        "true" -> true.asSuccess()
        "false" -> false.asSuccess()
        else -> parsingFailure("a Boolean", token, tokens.lastPosRead(), path, "valid values: false, true")
    }

    else -> parsingFailure("a Boolean", token, tokens.lastPosRead(), path, "valid values: false, true")
}

inline fun <reified T : Number> parseNumber(
    tokens: TokensStream,
    path: NodePath,
    converter: (String) -> JsonOutcome<T>
): JsonOutcome<T> {
    val position = tokens.lastPosRead()
    return when (val token = tokens.peek()) {
        is Value ->
            try {
                tokens.next()
                converter(token.text)
            } catch (t: Exception) {
                println("!!! ${T::class.java} ex $t")


                parsingFailure("a Number or NaN", token.desc, position, path, t.message.orEmpty())
            }

        is OpeningQuotesSep -> //case NaN Infinity
            parseString(tokens, path)
                .bind { JDouble.parser(it) }
                .transform { it as T }

        else -> parsingFailure("a Number value", token, position, path, "not a valid number")
    }
}

fun parseString(tokens: TokensStream, path: NodePath, allowEmpty: Boolean = true): JsonOutcome<String> = surrounded2(
    OpeningQuotes, { tokens, path ->
        when (val token = tokens.peek()) {
            is Value -> token.text.asSuccess().also { tokens.next() }
            else -> if (allowEmpty) "".asSuccess() else parsingFailure(
                "a non empty String", token, tokens.lastPosRead(), path, "invalid Json"
            )
        }
    }, ClosingQuotes
)(tokens, path)

fun <T> parseArray(tokens: TokensStream, path: NodePath, converter: (TokensStream, NodePath) -> JsonOutcome<T>) =
    commaSeparated(tokens, path) { t, p -> parseNewValue(t, p, converter) }

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
