package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.NodePathRoot
import com.ubertob.kondor.outcome.Failure
import com.ubertob.kondor.randomList
import com.ubertob.kondortools.expectSuccess
import com.ubertob.kondortools.printIt
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isA
import strikt.assertions.isEqualTo

class JDataClassTest {

    @Test
    fun `Json Person pretty rendering`() {

        val expectedJson = """
            {
              "id": 1234,
              "name": "John Smith"
            }
        """.trimIndent()
        val value = Person(1234, "John Smith")
        val jsonStr = Person.Json.toJson(value, JsonStyle.prettyWithNulls)

        expectThat(Person.Json.fromJson(jsonStr).expectSuccess()).isEqualTo(value)
        expectThat(jsonStr).isEqualTo(expectedJson)
    }

    @Test
    fun `Json Person and back`() {

        repeat(10) {
            val value = randomPerson()
            val json = Person.Json.toJsonNode(value)

            val actual = Person.Json.fromJsonNode(json, NodePathRoot).expectSuccess()

            expectThat(actual).isEqualTo(value)

            val jsonStr = Person.Json.toJson(value)

            expectThat(Person.Json.fromJson(jsonStr).expectSuccess()).isEqualTo(value)
        }
    }


    @Test
    fun `json array for a Set of Person`() {

        repeat(10) {
            val jsonUserArray = JSet(Person.Json)

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
            val json = Product.Json.toJsonNode(product)

            val actual = Product.Json.fromJsonNode(json, NodePathRoot).expectSuccess()

            expectThat(actual).isEqualTo(product)

            val jsonStr = Product.Json.toJson(product).printIt()

            expectThat(Product.Json.fromJson(jsonStr).expectSuccess()).isEqualTo(product)

            val jsonStrNull = Product.Json.toJson(product, JsonStyle.prettyWithNulls)

            val newProduct = Product.Json.fromJson(jsonStrNull).expectSuccess()
            expectThat(newProduct).isEqualTo(product)

        }

    }

    @Test
    fun `Json with objects inside and back`() {

        repeat(100) {
            val invoice = randomInvoice()
            val json = Invoice.Json.toJsonNode(invoice)

            val actual = Invoice.Json.fromJsonNode(json, NodePathRoot).expectSuccess()

            expectThat(actual).isEqualTo(invoice)

            val jsonStr = Invoice.Json.toJson(invoice)

            expectThat(Invoice.Json.fromJson(jsonStr).expectSuccess()).isEqualTo(invoice)
        }
    }

    @Test
    fun `Json DataClass converter auto test`() {
        Invoice.Json.testParserAndRender { randomInvoice() }
        Person.Json.testParserAndRender { randomPerson() }
    }

    object WrongJPerson : JDataClass<Person>(Person::class) {
        val name by str(Person::name)
        val id by num(Person::id)
    }

    @Test
    fun `Json DataClass converter failing test`() {
        val person = randomPerson()
        val json = WrongJPerson.toJson(person)
        val res = WrongJPerson.fromJson(json)

        expectThat(res).isA<Failure<JsonError>>()
        val error = (res as Failure<JsonError>).error

        expectThat(error.reason).isEqualTo("Error calling constructor with signature [int, String] using params {name=${person.name}, id=${person.id}}")
    }


    object PersonRefl : JDataClassAuto<Person>(Person::class)

    @Test
    fun `JDataClassAuto doesn't need the fields declaration`() {

        PersonRefl.registerAllProperties() //temp hack

        PersonRefl.toJson(randomPerson(), JsonStyle.prettyWithNulls)

        PersonRefl.testParserAndRender(100) { randomPerson() }

    }
}