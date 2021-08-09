package com.ubertob.kondor.json.parser

import com.ubertob.kondor.json.jsonnode.NodePathRoot
import com.ubertob.kondor.json.parser.KondorSeparator.*
import com.ubertob.kondor.json.parser.LexerState.*


enum class LexerState {
    OutString, InString, Escaping
}

/*
    //todo extract the logic of tokenization
class JsonLexerLazy(val jsonStr: ByteArrayInputStream) {

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

*/
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
                        'n' -> currToken += '\n'
                        'f' -> currToken += '\t'
                        't' -> currToken += '\t'
                        'r' -> currToken += '\r'
                        'b' -> currToken += '\b'
                        '"' -> currToken += '\"'
                        else -> error("wrongly escaped char '\\$char' in Json string")
                    }.also { state = InString }
                }
                pos += 1
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
//    fun tokenize(jsonString: InputStream): TokensStream = JsonLexerLazy(TODO("")).tokenize()
}


/*
without StringBuilder, faster but there is a problem with escaping Json

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
                }.also { state = InString }
            }
            pos += 1
        }
        tokens.addValue()

    return  TokensStream(PeekingIteratorWrapper(tokens.iterator()))

}
 */


