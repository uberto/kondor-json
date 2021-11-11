package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.NodePathRoot
import com.ubertob.kondor.randomList
import com.ubertob.kondor.randomString
import com.ubertob.kondor.text
import com.ubertob.kondortools.expectSuccess
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import kotlin.random.Random

class JValuesTest {

    @Test
    fun `JsonNode String`() {
        repeat(100) {
            val value = randomString(text, 0, 30)

            val json = JString.toJsonNode(value, NodePathRoot)

            val actual = JString.fromJsonNode(json).expectSuccess()

            expectThat(actual).isEqualTo(value)

            val jsonStr = JString.toJson(value)

            expectThat(JString.fromJson(jsonStr).expectSuccess()).isEqualTo(value)
        }
    }

    @Test
    fun `JsonNode String with Unicode`() {

            val value = "Pre \u263A Post"

            val json = JString.toJsonNode(value, NodePathRoot)

            val actual = JString.fromJsonNode(json).expectSuccess()

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
                val json = JString.toJsonNode(value, NodePathRoot)
                val actual = JString.fromJsonNode(json).expectSuccess()
                expectThat(actual).isEqualTo(value)
                val jsonStr = JString.toJson(value)
                expectThat(JString.fromJson(jsonStr).expectSuccess()).isEqualTo(value)
            }
    }

    @Test
    fun `Json Double`() {
        repeat(10) {

            val value = Random.nextDouble()
            val json = JDouble.toJsonNode(value, NodePathRoot)

            val actual = JDouble.fromJsonNode(json).expectSuccess()

            expectThat(actual).isEqualTo(value)

            val jsonStr = JDouble.toJson(value)

            expectThat(JDouble.fromJson(jsonStr).expectSuccess()).isEqualTo(value)
        }
    }

    @Test
    fun `Json Int`() {

        repeat(10) {

            val value = Random.nextInt()
            val json = JInt.toJsonNode(value, NodePathRoot)

            val actual = JInt.fromJsonNode(json).expectSuccess()

            expectThat(actual).isEqualTo(value)

            val jsonStr = JInt.toJson(value)

            expectThat(JInt.fromJson(jsonStr).expectSuccess()).isEqualTo(value)
        }
    }

    @Test
    fun `Json Long`() {

        repeat(10) {

            val value = Random.nextLong()
            val json = JLong.toJsonNode(value, NodePathRoot)

            val actual = JLong.fromJsonNode(json).expectSuccess()

            expectThat(actual).isEqualTo(value)

            val jsonStr = JLong.toJson(value)

            expectThat(JLong.fromJson(jsonStr).expectSuccess()).isEqualTo(value)
        }
    }

    @Test
    fun `Json Boolean`() {

        repeat(3) {

            val value = Random.nextBoolean()
            val json = JBoolean.toJsonNode(value, NodePathRoot)

            val actual = JBoolean.fromJsonNode(json).expectSuccess()

            expectThat(actual).isEqualTo(value)

            val jsonStr = JBoolean.toJson(value)

            expectThat(JBoolean.fromJson(jsonStr).expectSuccess()).isEqualTo(value)
        }
    }

    @Test
    fun `json array for a List of Strings`() {

        repeat(10) {

            val value = randomList(0, 5) { randomString(text, 1, 10) }

            val node = JStringList.toJsonNode(value, NodePathRoot)

            val actual = JStringList.fromJsonNode(node).expectSuccess()

            expectThat(actual).isEqualTo(value)

            val jsonStr = JStringList.toJson(value)

            expectThat(JStringList.fromJson(jsonStr).expectSuccess()).isEqualTo(value)
        }
    }


    @Test
    fun `Json Person and back`() {

        repeat(10) {
            val value = randomPerson()
            val json = JPerson.toJsonNode(value, NodePathRoot)

            val actual = JPerson.fromJsonNode(json).expectSuccess()

            expectThat(actual).isEqualTo(value)

            val jsonStr = JPerson.toJson(value)

            expectThat(JPerson.fromJson(jsonStr).expectSuccess()).isEqualTo(value)
        }
    }


    @Test
    fun `json array for a Set of Customers`() {

        repeat(10) {
            val jsonUserArray = JSet(JPerson)

            val value = randomList(0, 10) { randomPerson() }.toSet()

            val node = jsonUserArray.toJsonNode(value, NodePathRoot)

            val actual = jsonUserArray.fromJsonNode(node).expectSuccess()

            expectThat(actual).isEqualTo(value)

            val jsonStr = jsonUserArray.toJson(value)

            expectThat(jsonUserArray.fromJson(jsonStr).expectSuccess()).isEqualTo(value)
        }
    }


    @Test
    fun `Json with nullable and back`() {

        repeat(100) {
            val product = randomProduct()
            val json = JProduct.toJsonNode(product, NodePathRoot)

            val actual = JProduct.fromJsonNode(json).expectSuccess()

            expectThat(actual).isEqualTo(product)

            val jsonStr = JProduct.toJson(product)

            expectThat(JProduct.fromJson(jsonStr).expectSuccess()).isEqualTo(product)

            val jsonStrNull = JProduct.toNullJson(product)

            expectThat(JProduct.fromJson(jsonStrNull).expectSuccess()).isEqualTo(product)

        }

    }


    @Test
    fun `Json with objects inside and back`() {

        repeat(100) {
            val invoice = randomInvoice()
            val json = JInvoice.toJsonNode(invoice, NodePathRoot)

            val actual = JInvoice.fromJsonNode(json).expectSuccess()

            expectThat(actual).isEqualTo(invoice)

            val jsonStr = JInvoice.toJson(invoice)


            expectThat(JInvoice.fromJson(jsonStr).expectSuccess()).isEqualTo(invoice)
        }
    }

    @Test
    fun `PrettyJson with objects inside and back`() {

        repeat(100) {
            val invoice = randomInvoice()
            val json = JInvoice.toJsonNode(invoice, NodePathRoot)

            val actual = JInvoice.fromJsonNode(json).expectSuccess()

            expectThat(actual).isEqualTo(invoice)

            val jsonStr = JInvoice.toPrettyJson(invoice)

            expectThat(JInvoice.fromJson(jsonStr).expectSuccess()).isEqualTo(invoice)
        }
    }



    @Test
    fun `JVariant and back`(){
        repeat(10) {
            val variant = randomVariant()

            val jsonStr = JVariant.toPrettyJson(variant)
            println(jsonStr)

            expectThat(JVariant.fromJson(jsonStr).expectSuccess()).isEqualTo(variant)
        }
    }

    @Test
    fun `JSealed with default field`(){
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
    }
}


//todo
// add test example with Java
// add Converters for all java.time, GUUID, URI, etc.
// add un-typed option JObject<Any>
// add constant fields (ignoring Json content)
// add support to serialize calculated fields
