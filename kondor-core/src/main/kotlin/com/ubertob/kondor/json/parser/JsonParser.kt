package com.ubertob.kondor.json.parser

import com.ubertob.kondor.json.InvalidJsonError
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

private fun parsingError(
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
    surrounded(OpeningQuotes, TokensPath::string, ClosingQuotes)()


fun TokensPath.parseJsonNodeArray(): JsonOutcome<JsonNodeArray> = surrounded(
    OpeningBracket, TokensPath::array, ClosingBracket
)()


fun TokensPath.parseJsonNodeObject(): JsonOutcome<JsonObjectNode> = surrounded(
    OpeningCurly, TokensPath::jsonObject, ClosingCurly
)()


typealias JsonParser<T> = TokensPath.() -> JsonOutcome<T>


fun <T> surrounded(
    openingToken: KondorSeparator, takeContent: JsonParser<T>, closingToken: KondorSeparator
): JsonParser<T> = {
    val middle = { _: KondorToken, middle: T, _: KondorToken -> middle }

    middle `!` take(openingToken) `*` takeContent() `*` take(closingToken)
}


fun <T> TokensPath.extractNodesIndexed(f: TokensPath.() -> JsonOutcome<T>?): JsonOutcome<List<T>> {
    var arrayIndex = 0
    val nodes = ArrayList<T>(128)
    while (true) { //is it possible to use recursion here?
        val nodeOutcome = f(subNodePath(arrayIndex++)) ?: break
        val node = nodeOutcome.onFailure { return it.asFailure() }
        nodes.add(node)
    }
    return nodes.asSuccess()
}

private fun TokensPath.subNodePath(nodeNumber: Int) = copy(path = NodePathSegment("[$nodeNumber]", path))

fun TokensPath.boolean(): JsonOutcome<JsonNodeBoolean> = when (val token = tokens.next()) {
    is Value -> when (token.text) {
        "true" -> true.asSuccess()
        "false" -> false.asSuccess()
        else -> parsingFailure("a Boolean", token, tokens.lastPosRead(), path, "valid values: false, true")
    }

    else -> parsingFailure("a Boolean", token, tokens.lastPosRead(), path, "valid values: false, true")
}.transform { JsonNodeBoolean(it) }


fun TokensPath.number(): JsonOutcome<JsonNodeNumber> = when (val token = tokens.next()) {
    is Value -> stringToNumber(token, path)
    else -> parsingFailure("a Number", token, tokens.lastPosRead(), path, "not a valid number")
}.transform { JsonNodeNumber(it) }

private fun stringToNumber(token: Value, nodePath: NodePath): Outcome<JsonError, Number> = try {
    BigDecimal(token.text).asSuccess()
} catch (e: NumberFormatException) {
    try {
        token.text.toDouble().asSuccess()
    } catch (t: NumberFormatException) {
        parsingFailure("a Number", token, nodePath, t.message.orEmpty())
    }

 check this
private fun TokensPath.stringToBigDecimal(token: Value, nodePath: NodePath): Outcome<JsonError, BigDecimal> = try {
    BigDecimal(string(token)).asSuccess()
} catch (t: NumberFormatException) {
    parsingFailure("a Number", token, tokens.lastPosRead(), nodePath, t.message.orEmpty())
}


fun TokensPath.string(allowEmpty: Boolean = true): JsonOutcome<JsonNodeString> = when (val token = tokens.peek()) {
    is Value -> token.text.asSuccess().also { tokens.next() }
    else -> if (allowEmpty) "".asSuccess() else parsingFailure(
        "a non empty String",
        token,
        tokens.lastPosRead(),
        path,
        "invalid Json"
    )
}.transform { JsonNodeString(it) }


fun TokensPath.array(): JsonOutcome<JsonNodeArray> =
    commaSepared { parseNewNode() }.transform { JsonNodeArray(it) }

fun TokensPath.jsonObject(): JsonOutcome<JsonObjectNode> = commaSepared(withParentNode {
    keyValue {
        parseNewNode() ?: parsingFailure("a valid node", "nothing", tokens.last()?.pos ?: 0, path, "invalid Json")
    }
}).transform(::checkForDuplicateKeys).transform { JsonObjectNode(it.toMap()) }

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
    extractNodesIndexed {
        contentParser()?.bindAndIgnore {
            takeOrNull(Comma) ?: null.asSuccess()
        }
    }

private fun TokensPath.explicitNull(): JsonOutcome<JsonNodeNull> = tokens.next().let { token ->
    if (token.sameValueAs("null")) JsonNodeNull.asSuccess()
    else parsingFailure("a Null", token, tokens.lastPosRead(), path, "valid values: null")
}


fun TokensPath.take(separator: KondorSeparator): JsonOutcome<KondorToken> =
    if (tokens.hasNext()) {
        tokens.next().let { token ->
            if (token.sameAs(separator))
                token.asSuccess()
            else
                parsingFailure(separator.name, token, tokens.lastPosRead(), path, "invalid Json")
        }
    } else {
        parsingFailure(separator.name, "end of file", tokens.last()?.pos ?: 0, path, "invalid Json")
    }

private fun TokensPath.takeOrNull(separator: KondorSeparator): JsonOutcome<KondorToken>? =
    tokens.peek().let { currToken ->
        if (currToken.sameAs(separator)) take(separator)
        else null

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

private fun string(token: Value) = token.text


fun <T, U, E : OutcomeError> Outcome<E, T>.bindAndIgnore(f: (T) -> Outcome<E, U>): Outcome<E, T> = when (this) {
    is Failure -> this
    is Success -> f(value).transform { value }
}


typealias ObjectFields = Map<String, Any?>

fun TokensPath.toObjectFields(): JsonOutcome<ObjectFields> = commaSepared(withParentNode {
    keyValue {
        parseNewNode() ?: parsingFailure("a valid node", "nothing", tokens.last()?.pos ?: 0, path, "invalid Json")
    }
}).transform(::checkForDuplicateKeys).transform { it.toMap() }