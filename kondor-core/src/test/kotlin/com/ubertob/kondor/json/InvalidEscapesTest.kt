package com.ubertob.kondor.json

import com.ubertob.kondor.json.parser.KondorTokenizer
import com.ubertob.kondortools.expectFailure
import com.ubertob.kondortools.expectSuccess
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.fail
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo

// the String and the InputStream paths use different lexers: they must agree on escaped and invalid strings
class InvalidEscapesTest {

    @Test
    fun `escaped control characters round trip`() {
        val value = "form\u000Cfeed tab\t new\n return\r back\b quote\" slash\\ ok"

        val json = JString.toJson(value)
        expectThat(JString.fromJson(json).expectSuccess()).isEqualTo(value)
        expectThat(JString.fromJson(json.byteInputStream()).expectSuccess()).isEqualTo(value)
    }

    @Test
    fun `escaped form feed is not a tab`() {
        expectThat(parseBothWays(""""a\fb"""").expectSuccess()).isEqualTo("a\u000Cb")
    }

    @Test
    fun `a wrongly escaped char is an error on both paths`() {
        val error = parseBothWays(""""a\xb"""").expectFailure()

        expectThat(error.msg).isEqualTo("Error parsing node <[root]> at position 4: expected a valid Json but found wrongly escaped char '\\x' inside a Json string after 'a' - Invalid Json")
    }

    @Test
    fun `an invalid unicode escape is an error on both paths`() {
        val error = parseBothWays(""""a\u00zzb"""").expectFailure()

        expectThat(error.msg).isEqualTo("Error parsing node <[root]> at position 8: expected a valid Json but found invalid unicode escape sequence '\\u00zz' after 'a' - Invalid Json")
    }

    @Test
    fun `a wrongly escaped char inside an object is an error on both paths`() {
        val json = """{"user": {"id": 1, "name": "Fr\qnk"}, "file": {"file_name": "f"}}"""

        expectThat(parseBothWays(json, JUserFile).expectFailure().msg)
            .isEqualTo("Error parsing node <[root]> at position 32: expected a valid Json but found wrongly escaped char '\\q' inside a Json string after 'Fr' - Invalid Json")
    }

    @Test
    fun `a wrongly escaped char in a JAny and in a JMap is an error on both paths`() {
        expectThat(parseBothWays("""{"id": 1, "short-desc": "a\q", "long_description": "b", "price": null}""", Product.Json).expectFailure().msg)
            .isEqualTo("Error parsing node <[root]> at position 28: expected a valid Json but found wrongly escaped char '\\q' inside a Json string after 'a' - Invalid Json")
        expectThat(parseBothWays("""{"a": "x\q"}""", JMap(JString)).expectFailure().msg)
            .isEqualTo("Error parsing node <[root]> at position 10: expected a valid Json but found wrongly escaped char '\\q' inside a Json string after 'x' - Invalid Json")
    }

    @Test
    fun `a wrongly escaped char in an array element is an error on both paths`() {
        expectThat(parseBothWays("""["a", "b\p", "c"]""", JStringList).expectFailure().msg)
            .isEqualTo("Error parsing node <[root]> at position 10: expected a valid Json but found wrongly escaped char '\\p' inside a Json string after 'b' - Invalid Json")
    }

    @Test
    fun `an invalid unicode escape with a sign is an error on both paths`() {
        expectThat(parseBothWays(""""a\u-123b"""").expectFailure().msg)
            .isEqualTo("Error parsing node <[root]> at position 8: expected a valid Json but found invalid unicode escape sequence '\\u-123' after 'a' - Invalid Json")
        expectThat(parseBothWays(""""a\u+041b"""").expectFailure().msg)
            .isEqualTo("Error parsing node <[root]> at position 8: expected a valid Json but found invalid unicode escape sequence '\\u+041' after 'a' - Invalid Json")
    }

    @Test
    fun `a unicode escape with non ascii digits is an error on both paths`() {
        expectThat(parseBothWays(""""a\u٣٣٣٣b"""").expectFailure().msg)
            .contains("invalid unicode escape sequence")
        expectThat(parseBothWays(""""a\u３３３３b"""").expectFailure().msg)
            .contains("invalid unicode escape sequence")
    }

    @Test
    fun `a unicode escape with any ascii hex digits is read on both paths`() {
        expectThat(parseBothWays(""""\u0000\uffff\uFFFE\u00e9"""").expectSuccess())
            .isEqualTo("\u0000\uFFFF\uFFFE\u00E9")
    }

    @Test
    fun `a string ending after a wrong escape is an error on both paths`() {
        expectThat(parseBothWays(""""a\q""").expectFailure().msg).contains("wrongly escaped char '\\q'")
        expectThat(parseBothWays(""""abc\""").expectFailure().msg).contains("expected ClosingQuotes but found end of file")
    }

    @Test
    fun `a wrong escape across the stream buffer boundary is an error`() {
        val padding = "-".repeat(8_200)

        (8_186..8_196).forEach { position ->
            val json = """"${padding.take(position - 1)}\q""""
            val fromStream = JString.fromJson(json.byteInputStream()).expectFailure().msg

            expectThat(fromStream).isEqualTo(JString.fromJson(json).expectFailure().msg)
            expectThat(fromStream).contains("wrongly escaped char '\\q'")
        }
    }

    @Test
    fun `an invalid escape after the end of the Json is an error on both paths, with different messages`() {
        val json = """"abc" "a\q""""

        // the lazy lexer reads only up to the end of the parsed value, so the trailing content is reported first
        expectThat(JString.fromJson(json).expectFailure().msg)
            .isEqualTo("Error parsing node <[root]> at position 10: expected a valid Json but found wrongly escaped char '\\q' inside a Json string after 'a' - Invalid Json")
        expectThat(JString.fromJson(json.byteInputStream()).expectFailure().msg)
            .isEqualTo("Error parsing node <[root]> at position 6: expected EOF but found OpeningQuotes - json continue after end")
    }

    @Test
    fun `tokenizing an invalid escape fails on both paths`() {
        val json = """{"a": "b\q"}"""

        expectThat(KondorTokenizer.tokenize(json).expectFailure().msg)
            .isEqualTo("Error parsing node <[root]> at position 10: expected a valid Json but found wrongly escaped char '\\q' inside a Json string after 'b' - Invalid Json")

        // the lazy lexer fails only while the tokens are read
        val tokens = KondorTokenizer.tokenize(json.byteInputStream()).expectSuccess()
        val exception = assertThrows<JsonParsingException> { tokens.toList() }
        expectThat(exception.error.msg)
            .isEqualTo("Error parsing node <[root]> at position 10: expected a valid Json but found wrongly escaped char '\\q' inside a Json string after 'b' - Invalid Json")
        expectThat(tokens.lexerError()?.msg).isEqualTo(exception.error.msg)
    }

    private fun parseBothWays(json: String): JsonOutcome<String> = parseBothWays(json, JString)

    // when the parser reads as far as the escape, both paths give the same outcome: the one from the String is returned
    private fun <T : Any> parseBothWays(json: String, converter: JsonConverter<T, *>): JsonOutcome<T> {
        val fromString = converter.fromJson(json)
        val fromStream = try {
            converter.fromJson(json.byteInputStream())
        } catch (e: Exception) {
            fail("Exception parsing from an InputStream: $json", e)
        }
        expectThat(fromStream).isEqualTo(fromString)
        return fromString
    }
}
