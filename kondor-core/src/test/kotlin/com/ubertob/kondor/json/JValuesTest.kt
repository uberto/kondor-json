package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.NodePathRoot
import com.ubertob.kondor.randomList
import com.ubertob.kondor.randomString
import com.ubertob.kondor.text
import com.ubertob.kondortools.expectSuccess
import com.ubertob.kondortools.printIt
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import java.util.*
import kotlin.random.Random

class JValuesTest {

    @Test
    fun `JsonNode String`() {
        repeat(100) {
            val value = randomString(text, 0, 30)

            val json = JString.toJsonNode(value)

            val actual = JString.fromJsonNode(json, NodePathRoot).expectSuccess()

            expectThat(actual).isEqualTo(value)

            val jsonStr = JString.toJson(value)

            expectThat(JString.fromJson(jsonStr).expectSuccess()).isEqualTo(value)
        }
    }

    @Test
    fun `JsonNode String with Unicode`() {

        val value = "Pre \u263A Post"

        val json = JString.toJsonNode(value)

        val actual = JString.fromJsonNode(json, NodePathRoot).expectSuccess()

        expectThat(actual).isEqualTo(value)

        val jsonStr = JString.toJson(value)
        expectThat(jsonStr).isEqualTo(""""Pre ☺ Post"""")

        expectThat(JString.fromJson(jsonStr).expectSuccess()).isEqualTo(value)
    }

    @Test
    fun `JsonNode String with special characters`() {
        listOf("\\", "\n", "\b", "\r", "\t", "\"")
            .map { "foo${it}bar" }
            .forEach { value ->
                val json = JString.toJsonNode(value)
                val actual = JString.fromJsonNode(json, NodePathRoot).expectSuccess()
                expectThat(actual).isEqualTo(value)
                val jsonStr = JString.toJson(value)
                expectThat(JString.fromJson(jsonStr).expectSuccess()).isEqualTo(value)
            }
    }

    @Test
    fun `Json Float`() {
        repeat(10) {
            val value = Random.nextFloat()

            val json = JFloat.toJsonNode(value)
            val actual = JFloat.fromJsonNode(json, NodePathRoot).expectSuccess()
            expectThat(actual).isEqualTo(value)

            val jsonStr = JFloat.toJson(value)
            expectThat(JFloat.fromJson(jsonStr).expectSuccess()).isEqualTo(value)
        }
    }

    @Test
    fun `Json Double`() {
        repeat(10) {

            val value = Random.nextDouble()
            val json = JDouble.toJsonNode(value)

            val actual = JDouble.fromJsonNode(json, NodePathRoot).expectSuccess()

            expectThat(actual).isEqualTo(value)

            val jsonStr = JDouble.toJson(value)

            expectThat(JDouble.fromJson(jsonStr).expectSuccess()).isEqualTo(value)
        }
    }

    @Test
    fun `Json Int`() {

        repeat(10) {

            val value = Random.nextInt()
            val json = JInt.toJsonNode(value)

            val actual = JInt.fromJsonNode(json, NodePathRoot).expectSuccess()

            expectThat(actual).isEqualTo(value)

            val jsonStr = JInt.toJson(value)

            expectThat(JInt.fromJson(jsonStr).expectSuccess()).isEqualTo(value)
        }
    }

    @Test
    fun `Json Long`() {

        repeat(50) {

            val value = Random.nextLong()
            val json = JLong.toJsonNode(value)

            val actual = JLong.fromJsonNode(json, NodePathRoot).expectSuccess()

            expectThat(actual).isEqualTo(value)

            val jsonStr = JLong.toJson(value)

            expectThat(JLong.fromJson(jsonStr).expectSuccess()).isEqualTo(value)
        }
    }

    @Test
    fun `Json Boolean`() {

        repeat(3) {

            val value = Random.nextBoolean()
            val json = JBoolean.toJsonNode(value)

            val actual = JBoolean.fromJsonNode(json, NodePathRoot).expectSuccess()

            expectThat(actual).isEqualTo(value)

            val jsonStr = JBoolean.toJson(value)

            expectThat(JBoolean.fromJson(jsonStr).expectSuccess()).isEqualTo(value)
        }
    }

    @Test
    fun `Json Enum`() {

        val jTaxType = JEnumClass(TaxType::class)

        repeat(10) {

            val value = TaxType.values().random()
            val json = jTaxType.toJsonNode(value)

            val actual = jTaxType.fromJsonNode(json, NodePathRoot).expectSuccess()

            expectThat(actual).isEqualTo(value)

            val jsonStr = jTaxType.toJson(value)

            expectThat(jTaxType.fromJson(jsonStr).expectSuccess()).isEqualTo(value)
        }
    }

    @Test
    fun `Json UUID`() {
        repeat(10) {
            val value = UUID.randomUUID()

            val json = JUUID.toJsonNode(value)
            val actual = JUUID.fromJsonNode(json, NodePathRoot).expectSuccess()
            expectThat(actual).isEqualTo(value)

            val jsonStr = JUUID.toJson(value)
            expectThat(JUUID.fromJson(jsonStr).expectSuccess()).isEqualTo(value)
        }
    }

    @Test
    fun `json array for a List of Strings`() {

        repeat(10) {

            val value = randomList(0, 5) { randomString(text, 1, 10) }

            val node = JStringList.toJsonNode(value)

            val actual = JStringList.fromJsonNode(node, NodePathRoot).expectSuccess()

            expectThat(actual).isEqualTo(value)

            val jsonStr = JStringList.toJson(value)

            expectThat(JStringList.fromJson(jsonStr).expectSuccess()).isEqualTo(value)
        }
    }


    @Test
    fun `Json Person and back`() {

        repeat(10) {
            val value = randomPerson()
            val json = JPerson.toJsonNode(value)

            val actual = JPerson.fromJsonNode(json, NodePathRoot).expectSuccess()

            expectThat(actual).isEqualTo(value)

            val jsonStr = JPerson.toJson(value)

            expectThat(JPerson.fromJson(jsonStr).expectSuccess()).isEqualTo(value)
        }
    }


    @Test
    fun `json array for a Set of Person`() {

        repeat(10) {
            val jsonUserArray = JSet(JPerson)

            val value = randomList(0, 10) { randomPerson() }.toSet()

            val node = jsonUserArray.toJsonNode(value)

            val actual = jsonUserArray.fromJsonNode(node, NodePathRoot).expectSuccess()

            expectThat(actual).isEqualTo(value)

            val jsonStr = jsonUserArray.toJson(value)

            expectThat(jsonUserArray.fromJson(jsonStr).expectSuccess()).containsExactly(value)
        }
    }


    @Test
    fun `Json with nullable and back`() {

        repeat(100) {
            val product = randomProduct()
            val json = JProduct.toJsonNode(product)

            val actual = JProduct.fromJsonNode(json, NodePathRoot).expectSuccess()

            expectThat(actual).isEqualTo(product)

            val jsonStr = JProduct.toJson(product).printIt()

            expectThat(JProduct.fromJson(jsonStr).expectSuccess()).isEqualTo(product)

            val jsonStrNull = JProduct.toJson(product, JsonStyle.prettyWithNulls)

            expectThat(JProduct.fromJson(jsonStrNull).expectSuccess()).isEqualTo(product)

        }

    }


    @Test
    fun `Json with objects inside and back`() {

        repeat(100) {
            val invoice = randomInvoice()
            val json = JInvoice.toJsonNode(invoice)

            val actual = JInvoice.fromJsonNode(json, NodePathRoot).expectSuccess()

            expectThat(actual).isEqualTo(invoice)

            val jsonStr = JInvoice.toJson(invoice)

            expectThat(JInvoice.fromJson(jsonStr).expectSuccess()).isEqualTo(invoice)
        }
    }

    @Test
    fun `PrettyJson with objects inside and back`() {

        repeat(100) {
            val invoice = randomInvoice()
            val json = JInvoice.toJsonNode(invoice)

            val actual = JInvoice.fromJsonNode(json, NodePathRoot).expectSuccess()

            expectThat(actual).isEqualTo(invoice)

            val jsonStr = JInvoice.toJson(invoice, JsonStyle.pretty)

            expectThat(JInvoice.fromJson(jsonStr).expectSuccess()).isEqualTo(invoice)
        }
    }

    @Test
    fun `Compact Json with objects inside and back`() {

        repeat(100) {
            val invoice = randomInvoice()
            val json = JInvoice.toJsonNode(invoice)

            val actual = JInvoice.fromJsonNode(json, NodePathRoot).expectSuccess()

            expectThat(actual).isEqualTo(invoice)

            val jsonStr = JInvoice.toJson(invoice, JsonStyle.compact)

            expectThat(jsonStr).not().contains("\n")
            expectThat(JInvoice.fromJson(jsonStr).expectSuccess()).isEqualTo(invoice)
        }
    }


    @Test
    fun `JVariant and back`() {
        repeat(10) {
            val variant = randomVariant()

            val jsonStr = JVariant.toJson(variant, JsonStyle.pretty)

            expectThat(JVariant.fromJson(jsonStr).expectSuccess()).isEqualTo(variant)
        }
    }

    @Test
    fun `JSealed with default field`() {
        val json = """
            [{
              "name": "1",
              "type": "VariantString",
              "value": "1"
            },
            {
              "name": "2",
              "type": "VariantInt",
              "value": 2
            },
            {
              "name": "3!!",
              "value": "3"
            },
            {
              "name": "4",
              "type": "VariantInt",
              "value": 4
            }]

        """.trimIndent()

        val JVariants = JList(JVariant)

        val values = JVariants.fromJson(json).expectSuccess()
        expectThat(values).containsExactly(
            VariantString("1", "1"),
            VariantInt("2", 2),
            VariantString("3!!", "3"),
            VariantInt("4", 4),
        )

        val producedJson = JVariants.toJson(values)
        val valuesFromProducedJson = JVariants.fromJson(producedJson).expectSuccess()

        expectThat(valuesFromProducedJson).isEqualTo(values)
    }
}


//Possible extensions:
// add Converters for all java.time, GUUID, URI, etc.
// add un-typed option JObject<Any>
// add constant fields (ignoring Json content)
// add support to serialize calculated fields
