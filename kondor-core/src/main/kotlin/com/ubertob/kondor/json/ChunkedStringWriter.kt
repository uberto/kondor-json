package com.ubertob.kondor.json

import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.io.OutputStreamWriter


interface StrAppendable {
    fun append(str: String): StrAppendable
    fun append(cbuf: CharArray, len: Int): StrAppendable
    fun append(c: Char): StrAppendable
}


class ChunkedStringWriter(val outputStream: OutputStream, val bufferSize: Int = 8192) : StrAppendable {

    private val stream = ByteArrayOutputStream(bufferSize)

    private val out = OutputStreamWriter(outputStream) //StringBuilder()
    private val charArray = CharArray(bufferSize)
    private var nextChar: Int = 0

    fun flushBuffer() {
        if (nextChar == 0) return
        out.write(charArray, 0, nextChar)
        nextChar = 0
    }


    private fun min(a: Int, b: Int): Int = if (a < b) a else b

    override fun append(cbuf: CharArray, len: Int): StrAppendable {
        if (len == 0) {
            return this
        }

        if (len >= bufferSize) {
            flushBuffer()
            out.write(cbuf)
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

    fun reset(): ChunkedStringWriter {
        nextChar = 0
        out.flush()
        stream.reset()

        return this
    }

    override fun append(str: String): StrAppendable {
        val len = str.length
        if (len == 0) {
            return this
        }
        if (len < bufferSize - nextChar) {
            str.toCharArray(charArray, nextChar, startIndex = 0, endIndex = len)
            nextChar += len
        } else {
            append(str.toCharArray(), len)
        }
        return this
    }

    override fun append(c: Char): StrAppendable {
        if (nextChar >= bufferSize) flushBuffer()
        charArray[nextChar++] = c
        return this
    }

    override fun toString(): String {
        flushBuffer()
        return out.toString()
    }


}