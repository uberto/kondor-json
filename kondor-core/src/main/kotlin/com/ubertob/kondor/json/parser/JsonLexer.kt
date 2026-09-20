package com.ubertob.kondor.json.parser

import com.ubertob.kondor.json.ChunkedStringWriter
import com.ubertob.kondor.json.ChunkedWriter
import com.ubertob.kondor.json.JsonError
import com.ubertob.kondor.json.JsonOutcome
import com.ubertob.kondor.json.jsonnode.NodePathRoot
import com.ubertob.kondor.json.parser.LexerState.*
import com.ubertob.kondor.outcome.asFailure
import com.ubertob.kondor.outcome.asSuccess
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset


enum class LexerState {
    OutString, InString, Escaping, Unicode
}

// the escape table and its errors are shared by the two lexers, so that they cannot diverge

// the escaped char, or null if it cannot be escaped. 'u' is not here: it starts a unicode escape
private fun escapedChar(char: Char): Char? = when (char) {
    '\\' -> '\\'
    '/' -> '/'
    '"' -> '"'
    'n' -> '\n'
    'f' -> '\u000C'
    't' -> '\t'
    'r' -> '\r'
    'b' -> '\b'
    else -> null
}

// only the ASCII hex digits are valid in a Json unicode escape
private fun unicodeChar(hexDigits: String): Char? =
    hexDigits.fold(0) { code, digit ->
        val value = when (digit) {
            in '0'..'9' -> digit - '0'
            in 'a'..'f' -> digit - 'a' + 10
            in 'A'..'F' -> digit - 'A' + 10
            else -> return null
        }
        code * 16 + value
    }.toChar()

private fun wrongEscapeError(char: Char, lastChars: String, pos: Int): JsonError =
    parsingError(
        "a valid Json", "wrongly escaped char '\\$char' inside a Json string after '$lastChars'",
        pos, NodePathRoot, "Invalid Json"
    )

private fun invalidUnicodeError(hexDigits: String, lastChars: String, pos: Int): JsonError =
    parsingError(
        "a valid Json", "invalid unicode escape sequence '\\u$hexDigits' after '$lastChars'",
        pos, NodePathRoot, "Invalid Json"
    )


class JsonLexerLazy(val inputStream: InputStream) {

    private companion object {
        const val BUFFER_SIZE = 8192
    }

    fun tokenize(): JsonOutcome<TokensStream> =
        TokensStream(LazyTokenIterator()).asSuccess()

    private inner class LazyTokenIterator : PeekingIterator<KondorToken>, LexerErrorSource {
        private val reader: InputStreamReader = inputStream.reader(Charset.forName("UTF-8"))
        private val buffer: CharArray = CharArray(BUFFER_SIZE)
        private var charsRead: Int = 0
        private var bufferPos: Int = 0
        private var finished: Boolean = false

        private var state: LexerState = OutString
        private var unicodeCharacterPointString: String = ""
        private var currPos: Int = 1

        private val charWriter: ChunkedWriter = ChunkedStringWriter(256)

        private var lexerError: JsonError? = null
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

        override fun lexerError(): JsonError? = lexerError

        // after an invalid escape the string cannot be read: no more tokens are produced and the error is kept
        private fun stopWith(error: JsonError) {
            lexerError = error
            pending = null // no token is pending here: every branch setting it returns immediately
            closeReader()
        }

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
            if (lexerError != null) return
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

                    Escaping -> if (char == 'u') state = Unicode
                    else {
                        val escaped = escapedChar(char)
                            ?: return stopWith(
                                wrongEscapeError(char, charWriter.takeLast(10), currPos - 1)
                            )
                        charWriter.write(escaped)
                        state = InString
                    }

                    Unicode -> {
                        unicodeCharacterPointString += char
                        if (unicodeCharacterPointString.length == 4) {
                            val unicodeChar = unicodeChar(unicodeCharacterPointString)
                                ?: return stopWith(
                                    invalidUnicodeError(
                                        unicodeCharacterPointString, charWriter.takeLast(10), currPos - 1
                                    )
                                )
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

                Escaping -> if (char == 'u') state = Unicode
                else {
                    val escaped = escapedChar(char)
                        ?: return wrongEscapeError(char, charWriter.takeLast(10), pos).asFailure()
                    charWriter.write(escaped)
                    state = InString
                }

                Unicode -> {
                    unicodeCharacterPointString += char

                    if (unicodeCharacterPointString.length == 4) {
                        val unicodeChar = unicodeChar(unicodeCharacterPointString)
                            ?: return invalidUnicodeError(unicodeCharacterPointString, charWriter.takeLast(10), pos)
                                .asFailure()

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
