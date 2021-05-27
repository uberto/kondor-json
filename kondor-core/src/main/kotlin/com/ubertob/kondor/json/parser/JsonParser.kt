package com.ubertob.kondor.json.parser

import com.ubertob.kondor.json.JsonError
import com.ubertob.kondor.json.JsonOutcome
import com.ubertob.kondor.json.jsonnode.*
import com.ubertob.kondor.outcome.*
import com.ubertob.kondor.outcome.Outcome.Companion.tryOrFail
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
                    parsingError(expected, actual, position, path, it.msg)
                else ->
                    parsingError(expected, "${it.msg} after $actual", position, path, "Invalid Json")
            }
        }

data class TokensPath(val tokens: TokensStream, val path: NodePath)


fun <T> TokensPath.tryParseBind(
    expected: String,
    f: TokensPath.() -> Outcome<JsonError, T>
): Outcome<JsonError, T> =
    try {
        f()
    } catch (t: NumberFormatException) {
        parsingError(expected, "'${tokens.last()}'", tokens.position(), path, t.message.orEmpty()).asFailure()
    } catch (t: Throwable) {
        parsingError(
            expected, "${t.message.orEmpty()} after ${tokens.last()}", tokens.position(),
            path, "Invalid Json"
        ).asFailure()
    }


private fun parsingError(expected: String, actual: String, position: Int, path: NodePath, details: String) = JsonError(
    path, "at position $position: expected $expected but found $actual - $details"
)

private fun parsingError(
    expected: String,
    actual: KondorToken?,
    position: Int,
    path: NodePath,
    details: String
): JsonError =
    parsingError(expected, "'$actual'", position, path, details)

fun parsingFailure(expected: String, actual: String, position: Int, path: NodePath, details: String) =
    parsingError(expected, actual, position, path, details).asFailure()

fun parsingFailure(expected: String, actual: KondorToken, position: Int, path: NodePath, details: String) =
    parsingError(expected, actual, position, path, details).asFailure()


fun TokensPath.parseJsonNodeBoolean(): JsonOutcome<JsonNodeBoolean> =
    tryParseBind(
        "a Boolean",
        TokensPath::boolean
    )

fun TokensPath.parseJsonNodeNum(): Outcome<JsonError, JsonNodeNumber> =
    tryParseBind(
        "a Number",
        TokensPath::number
    )

fun TokensPath.parseJsonNodeNull(): Outcome<JsonError, JsonNodeNull> =
    tryParseBind(
        "a Null",
        TokensPath::explicitNull
    )

fun TokensPath.parseJsonNodeString(): JsonOutcome<JsonNodeString> =
    tryParseBind(
        "a String",
        surrounded(OpeningQuotes, TokensPath::string, ClosingQuotes)
    )

fun TokensPath.parseJsonNodeArray(): JsonOutcome<JsonNodeArray> =
    tryParseBind(
        "an Array",
        surrounded(
            OpeningBracket, TokensPath::array, ClosingBracket
        )
    )

fun TokensPath.parseJsonNodeObject(): JsonOutcome<JsonNodeObject> =
    tryParseBind(
        "an Object",
        surrounded(
            OpeningCurly, TokensPath::jsonObject, ClosingCurly
        )
    )


typealias JsonParser<T> = TokensPath.() -> JsonOutcome<T>


fun <T> surrounded(openingToken: KondorToken, takeContent: JsonParser<T>, closingToken: KondorToken): JsonParser<T> =
    {
        val middle = { _: KondorToken, middle: T, _: KondorToken -> middle }

        middle `!` take(openingToken) `*` takeContent() `*` take(closingToken)
    }


fun <T> TokensPath.extractNodesIndexed(f: TokensPath.() -> JsonOutcome<T>?): JsonOutcome<List<T>> =
    naturals().map { f(subNodePath(it)) }
        .takeWhileNotNull()
        .extractList()

private fun TokensPath.subNodePath(nodeNumber: Int) =
    copy(path = NodePathSegment("[$nodeNumber]", path))

