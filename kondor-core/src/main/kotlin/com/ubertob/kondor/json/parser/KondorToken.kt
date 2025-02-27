package com.ubertob.kondor.json.parser

enum class KondorSeparator(val sign: Char) {
    Colon(':'), Comma(','), OpeningBracket('['), OpeningCurly('{'), OpeningQuotes('"'), ClosingBracket(']'), ClosingCurly(
        '}'
    ),
    ClosingQuotes('"')
}

sealed class KondorToken {
    abstract val pos: Int //we need this because original token pos (including spaces etc.)
    abstract val desc: String //used for error messages
}

sealed class Separator(val sep: KondorSeparator) : KondorToken() {
    override val pos: Int = -1 //unused
    override val desc: String = sep.name
}

object ColonSep : Separator(KondorSeparator.Colon)
object CommaSep : Separator(KondorSeparator.Comma)
object OpeningBracketSep : Separator(KondorSeparator.OpeningBracket)
object OpeningCurlySep : Separator(KondorSeparator.OpeningCurly)
object OpeningQuotesSep : Separator(KondorSeparator.OpeningQuotes)
object ClosingBracketSep : Separator(KondorSeparator.ClosingBracket)
object ClosingCurlySep : Separator(KondorSeparator.ClosingCurly)
object ClosingQuotesSep : Separator(KondorSeparator.ClosingQuotes)


data class Value(val text: String, override val pos: Int) : KondorToken() {
    override val desc: String = "'$text'"
}

data class TokensStream(private val iterator: PeekingIterator<KondorToken>) :
    PeekingIterator<KondorToken> by iterator {
    fun toList(): List<KondorToken> = iterator.asSequence().toList()

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
