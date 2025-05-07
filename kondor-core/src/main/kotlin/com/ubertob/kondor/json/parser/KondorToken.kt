package com.ubertob.kondor.json.parser

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
