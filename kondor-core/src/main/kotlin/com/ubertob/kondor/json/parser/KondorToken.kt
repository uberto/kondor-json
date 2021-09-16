package com.ubertob.kondor.json.parser


enum class KondorSeparator(val sign: Char) {
    Colon(':'), Comma(','), OpeningBracket('['), OpeningCurly('{'), OpeningQuotes('"'), ClosingBracket(']'), ClosingCurly('}'), ClosingQuotes('"')
}

sealed class KondorToken{
    abstract fun sameValueAs(text: String): Boolean
    abstract fun sameAs(separator: KondorSeparator): Boolean

    abstract val pos: Int
    abstract val desc: String
}

data class Separator(val sep: KondorSeparator, override val pos: Int): KondorToken() {
    override fun sameValueAs(text: String): Boolean = false

    override fun sameAs(separator: KondorSeparator): Boolean = sep == separator
    override val desc: String = sep.name
}

data class Value(val text: String, override val pos: Int) : KondorToken() {
    override fun sameValueAs(text: String): Boolean = this.text == text

    override fun sameAs(separator: KondorSeparator): Boolean = false
    override val desc: String = "'$text'"

}


data class TokensStream(private val iterator: PeekingIterator<KondorToken>) :
    PeekingIterator<KondorToken> by iterator {
        fun toList(): List<KondorToken> = iterator.asSequence().toList()
    }