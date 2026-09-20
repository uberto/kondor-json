package com.ubertob.kondor.json.parser

import com.ubertob.kondor.json.JsonError
import com.ubertob.kondor.json.JsonParsingException

enum class KondorSeparator(val sign: Char) {
    Colon(':'), Comma(','), OpeningBracket('['), OpeningCurly('{'), OpeningQuotes('"'), ClosingBracket(']'), ClosingCurly(
        '}'
    ),
    ClosingQuotes('"')
}

sealed class KondorToken {
    abstract val desc: String //used for error messages
}

sealed class Separator(val sep: KondorSeparator) : KondorToken() {
    override val desc: String = sep.name
}

data object ColonSep : Separator(KondorSeparator.Colon)
data object CommaSep : Separator(KondorSeparator.Comma)
data object OpeningBracketSep : Separator(KondorSeparator.OpeningBracket)
data object OpeningCurlySep : Separator(KondorSeparator.OpeningCurly)
data object OpeningQuotesSep : Separator(KondorSeparator.OpeningQuotes)
data object ClosingBracketSep : Separator(KondorSeparator.ClosingBracket)
data object ClosingCurlySep : Separator(KondorSeparator.ClosingCurly)
data object ClosingQuotesSep : Separator(KondorSeparator.ClosingQuotes)


data class Value(val text: String, val pos: Int) : KondorToken() {
    override val desc: String = "'$text'"
}

/**
 * A lexer reading the input lazily can only fail while the tokens are read: it stops the iteration and keeps the error
 * here, so that [com.ubertob.kondor.json.JsonConverter.fromJson] reports it instead of the end of the input.
 */
internal interface LexerErrorSource {
    fun lexerError(): JsonError?
}

data class TokensStream(private val iterator: PeekingIterator<KondorToken>) :
    PeekingIterator<KondorToken> by iterator {

    fun lexerError(): JsonError? = (iterator as? LexerErrorSource)?.lexerError()

    /**
     * All the remaining tokens.
     * @throws JsonParsingException if the input is not valid Json and it is read lazily, from an InputStream
     */
    fun toList(): List<KondorToken> = iterator.asSequence().toList()
        .also { lexerError()?.let { error -> throw JsonParsingException(error) } }

    private var currPos = 0

    fun lastPosRead(): Int = currPos

    override fun next(): KondorToken =
        iterator.next().also {
            currPos = when (it) {
                is Separator -> currPos +1
                is Value -> it.pos + it.text.length - 1
            }
        }

}
