package com.ubertob.kondor.jsonSimplified


sealed class KondorToken
object ClosingBracket : KondorToken() {
    override fun toString(): String = "]"
}

object ClosingCurly : KondorToken() {
    override fun toString(): String = "}"
}

object ClosingQuotes : KondorToken() {
    override fun toString(): String = "closing quotes"
}

object Colon : KondorToken() {
    override fun toString(): String = ":"
}

object Comma : KondorToken() {
    override fun toString(): String = ","
}

object OpeningBracket : KondorToken() {
    override fun toString(): String = "["
}

object OpeningCurly : KondorToken() {
    override fun toString(): String = "{"
}

object OpeningQuotes : KondorToken() {
    override fun toString(): String = "opening quotes"
}

data class Value(val text: String) : KondorToken() {
    override fun toString(): String = text
}


data class TokensStream(private val iterator: PeekingIterator<KondorToken>) :
    PeekingIterator<KondorToken> by iterator