package com.ubertob.kondor.json


interface CharWriter {
    fun write(str: String): CharWriter
    fun write(cbuf: CharArray, len: Int): CharWriter
    fun write(c: Char): CharWriter
    fun clear(): CharWriter
    fun isEmpty(): Boolean
    fun takeLast(numberOfChar: Int): String
}


class ChunkedStringWriter(val bufferSize: Int = 8192) : CharWriter {

    private val out = StringBuilder()
    private val charArray = CharArray(bufferSize)
    private var nextChar: Int = 0

    fun flushBuffer() {
        if (nextChar == 0) return
        out.appendRange(charArray, 0, nextChar)
        nextChar = 0
    }

    private fun min(a: Int, b: Int): Int = if (a < b) a else b

    override fun write(cbuf: CharArray, len: Int): CharWriter {
        if (len == 0) {
            return this
        }

        if (len >= bufferSize) {
            flushBuffer()
            out.append(cbuf)
            return this
        }

        var b = 0
        val t = len
        while (b < t) {
            val d = min(bufferSize - nextChar, t - b)
            System.arraycopy(cbuf, b, charArray, nextChar, d)
            b += d
            nextChar += d
            if (nextChar >= bufferSize) flushBuffer()
        }
        return this
    }


    override fun write(str: String): CharWriter {
        val len = str.length
        if (len == 0) {
            return this
        }
        if (len < bufferSize - nextChar) {
            str.toCharArray(charArray, nextChar, startIndex = 0, endIndex = len)
            nextChar += len
        } else {
            write(str.toCharArray(), len)
        }
        return this
    }

    override fun write(c: Char): CharWriter {
        if (nextChar >= bufferSize) flushBuffer()
        charArray[nextChar++] = c
        return this
    }

    override fun isEmpty(): Boolean = nextChar == 0
    override fun takeLast(numberOfChar: Int): String =
        String(
            charArray.copyOfRange(
                if (nextChar < numberOfChar)
                    0
                else
                    nextChar - numberOfChar,
                nextChar
            )
        )

    override fun clear(): CharWriter {
        nextChar = 0
        out.clear()
        return this
    }

    override fun toString(): String {
        flushBuffer()
        return out.toString()
    }


}