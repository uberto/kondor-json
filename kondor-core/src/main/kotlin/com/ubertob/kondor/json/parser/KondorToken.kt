package com.ubertob.kondor.json.parser


enum class KondorSeparator(val sign: Char) {
    Colon(':'), Comma(','), OpeningSquare('['), OpeningCurly('{'), OpeningQuotes('"'), ClosingSquare(']'), ClosingCurly(
        '}'
    ),
    ClosingQuotes('"')
}

sealed interface KondorToken {
    val pos: Int
    val desc: String

    fun sameValueAs(text: String): Boolean
    fun sameAs(separator: KondorSeparator): Boolean


}

sealed interface ValueToken : KondorToken {
    val text: String
}

sealed class SeparatorToken(val sep: KondorSeparator) : KondorToken {
    override fun sameValueAs(text: String): Boolean = false

    override fun sameAs(separator: KondorSeparator): Boolean = sep == separator
    override val pos: Int = -1 //unused
    override val desc: String = sep.name
}

object ColonSep : SeparatorToken(KondorSeparator.Colon)
object CommaSep : SeparatorToken(KondorSeparator.Comma)
object OpeningSquareSep : SeparatorToken(KondorSeparator.OpeningSquare)
object OpeningCurlySep : SeparatorToken(KondorSeparator.OpeningCurly)
object OpeningQuotesSep : SeparatorToken(KondorSeparator.OpeningQuotes)
object ClosingSquareSep : SeparatorToken(KondorSeparator.ClosingSquare)
object ClosingCurlySep : SeparatorToken(KondorSeparator.ClosingCurly)
object ClosingQuotesSep : SeparatorToken(KondorSeparator.ClosingQuotes)


data class ValueTokenEager(override val text: String, override val pos: Int) : ValueToken {
    override fun sameValueAs(text: String): Boolean = this.text == text

    override fun sameAs(separator: KondorSeparator): Boolean = false
    override val desc: String = "'$text'"
}