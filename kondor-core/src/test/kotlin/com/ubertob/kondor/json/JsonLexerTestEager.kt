package com.ubertob.kondor.json

import com.ubertob.kondor.json.parser.KondorTokenizer
import com.ubertob.kondor.json.parser.TokensStreamIter

class JsonLexerTestEager : JsonLexerTestAbstract() {
    override fun tokenize(jsonStr: String): JsonOutcome<TokensStreamIter> = KondorTokenizer.tokenize(jsonStr)
}