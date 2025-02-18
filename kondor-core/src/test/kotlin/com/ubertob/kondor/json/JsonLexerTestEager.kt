package com.ubertob.kondor.json

import com.ubertob.kondor.json.parser.KondorTokenizer
import com.ubertob.kondor.json.parser.TokensStreamEager

class JsonLexerTestEager : JsonLexerTestAbstract() {
    override fun tokenize(jsonStr: String): JsonOutcome<TokensStreamEager> = KondorTokenizer.tokenize(jsonStr)
}