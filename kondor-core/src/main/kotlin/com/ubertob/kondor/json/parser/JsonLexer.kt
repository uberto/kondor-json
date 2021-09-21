package com.ubertob.kondor.json.parser

import com.ubertob.kondor.json.jsonnode.NodePathRoot
import com.ubertob.kondor.json.parser.KondorSeparator.*
import com.ubertob.kondor.json.parser.LexerState.*
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset


enum class LexerState {
    OutString, InString, Escaping
}


class JsonLexerLazy(val inputStream: InputStream) {

    private var currPos = 1

    fun tokenize(): TokensStream =
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
                                    yield(Separator(OpeningCurly, currPos))
                                }
                                '}' -> {
                                    yieldValue(currToken, currPos)
                                    yield(Separator(ClosingCurly, currPos))
                                }
                                '[' -> {
                                    yieldValue(currToken, currPos)
                                    yield(Separator(OpeningBracket, currPos))
                                }
                                ']' -> {
                                    yieldValue(currToken, currPos)
                                    yield(Separator(ClosingBracket, currPos))
                                }
                                ',' -> {
                                    yieldValue(currToken, currPos)
                                    yield(Separator(Comma, currPos))
                                }
                                ':' -> {
                                    yieldValue(currToken, currPos)
                                    yield(Separator(Colon, currPos))
                                }
                                '"' -> {
                                    yieldValue(currToken, currPos)
                                    yield(Separator(OpeningQuotes, currPos))
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
                                yield(Separator(ClosingQuotes, currPos))
                                state = OutString
                            }
                            else -> currToken += char
                        }
                        Escaping -> when (char) {
                            '\\' -> currToken += '\\'
                            '"' -> currToken += '\"'
                            'n' -> currToken += '\n'
                            'f' -> currToken += '\t'
                            't' -> currToken += '\t'
                            'r' -> currToken += '\r'
                            'b' -> currToken += '\b'
                            'u' -> currToken.append( "\\u") //technically Unicode shouldn't be escaped in Json since it's UTF-8 but since people insist on using it...
                            else -> error("wrongly escaped char '\\$char' in Json string")
                        }.also { state = InString }
                    }
                    currPos++
                }
            yieldValue(currToken, currPos)

        }.peekingIterator().let { TokensStream(it) }

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

    fun tokenize(): TokensStream { //maybe an Outcome?
        var pos = 1
        val currToken = StringBuilder()
        var state = OutString
        val tokens = mutableListOf<KondorToken>()
        try {
            jsonStr.forEach { char ->
                when (state) {
                    OutString ->
                        when (char) {
                            ' ', '\t', '\n', '\r', '\b' -> tokens.addValue(currToken, pos)
                            '{' -> {
                                tokens.addValue(currToken, pos)
                                tokens.add(Separator(OpeningCurly, pos))
                            }
                            '}' -> {
                                tokens.addValue(currToken, pos)
                                tokens.add(Separator(ClosingCurly, pos))
                            }
                            '[' -> {
                                tokens.addValue(currToken, pos)
                                tokens.add(Separator(OpeningBracket, pos))
                            }
                            ']' -> {
                                tokens.addValue(currToken, pos)
                                tokens.add(Separator(ClosingBracket, pos))
                            }
                            ',' -> {
                                tokens.addValue(currToken, pos)
                                tokens.add(Separator(Comma, pos))
                            }
                            ':' -> {
                                tokens.addValue(currToken, pos)
                                tokens.add(Separator(Colon, pos))
                            }
                            '"' -> {
                                tokens.addValue(currToken, pos)
                                tokens.add(Separator(OpeningQuotes, pos))
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
                            tokens.add(Separator(ClosingQuotes, pos))
                            state = OutString
                        }
                        else -> currToken += char
                    }
                    Escaping -> when (char) {
                        '\\' -> currToken += '\\'
                        '"' -> currToken += '\"'
                        'n' -> currToken += '\n'
                        'f' -> currToken += '\t'
                        't' -> currToken += '\t'
                        'r' -> currToken += '\r'
                        'b' -> currToken += '\b'
                        'u' -> currToken.append( "\\u")
                        else -> error("wrongly escaped char '\\$char' in Json string")
                    }.also { state = InString }
                }
                pos++
            }
            tokens.addValue(currToken, pos)

        } catch (t: Throwable) {
            throw parsingException(
                "a valid Json", "${t.message.orEmpty()} after '${currToken.takeLast(10)}'", pos,
                NodePathRoot, "Invalid Json"
            )
        }

        return TokensStream(PeekingIteratorWrapper(tokens.iterator()))

    }

}

object KondorTokenizer {
    fun tokenize(jsonString: CharSequence): TokensStream = JsonLexerEager(jsonString).tokenize()
    fun tokenize(jsonStream: InputStream): TokensStream = JsonLexerLazy(jsonStream).tokenize()
}


/*
without StringBuilder, it's faster but it doesn't de-escape string (see String.translateEscapes)
todo: de-escape only if needed when creating string token

class JsonLexerEager(val jsonStr: CharSequence) {


    //todo extract the logic of tokenization
    fun tokenize(): TokensStream {

        var currTokenStart = 0
        var pos = 1

        fun MutableList<KondorToken>.addValue() {
            if (currTokenStart < pos -1) {
                add(Value(jsonStr.substring(currTokenStart, pos -1), currTokenStart + 1))
            }
            currTokenStart = pos
        }



        var state = OutString
        val tokens = mutableListOf<KondorToken>()
        jsonStr.forEach { char ->
            when (state) {
                OutString ->
                    when (char) {
                        ' ', '\t', '\n', '\r', '\b' -> tokens.addValue()
                        '{' -> {
                            tokens.addValue()
                            tokens.add(Separator(OpeningCurly, pos))
                        }
                        '}' -> {
                            tokens.addValue()
                            tokens.add(Separator(ClosingCurly,pos))
                        }
                        '[' -> {
                            tokens.addValue()
                            tokens.add(Separator(OpeningBracket,pos))
                        }
                        ']' -> {
                            tokens.addValue()
                            tokens.add(Separator(ClosingBracket,pos))
                        }
                        ',' -> {
                            tokens.addValue()
                            tokens.add(Separator(Comma,pos))
                        }
                        ':' -> {
                            tokens.addValue()
                            tokens.add(Separator(Colon,pos))
                        }
                        '"' -> {
                            tokens.addValue()
                            tokens.add(Separator(OpeningQuotes,pos))
                            state = InString
                        }
                         // else -> nothing
                    }

                InString -> when (char) {
                    '\\' -> {
                        state = Escaping
                    }
                    '"' -> {
                        tokens.addValue()
                        tokens.add(Separator(ClosingQuotes,pos))
                        state = OutString
                    }
                    // else -> nothing
                }
                Escaping -> when (char) {
                    '\\',
                    'n',
                    'f',
                    't',
                    'r',
                    'b',
                    '"' -> char //nothing
                    else -> error("wrongly escaped char '\\$char' in Json string")
                }.also { state = InStringWithEsc }
            }
            pos += 1
        }
        tokens.addValue()

    return  TokensStream(PeekingIteratorWrapper(tokens.iterator()))

}
 */


