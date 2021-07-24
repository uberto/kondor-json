package com.ubertob.kondor.json.parser

import com.ubertob.kondor.json.parser.LexerState.*
import java.util.concurrent.atomic.AtomicInteger


enum class LexerState {
    OutString, InString, Escaping
}

class JsonLexerLazy(val jsonStr: CharSequence) {

    private val pos = AtomicInteger(0)

    fun tokenize(): TokensStream =
        sequence {
            val currToken = StringBuilder()
            var state = OutString
            jsonStr.forEach { char ->
                pos.incrementAndGet()
                when (state) {
                    OutString ->
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
                                state = InString
                            }
                            else -> currToken.append(char)
                        }

                    InString -> when (char) {
                        '\\' -> {
                            state = Escaping
                        }
                        '"' -> {
                            yieldValue(currToken)
                            yield(ClosingQuotes)
                            state = OutString
                        }
                        else -> currToken += char
                    }
                    Escaping -> when (char) {
                        '\\' -> currToken += '\\'
                        'n' -> currToken += '\n'
                        'f' -> currToken += '\t'
                        't' -> currToken += '\t'
                        'r' -> currToken += '\r'
                        'b' -> currToken += '\b'
                        '"' -> currToken += '\"'
                        else -> error("wrongly escaped char '\\$char' in Json string")
                    }.also { state = InString }
                }
            }
            yieldValue(currToken)
        }.peekingIterator().let { TokensStream(pos::get, it) }

    private suspend fun SequenceScope<KondorToken>.yieldValue(currWord: StringBuilder) {
        if (currWord.isNotEmpty()) {
            yield(Value(currWord.toString()))
        }
        currWord.clear()
    }

}

operator fun StringBuilder.plusAssign(c: Char) {
    append(c)
}



class JsonLexerEager(val jsonStr: CharSequence) {

    private val pos = AtomicInteger(0)

    fun MutableList<KondorToken>.addValue(currWord: StringBuilder) {
        if (currWord.isNotEmpty()) {
            add(Value(currWord.toString()))
        }
        currWord.clear()
    }

    //todo extract the logic of tokenization
    fun tokenize(): TokensStream {
        val currToken = StringBuilder()
        var state = OutString
        val tokens = mutableListOf<KondorToken>()
        jsonStr.forEach { char ->
            pos.incrementAndGet()
            when (state) {
                OutString ->
                    when (char) {
                        ' ', '\t', '\n', '\r', '\b' -> tokens.addValue(currToken)
                        '{' -> {
                            tokens.addValue(currToken)
                            tokens.add(OpeningCurly)
                        }
                        '}' -> {
                            tokens.addValue(currToken)
                            tokens.add(ClosingCurly)
                        }
                        '[' -> {
                            tokens.addValue(currToken)
                            tokens.add(OpeningBracket)
                        }
                        ']' -> {
                            tokens.addValue(currToken)
                            tokens.add(ClosingBracket)
                        }
                        ',' -> {
                            tokens.addValue(currToken)
                            tokens.add(Comma)
                        }
                        ':' -> {
                            tokens.addValue(currToken)
                            tokens.add(Colon)
                        }
                        '"' -> {
                            tokens.addValue(currToken)
                            tokens.add(OpeningQuotes)
                            state = InString
                        }
                        else -> currToken.append(char)
                    }

                InString -> when (char) {
                    '\\' -> {
                        state = Escaping
                    }
                    '"' -> {
                        tokens.addValue(currToken)
                        tokens.add(ClosingQuotes)
                        state = OutString
                    }
                    else -> currToken += char
                }
                Escaping -> when (char) {
                    '\\' -> currToken += '\\'
                    'n' -> currToken += '\n'
                    'f' -> currToken += '\t'
                    't' -> currToken += '\t'
                    'r' -> currToken += '\r'
                    'b' -> currToken += '\b'
                    '"' -> currToken += '\"'
                    else -> error("wrongly escaped char '\\$char' in Json string")
                }.also { state = InString }
            }
        }
        tokens.addValue(currToken)

    return  TokensStream(pos::get,  PeekingIteratorWrapper(tokens.iterator()))

}

}

object KondorTokenizer {
    fun tokenize(jsonString: CharSequence): TokensStream = JsonLexerEager(jsonString).tokenize()
}


