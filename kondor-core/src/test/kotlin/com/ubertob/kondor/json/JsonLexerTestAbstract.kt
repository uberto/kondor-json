package com.ubertob.kondor.json

import com.ubertob.kondor.json.parser.*
import com.ubertob.kondortools.expectSuccess
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

abstract class JsonLexerTestAbstract {

    abstract fun tokenize(jsonStr: String): JsonOutcome<TokensStream>

    @Test
    fun `single word`() {
        val json = "abc"
        val tokensStream = tokenize(json).expectSuccess()

        expectThat(tokensStream.toList()).isEqualTo(listOf(Value(json, 1)))
    }

    @Test
    fun `spaces tab and new lines word`() {
        val json = "  abc   def\ngh\tijk\r lmn \n\n opq"
        val tokens = tokenize(json).expectSuccess()

        expectThat(tokens.toList()).isEqualTo(
            listOf(
                Value("abc", 3),
                Value("def", 9),
                Value("gh", 13),
                Value("ijk", 16),
                Value("lmn", 21),
                Value("opq", 28)
            )
        )
    }

    @Test
    fun `json special tokens`() {
        val json = "[]{}:, \"\" [a,b,c]  {d:e}"
        val tokens = tokenize(json).expectSuccess()

        expectThat(tokens.toList()).isEqualTo(
            listOf(
                OpeningBracketSep,
                ClosingBracketSep,
                OpeningCurlySep,
                ClosingCurlySep,
                ColonSep,
                CommaSep,
                OpeningQuotesSep,
                ClosingQuotesSep,
                OpeningBracketSep,
                Value("a", 12),
                CommaSep,
                Value("b", 14),
                CommaSep,
                Value("c", 16),
                ClosingBracketSep,
                OpeningCurlySep,
                Value("d", 21),
                ColonSep,
                Value("e", 23),
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
                Value("abc", 4),
                ClosingQuotesSep,
                ColonSep,
                Value("123", 10),
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
                Value("abc", 3),
                ClosingQuotesSep,
                ColonSep,
                OpeningQuotesSep,
                Value("abc\"\\ \n}", 12),
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
                Value("abc \\u263A", 2),
                ClosingQuotesSep,
            )
        )
    }

}