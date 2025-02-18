package com.ubertob.kondor.json.parser

class LazyTokenStream(
    private val tokenGenerator: () -> KondorToken?
) : PeekingIterator<KondorToken> {
    private var nextToken: KondorToken? = null
    private var lastToken: KondorToken? = null
    private var consumed = true

    override fun hasNext(): Boolean {
        if (consumed) {
            nextToken = tokenGenerator()
            consumed = false
        }
        return nextToken != null
    }

    override fun next(): KondorToken {
        if (!hasNext()) {
            throw NoSuchElementException()
        }
        consumed = true
        return nextToken!!.also { lastToken = it }
    }

    override fun peek(): KondorToken {
        if (!hasNext()) {
            throw NoSuchElementException()
        }
        return nextToken!!
    }

    override fun last(): KondorToken? = lastToken

}