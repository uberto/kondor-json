package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.FieldsValues
import com.ubertob.kondor.json.jsonnode.JsonNodeNumber
import com.ubertob.kondor.json.jsonnode.JsonNodeObject
import com.ubertob.kondor.json.jsonnode.NodePath
import com.ubertob.kondor.json.jsonnode.NegativeZero
import com.ubertob.kondor.json.jsonnode.NodePathRoot
import com.ubertob.kondor.json.jsonnode.asNumValue
import com.ubertob.kondor.json.jsonnode.parseJsonNode
import com.ubertob.kondortools.expectFailure
import com.ubertob.kondortools.expectSuccess
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo
import strikt.assertions.matches
import java.math.BigDecimal
import java.math.BigInteger

// -0.0 is a Double of its own: -0.0 == 0.0 is true, but (-0.0).equals(0.0) is false, so a value holding one has to
// read back as -0.0 or it does not equal itself any more. A BigDecimal cannot hold the sign, so a zero written with
// a minus is read as a NegativeZero, which keeps the sign together with the parsed number.
class NegativeZeroTest {

    private val zeroTexts = listOf("-0.0", "-0", "-0.000", "-0e10", "-0E-10", "-0.0e+100")

    @Test
    fun `a negative zero Double is read from a String`() {
        expectThat(JDouble.toJson(-0.0)).isEqualTo("-0.0")
        zeroTexts.forEach { json ->
            expectThat(JDouble.fromJson(json).expectSuccess()).describedAs(json).isEqualTo(-0.0)
        }
    }

    @Test
    fun `a negative zero Double is read from a stream`() {
        zeroTexts.forEach { json ->
            expectThat(JDouble.fromJson(json.byteInputStream()).expectSuccess()).describedAs(json).isEqualTo(-0.0)
        }
    }

    @Test
    fun `a negative zero Double is read from a JsonNode`() {
        zeroTexts.forEach { json ->
            val node = parseJsonNode(json).expectSuccess()

            expectThat(JDouble.fromJsonNodeBase(node, NodePathRoot).expectSuccess()).describedAs(json).isEqualTo(-0.0)
        }
    }

    @Test
    fun `a negative zero Float is read on both paths`() {
        expectThat(JFloat.toJson(-0.0f)).isEqualTo("-0.0")
        expectThat(JFloat.toJsonNode(-0.0f).render()).isEqualTo("-0.0")
        zeroTexts.forEach { json ->
            expectThat(JFloat.fromJson(json).expectSuccess()).describedAs(json).isEqualTo(-0.0f)
            expectThat(JFloat.fromJson(json.byteInputStream()).expectSuccess()).describedAs(json).isEqualTo(-0.0f)
            expectThat(JFloat.fromJsonNodeBase(parseJsonNode(json).expectSuccess(), NodePathRoot).expectSuccess())
                .describedAs(json).isEqualTo(-0.0f)
        }
    }

    @Test
    fun `a negative zero is read in an object, a list and a map`() {
        val json = """{"amount":-0.0,"ratios":[-0.0,1.5],"named":{"a":-0.0}}"""

        val value = Zeros(-0.0, listOf(-0.0, 1.5), mapOf("a" to -0.0))
        expectThat(JZeros.fromJson(json).expectSuccess()).isEqualTo(value)
        expectThat(JZeros.fromJsonNode(parseJsonNode(json).expectSuccess() as JsonNodeObject, NodePathRoot).expectSuccess())
            .isEqualTo(value)
        expectThat(JZeros.toJson(value)).isEqualTo("""{"amount": -0.0, "ratios": [-0.0, 1.5], "named": {"a": -0.0}}""")
    }

    @Test
    fun `a JsonNode renders a negative zero with its sign and its scale`() {
        listOf("-0.0" to "-0.0", "-0" to "-0", "-0.000" to "-0.000", "-0e10" to "-0E+10", "-0E-10" to "-0E-10")
            .forEach { (json, rendered) ->
                expectThat(parseJsonNode(json).expectSuccess().render()).describedAs(json).isEqualTo(rendered)
            }
    }

