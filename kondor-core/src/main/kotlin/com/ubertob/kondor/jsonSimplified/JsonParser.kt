package com.ubertob.kondor.jsonSimplified

import com.ubertob.kondor.outcome.*
import java.math.BigDecimal

private fun parsingError(expected: String, actual: String) = JsonError(
    "expected $expected but found $actual"
)

private fun parsingError(
    expected: String,
    actual: KondorToken?
): JsonError =
    parsingError(expected, "'$actual'")

fun parsingFailure(expected: String, actual: String) =
    parsingError(expected, actual).asFailure()

fun parsingFailure(expected: String, actual: KondorToken) =
    parsingError(expected, actual).asFailure()




typealias JsonParser<T> = TokensStream.() -> JsonOutcome<T>


fun <T> TokensStream.surrounded(
    openingToken: KondorToken,
    takeContent: JsonParser<T>,
    closingToken: KondorToken
): JsonOutcome<T> =
    { _: KondorToken, middle: T, _: KondorToken -> middle } `!` take(openingToken) `*` takeContent() `*` take(
        closingToken
    )


fun <T> TokensStream.extractNodesIndexed(f: TokensStream.() -> JsonOutcome<T>?): JsonOutcome<List<T>> =
    naturals().map { f() }
        .takeWhileNotNull()
        .extractList()


fun TokensStream.boolean(): JsonOutcome<JsonNodeBoolean> =
    when (val token = next()) {
        Value("true") -> true.asSuccess()
        Value("false") -> false.asSuccess()
        else -> parsingFailure("a Boolean", token)
    }.transform { JsonNodeBoolean(it) }


fun TokensStream.number(): JsonOutcome<JsonNodeNumber> =
    when (val token = next()) {
        is Value -> BigDecimal(token.text).asSuccess()
        else -> parsingFailure("a Number", token)
    }.transform { JsonNodeNumber(it) }


fun TokensStream.string(allowEmpty: Boolean = true): JsonOutcome<JsonNodeString> =
    surrounded(
        OpeningQuotes,
        { value(allowEmpty) },
        ClosingQuotes
    ).transform { JsonNodeString(it) }

fun TokensStream.value(allowEmpty: Boolean): JsonOutcome<String> =
    when (val token = peek()) {
        is Value -> token.text.asSuccess().also { next() }
        else -> if (allowEmpty) "".asSuccess() else
            parsingFailure("a non empty String", token)
    }


fun TokensStream.array(): JsonOutcome<JsonNodeArray> =
    surrounded(
        OpeningBracket,
        { commaSepared { parseNewNode() } },
        ClosingQuotes
    )
        .transform { JsonNodeArray(it) }

fun TokensStream.jsonObject(): JsonOutcome<JsonNodeObject> =
    surrounded(
        OpeningCurly,
        {
            commaSepared {
                keyValue {
                    parseNewNode() ?: parsingFailure("sm", "nothing")
                }
            }
        },
        ClosingQuotes
    )
        .transform { JsonNodeObject(it.toMap()) }


fun <T> TokensStream.keyValue(contentParser: TokensStream.() -> JsonOutcome<T>): JsonOutcome<Pair<String, T>>? =
    parseOptionalKeyNode()
        ?.bind { key ->
            take(Colon)
                .bind { contentParser() }
                .transform { value -> key to value }
        }

private fun TokensStream.parseOptionalKeyNode(): JsonOutcome<String>? =
    parseNewNode()?.transformFailure {
        parsingError(
            "a valid key", last()
        )
    }?.bind { takeKey(it) }

private fun takeKey(keyNode: JsonNode): JsonOutcome<String> =
    when (keyNode) {
        is JsonNodeString -> keyNode.text.asSuccess()
        else -> parsingFailure("not a key", keyNode.toString())
    }

fun <T> TokensStream.commaSepared(contentParser: TokensStream.() -> JsonOutcome<T>?): JsonOutcome<List<T>> =
    extractNodesIndexed {
        contentParser()?.bindAndIgnore {
            takeOrNull(Comma) ?: null.asSuccess()
        }
    }

fun TokensStream.explicitNull(): JsonOutcome<JsonNodeNull> =
    when (val token = next()) {
        Value("null") -> Unit.asSuccess()
        else -> parsingFailure("a Null", token)
    }.transform { JsonNodeNull }


private fun TokensStream.take(kondorToken: KondorToken): JsonOutcome<KondorToken> =
    next().let { currToken ->
        if (currToken != kondorToken)
            parsingFailure("'$kondorToken'", currToken)
        else
            currToken.asSuccess()
    }

private fun TokensStream.takeOrNull(kondorToken: KondorToken): JsonOutcome<KondorToken>? =
    peek().let { currToken ->
        if (currToken != kondorToken)
            null
        else
            take(kondorToken)
    }

//---

fun TokensStream.parseNewNode(): JsonOutcome<JsonNode>? =
    when (val t = peek()) {
        Value("null") -> explicitNull()
        Value("false"), Value("true") -> boolean()
        is Value -> number()
        OpeningQuotes -> string()
        OpeningBracket -> array()
        OpeningCurly -> jsonObject()
        ClosingBracket, ClosingCurly -> null //no more nodes
        ClosingQuotes, Comma, Colon -> parsingError(
            "a new node", last()
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