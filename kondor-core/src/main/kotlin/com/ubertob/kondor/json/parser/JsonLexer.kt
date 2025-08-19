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
    OutString, InString, Escaping, Unicode
}


class JsonLexerLazy(val inputStream: InputStream) {

    private var currPos = 1

    fun tokenize(): JsonOutcome<TokensStream> =
        sequence {
            val currToken = ChunkedStringWriter(256)
            var state = OutString
            var unicodeCharacterPointString = ""

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
                            '\\' -> currToken.write('\\').also { state = InString }
                            '/' -> currToken.write('/').also { state = InString }
                            '"' -> currToken.write('\"').also { state = InString }
                            'n' -> currToken.write('\n').also { state = InString }
                            'f' -> currToken.write('\t').also { state = InString }
                            't' -> currToken.write('\t').also { state = InString }
                            'r' -> currToken.write('\r').also { state = InString }
                            'b' -> currToken.write('\b').also { state = InString }
                            'u' -> {
                                state = Unicode
                            }
                            else -> error("wrongly escaped char '\\$char' inside a Json string")
                        }

                        Unicode -> {
                            unicodeCharacterPointString += char

                            if (unicodeCharacterPointString.length == 4) {
                                val unicodeChar = unicodeCharacterPointString.toIntOrNull(16)?.toChar()

                                if (unicodeChar == null) error("invalid unicode escape sequence '\\u${unicodeCharacterPointString}'")

                                currToken.write(unicodeChar)

                                unicodeCharacterPointString = ""
                                state = InString
                            }
                        }
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
        var unicodeCharacterPointString = ""
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
                    '\\' -> charWriter.write('\\').also { state = InString }
                    '"' -> charWriter.write('"').also { state = InString }
                    '/' -> charWriter.write('/').also { state = InString }
                    'n' -> charWriter.write('\n').also { state = InString }
                    'f' -> charWriter.write('\t').also { state = InString }
                    't' -> charWriter.write('\t').also { state = InString }
                    'r' -> charWriter.write('\r').also { state = InString }
                    'b' -> charWriter.write('\b').also { state = InString }
                    'u' -> state = Unicode
                    else -> return parsingFailure(
                        "a valid Json",
                        "wrongly escaped char '\\$char' inside a Json string after '${charWriter.takeLast(10)}'",
                        pos,
                        NodePathRoot,
                        "Invalid Json"
                    ).also { state = InString }
                }

                Unicode -> {
                    unicodeCharacterPointString += char

                    if (unicodeCharacterPointString.length == 4) {
                        val unicodeChar = unicodeCharacterPointString.toIntOrNull(16)?.toChar()

                        if (unicodeChar == null) {
                            return parsingFailure(
                                "a valid Json",
                                "invalid unicode escape sequence '\\u${unicodeCharacterPointString}' after '${
                                    charWriter.takeLast(
                                        10
                                    )
                                }'",
                                pos,
                                NodePathRoot,
                                "Invalid Json"
                            )
                        }

                        charWriter.write(unicodeChar)
                        unicodeCharacterPointString = ""
                        state = InString
                    }
                }
            }
            pos++
        }
        tokens.addValue(charWriter, pos)
        return TokensStream(PeekingIteratorWrapper(tokens.iterator())).asSuccess()
    }
}

object KondorTokenizer {
    //!!!look at Jacksons ReaderBasedJsonParser for a fast lazy tokenizer

    //faster but putting all in memory
    fun tokenize(jsonString: CharSequence): JsonOutcome<TokensStream> = JsonLexerEager(jsonString).tokenize()

    //a bit slower but consuming as little memory as possible
    fun tokenize(jsonStream: InputStream): JsonOutcome<TokensStream> = JsonLexerLazy(jsonStream).tokenize()
}
