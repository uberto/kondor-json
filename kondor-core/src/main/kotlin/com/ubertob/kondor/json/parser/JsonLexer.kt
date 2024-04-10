package com.ubertob.kondor.json.parser

import com.ubertob.kondor.json.ChunkedStringWriter
import com.ubertob.kondor.json.ChunkedWriter
import com.ubertob.kondor.json.JsonOutcome
import com.ubertob.kondor.json.jsonnode.NodePathRoot
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
            val currToken = ChunkedStringWriter(256)
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

                                else -> currToken.write(char)
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

                            else -> currToken.write(char)
                        }

                        Escaping -> when (char) {
                            '\\' -> currToken.write('\\')
                            '"' -> currToken.write('\"')
                            'n' -> currToken.write('\n')
                            'f' -> currToken.write('\t')
                            't' -> currToken.write('\t')
                            'r' -> currToken.write('\r')
                            'b' -> currToken.write('\b')
                            'u' -> currToken.write("\\u") //technically Unicode shouldn't be escaped in Json since it's UTF-8 but since people insist on using it...
                            else -> error("wrongly escaped char '\\$char' inside a Json string")
                        }.also { state = InString }
                    }
                    currPos++
                }
            yieldValue(currToken, currPos)
        }.peekingIterator().let { TokensStream(it).asSuccess() }

    private suspend fun SequenceScope<KondorToken>.yieldValue(currWord: ChunkedWriter, pos: Int) {
        if (!currWord.isEmpty()) {
            val text = currWord.toString()
            yield(Value(text, pos - text.length))
            currWord.clear()
        }
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

    fun MutableList<KondorToken>.addValue(charWriter: ChunkedWriter, startPos: Int) {
        if (!charWriter.isEmpty()) {
            val text = charWriter.toString()
            add(Value(text, startPos - text.length))
            charWriter.clear()
        }
    }

    fun tokenize(): JsonOutcome<TokensStream> {
        var pos = 1
        val charWriter = ChunkedStringWriter(256)
        var state = OutString
        val tokens = ArrayList<KondorToken>(128)
        for (char in jsonStr) {
            when (state) {
                OutString ->
                    when (char) {
                        ' ', '\t', '\n', '\r', '\b' ->
                            tokens.addValue(charWriter, pos)
                        '{' -> {
                            tokens.addValue(charWriter, pos)
                            tokens.add(OpeningCurlySep)
                        }

                        '}' -> {
                            tokens.addValue(charWriter, pos)
                            tokens.add(ClosingCurlySep)
                        }

                        '[' -> {
                            tokens.addValue(charWriter, pos)
                            tokens.add(OpeningBracketSep)
                        }

                        ']' -> {
                            tokens.addValue(charWriter, pos)
                            tokens.add(ClosingBracketSep)
                        }

                        ',' -> {
                            tokens.addValue(charWriter, pos)
                            tokens.add(CommaSep)
                        }

                        ':' -> {
                            tokens.addValue(charWriter, pos)
                            tokens.add(ColonSep)
                        }

                        '"' -> {
                            tokens.addValue(charWriter, pos)
                            tokens.add(OpeningQuotesSep)
                            state = InString
                        }

                        else -> charWriter.write(char)
                    }

                InString -> when (char) {
                    '\\' -> {
                        state = Escaping
                    }

                    '"' -> {
                        tokens.addValue(charWriter, pos)
                        tokens.add(ClosingQuotesSep)
                        state = OutString
                    }

                    else -> charWriter.write(char)
                }

                Escaping -> when (char) {
                    '\\' -> charWriter.write('\\')
                    '"' -> charWriter.write('"')
                    '/' -> charWriter.write('/')
                    'n' -> charWriter.write('\n')
                    'f' -> charWriter.write('\t')
                    't' -> charWriter.write('\t')
                    'r' -> charWriter.write('\r')
                    'b' -> charWriter.write('\b')
                    'u' -> charWriter.write("\\u")
                    else -> return parsingFailure(
                        "a valid Json",
                        "wrongly escaped char '\\$char' inside a Json string after '${charWriter.takeLast(10)}'",
                        pos,
                        NodePathRoot,
                        "Invalid Json"
                    )
                }.also { state = InString }
            }
            pos++
        }
        tokens.addValue(charWriter, pos)
        return TokensStream(PeekingIteratorWrapper(tokens.iterator())).asSuccess()
    }
}

object KondorTokenizer {

    //faster but putting all in memory
    fun tokenize(jsonString: CharSequence): JsonOutcome<TokensStream> = JsonLexerEager(jsonString).tokenize()

    //a bit slower but consuming as little memory as possible
    fun tokenize(jsonStream: InputStream): JsonOutcome<TokensStream> = JsonLexerLazy(jsonStream).tokenize()
}
