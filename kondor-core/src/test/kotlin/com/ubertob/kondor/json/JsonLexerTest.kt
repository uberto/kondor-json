package com.ubertob.kondor.json

import JsonLexer
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class JsonLexerTest {

    @Test
    fun `single word`() {
        val json = "abc"
        val seq = JsonLexer(json).tokenize()

        expectThat(seq.asSequence().toList()).isEqualTo(listOf(Value(json)))
    }

    @Test
    fun `spaces tab and new lines word`() {
        val json = "  abc   def\ngh\tijk\r lmn \n\n opq"
        val seq = JsonLexer(json).tokenize()

        expectThat(seq.asSequence().toList()).isEqualTo(
            listOf(
                "abc", "def", "gh", "ijk", "lmn", "opq"
            ).map(::Value)
        )
    }

    @Test
    fun `json special tokens`() {
        val json = "[]{}:, \"\" [a,b,c]  {d:e}"
        val seq = JsonLexer(json).tokenize()

        expectThat(seq.asSequence().toList()).isEqualTo(
            listOf(
                OpeningBracket,
                ClosingBracket,
                OpeningCurly,
                ClosingCurly,
                Colon,
                Comma,
                OpeningQuotes,
                ClosingQuotes,
                OpeningBracket,
                Value("a"),
                Comma,
                Value("b"),
                Comma,
                Value("c"),
                ClosingBracket,
                OpeningCurly,
                Value("d"),
                Colon,
                Value("e"),
                ClosingCurly
            )
        )
    }

    @Test
    fun `json strings`() {
        val json = """
            { "abc": 123}
        """.trimIndent()
        val seq = JsonLexer(json).tokenize()

        expectThat(seq.asSequence().toList()).isEqualTo(
            listOf(
                OpeningCurly, OpeningQuotes, Value("abc"), ClosingQuotes, Colon, Value("123"), ClosingCurly
            )
        )
    }

    @Test
    fun `json strings with escapes`() {
        val json = """
            {"abc":"abc\"\\ \n}"}
        """.trimIndent()
        val seq = JsonLexer(json).tokenize()

        expectThat(seq.asSequence().toList()).isEqualTo(
            listOf(
                OpeningCurly, OpeningQuotes, Value("abc"), ClosingQuotes, Colon, OpeningQuotes, Value("abc\"\\ \n}"), ClosingQuotes, ClosingCurly
            )
        )
    }
}