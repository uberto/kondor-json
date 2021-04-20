package com.ubertob.kondor.json

import com.ubertob.kondor.expectSuccess
import com.ubertob.kondor.randomList
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.time.Instant
import kotlin.random.Random

class JValuesExtraTest {

    @Test
    fun `Json Company`() {

        repeat(5) {

            val value = randomCompany()
            val json = JCompany.toJsonNode(value, NodePathRoot)

            val actual = JCompany.fromJsonNode(json).expectSuccess()

            expectThat(actual).isEqualTo(value)

            val jsonStr = JCompany.toJson(value)

            expectThat(JCompany.fromJson(jsonStr).expectSuccess()).isEqualTo(value)
        }
    }

    @Test
    fun `Json Customer`() {

        repeat(10) {

            val value = randomCustomer()
            val json = JCustomer.toJsonNode(value, NodePathRoot)

            val actual = JCustomer.fromJsonNode(json).expectSuccess()

            expectThat(actual).isEqualTo(value)

            val jsonStr = JCustomer.toJson(value)

            expectThat(JCustomer.fromJson(jsonStr).expectSuccess()).isEqualTo(value)
        }
    }

    @Test
    fun `Json ExpenseReport`() {

        repeat(10) {

            val value = randomExpenseReport()
            val json = JExpenseReport.toJsonNode(value, NodePathRoot)

            val actual = JExpenseReport.fromJsonNode(json).expectSuccess()

            expectThat(actual).isEqualTo(value)

            val jsonStr = JExpenseReport.toJson(value)

//            println(jsonStr)

            expectThat(JExpenseReport.fromJson(jsonStr).expectSuccess()).isEqualTo(value)
        }
    }

    @Test
    fun `Json Notes`() {

        repeat(10) {

            val value = randomNotes()
            val json = JNotes.toJsonNode(value, NodePathRoot)

            val actual = JNotes.fromJsonNode(json).expectSuccess()

            expectThat(actual).isEqualTo(value)

            val jsonStr = JNotes.toPrettyJson(value)

//            println(jsonStr)

            expectThat(JNotes.fromJson(jsonStr).expectSuccess()).isEqualTo(value)
        }
    }

    @Test
    fun `Json Products`() {

        repeat(10) {

            val value = Products.fromIterable(randomList(0, 10) { randomProduct() })
            val json = JProducts.toJsonNode(value, NodePathRoot)

            val actual = JProducts.fromJsonNode(json).expectSuccess()

            expectThat(actual).isEqualTo(value)

            val jsonStr = JProducts.toPrettyJson(value)

//            println(jsonStr)

            expectThat(JProducts.fromJson(jsonStr).expectSuccess()).isEqualTo(value)
        }
    }


    @Test
    fun `Json render flatten objects like fields`() {

        val selectedFile = SelectedFile(true, FileInfo("filename", Instant.ofEpochMilli(0), 123, "/"))

        val json = JSelectedFile.toPrettyJson(selectedFile)

        expectThat(json).isEqualTo(
            """{
  "date": 0,
  "folderPath": "/",
  "name": "filename",
  "selected": true,
  "size": 123
}""".trimIndent()
        )

    }


    @Test
    fun `Json SelectedFile`() {

        repeat(10) {

            val value = SelectedFile(Random.nextBoolean(), randomFileInfo())
            val json = JSelectedFile.toJsonNode(value, NodePathRoot)

            val actual = JSelectedFile.fromJsonNode(json).expectSuccess()

            expectThat(actual).isEqualTo(value)

            val jsonStr = JSelectedFile.toJson(value)

            expectThat(JSelectedFile.fromJson(jsonStr).expectSuccess()).isEqualTo(value)
        }
    }


}


