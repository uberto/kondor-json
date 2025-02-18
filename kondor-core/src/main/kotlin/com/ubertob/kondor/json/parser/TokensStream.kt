package com.ubertob.kondor.json.parser


interface TokensStream : Iterator<KondorToken> {
    fun peek(): KondorToken
    fun last(): KondorToken?
    fun getString(token: ValueToken): String
    fun lastPosRead(): Int
} 