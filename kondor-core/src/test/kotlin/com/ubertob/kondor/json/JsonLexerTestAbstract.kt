package com.ubertob.kondor.json

import com.ubertob.kondor.json.parser.TokensStream
import com.ubertob.kondor.json.parser.ValueToken
import com.ubertob.kondortools.expectSuccess
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

abstract class JsonLexerTestAbstract {

    abstract fun tokenize(jsonStr: String): JsonOutcome<TokensStream>

    @Test
    fun `single word`() {
        val json = """"abc""""
        val tokensStream = tokenize(json).expectSuccess()

        expectThat(tokensStream.toDesc()).isEqualTo(
            listOf(
                "'abc'@1"
            )
        )
    }

    @Test
    fun `spaces tab and new lines word`() {
        val json = "  abc   def\ngh\tijk\r lmn \n\n opq"
        val tokens = tokenize(json).expectSuccess()

        expectThat(tokens.toDesc()).isEqualTo(
            listOf(
                "'abc'@3", "'def'@9", "'gh'@13", "'ijk'@16", "'lmn'@21", "'opq'@28"
            )
        )
    }

    @Test
    fun `json special tokens`() {
        val json = "[]{}:, \"\" [a,b,c]  {d:e}"
        val tokens = tokenize(json).expectSuccess()

        expectThat(tokens.toDesc()).isEqualTo(
            listOf(
                "OpeningSquare",
                "ClosingSquare",
                "OpeningCurly",
                "ClosingCurly",
                "Colon",
                "Comma",
                "OpeningQuotes",
                "ClosingQuotes",
                "OpeningSquare",
                "'a'@12",
                "Comma",
                "'b'@14",
                "Comma",
                "'c'@16",
                "ClosingSquare",
                "OpeningCurly",
                "'d'@21",
                "Colon",
                "'e'@23",
                "ClosingCurly"
            )
        )
    }

    @Test
    fun `json strings`() {
        val json = """
            { "abc": 123}
        """.trimIndent()
        val tokens = tokenize(json).expectSuccess()

        val kondorTokens = tokens.toDesc()
        expectThat(kondorTokens).isEqualTo(
            listOf("OpeningCurly", "OpeningQuotes", "'abc'@4", "ClosingQuotes", "Colon", "'123'@10", "ClosingCurly")
        )
    }


    @Test
    fun `json strings with escapes`() {
        val json = """
            {"abc":"abc\"\\ \n}"}
        """.trimIndent()
        val tokens = tokenize(json).expectSuccess()

        expectThat(tokens.toDesc()).isEqualTo(
            listOf(
                "OpeningCurly", "OpeningQuotes", "'abc'@3", "ClosingQuotes", "Colon", "OpeningQuotes", """'abc"\ 
}'@12""", "ClosingQuotes", "ClosingCurly"
            )
        )
    }


    @Test
    fun `json strings with unicode`() {
        val json = """
            "abc \u263A"
        """.trimIndent()
        val tokens = tokenize(json).expectSuccess()

        expectThat(tokens.toDesc()).isEqualTo(
            listOf(
                "OpeningQuotes", "'abc \\u263A'@2", "ClosingQuotes"
            )
        )
    }

    private fun TokensStream.toDesc() = toList().map {
        when (it) {
            is ValueToken -> "${it.desc}@${it.pos}"
            else -> it.desc
        }
    }
}