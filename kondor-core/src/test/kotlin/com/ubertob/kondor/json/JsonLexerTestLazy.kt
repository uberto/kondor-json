package com.ubertob.kondor.json

import com.ubertob.kondor.json.parser.KondorTokenizer
import com.ubertob.kondor.json.parser.TokensStream
import java.io.ByteArrayInputStream
import java.nio.charset.Charset

class JsonLexerTestLazy : JsonLexerTestAbstract() {
    override fun tokenize(jsonStr: String): TokensStream =
        KondorTokenizer.tokenize(
            ByteArrayInputStream(jsonStr.toByteArray(Charset.forName("UTF-8")))
        )
}