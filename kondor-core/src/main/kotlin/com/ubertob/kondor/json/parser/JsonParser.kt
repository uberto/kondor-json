package com.ubertob.kondor.json.parser

import com.ubertob.kondor.json.InvalidJsonError
import com.ubertob.kondor.json.JsonError
import com.ubertob.kondor.json.JsonOutcome
import com.ubertob.kondor.json.JsonParsingException
import com.ubertob.kondor.json.jsonnode.*
import com.ubertob.kondor.json.parser.KondorSeparator.*
import com.ubertob.kondor.outcome.*
import java.math.BigDecimal


data class TokensPath(val tokens: TokensStream, val path: NodePath)

fun TokensStream.lastToken(): KondorToken = this.last() ?: Value("Nothing", 0)


private fun parsingError(expected: String, actual: String, position: Int, path: NodePath, details: String) = InvalidJsonError(
    path, "at position $position: expected $expected but found $actual - $details"
)

private fun parsingError(
    expected: String,
    actual: KondorToken,
    path: NodePath,
    details: String
): JsonError = parsingError(expected, actual.desc, actual.pos, path, details)

fun parsingFailure(expected: String, actual: String, position: Int, path: NodePath, details: String) =
    parsingError(expected, actual, position, path, details).asFailure()

fun parsingException(expected: String, actual: String, position: Int, path: NodePath, details: String) =
    JsonParsingException(parsingError(expected, actual, position, path, details))

fun parsingFailure(expected: String, actual: KondorToken, path: NodePath, details: String) =
    parsingError(expected, actual, path, details).asFailure()


fun TokensPath.parseJsonNodeBoolean(): JsonOutcome<JsonNodeBoolean> =
    TokensPath::boolean.invoke(this)

fun TokensPath.parseJsonNodeNum(): Outcome<JsonError, JsonNodeNumber> =
    TokensPath::number.invoke(this)

fun TokensPath.parseJsonNodeNull(): Outcome<JsonError, JsonNodeNull> =
    TokensPath::explicitNull.invoke(this)


fun TokensPath.parseJsonNodeString(): JsonOutcome<JsonNodeString> =
    surrounded(OpeningQuotes, TokensPath::string, ClosingQuotes)()


fun TokensPath.parseJsonNodeArray(): JsonOutcome<JsonNodeArray> =
    surrounded(
        OpeningBracket, TokensPath::array, ClosingBracket
    )()


fun TokensPath.parseJsonNodeObject(): JsonOutcome<JsonNodeObject> =
    surrounded(
        OpeningCurly, TokensPath::jsonObject, ClosingCurly
    )()



typealias JsonParser<T> = TokensPath.() -> JsonOutcome<T>


fun <T> surrounded(
    openingToken: KondorSeparator,
    takeContent: JsonParser<T>,
    closingToken: KondorSeparator
): JsonParser<T> =
    {
        val middle = { _: KondorToken, middle: T, _: KondorToken -> middle }

        middle `!` take(openingToken) `*` takeContent() `*` take(closingToken)
    }


//todo make it recursive
fun <T> TokensPath.extractNodesIndexed(f: TokensPath.() -> JsonOutcome<T>?): JsonOutcome<List<T>> {
    var arrayIndex = 0
    val nodes = mutableListOf<T>()
    while (true) {
        val v = f(subNodePath(arrayIndex++)) ?: break
        v.transform { nodes.add(it) }.onFailure { return it.asFailure() }
    }
    return nodes.asSuccess()
}

private fun TokensPath.subNodePath(nodeNumber: Int) =
    copy(path = NodePathSegment("[$nodeNumber]", path))

fun TokensPath.boolean(): JsonOutcome<JsonNodeBoolean> =
    when (val token = tokens.next()) {
        is Value -> when (token.text) {
            "true" -> true.asSuccess()
            "false" -> false.asSuccess()
            else -> parsingFailure("a Boolean", token, path, "valid values: false, true")
        }
        else -> parsingFailure("a Boolean", token, path, "valid values: false, true")
    }.transform { JsonNodeBoolean(it, path) }


fun TokensPath.number(): JsonOutcome<JsonNodeNumber> =
    when (val token = tokens.next()) {
        is Value -> stringToBigDecimal(token, path)
        else -> parsingFailure("a Number", token, path, "not a valid number")
    }.transform { JsonNodeNumber(it, path) }

