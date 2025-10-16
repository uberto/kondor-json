package com.ubertob.kondor.json.parser

data class Location(val line: Int, val column: Int) {
    override fun toString(): String = "line $line, column $column"
}

enum class KondorSeparator(val sign: Char) {
    Colon(':'), Comma(','), OpeningBracket('['), OpeningCurly('{'), OpeningQuotes('"'), ClosingBracket(']'), ClosingCurly(
        '}'
    ),
    ClosingQuotes('"')
}

sealed class KondorToken {
    abstract val desc: String //used for error messages
    abstract val location: Location
}

sealed class Separator(val sep: KondorSeparator, override val location: Location) : KondorToken() {
    override val desc: String = sep.name
}

data class ColonSep(override val location: Location) : Separator(KondorSeparator.Colon, location)
data class CommaSep(override val location: Location) : Separator(KondorSeparator.Comma, location)
data class OpeningBracketSep(override val location: Location) : Separator(KondorSeparator.OpeningBracket, location)
data class OpeningCurlySep(override val location: Location) : Separator(KondorSeparator.OpeningCurly, location)
data class OpeningQuotesSep(override val location: Location) : Separator(KondorSeparator.OpeningQuotes, location)
data class ClosingBracketSep(override val location: Location) : Separator(KondorSeparator.ClosingBracket, location)
data class ClosingCurlySep(override val location: Location) : Separator(KondorSeparator.ClosingCurly, location)
data class ClosingQuotesSep(override val location: Location) : Separator(KondorSeparator.ClosingQuotes, location)


data class Value(val text: String, override val location: Location) : KondorToken() {
    override val desc: String = "'$text'"
}

data class TokensStream(private val iterator: PeekingIterator<KondorToken>) :
    PeekingIterator<KondorToken> by iterator {
    fun toList(): List<KondorToken> = iterator.asSequence().toList()
}
