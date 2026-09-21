package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.FieldsValues
import com.ubertob.kondor.json.jsonnode.JsonNodeArray
import com.ubertob.kondor.json.jsonnode.JsonNodeObject
import com.ubertob.kondor.json.jsonnode.NodePath
import com.ubertob.kondor.json.jsonnode.NodePathRoot
import com.ubertob.kondor.json.jsonnode.parseJsonNode
import com.ubertob.kondortools.expectFailure
import com.ubertob.kondortools.expectSuccess
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

// NaN and Infinity are not valid Json numbers, so the converters write them as text: reading a Json back must work
// on the tokens (JObj) and on the JsonNode (JAny) alike. "+Infinity" is accepted although it is never written.
class NonFiniteNumbersTest {

    private val nonFiniteDoubles = listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)
    private val nonFiniteFloats = listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)

    @Test
    fun `a non finite Double is read from the text it is written as`() {
        nonFiniteDoubles.forEach { value ->
            val node = parseJsonNode(JDouble.toJson(value)).expectSuccess()

            expectThat(JDouble.fromJsonNodeBase(node, NodePathRoot).expectSuccess()).isEqualTo(value)
        }
        expectThat(JDouble.fromJsonNodeBase(parseJsonNode(""""+Infinity"""").expectSuccess(), NodePathRoot).expectSuccess())
            .isEqualTo(Double.POSITIVE_INFINITY)
    }

    @Test
    fun `a non finite Float is read from the text it is written as`() {
        nonFiniteFloats.forEach { value ->
            val node = parseJsonNode(JFloat.toJson(value)).expectSuccess()

            expectThat(JFloat.fromJsonNodeBase(node, NodePathRoot).expectSuccess()).isEqualTo(value)
        }
        expectThat(JFloat.fromJsonNodeBase(parseJsonNode(""""+Infinity"""").expectSuccess(), NodePathRoot).expectSuccess())
            .isEqualTo(Float.POSITIVE_INFINITY)
    }

    @Test
    fun `a non finite field round trips in a JAny and in a JObj`() {
        nonFiniteDoubles.forEach { double ->
            val value = Measure(double, double.toFloat(), double, double.toFloat())

            listOf(JMeasureAny, JMeasureObj).forEach { converter ->
                val json = converter.toJson(value)

                expectThat(converter.fromJson(json).expectSuccess()).isEqualTo(value)
                expectThat(converter.fromJson(json.byteInputStream()).expectSuccess()).isEqualTo(value)
                expectThat(converter.fromJson(converter.toJson(value, JsonStyle.prettyWithNulls)).expectSuccess())
                    .isEqualTo(value)

                val node = parseJsonNode(json).expectSuccess() as JsonNodeObject
                expectThat(converter.fromJsonNode(node, NodePathRoot).expectSuccess()).isEqualTo(value)
            }
        }
    }

    @Test
    fun `absent or null non finite optional fields are null in a JAny and in a JObj`() {
        val absent = """{"amount": "NaN", "ratio": "-Infinity"}"""
        val explicitNulls = """{"amount": "NaN", "ratio": "-Infinity", "optional_amount": null, "optional_ratio": null}"""
        val absentNode = parseJsonNode(absent).expectSuccess() as JsonNodeObject
        val nullsNode = parseJsonNode(explicitNulls).expectSuccess() as JsonNodeObject
        val expected = Measure(Double.NaN, Float.NEGATIVE_INFINITY, null, null)

        listOf(JMeasureAny, JMeasureObj).forEach { converter ->
            expectThat(converter.fromJson(absent).expectSuccess()).isEqualTo(expected)
            expectThat(converter.fromJsonNode(absentNode, NodePathRoot).expectSuccess()).isEqualTo(expected)
            expectThat(converter.fromJson(explicitNulls).expectSuccess()).isEqualTo(expected)
            expectThat(converter.fromJsonNode(nullsNode, NodePathRoot).expectSuccess()).isEqualTo(expected)
        }
    }

    @Test
    fun `non finite values are read in a list and in a map`() {
        val json = JList(JFloat).toJson(nonFiniteFloats)
        expectThat(JList(JFloat).fromJson(json).expectSuccess()).isEqualTo(nonFiniteFloats)
        expectThat(JList(JFloat).fromJsonNode(parseJsonNode(json).expectSuccess() as JsonNodeArray, NodePathRoot).expectSuccess())
            .isEqualTo(nonFiniteFloats)

        val map = mapOf("nan" to Double.NaN, "inf" to Double.POSITIVE_INFINITY)
        val mapJson = JMap(JDouble).toJson(map)
        expectThat(JMap(JDouble).fromJson(mapJson).expectSuccess()).isEqualTo(map)
        expectThat(JMap(JDouble).fromJsonNode(parseJsonNode(mapJson).expectSuccess() as JsonNodeObject, NodePathRoot).expectSuccess())
            .isEqualTo(map)
    }

    @Test
    fun `a converter of a wrapper type reads its non finite values`() {
        val json = JTemperature.toJson(Temperature(Double.NEGATIVE_INFINITY))

        expectThat(JTemperature.fromJson(json).expectSuccess()).isEqualTo(Temperature(Double.NEGATIVE_INFINITY))
        expectThat(JTemperature.fromJsonNodeBase(parseJsonNode(json).expectSuccess(), NodePathRoot).expectSuccess())
            .isEqualTo(Temperature(Double.NEGATIVE_INFINITY))
    }

    @Test
    fun `a converter throwing on a non finite value fails with its path`() {
        val node = parseJsonNode(""""NaN"""").expectSuccess()

        expectThat(JRefusingNaN.fromJsonNodeBase(node, NodePathRoot).expectFailure().msg)
            .isEqualTo("Error converting node <[root]> NaN is not a temperature")
    }

    @Test
    fun `a text which is not a non finite number is an error`() {
        listOf(""""abc"""", """"Inf"""", """"nan"""", """"++Infinity"""", """"1.5"""").forEach { json ->
            val node = parseJsonNode(json).expectSuccess()
            val text = json.trim('"')

            expectThat(JDouble.fromJsonNodeBase(node, NodePathRoot).expectFailure().msg)
                .isEqualTo("Error converting node <[root]> expected a non finite Number (NaN, Infinity, -Infinity) but found '$text'")
            expectThat(JFloat.fromJsonNodeBase(node, NodePathRoot).expectFailure().msg)
                .isEqualTo("Error converting node <[root]> expected a non finite Number (NaN, Infinity, -Infinity) but found '$text'")
        }
    }

    @Test
    fun `a Json value which is not a number is an error`() {
        val node = parseJsonNode("true").expectSuccess()

        expectThat(JFloat.fromJsonNodeBase(node, NodePathRoot).expectFailure().msg)
            .isEqualTo("Error converting node <[root]> expected a Number or NaN but found Boolean")
        expectThat(JDouble.fromJsonNodeBase(node, NodePathRoot).expectFailure().msg)
            .isEqualTo("Error converting node <[root]> expected a Number or NaN but found Boolean")
    }

    @Test
    fun `the integer converters keep refusing a text`() {
        val node = parseJsonNode(""""NaN"""").expectSuccess()

        listOf(JInt, JLong, JBigDecimal, JBigInteger).forEach { converter ->
            expectThat(converter.fromJsonNodeBase(node, NodePathRoot).expectFailure().msg)
                .isEqualTo("Error converting node <[root]> expected a Number but found String 'NaN'")
        }
    }
}

private data class Measure(
    val amount: Double,
    val ratio: Float,
    val optionalAmount: Double?,
    val optionalRatio: Float?
)

private object JMeasureAny : JAny<Measure>() {
    private val amount by num(Measure::amount)
    private val ratio by num(JFloat, Measure::ratio)
    private val optional_amount by num(Measure::optionalAmount)
    private val optional_ratio by num(JFloat, Measure::optionalRatio)

    override fun JsonNodeObject.deserializeOrThrow() =
        Measure(+amount, +ratio, +optional_amount, +optional_ratio)
}

private object JMeasureObj : JObj<Measure>() {
    private val amount by num(Measure::amount)
    private val ratio by num(JFloat, Measure::ratio)
    private val optional_amount by num(Measure::optionalAmount)
    private val optional_ratio by num(JFloat, Measure::optionalRatio)

    override fun FieldsValues.deserializeOrThrow(path: NodePath) =
        Measure(+amount, +ratio, +optional_amount, +optional_ratio)
}

private data class Temperature(val celsius: Double)

private object JTemperature : JDoubleRepresentable<Temperature>() {
    override val cons: (Double) -> Temperature = ::Temperature
    override val render: (Temperature) -> Double = Temperature::celsius
}

private object JRefusingNaN : JDoubleRepresentable<Temperature>() {
    override val cons: (Double) -> Temperature =
        { if (it.isNaN()) error("NaN is not a temperature") else Temperature(it) }
    override val render: (Temperature) -> Double = Temperature::celsius
}