    @Test
    fun `a number written loosely is rendered as valid Json`() {
        //the parser accepts more than Json does, but it must not write those numbers back
        listOf("-.0", "-0.", "-00", "-000.000", "-0e-9").forEach { json ->
            val rendered = parseJsonNode(json).expectSuccess().render()

            expectThat(JDouble.fromJson(rendered).expectSuccess()).describedAs(json).isEqualTo(-0.0)
            expectThat(rendered).describedAs(json).matches(Regex("-(0|[1-9]\\d*)(\\.\\d+)?([eE][-+]?\\d+)?"))
        }
    }

    @Test
    fun `a negative zero in a JsonNode is a NegativeZero, not a BigDecimal`() {
        val number = parseJsonNode("-0.000").expectSuccess().asNumValue()

        expectThat(number).isEqualTo(NegativeZero(BigDecimal("-0.000")))
        expectThat(number!!.toDouble()).isEqualTo(-0.0)
        expectThat(NegativeZero(BigDecimal("0E+10"))).isEqualTo(NegativeZero(BigDecimal("0e10")))
        expectThat(parseJsonNode("-0.0").expectSuccess()).isNotEqualTo(JDouble.toJsonNode(-0.0))
    }

    @Test
    fun `the integer converters read a negative zero as before`() {
        expectThat(JBigInteger.fromJson("-0").expectSuccess()).isEqualTo(BigInteger.ZERO)
        expectThat(JBigInteger.fromJsonNode(parseJsonNode("-0").expectSuccess() as JsonNodeNumber, NodePathRoot).expectSuccess())
            .isEqualTo(BigInteger.ZERO)
        expectThat(JInt.fromJson("-0").expectSuccess()).isEqualTo(0)
        expectThat(JLong.fromJsonNode(parseJsonNode("-0.0").expectSuccess() as JsonNodeNumber, NodePathRoot).expectSuccess())
            .isEqualTo(0L)

        //a number with decimals is not a BigInteger, on either path
        JBigInteger.fromJson("-0.0").expectFailure()
        JBigInteger.fromJsonNode(parseJsonNode("-0.0").expectSuccess() as JsonNodeNumber, NodePathRoot).expectFailure()
    }

    @Test
    fun `a BigDecimal keeps its scale on both paths`() {
        listOf("-0.000" to 3, "-0" to 0, "0.000" to 3).forEach { (json, scale) ->
            expectThat(JBigDecimal.fromJson(json).expectSuccess().scale()).describedAs(json).isEqualTo(scale)
            expectThat(
                JBigDecimal.fromJsonNode(parseJsonNode(json).expectSuccess() as JsonNodeNumber, NodePathRoot)
                    .expectSuccess().scale()
            ).describedAs(json).isEqualTo(scale)
        }
    }

    @Test
    fun `a positive zero stays positive`() {
        listOf("0.0", "0", "0e10", "0.000").forEach { json ->
            expectThat(JDouble.fromJson(json).expectSuccess()).describedAs(json).isEqualTo(0.0)
            expectThat(JDouble.fromJsonNodeBase(parseJsonNode(json).expectSuccess(), NodePathRoot).expectSuccess())
                .describedAs(json).isEqualTo(0.0)
        }
    }

    @Test
    fun `a number too small for a Double keeps its sign`() {
        expectThat(JDouble.fromJson("-1e-400").expectSuccess()).isEqualTo(-0.0)
        expectThat(JDouble.fromJson("1e-400").expectSuccess()).isEqualTo(0.0)
        expectThat(parseJsonNode("-1e-400").expectSuccess().asNumValue()).isEqualTo(BigDecimal("-1E-400"))
    }

    @Test
    fun `the other numbers are not changed`() {
        expectThat(parseJsonNode("-1.5").expectSuccess().asNumValue()).isEqualTo(BigDecimal("-1.5"))
        expectThat(parseJsonNode("1.5000").expectSuccess().render()).isEqualTo("1.5000")
        expectThat(JDouble.fromJson("-1.5").expectSuccess()).isEqualTo(-1.5)
    }
}

private data class Zeros(val amount: Double, val ratios: List<Double>, val named: Map<String, Double>)

private object JZeros : JObj<Zeros>() {
    private val amount by num(Zeros::amount)
    private val ratios by array(JDouble, Zeros::ratios)
    private val named by obj(JMap(JDouble), Zeros::named)

    override fun FieldsValues.deserializeOrThrow(path: NodePath) = Zeros(+amount, +ratios, +named)
}
