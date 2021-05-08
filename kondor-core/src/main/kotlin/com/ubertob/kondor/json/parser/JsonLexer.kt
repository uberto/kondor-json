import LexerState.*
import com.ubertob.kondor.json.*
import java.util.concurrent.atomic.AtomicInteger

enum class LexerState {
    OutString, InString, Escaping
}

class JsonLexer(val jsonStr: CharSequence) {

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
                        else -> error("Wrongly escaped char '\\$char' in Json string")
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

    companion object {
        fun tokenize(jsonString: CharSequence): TokensStream = JsonLexer(jsonString).tokenize()
    }
}

operator fun StringBuilder.plusAssign(c: Char) {
    append(c)
}