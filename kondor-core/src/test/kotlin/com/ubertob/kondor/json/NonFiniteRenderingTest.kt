package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.FieldsValues
import com.ubertob.kondor.json.jsonnode.JsonNodeNumber
import com.ubertob.kondor.json.jsonnode.JsonNodeObject
import com.ubertob.kondor.json.jsonnode.NodePath
import com.ubertob.kondor.json.jsonnode.NodePathRoot
import com.ubertob.kondor.json.jsonnode.parseJsonNode
import com.ubertob.kondortools.expectSuccess
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

// NaN and Infinity are not valid Json numbers: a JsonNode holding one renders them as text, as the converters do
class NonFiniteRenderingTest {

    private val nonFinite = listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)
    private val nonFiniteFloats = listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)

    @Test
    fun `a non finite number is rendered as text`() {
        expectThat(JsonNodeNumber(Double.NaN).render()).isEqualTo(""""NaN"""")
        expectThat(JsonNodeNumber(Double.POSITIVE_INFINITY).render()).isEqualTo(""""Infinity"""")
        expectThat(JsonNodeNumber(Float.NEGATIVE_INFINITY).render()).isEqualTo(""""-Infinity"""")
    }

    @Test
    fun `a value renders the same as its JsonNode`() {
        nonFinite.forEach { value ->
            expectThat(JDouble.toJsonNode(value).render(JDouble.jsonStyle)).isEqualTo(JDouble.toJson(value))

            listOf(JMeasureObj, JMeasureAny).forEach { converter ->
                expectThat(converter.toJsonNode(Measure(value)).render(converter.jsonStyle))
                    .isEqualTo(converter.toJson(Measure(value)))
            }
        }
        nonFiniteFloats.forEach { value ->
            expectThat(JFloat.toJsonNode(value).render(JFloat.jsonStyle)).isEqualTo(JFloat.toJson(value))
        }
        expectThat(JList(JDouble).toJsonNode(nonFinite).render(JList(JDouble).jsonStyle))
            .isEqualTo(JList(JDouble).toJson(nonFinite))
        expectThat(JMap(JFloat).toJsonNode(mapOf("f" to Float.NaN)).render(JMap(JFloat).jsonStyle))
            .isEqualTo(JMap(JFloat).toJson(mapOf("f" to Float.NaN)))
    }

    @Test
    fun `a rendered JsonNode with non finite numbers is valid Json and is read back`() {
        nonFinite.forEach { value ->
            listOf(JMeasureObj, JMeasureAny).forEach { converter ->
                val json = converter.toJsonNode(Measure(value)).render()

                expectThat(json).isEqualTo("""{"amount":"$value"}""")
                expectThat(converter.fromJson(json).expectSuccess()).isEqualTo(Measure(value))
            }
        }
    }

    @Test
    fun `the numbers that are valid Json are rendered as numbers`() {
        expectThat(JsonNodeNumber(42).render()).isEqualTo("42")
        expectThat(JsonNodeNumber(1.5).render()).isEqualTo("1.5")
        expectThat(JsonNodeNumber(Long.MAX_VALUE).render()).isEqualTo("9223372036854775807")
        expectThat(JDouble.toJsonNode(-0.0).render()).isEqualTo("-0.0")
    }

    private data class Measure(val amount: Double)

    private object JMeasureObj : JObj<Measure>() {
        private val amount by num(Measure::amount)

        override fun FieldsValues.deserializeOrThrow(path: NodePath) = Measure(+amount)
    }

    private object JMeasureAny : JAny<Measure>() {
        private val amount by num(Measure::amount)

        override fun JsonNodeObject.deserializeOrThrow() = Measure(+amount)
    }
}