private fun stringToBigDecimal(token: Value, nodePath: NodePath): Outcome<JsonError, BigDecimal> =
    try {
        BigDecimal(string(token)).asSuccess()
    } catch (t: NumberFormatException) {
        parsingFailure("a Number", token, nodePath, t.message.orEmpty())
    }


fun TokensPath.string(allowEmpty: Boolean = true): JsonOutcome<JsonNodeString> =
    when (val token = tokens.peek()) {
        is Value -> token.text.asSuccess().also { tokens.next() }
        else -> if (allowEmpty) "".asSuccess() else
            parsingFailure("a non empty String", token, path, "invalid Json")
    }.transform { JsonNodeString(it, path) }


fun TokensPath.array(): JsonOutcome<JsonNodeArray> =
    commaSepared { parseNewNode() }
        .transform { JsonNodeArray(it, path) }

fun TokensPath.jsonObject(): JsonOutcome<JsonNodeObject> =
    commaSepared(withParentNode {
        keyValue {
            parseNewNode() ?: parsingFailure("a valid node", "nothing", tokens.last()?.pos ?: 0, path, "invalid Json")
        }
    })
        .transform(::checkForDuplicateKeys)
        .transform { JsonNodeObject(it.toMap(), path) }

private fun checkForDuplicateKeys(pairs: List<Pair<String, JsonNode>>): List<Pair<String, JsonNode>> =
    pairs.sortedBy { it.first }


fun <T> withParentNode(f: TokensPath.() -> JsonOutcome<T>?): TokensPath.() -> JsonOutcome<T>? =
    { f(copy(path = path.parent())) }


fun <T> TokensPath.keyValue(contentParser: TokensPath.() -> JsonOutcome<T>): JsonOutcome<Pair<String, T>>? =
    parseOptionalKeyNode()
        ?.bind { key ->
            take(Colon)
                .bind { contentParser(copy(path = NodePathSegment(key, path))) }
                .transform { value -> key to value }
        }

private fun TokensPath.parseOptionalKeyNode(): JsonOutcome<String>? =
    parseNewNode()?.transformFailure {
        parsingError(
            "a valid key", tokens.lastToken(), path,
            "key missing in object field"
        )
    }?.bind { takeKey(it) }

private fun TokensPath.takeKey(keyNode: JsonNode): JsonOutcome<String> =
    when (keyNode) {
        is JsonNodeString -> keyNode.text.asSuccess()
        else -> parsingFailure("not a key", keyNode.toString(), tokens.last()?.pos ?: 0, path, "invalid Json")
    }

fun <T> TokensPath.commaSepared(contentParser: TokensPath.() -> JsonOutcome<T>?): JsonOutcome<List<T>> =
    extractNodesIndexed {
        contentParser()?.bindAndIgnore {
            takeOrNull(Comma) ?: null.asSuccess()
        }
    }

private fun TokensPath.explicitNull(): JsonOutcome<JsonNodeNull> =
    tokens.next().let { currToken ->
        if (currToken.sameValueAs("null"))
            JsonNodeNull(path).asSuccess()
        else
            parsingFailure("a Null", currToken, path, "valid values: null")
    }


fun TokensPath.take(separator: KondorSeparator): JsonOutcome<KondorToken> =
    tokens.next().let { currToken ->
        if (currToken.sameAs(separator))
            currToken.asSuccess()
        else
            parsingFailure("${separator.name}", currToken, path, "invalid Json")

    }

private fun TokensPath.takeOrNull(separator: KondorSeparator): JsonOutcome<KondorToken>? =
    tokens.peek().let { currToken ->
        if (currToken.sameAs(separator))
            take(separator)
        else
            null

    }


fun TokensPath.parseNewNode(): JsonOutcome<JsonNode>? =
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
            ClosingQuotes, Comma, Colon ->
                parsingError(
                    "a new node", tokens.lastToken(),
                    path, "${t.desc} in wrong position"
                ).asFailure()
        }
    }

private fun string(token: Value) = token.text


fun <T, U, E : OutcomeError> Outcome<E, T>.bindAndIgnore(f: (T) -> Outcome<E, U>): Outcome<E, T> =
    when (this) {
        is Failure -> this
        is Success -> f(value).transform { value }
    }
