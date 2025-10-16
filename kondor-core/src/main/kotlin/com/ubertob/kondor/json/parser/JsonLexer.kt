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
        TokensStream(LazyTokenIterator()).asSuccess()

    private inner class LazyTokenIterator : PeekingIterator<KondorToken> {
        private val reader: InputStreamReader = inputStream.reader(Charset.forName("UTF-8"))
        private val buffer: CharArray = CharArray(BUFFER_SIZE)
        private var charsRead: Int = 0
        private var bufferPos: Int = 0
        private var finished: Boolean = false

        private var state: LexerState = OutString
        private var unicodeCharacterPointString: String = ""
        private var currPos: Int = 1

        private val charWriter: ChunkedWriter = ChunkedStringWriter(256)

        private var pending: KondorToken? = null
        private var queuedSeparator: KondorToken? = null
        private var lastToken: KondorToken? = null

        override fun peek(): KondorToken {
            if (pending == null) advance()
            return pending ?: throw EndOfCollection
        }

        override fun hasNext(): Boolean {
            if (pending != null) return true
            advance()
            return pending != null
        }

        override fun next(): KondorToken {
            val tok = peek()
            pending = null
            lastToken = tok
            return tok
        }

        override fun last(): KondorToken? = pending ?: lastToken

        private fun closeReader() {
            if (!finished) {
                finished = true
                reader.close()
            }
        }

        private fun readMore(): Boolean {
            if (finished) return false

            charsRead = reader.read(buffer)
            bufferPos = 0
            if (charsRead <= 0) {
                closeReader()
                return false
            } else {
                return true
            }

        }

        private fun haveChar(): Boolean = bufferPos < charsRead || readMore()

        private fun nextChar(): Char {
            val c = buffer[bufferPos]
            bufferPos++
            currPos++
            return c
        }

        private fun yieldValueIfAny(pos: Int): Boolean {
            if (!charWriter.isEmpty()) {
                val text = charWriter.toString()
                pending = Value(text, pos - text.length)
                charWriter.clear()
                return true
            }
            return false
        }

        private fun queueOrSetSeparator(sep: KondorToken, pos: Int) {
            if (yieldValueIfAny(pos)) {
                queuedSeparator = sep
            } else {
                pending = sep
            }
        }

        private fun advance() {
            if (pending != null) return
            if (queuedSeparator != null) {
                pending = queuedSeparator
                queuedSeparator = null
                return
            }

            while (true) {
                if (!haveChar()) {
                    // End of input: flush any remaining value
                    if (yieldValueIfAny(currPos)) return
                    pending = null
                    return
                }

                val char = nextChar()

                when (state) {
                    OutString -> when (char) {
                        ' ', '\t', '\n', '\r', '\b' -> if (yieldValueIfAny(currPos - 1)) return else continue
                        '{' -> {
                            queueOrSetSeparator(OpeningCurlySep, currPos - 1); return
                        }

                        '}' -> {
                            queueOrSetSeparator(ClosingCurlySep, currPos - 1); return
                        }

                        '[' -> {
                            queueOrSetSeparator(OpeningBracketSep, currPos - 1); return
                        }

                        ']' -> {
                            queueOrSetSeparator(ClosingBracketSep, currPos - 1); return
                        }

                        ',' -> {
                            queueOrSetSeparator(CommaSep, currPos - 1); return
                        }

                        ':' -> {
                            queueOrSetSeparator(ColonSep, currPos - 1); return
                        }

                        '"' -> {
                            // entering string
                            state = InString
                            queueOrSetSeparator(OpeningQuotesSep, currPos - 1)
                            return
                        }

                        else -> charWriter.write(char)
                    }

                    InString -> when (char) {
                        '\\' -> state = Escaping
                        '"' -> {
                            state = OutString
                            queueOrSetSeparator(ClosingQuotesSep, currPos - 1)
                            return
                        }

                        else -> charWriter.write(char)
                    }

                    Escaping -> when (char) {
                        '\\' -> charWriter.write('\\').also { state = InString }
                        '/' -> charWriter.write('/').also { state = InString }
                        '"' -> charWriter.write('"').also { state = InString }
                        'n' -> charWriter.write('\n').also { state = InString }
                        'f' -> charWriter.write('\t').also { state = InString }
                        't' -> charWriter.write('\t').also { state = InString }
                        'r' -> charWriter.write('\r').also { state = InString }
                        'b' -> charWriter.write('\b').also { state = InString }
                        'u' -> {
                            state = Unicode
                        }

                        else -> error("wrongly escaped char '\\$char' inside a Json string")
                    }

                    Unicode -> {
                        unicodeCharacterPointString += char
                        if (unicodeCharacterPointString.length == 4) {
                            val unicodeChar = unicodeCharacterPointString.toIntOrNull(16)?.toChar()
                                ?: error("invalid unicode escape sequence '\\u${unicodeCharacterPointString}'")
                            charWriter.write(unicodeChar)
                            unicodeCharacterPointString = ""
                            state = InString
                        }
                    }
                }
            }
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
