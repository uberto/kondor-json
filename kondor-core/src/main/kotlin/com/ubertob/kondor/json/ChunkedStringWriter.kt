package com.ubertob.kondor.json

class ChunkedStringWriter(val bufferSize: Int = 88192) : Appendable {

    private val out = StringBuilder()
    private val cb = CharArray(bufferSize)
    private var nextChar: Int = 0

    fun flushBuffer() {
        if (nextChar == 0) return
        out.appendRange(cb, 0, nextChar)
        nextChar = 0
    }

    private fun write(c: Char) {
        if (nextChar >= bufferSize) flushBuffer()
        cb[nextChar++] = c
    }

    private inline fun min(a: Int, b: Int): Int = if (a < b) a else b

    private fun write(cbuf: CharArray, len: Int) {
        if (len == 0) {
            return
        }

        if (len >= bufferSize) {
            flushBuffer()
            out.append(cbuf)
            return
        }

        var b = 0
        val t =  len
        while (b < t) {
            val d = min(bufferSize - nextChar, t - b)
            System.arraycopy(cbuf, b, cb, nextChar, d)
            b += d
            nextChar += d
            if (nextChar >= bufferSize) flushBuffer()
        }
    }

    fun reset(): ChunkedStringWriter {
        nextChar = 0
        out.clear()
        return this
    }

    override fun append(csq: CharSequence?): Appendable {
        val s = csq?.toString() ?: "null"
        write(s.toCharArray(), s.length)
        return this
    }

    override fun append(csq: CharSequence?, start: Int, end: Int): Appendable =
        append(csq?.subSequence(start, end) ?: "null")

    override fun append(c: Char): Appendable {
        write(c)
        return this
    }

    override fun toString(): String {
        flushBuffer()
        return out.toString()
    }


}