package com.ubertob.kondor.json

import com.ubertob.kondor.json.parser.KondorTokenizer
import com.ubertob.kondor.json.parser.TokensStreamEager
import java.io.ByteArrayInputStream
import java.nio.charset.Charset

class JsonLexerTestLazy : JsonLexerTestAbstract() {
    override fun tokenize(jsonStr: String): JsonOutcome<TokensStreamEager> =
        KondorTokenizer.tokenize(
            ByteArrayInputStream(jsonStr.toByteArray(Charset.forName("UTF-8")))
        )
}