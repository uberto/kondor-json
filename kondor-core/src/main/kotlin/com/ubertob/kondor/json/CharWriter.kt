package com.ubertob.kondor.json

interface CharWriter {
    fun write(str: String): CharWriter
    fun write(cbuf: CharArray, len: Int): CharWriter
    fun write(c: Char): CharWriter

}