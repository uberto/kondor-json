package com.ubertob.kondor.json

import com.ubertob.kondor.randomList
import com.ubertob.kondortools.expectSuccess
import com.ubertob.kondortools.printIt
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class JDataClassTest {
    @Test
    fun `Json Person and back`() {

        repeat(10) {
            val value = randomPerson()
            val json = Person.Json.toJsonNode(value)

            val actual = Person.Json.fromJsonNode(json).expectSuccess()

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
            val json = Product.Json.toJsonNode(product)

            val actual = Product.Json.fromJsonNode(json).expectSuccess()

            expectThat(actual).isEqualTo(product)

            val jsonStr = Product.Json.toJson(product).printIt()

            expectThat(Product.Json.fromJson(jsonStr).expectSuccess()).isEqualTo(product)

            val jsonStrNull = Product.Json.toJson(product, JsonStyle.prettyWithNulls)

            expectThat(Product.Json.fromJson(jsonStrNull).expectSuccess()).isEqualTo(product)

        }

    }

    @Test
    fun `Json with objects inside and back`() {

        repeat(100) {
            val invoice = randomInvoice()
            val json = Invoice.Json.toJsonNode(invoice)

            val actual = Invoice.Json.fromJsonNode(json).expectSuccess()

            expectThat(actual).isEqualTo(invoice)

            val jsonStr = Invoice.Json.toJson(invoice)

            expectThat(Invoice.Json.fromJson(jsonStr).expectSuccess()).isEqualTo(invoice)
        }
    }
}