fun TokensPath.boolean(): JsonOutcome<JsonNodeBoolean> =
    when (val token = tokens.next()) {
        Value("true") -> true.asSuccess()
        Value("false") -> false.asSuccess()
        else -> parsingFailure("a Boolean", token, tokens.position(), path, "valid values: false, true")
    }.transform { JsonNodeBoolean(it, path) }


fun TokensPath.number(): JsonOutcome<JsonNodeNumber> =
    when (val token = tokens.next()) {
        is Value -> BigDecimal(token.text).asSuccess()
        else -> parsingFailure("a Number", token, tokens.position(), path, "not a valid number")
    }.transform { JsonNodeNumber(it, path) }


fun TokensPath.string(allowEmpty: Boolean = true): JsonOutcome<JsonNodeString> =
    when (val token = tokens.peek()) {
        is Value -> token.text.asSuccess().also { tokens.next() }
        else -> if (allowEmpty) "".asSuccess() else
            parsingFailure("a non empty String", token, tokens.position(), path, "invalid Json")
    }.transform { JsonNodeString(it, path) }


fun TokensPath.array(): JsonOutcome<JsonNodeArray> =
    commaSepared { parseNewNode() }
        .transform { JsonNodeArray(it, path) }

fun TokensPath.jsonObject(): JsonOutcome<JsonNodeObject> =
    commaSepared(withParentNode {
        keyValue {
            parseNewNode() ?: parsingFailure("sm", "nothing", tokens.position(), path, "!!!")
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
            "a valid key", tokens.last(), tokens.position(), path,
            "key missing in object field"
        )
    }?.bind { takeKey(it) }

private fun TokensPath.takeKey(keyNode: JsonNode): JsonOutcome<String> =
    when (keyNode) {
        is JsonNodeString -> keyNode.text.asSuccess()
        else -> parsingFailure("not a key", keyNode.toString(), tokens.position(), path, "invalid Json")
    }

fun <T> TokensPath.commaSepared(contentParser: TokensPath.() -> JsonOutcome<T>?): JsonOutcome<List<T>> =
    extractNodesIndexed {
        contentParser()?.bindAndIgnore {
            takeOrNull(Comma) ?: null.asSuccess()
        }
    }

private fun TokensPath.explicitNull(): JsonOutcome<JsonNodeNull> =
    when (val token = tokens.next()) {
        Value("null") -> Unit.asSuccess()
        else -> parsingFailure("a Null", token, tokens.position(), path, "valid values: null")
    }.transform { JsonNodeNull(path) }


private fun TokensPath.take(kondorToken: KondorToken): JsonOutcome<KondorToken> =
    tokens.next().let { currToken ->
        if (currToken != kondorToken)
            parsingFailure("'$kondorToken'", currToken, tokens.position(), path, "invalid Json")
        else
            currToken.asSuccess()
    }

private fun TokensPath.takeOrNull(kondorToken: KondorToken): JsonOutcome<KondorToken>? =
    tokens.peek().let { currToken ->
        if (currToken != kondorToken)
            null
        else
            take(kondorToken)
    }

//---

fun TokensPath.parseNewNode(): JsonOutcome<JsonNode>? =
    when (val t = tokens.peek()) {
        Value("null") -> parseJsonNodeNull()
        Value("false"), Value("true") -> parseJsonNodeBoolean()
        is Value -> parseJsonNodeNum()
        OpeningQuotes -> parseJsonNodeString()
        OpeningBracket -> parseJsonNodeArray()
        OpeningCurly -> parseJsonNodeObject()
        ClosingBracket, ClosingCurly -> null //no more nodes
        ClosingQuotes, Comma, Colon -> parsingError(
            "a new node", tokens.last(),
            tokens.position(), path, "'$t' in wrong position"
        ).asFailure()
    }


inline fun <T, U, E : OutcomeError> Outcome<E, T>.bindAndIgnore(f: (T) -> Outcome<E, U>): Outcome<E, T> =
    when (this) {
        is Failure -> this
        is Success -> f(value).transform { value }
    }

@Suppress("UNCHECKED_CAST")
fun <T : Any> Sequence<T?>.takeWhileNotNull(): Sequence<T> = takeWhile { it != null } as Sequence<T>


fun naturals(): Sequence<Int> = generateSequence(0) { it + 1 }