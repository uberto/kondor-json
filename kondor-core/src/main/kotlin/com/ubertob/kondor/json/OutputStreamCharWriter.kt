package com.ubertob.kondor.json

import java.io.OutputStream

data class OutputStreamCharWriter(private val out: OutputStream) : CharWriter {
    override fun write(str: String): CharWriter {
        out.write(str.toByteArray())
        return this
    }

    override fun write(cbuf: CharArray, len: Int): CharWriter {
        for (c in cbuf) {
            write(c)
        }
        return this
    }

    override fun write(c: Char): CharWriter {
        out.write(c.code)
        return this
    }

}