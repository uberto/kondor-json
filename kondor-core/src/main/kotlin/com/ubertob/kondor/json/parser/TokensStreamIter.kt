package com.ubertob.kondor.json.parser

data class TokensStreamIter(private val iterator: PeekingIterator<KondorToken>) :
    TokensStream {
    override fun toList(): List<KondorToken> = iterator.asSequence().toList()

    private var currPos = 0

    override fun lastPosRead(): Int = currPos

    override fun hasNext(): Boolean = iterator.hasNext()

    override fun peek(): KondorToken = iterator.peek()

    override fun last(): KondorToken? = iterator.last()

    override fun next(): KondorToken =
        iterator.next().also {
            currPos = when (it) {
                is SeparatorToken -> currPos + 1
                is ValueTokenEager -> it.pos + it.text.length - 1
                else -> currPos + 1
            }
        }

}