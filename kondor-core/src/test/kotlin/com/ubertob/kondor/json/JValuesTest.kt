package com.ubertob.kondor.json

import com.ubertob.kondor.expectSuccess
import com.ubertob.kondor.json.datetime.JLocalDate
import com.ubertob.kondor.json.jsonnode.JsonNodeString
import com.ubertob.kondor.json.jsonnode.NodePathRoot
import com.ubertob.kondor.randomList
import com.ubertob.kondor.randomString
import com.ubertob.kondor.text
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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
    fun `Json LocalDate with custom format`() {
        val date = LocalDate.of(2020, 10, 15)
        val format = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val jLocalDate = JLocalDate.withFormatter(format)

        val dateAsdJsonNode = JsonNodeString("15/10/2020", NodePathRoot)
        expectThat(jLocalDate.toJsonNode(date, NodePathRoot)).isEqualTo(dateAsdJsonNode)
        expectThat(jLocalDate.fromJsonNode(dateAsdJsonNode).expectSuccess()).isEqualTo(date)

        val dateAsString = "\"15/10/2020\""
        expectThat(jLocalDate.toJson(date)).isEqualTo(dateAsString)
        expectThat(jLocalDate.fromJson(dateAsString).expectSuccess()).isEqualTo(date)
    }

    @Test
    fun `Json LocalDate with custom format as String`() {
        val date = LocalDate.of(2021, 1, 6)
        val format = "dd-MM-yyyy"
        val jLocalDate = JLocalDate.withPattern(format)

        val dateAsJsonNode = JsonNodeString("06-01-2021", NodePathRoot)
        expectThat(jLocalDate.toJsonNode(date, NodePathRoot)).isEqualTo(dateAsJsonNode)
        expectThat(jLocalDate.fromJsonNode(dateAsJsonNode).expectSuccess()).isEqualTo(date)

        val dateAsString = "\"06-01-2021\""
        expectThat(jLocalDate.toJson(date)).isEqualTo(dateAsString)
        expectThat(jLocalDate.fromJson(dateAsString).expectSuccess()).isEqualTo(date)
    }
}


//todo
// add parseJson from InputStream
// add a method to compare json equivalent content
// add test example with Java
// add Converters for all java.time, GUUID, URI, etc.
// measure performance against other libs
// measure performance under concurrency
// add un-typed option JObject<Any>
// add constant fields (ignoring Json content)
// add support to serialize calculated fields
// use Kotlin require() and check() contracts
