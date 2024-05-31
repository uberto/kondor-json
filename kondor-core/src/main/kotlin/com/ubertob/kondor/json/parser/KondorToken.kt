package com.ubertob.kondor.json.parser

enum class KondorSeparator(val sign: Char) {
    Colon(':'), Comma(','), OpeningBracket('['), OpeningCurly('{'), OpeningQuotes('"'), ClosingBracket(']'), ClosingCurly(
        '}'
    ),
    ClosingQuotes('"')
}

sealed class KondorToken {
    abstract fun sameValueAs(text: String): Boolean
    abstract fun sameAs(separator: KondorSeparator): Boolean

    abstract val pos: Int
    abstract val desc: String
}

sealed class Separator(val sep: KondorSeparator) : KondorToken() {
    override fun sameValueAs(text: String): Boolean = false

    override fun sameAs(separator: KondorSeparator): Boolean = sep == separator
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
    override fun sameValueAs(text: String): Boolean = this.text == text

    override fun sameAs(separator: KondorSeparator): Boolean = false
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