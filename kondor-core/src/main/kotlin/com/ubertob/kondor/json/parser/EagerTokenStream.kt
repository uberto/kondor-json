package com.ubertob.kondor.json.parser
//
//class EagerTokenStream(private val tokens: List<KondorToken>) : TokensStream {
//    private var currentIndex = 0
//    private var lastToken: KondorToken? = null
//    private var currPos = 0
//
//    override fun hasNext(): Boolean = currentIndex < tokens.size
//
//    override fun next(): KondorToken {
//        if (!hasNext()) {
//            throw NoSuchElementException()
//        }
//        return tokens[currentIndex++].also {
//            lastToken = it
//            currPos = it.pos
//        }
//    }
//
//    override fun peek(): KondorToken {
//        if (!hasNext()) {
//            throw NoSuchElementException()
//        }
//        return tokens[currentIndex]
//    }
//
//    override fun last(): KondorToken? = lastToken
//
//    override fun lastPosRead(): Int = currPos
//
//    override fun getString(token: KondorToken.StringToken): String =
//        when (token) {
//            is KondorToken.StringToken -> token.text
//            else -> error("Invalid token type")
//        }
//
//    override fun getNumber(token: KondorToken.NumberToken): Number =
//        when (token) {
//            is KondorToken.NumberToken -> token.toNumber()
//            else -> error("Invalid token type")
//        }
//}