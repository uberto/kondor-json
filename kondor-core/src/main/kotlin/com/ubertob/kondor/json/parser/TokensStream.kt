package com.ubertob.kondor.json.parser


interface TokensStream : Iterator<KondorToken> {
    fun peek(): KondorToken
    fun last(): KondorToken?
    fun lastPosRead(): Int
    fun toList(): List<KondorToken>
}