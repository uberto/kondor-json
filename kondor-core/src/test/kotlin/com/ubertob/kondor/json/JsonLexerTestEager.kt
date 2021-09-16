package com.ubertob.kondor.json

import com.ubertob.kondor.json.parser.KondorTokenizer
import com.ubertob.kondor.json.parser.TokensStream

class JsonLexerTestEager : JsonLexerTestAbstract() {
    override fun tokenize(jsonStr: String): TokensStream = KondorTokenizer.tokenize(jsonStr)
}