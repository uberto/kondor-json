package com.ubertob.kondor.jsonSimplified

import java.util.concurrent.atomic.AtomicInteger

enum class LexerState {
    OutString, InString, Escaping
}

class JsonLexer(val jsonStr: CharSequence) {

    fun tokenize(): TokensStream =
        sequence {
            val currToken = StringBuilder()
            var state = LexerState.OutString
            jsonStr.forEach { char ->
                when (state) {
                    LexerState.OutString ->
                        when (char) {
                            ' ', '\t', '\n', '\r', '\b' -> yieldValue(currToken)
                            '{' -> {
                                yieldValue(currToken)
                                yield(OpeningCurly)
                            }
                            '}' -> {
                                yieldValue(currToken)
                                yield(ClosingCurly)
                            }
                            '[' -> {
                                yieldValue(currToken)
                                yield(OpeningBracket)
                            }
                            ']' -> {
                                yieldValue(currToken)
                                yield(ClosingBracket)
                            }
                            ',' -> {
                                yieldValue(currToken)
                                yield(Comma)
                            }
                            ':' -> {
                                yieldValue(currToken)
                                yield(Colon)
                            }
                            '"' -> {
                                yieldValue(currToken)
                                yield(OpeningQuotes)
                                state = LexerState.InString
                            }
                            else -> currToken.append(char)
                        }

                    LexerState.InString -> when (char) {
                        '\\' -> {
                            state = LexerState.Escaping
                        }
                        '"' -> {
                            yieldValue(currToken)
                            yield(ClosingQuotes)
                            state = LexerState.OutString
                        }
                        else -> currToken += char
                    }
                    LexerState.Escaping -> when (char) {
                        '\\' -> currToken += '\\'
                        'n' -> currToken += '\n'
                        'f' -> currToken += '\t'
                        't' -> currToken += '\t'
                        'r' -> currToken += '\r'
                        'b' -> currToken += '\b'
                        '"' -> currToken += '\"'
                        else -> error("wrongly escaped char '\\$char' in Json string")
                    }.also { state = LexerState.InString }
                }
            }
            yieldValue(currToken)
        }.peekingIterator().let { TokensStream(it) }

    private suspend fun SequenceScope<KondorToken>.yieldValue(currWord: StringBuilder) {
        if (currWord.isNotEmpty()) {
            yield(Value(currWord.toString()))
        }
        currWord.clear()
    }

    companion object {
        fun tokenize(jsonString: CharSequence): TokensStream = JsonLexer(jsonString).tokenize()
    }
}

operator fun StringBuilder.plusAssign(c: Char) {
    append(c)
}