package com.ubertob.kondor.json

import com.ubertob.kondor.json.parser.*
import com.ubertob.kondortools.expectFailure
import com.ubertob.kondortools.expectSuccess
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo

abstract class JsonLexerTestAbstract {

    abstract fun tokenize(jsonStr: String): JsonOutcome<TokensStream>

    @Test
    fun `single word`() {
        val json = "abc"
        val tokensStream = tokenize(json).expectSuccess()

        expectThat(tokensStream.toList()).isEqualTo(listOf(Value(json, Location(1, 1))))
    }

    @Test
    fun `spaces tab and new lines word`() {
        val json = "  abc   def\ngh\tijk\r lmn \n\n opq"
        val tokens = tokenize(json).expectSuccess()

        expectThat(tokens.toList()).isEqualTo(
            listOf(
                Value("abc", Location(1, 3)),
                Value("def", Location(1, 9)),
                Value("gh", Location(2, 1)),
                Value("ijk", Location(2, 4)),
                Value("lmn", Location(2, 9)),
                Value("opq", Location(4, 2))
            )
        )
    }

    @Test
    fun `json special tokens`() {
        val json = "[]{}:, \"\" [a,b,c]  {d:e}"
        val tokens = tokenize(json).expectSuccess()

        expectThat(tokens.toList()).isEqualTo(
            listOf(
                OpeningBracketSep(Location(1, 1)),
                ClosingBracketSep(Location(1, 2)),
                OpeningCurlySep(Location(1, 3)),
                ClosingCurlySep(Location(1, 4)),
                ColonSep(Location(1, 5)),
                CommaSep(Location(1, 6)),
                OpeningQuotesSep(Location(1, 8)),
                ClosingQuotesSep(Location(1, 9)),
                OpeningBracketSep(Location(1, 11)),
                Value("a", Location(1, 12)),
                CommaSep(Location(1, 13)),
                Value("b", Location(1, 14)),
                CommaSep(Location(1, 15)),
                Value("c", Location(1, 16)),
                ClosingBracketSep(Location(1, 17)),
                OpeningCurlySep(Location(1, 20)),
                Value("d", Location(1, 21)),
                ColonSep(Location(1, 22)),
                Value("e", Location(1, 23)),
                ClosingCurlySep(Location(1, 24))
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
                OpeningCurlySep(Location(1, 1)),
                OpeningQuotesSep(Location(1, 3)),
                Value("abc", Location(1, 4)),
                ClosingQuotesSep(Location(1, 7)),
                ColonSep(Location(1, 8)),
                Value("123", Location(1, 10)),
                ClosingCurlySep(Location(1, 13))
            )
        )
    }

    @Test
    fun `json strings with escapes`() {
        val json = """
            {"abc":"abc\"\\ \n\/}"}
        """.trimIndent()
        val tokens = tokenize(json).expectSuccess()

        expectThat(tokens.toList()).isEqualTo(
            listOf(
                OpeningCurlySep(Location(1, 1)),
                OpeningQuotesSep(Location(1, 2)),
                Value("abc", Location(1, 3)),
                ClosingQuotesSep(Location(1, 6)),
                ColonSep(Location(1, 7)),
                OpeningQuotesSep(Location(1, 8)),
                Value("abc\"\\ \n/}", Location(1, 9)),
                ClosingQuotesSep(Location(1, 22)),
                ClosingCurlySep(Location(1, 23))
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
                OpeningQuotesSep(Location(1, 1)),
                Value("abc \u263A", Location(1, 2)),
                ClosingQuotesSep(Location(1, 12)),
            )
        )
    }
}
