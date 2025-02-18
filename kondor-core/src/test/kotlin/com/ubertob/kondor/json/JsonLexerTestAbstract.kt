package com.ubertob.kondor.json

import com.ubertob.kondor.json.parser.*
import com.ubertob.kondortools.expectSuccess
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

abstract class JsonLexerTestAbstract {

    abstract fun tokenize(jsonStr: String): JsonOutcome<TokensStreamIter>

    @Test
    fun `single word`() {
        val json = "abc"
        val tokensStream = tokenize(json).expectSuccess()

        expectThat(tokensStream.toList()).isEqualTo(listOf(ValueTokenEager(json, 1)))
    }

    @Test
    fun `spaces tab and new lines word`() {
        val json = "  abc   def\ngh\tijk\r lmn \n\n opq"
        val tokens = tokenize(json).expectSuccess()

        expectThat(tokens.toList()).isEqualTo(
            listOf(
                ValueTokenEager("abc", 3),
                ValueTokenEager("def", 9),
                ValueTokenEager("gh", 13),
                ValueTokenEager("ijk", 16),
                ValueTokenEager("lmn", 21),
                ValueTokenEager("opq", 28)
            )
        )
    }

    @Test
    fun `json special tokens`() {
        val json = "[]{}:, \"\" [a,b,c]  {d:e}"
        val tokens = tokenize(json).expectSuccess()

        expectThat(tokens.toList()).isEqualTo(
            listOf(
                OpeningSquareSep,
                ClosingSquareSep,
                OpeningCurlySep,
                ClosingCurlySep,
                ColonSep,
                CommaSep,
                OpeningQuotesSep,
                ClosingQuotesSep,
                OpeningSquareSep,
                ValueTokenEager("a", 12),
                CommaSep,
                ValueTokenEager("b", 14),
                CommaSep,
                ValueTokenEager("c", 16),
                ClosingSquareSep,
                OpeningCurlySep,
                ValueTokenEager("d", 21),
                ColonSep,
                ValueTokenEager("e", 23),
                ClosingCurlySep
            )
        )
    }

    @Test
    fun `json strings`() {
        val json = """
            { "abc": 123}
        """.trimIndent()
        val tokens = tokenize(json).expectSuccess()

        expectThat(tokens.toList()).isEqualTo(
            listOf(
                OpeningCurlySep,
                OpeningQuotesSep,
                ValueTokenEager("abc", 4),
                ClosingQuotesSep,
                ColonSep,
                ValueTokenEager("123", 10),
                ClosingCurlySep
            )
        )
    }

    @Test
    fun `json strings with escapes`() {
        val json = """
            {"abc":"abc\"\\ \n}"}
        """.trimIndent()
        val tokens = tokenize(json).expectSuccess()

        expectThat(tokens.toList()).isEqualTo(
            listOf(
                OpeningCurlySep,
                OpeningQuotesSep,
                ValueTokenEager("abc", 3),
                ClosingQuotesSep,
                ColonSep,
                OpeningQuotesSep,
                ValueTokenEager("abc\"\\ \n}", 12),
                ClosingQuotesSep,
                ClosingCurlySep
            )
        )
    }


    @Test
    fun `json strings with unicode`() {
        val json = """
            "abc \u263A"
        """.trimIndent()
        val tokens = tokenize(json).expectSuccess()

        expectThat(tokens.toList()).isEqualTo(
            listOf(
                OpeningQuotesSep,
                ValueTokenEager("abc \\u263A", 2),
                ClosingQuotesSep,
            )
        )
    }

}