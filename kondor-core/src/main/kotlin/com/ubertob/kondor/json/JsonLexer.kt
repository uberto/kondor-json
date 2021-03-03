import LexerState.*
import com.ubertob.kondor.json.TokensStream
import com.ubertob.kondor.json.peekingIterator
import java.util.concurrent.atomic.AtomicInteger

enum class LexerState {
    OutString, InString, Escaping
}

class JsonLexer(val jsonStr: CharSequence) {

    private val pos = AtomicInteger(0)

    fun tokenize(): TokensStream =
        sequence {
            val currToken = StringBuilder()  //replace with index and substring for perf
            var state = OutString
            jsonStr.forEach { char ->
                pos.incrementAndGet()
                when (state) {
                    OutString ->
                        when (char) {
                            ' ', '\t', '\n', '\r', '\b' -> yieldIfNotEmpty(currToken)
                            '{', '}', '[', ']', ',', ':' -> {
                                yieldIfNotEmpty(currToken)
                                yield(char.toString())
                            }
                            '"' -> {
                                yieldIfNotEmpty(currToken)
                                yield(char.toString())
                                state = InString
                            }
                            else -> currToken.append(char)
                        }

                    InString -> when (char) {
                        '\\' -> {
                            state = Escaping
                        }
                        '"' -> {
                            yieldIfNotEmpty(currToken)
                            yield(char.toString())
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
                        else -> error("Wrong escape char $char")
                    }.also { state = InString }
                }
            }
            yieldIfNotEmpty(currToken)
        }.peekingIterator().let { TokensStream(pos::get, it) }

    private suspend fun SequenceScope<String>.yieldIfNotEmpty(currWord: StringBuilder) {
        if (currWord.isNotEmpty()) {
            yield(currWord.toString())
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