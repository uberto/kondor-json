package com.ubertob.kondor.json.parser

interface PeekingIterator<T> : Iterator<T> {
    fun peek(): T
    fun last(): T?
}

class PeekingIteratorWrapper<T>(val innerIterator: Iterator<T>) : PeekingIterator<T> {

    private var next: T? = null
    private var last: T? = null

    override fun peek(): T = next ?: run {
        val nn = innerIterator.next()
        next = nn
        nn
    }

    override fun hasNext(): Boolean = next != null || innerIterator.hasNext()

    override fun next(): T = (next ?: advanceIterator()).also {
        last = it
        next = null
    }

    private fun advanceIterator(): T =
        if (innerIterator.hasNext())
            innerIterator.next()
        else
            error("unexpected end of file")

    override fun last(): T? = next ?: last  //last seen

}

fun <T> Sequence<T>.peekingIterator(): PeekingIterator<T> = PeekingIteratorWrapper(iterator())