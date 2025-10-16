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

    private companion object {
        const val BUFFER_SIZE = 8192
    }

    fun tokenize(): JsonOutcome<TokensStream> =
        sequence {
            val currToken = ChunkedStringWriter(256)
            var state = OutString
            var unicodeCharacterPointString = ""
            var currLine = 1
            var currColumn = 1
            var tokenStartLine = 1
            var tokenStartColumn = 1

            val reader = inputStream.reader(Charset.forName("UTF-8"))
            val buffer = CharArray(BUFFER_SIZE)

            reader.use {
                var charsRead = reader.read(buffer)
                while (charsRead > 0) {
                    var bufferPos = 0
                    while (bufferPos < charsRead) {
                        val char = buffer[bufferPos]
                        bufferPos++

                        when (state) {
                            OutString ->
                                when (char) {
                                    ' ', '\t', '\r', '\b' -> yieldValue(currToken, tokenStartLine, tokenStartColumn)
                                    '\n' -> {
                                        yieldValue(currToken, tokenStartLine, tokenStartColumn)
                                        currLine++
                                        currColumn = 0
                                    }
                                    '{' -> {
                                        yieldValue(currToken, tokenStartLine, tokenStartColumn)
                                        yield(OpeningCurlySep(Location(currLine, currColumn)))
                                    }

                                    '}' -> {
                                        yieldValue(currToken, tokenStartLine, tokenStartColumn)
                                        yield(ClosingCurlySep(Location(currLine, currColumn)))
                                    }

                                    '[' -> {
                                        yieldValue(currToken, tokenStartLine, tokenStartColumn)
                                        yield(OpeningBracketSep(Location(currLine, currColumn)))
                                    }

                                    ']' -> {
                                        yieldValue(currToken, tokenStartLine, tokenStartColumn)
                                        yield(ClosingBracketSep(Location(currLine, currColumn)))
                                    }

                                    ',' -> {
                                        yieldValue(currToken, tokenStartLine, tokenStartColumn)
                                        yield(CommaSep(Location(currLine, currColumn)))
                                    }

                                    ':' -> {
                                        yieldValue(currToken, tokenStartLine, tokenStartColumn)
                                        yield(ColonSep(Location(currLine, currColumn)))
                                    }

                                    '"' -> {
                                        yieldValue(currToken, tokenStartLine, tokenStartColumn)
                                        yield(OpeningQuotesSep(Location(currLine, currColumn)))
                                        state = InString
                                    }

                                    else -> {
                                        if (currToken.isEmpty()) {
                                            tokenStartLine = currLine
                                            tokenStartColumn = currColumn
                                        }
                                        currToken.write(char)
                                    }
                                }

                            InString -> when (char) {
                                '\\' -> {
                                    state = Escaping
                                }

                                '"' -> {
                                    yieldValue(currToken, tokenStartLine, tokenStartColumn)
                                    yield(ClosingQuotesSep(Location(currLine, currColumn)))
                                    state = OutString
                                }

                                else -> {
                                    if (currToken.isEmpty()) {
                                        tokenStartLine = currLine
                                        tokenStartColumn = currColumn
                                    }
                                    currToken.write(char)
                                }
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
                        currColumn++
                    }
                    charsRead = reader.read(buffer)
                }
            }
            yieldValue(currToken, tokenStartLine, tokenStartColumn)
        }.peekingIterator().let { TokensStream(it).asSuccess() }

    private suspend fun SequenceScope<KondorToken>.yieldValue(currWord: ChunkedWriter, line: Int, column: Int) {
        if (!currWord.isEmpty()) {
            val text = currWord.toString()
            yield(Value(text, Location(line, column)))
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

    fun MutableList<KondorToken>.addValue(charWriter: ChunkedWriter, line: Int, column: Int) {
        if (!charWriter.isEmpty()) {
            val text = charWriter.toString()
            add(Value(text, Location(line, column)))
            charWriter.clear()
        }
    }

    fun tokenize(): JsonOutcome<TokensStream> {
        var currLine = 1
        var currColumn = 1
        var tokenStartLine = 1
        var tokenStartColumn = 1
        val charWriter = ChunkedStringWriter(256)
        var state = OutString
        var unicodeCharacterPointString = ""
        val tokens = ArrayList<KondorToken>(128)
        for (char in jsonStr) {
            when (state) {
                OutString ->
                    when (char) {
                        ' ', '\t', '\r', '\b' ->
                            tokens.addValue(charWriter, tokenStartLine, tokenStartColumn)

                        '\n' -> {
                            tokens.addValue(charWriter, tokenStartLine, tokenStartColumn)
                            currLine++
                            currColumn = 0
                        }

                        '{' -> {
                            tokens.addValue(charWriter, tokenStartLine, tokenStartColumn)
                            tokens.add(OpeningCurlySep(Location(currLine, currColumn)))
                        }

                        '}' -> {
                            tokens.addValue(charWriter, tokenStartLine, tokenStartColumn)
                            tokens.add(ClosingCurlySep(Location(currLine, currColumn)))
                        }

                        '[' -> {
                            tokens.addValue(charWriter, tokenStartLine, tokenStartColumn)
                            tokens.add(OpeningBracketSep(Location(currLine, currColumn)))
                        }

                        ']' -> {
                            tokens.addValue(charWriter, tokenStartLine, tokenStartColumn)
                            tokens.add(ClosingBracketSep(Location(currLine, currColumn)))
                        }

                        ',' -> {
                            tokens.addValue(charWriter, tokenStartLine, tokenStartColumn)
                            tokens.add(CommaSep(Location(currLine, currColumn)))
                        }

                        ':' -> {
                            tokens.addValue(charWriter, tokenStartLine, tokenStartColumn)
                            tokens.add(ColonSep(Location(currLine, currColumn)))
                        }

                        '"' -> {
                            tokens.addValue(charWriter, tokenStartLine, tokenStartColumn)
                            tokens.add(OpeningQuotesSep(Location(currLine, currColumn)))
                            state = InString
                        }

                        else -> {
                            if (charWriter.isEmpty()) {
                                tokenStartLine = currLine
                                tokenStartColumn = currColumn
                            }
                            charWriter.write(char)
                        }
                    }

                InString -> when (char) {
                    '\\' -> {
                        state = Escaping
                    }

                    '"' -> {
                        tokens.addValue(charWriter, tokenStartLine, tokenStartColumn)
                        tokens.add(ClosingQuotesSep(Location(currLine, currColumn)))
                        state = OutString
                    }

                    else -> {
                        if (charWriter.isEmpty()) {
                            tokenStartLine = currLine
                            tokenStartColumn = currColumn
                        }
                        charWriter.write(char)
                    }
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
                        Location(currLine, currColumn),
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
                                Location(currLine, currColumn),
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
            currColumn++
        }
        tokens.addValue(charWriter, tokenStartLine, tokenStartColumn)
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
