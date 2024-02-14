package com.ubertob.kondor.json.parser

import com.ubertob.kondor.json.JsonOutcome
import com.ubertob.kondor.json.jsonnode.NodePathRoot
import com.ubertob.kondor.json.parser.KondorSeparator.*
import com.ubertob.kondor.json.parser.LexerState.*
import com.ubertob.kondor.outcome.asSuccess
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset


enum class LexerState {
    OutString, InString, Escaping
}


class JsonLexerLazy(val inputStream: InputStream) {

    private var currPos = 1

    fun tokenize(): JsonOutcome<TokensStream> =
        sequence {
            val currToken = StringBuilder()
            var state = OutString

            inputStream
                .reader(Charset.forName("UTF-8"))
                .forEach { char ->
                    when (state) {
                        OutString ->
                            when (char) {
                                ' ', '\t', '\n', '\r', '\b' -> yieldValue(currToken, currPos)
                                '{' -> {
                                    yieldValue(currToken, currPos)
                                    yield(OpeningCurlySep)
                                }

                                '}' -> {
                                    yieldValue(currToken, currPos)
                                    yield(ClosingCurlySep)
                                }

                                '[' -> {
                                    yieldValue(currToken, currPos)
                                    yield(OpeningBracketSep)
                                }

                                ']' -> {
                                    yieldValue(currToken, currPos)
                                    yield(ClosingBracketSep)
                                }

                                ',' -> {
                                    yieldValue(currToken, currPos)
                                    yield(CommaSep)
                                }

                                ':' -> {
                                    yieldValue(currToken, currPos)
                                    yield(ColonSep)
                                }

                                '"' -> {
                                    yieldValue(currToken, currPos)
                                    yield(OpeningQuotesSep)
                                    state = InString
                                }

                                else -> currToken.append(char)
                            }

                        InString -> when (char) {
                            '\\' -> {
                                state = Escaping
                            }

                            '"' -> {
                                yieldValue(currToken, currPos)
                                yield(ClosingQuotesSep)
                                state = OutString
                            }

                            else -> currToken += char
                        }

                        Escaping -> when (char) {
                            '\\' -> currToken.append('\\')
                            '"' -> currToken.append('\"')
                            'n' -> currToken.append('\n')
                            'f' -> currToken.append('\t')
                            't' -> currToken.append('\t')
                            'r' -> currToken.append('\r')
                            'b' -> currToken.append('\b')
                            'u' -> currToken.append("\\u") //technically Unicode shouldn't be escaped in Json since it's UTF-8 but since people insist on using it...
                            else -> error("wrongly escaped char '\\$char' inside a Json string")
                        }.also { state = InString }
                    }
                    currPos++
                }
            yieldValue(currToken, currPos)
        }.peekingIterator().let { TokensStream(it).asSuccess() }

    private suspend fun SequenceScope<KondorToken>.yieldValue(currWord: StringBuilder, pos: Int) {
        if (currWord.isNotEmpty()) {
            val text = currWord.toString()
            yield(Value(text, pos - text.length))
        }
        currWord.clear()
    }
}

private inline fun InputStreamReader.forEach(block: (Char) -> Unit) =
    use {
        var c = read()
        while (c >= 0) {
            block(c.toChar())
            c = read()
        }
    }


operator fun StringBuilder.plusAssign(c: Char) {
    append(c)
}


class JsonLexerEager(val jsonStr: CharSequence) {

    fun MutableList<KondorToken>.addValue(currWord: StringBuilder, startPos: Int) {
        if (currWord.isNotEmpty()) {
            val text = currWord.toString()
            add(Value(text, startPos - text.length))
        }
        currWord.clear()
    }

    fun tokenize(): JsonOutcome<TokensStream> {
        var pos = 1
        val currToken = StringBuilder()
        var state = OutString
        val tokens = mutableListOf<KondorToken>()
        for (char in jsonStr) {
            when (state) {
                OutString ->
                    when (char) {
                        ' ', '\t', '\n', '\r', '\b' -> tokens.addValue(currToken, pos)
                        '{' -> {
                            tokens.addValue(currToken, pos)
                            tokens.add(OpeningCurlySep)
                        }

                        '}' -> {
                            tokens.addValue(currToken, pos)
                            tokens.add(ClosingCurlySep)
                        }

                        '[' -> {
                            tokens.addValue(currToken, pos)
                            tokens.add(OpeningBracketSep)
                        }

                        ']' -> {
                            tokens.addValue(currToken, pos)
                            tokens.add(ClosingBracketSep)
                        }

                        ',' -> {
                            tokens.addValue(currToken, pos)
                            tokens.add(CommaSep)
                        }

                        ':' -> {
                            tokens.addValue(currToken, pos)
                            tokens.add(ColonSep)
                        }

                        '"' -> {
                            tokens.addValue(currToken, pos)
                            tokens.add(OpeningQuotesSep)
                            state = InString
                        }

                        else -> currToken.append(char)
                    }

                InString -> when (char) {
                    '\\' -> {
                        state = Escaping
                    }

                    '"' -> {
                        tokens.addValue(currToken, pos)
                        tokens.add(ClosingQuotesSep)
                        state = OutString
                    }

                    else -> currToken += char
                }

                Escaping -> when (char) {
                    '\\' -> currToken.append('\\')
                    '"' -> currToken.append('\"')
                    '/' -> currToken.append('/')
                    'n' -> currToken.append('\n')
                    'f' -> currToken.append('\t')
                    't' -> currToken.append('\t')
                    'r' -> currToken.append('\r')
                    'b' -> currToken.append('\b')
                    'u' -> currToken.append("\\u")
                    else -> return parsingFailure(
                        "a valid Json",
                        "wrongly escaped char '\\$char' inside a Json string after '${currToken.takeLast(10)}'",
                        pos,
                        NodePathRoot,
                        "Invalid Json"
                    )
                }.also { state = InString }
            }
            pos++
        }
        tokens.addValue(currToken, pos)
        return TokensStream(PeekingIteratorWrapper(tokens.iterator())).asSuccess()
    }
}

object KondorTokenizer {

    //faster but putting all in memory
    fun tokenize(jsonString: CharSequence): JsonOutcome<TokensStream> = JsonLexerEager(jsonString).tokenize()

    //a bit slower but consuming as little memory as possible
    fun tokenize(jsonStream: InputStream): JsonOutcome<TokensStream> = JsonLexerLazy(jsonStream).tokenize()
}
