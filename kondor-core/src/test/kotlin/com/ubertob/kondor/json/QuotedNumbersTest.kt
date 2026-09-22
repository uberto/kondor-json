package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.*
import com.ubertob.kondor.json.parser.KondorTokenizer
import com.ubertob.kondor.json.parser.parseNumber
import com.ubertob.kondor.json.parser.TokensPath
import com.ubertob.kondor.json.parser.parseJsonNodeNum
import com.ubertob.kondor.outcome.Failure
import com.ubertob.kondor.outcome.bind
import com.ubertob.kondortools.expectFailure
import com.ubertob.kondortools.expectSuccess
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue
import strikt.assertions.startsWith
import java.math.BigDecimal

// only NaN and Infinity are written, and read, between quotes: any other number between quotes is an error, and so
// is a text which is not a number, which used to throw instead of failing
class QuotedNumbersTest {

    private fun quotedError(text: String, path: String = "[root]", position: Int = 0) =
        """Error parsing node <$path> at position $position: expected a Number but found "$text" - """ +
                "a number between quotes is not valid Json, only NaN and Infinity are written as text"

    @Test
    fun `a quoted word is not a number`() {
        expectThat(expectParsingError(NumberNode, """"a"""").msg).isEqualTo(quotedError("a"))

        val fromStream = KondorTokenizer.tokenize(""""a"""".byteInputStream())
            .bind { TokensPath(it, NodePathRoot).parseJsonNodeNum() }
        expectThat(fromStream.expectFailure().msg).isEqualTo(quotedError("a"))
    }

    @Test
    fun `an empty quoted value is not a number`() {
        expectThat(expectParsingError(NumberNode, "\"\"").msg).isEqualTo(quotedError(""))
    }

    @Test
    fun `a quoted word is not a number for the other node kinds`() {
        expectThat(expectParsingError(ArrayNode, """"a"""").msg)
            .isEqualTo("Error parsing node <[root]> at position 1: expected OpeningBracket but found OpeningQuotes - invalid Json")
        expectThat(expectParsingError(ObjectNode, """"a"""").msg)
            .isEqualTo("Error parsing node <[root]> at position 1: expected OpeningCurly but found OpeningQuotes - invalid Json")
        expectThat(expectParsingError(BooleanNode, """"a"""").msg)
            .isEqualTo("Error parsing node <[root]> at position 1: expected a Boolean but found OpeningQuotes - valid values: false, true")
        expectThat(expectParsingError(NullNode, """"a"""").msg)
            .isEqualTo("Error parsing node <[root]> at position 1: expected a Null but found OpeningQuotes - valid values: null")
        expectThat(StringNode.fromJsonString(""""a"""").expectSuccess().text).isEqualTo("a")
    }

    @Test
    fun `a number Java accepts but Json does not is an error`() {
        listOf("1d", "0x1p3").forEach { json ->
            expectThat(expectParsingError(NumberNode, json).msg).contains("expected a valid Number")
        }
        expectThat(JDouble.fromJson("1d").expectFailure().msg).startsWith("Error converting node <[root]> Wrong number format ")
    }

    @Test
    fun `an exponent too big for a BigDecimal is an error instead of Infinity`() {
        expectThat(expectParsingError(NumberNode, "1e99999999999").msg).contains("Too many nonzero exponent digits")
        expectThat(JDouble.fromJson("1e99999999999").expectFailure().msg).contains("Too many nonzero exponent digits")

        expectThat(JDouble.fromJson("1e400").expectSuccess()).isEqualTo(Double.POSITIVE_INFINITY)
    }

    @Test
    fun `a quoted number in a converter is an error, not an exception`() {
        expectThat(JInt.fromJson(""""a"""").expectFailure().msg).isEqualTo(quotedError("a"))
        expectThat(JDouble.fromJson(""""a"""".byteInputStream()).expectFailure().msg).isEqualTo(quotedError("a"))
        expectThat(JInt.fromJson("a").expectFailure().msg)
            .startsWith("Error converting node <[root]> Wrong number format ")
    }

    @Test
    fun `a quoted number is not read as a number, on either path`() {
        val converters = listOf<Pair<String, JsonConverter<*, *>>>(
            "JInt" to JInt, "JLong" to JLong, "JDouble" to JDouble, "JFloat" to JFloat,
            "JBigDecimal" to JBigDecimal, "JBigInteger" to JBigInteger
        )

        listOf(""""42"""", """"1.5"""", """"-0.0"""").forEach { json ->
            converters.forEach { (name, converter) ->
                expectThat(converter.fromJson(json)).describedAs("$name reading $json").isA<Failure<*>>()
                expectThat(converter.fromJson(json.byteInputStream())).describedAs("$name reading $json from a stream")
                    .isA<Failure<*>>()
                expectThat(converter.fromJsonNodeBase(parseJsonNode(json).expectSuccess(), NodePathRoot))
                    .describedAs("$name reading $json from a node").isA<Failure<*>>()
            }
        }
    }

    @Test
    fun `a quoted number says what is wrong`() {
        expectThat(JInt.fromJson(""""42"""").expectFailure().msg).isEqualTo(quotedError("42"))
        //the error names the field and where it is, as for any other value
        expectThat(JProduct.fromJson("""{"id": 1, "short-desc": "s", "long_description": "l", "price": "12.5"}""")
            .expectFailure().msg).isEqualTo(quotedError("12.5", "/price", 62))
        expectThat(Product.Json.fromJson("""{"id": "1", "short-desc": "s", "long_description": "l", "price": 1.5}""")
            .expectFailure().msg).isEqualTo("Error converting node </id> expected a Number but found String '1'")
    }

    @Test
    fun `NaN and Infinity are read between quotes only by the converters having them`() {
        //JInt and JBigDecimal have no NaN: the converter refuses it, as it does for a bare NaN
        expectThat(JInt.fromJson(""""NaN"""").expectFailure().msg)
            .startsWith("Error converting node <[root]> Wrong number format ")
        expectThat(JBigDecimal.fromJson(""""Infinity"""").expectFailure().msg)
            .startsWith("Error converting node <[root]> Wrong number format ")
    }

    @Test
    fun `NaN and Infinity are still read between quotes`() {
        expectThat(JDouble.fromJson(""""NaN"""").expectSuccess().isNaN()).isTrue()
        expectThat(JDouble.fromJson(""""Infinity"""").expectSuccess()).isEqualTo(Double.POSITIVE_INFINITY)
        expectThat(JDouble.fromJson(""""-Infinity"""".byteInputStream()).expectSuccess()).isEqualTo(Double.NEGATIVE_INFINITY)
        expectThat(NumberNode.fromJsonString(""""NaN"""").expectSuccess().num.toDouble().isNaN()).isTrue()
        expectThat(NumberNode.fromJsonString(""""+Infinity"""").expectSuccess().num).isEqualTo(Double.POSITIVE_INFINITY)
    }

    @Test
    fun `an invalid number reports its path and position`() {
        expectThat(parseJsonNode("""{"k": zz}""").expectFailure().msg)
            .startsWith("Error parsing node </k> at position 5: expected a valid Number but found 'zz'")
        expectThat(parseJsonNode("""{"a": {"b": [0, 1d]}}""").expectFailure().msg)
            .startsWith("Error parsing node </a/b> at position 15: expected a valid Number but found '1d'")
        expectThat(JProduct.fromJson("""{"id": 1, "short-desc": "s", "long_description": "l", "price": 1d}""").expectFailure().msg)
            .startsWith("Error converting node </price> Wrong number format ")
    }

    @Test
    fun `a converter throwing on a number is an error, quoted or not`() {
        val jThrowing = object : JNumRepresentable<Int, Int>() {
            override val cons: (Int) -> Int = { it }
            override val render: (Int) -> Int = { it }
            override fun toNumberSubtype(number: Number): Int = number.toInt()
            override fun parser(value: String): JsonOutcome<Int> = throw IllegalStateException("bang $value")
        }

        expectThat(parseNumber(KondorTokenizer.tokenize("42").expectSuccess(), NodePathRoot, jThrowing::parser).expectFailure().msg)
            .isEqualTo("Error parsing node <[root]> at position 0: expected a Number or NaN but found '42' - IllegalStateException: bang 42")
        expectThat(parseNumber(KondorTokenizer.tokenize(""""NaN"""").expectSuccess(), NodePathRoot, jThrowing::parser).expectFailure().msg)
            .isEqualTo("Error parsing node <[root]> at position 0: expected a Number or NaN but found 'NaN' - IllegalStateException: bang NaN")
    }

    @Test
    fun `numbers are still read as usual`() {
        expectThat(NumberNode.fromJsonString("12.5").expectSuccess().num).isEqualTo(BigDecimal("12.5"))
        expectThat(NumberNode.fromJsonString("-1e300").expectSuccess().num).isEqualTo(BigDecimal("-1e300"))
        expectThat(JInt.fromJson("42").expectSuccess()).isEqualTo(42)
    }

    private fun expectParsingError(nodeKind: NodeKind<*>, json: String): JsonError =
        try {
            nodeKind.fromJsonString(json).expectFailure()
        } catch (e: RuntimeException) {
            fail("Exception parsing $json as ${nodeKind.desc}", e)
        }
}
