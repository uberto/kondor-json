package com.ubertob.kondor.json

import com.ubertob.kondor.json.JsonStyle.Companion.pretty
import com.ubertob.kondor.json.jsonnode.NodePathRoot
import com.ubertob.kondor.lowercase
import com.ubertob.kondor.randomList
import com.ubertob.kondor.randomString
import com.ubertob.kondor.validateJsonAgainstSchema
import com.ubertob.kondortools.expectSuccess
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import kotlin.random.Random

class JValuesExtraTest {

    @Test
    fun `Json Company`() {

        repeat(5) {

            val value = randomCompany()
            val json = JCompany.toJsonNode(value)

            val actual = JCompany.fromJsonNode(json, NodePathRoot).expectSuccess()

            expectThat(actual).isEqualTo(value)

            val jsonStr = JCompany.toJson(value)

            expectThat(JCompany.fromJson(jsonStr).expectSuccess()).isEqualTo(value)
        }
    }


    @Test
    fun `JSealed Customer`() {

        repeat(10) {

            val value = randomCustomer()
            val json = JCustomer.toJsonNode(value)

            val actual = JCustomer.fromJsonNode(json, NodePathRoot).expectSuccess()

            expectThat(actual).isEqualTo(value)

            val jsonStr = JCustomer.toJson(value)

            expectThat(JCustomer.fromJson(jsonStr).expectSuccess()).isEqualTo(value)
        }
    }

    @Test
    fun `JSealed rendered correctly with discriminant field`() {

        val expected =
            """[
              |  {
              |    "id": 1,
              |    "name": "Adam",
              |    "type": "private"
              |  },
              |  {
              |    "name": "Acme",
              |    "tax_type": "US",
              |    "type": "company"
              |  }
              |]""".trimMargin()
        val customers = listOf(
            Person(1, "Adam"),
            Company("Acme", TaxType.US)
        )
        val json = JList(JCustomer).toJson(customers, pretty)

        expectThat(json).isEqualTo(expected)

    }

    @Test
    fun `Json ExpenseReport`() {

        repeat(10) {

            val value = randomExpenseReport()
            val json = JExpenseReport.toJsonNode(value)

            val actual = JExpenseReport.fromJsonNode(json, NodePathRoot).expectSuccess()

            expectThat(actual).isEqualTo(value)

            val jsonStr = JExpenseReport.toJson(value)

            expectThat(JExpenseReport.fromJson(jsonStr).expectSuccess()).isEqualTo(value)
        }
    }

    @Test
    fun `Json Notes`() {

        repeat(10) {

            val value = randomNotes()
            val json = JNotes.toJsonNode(value)

            val actual = JNotes.fromJsonNode(json, NodePathRoot).expectSuccess()

            expectThat(actual).isEqualTo(value)

            val jsonStr = JNotes.toJson(value, pretty)

//            println(jsonStr)

            expectThat(JNotes.fromJson(jsonStr).expectSuccess()).isEqualTo(value)
        }
    }

    @Test
    fun `Json Tasks`() {
        repeat(10) {
            val value = randomTasks()

            val json = JTasks.toJsonNode(value)

            val actual = JTasks.fromJsonNode(json, NodePathRoot).expectSuccess()

            expectThat(actual).isEqualTo(value)

            val jsonStr = JTasks.toJson(value, pretty)

            expectThat(JTasks.fromJson(jsonStr).expectSuccess()).isEqualTo(value)
        }
    }

    @Test
    fun `JMap schema is valid`() {
        val value = randomTasks()

        val json = JTasks.toJson(value)
        val schemaJson = JTasks.schema().render(pretty)

        validateJsonAgainstSchema(schemaJson, json)
    }

    @Test
    fun `JMaps without a specified keyConverter do not double quote`() {
        val map = mapOf(
            "foo1" to "bar1",
            "foo2" to "bar2"
        )

        expectThat(JMap(JString).toJson(map, pretty)).isEqualTo(
            """{
                |  "foo1": "bar1",
                |  "foo2": "bar2"
                |}
            """.trimMargin()
        )
    }

    @Test
    fun `Json Products`() {

        repeat(10) {

            val value = Products.fromIterable(randomList(0, 10) { randomProduct() })
            val json = JProducts.toJsonNode(value)

            val actual = JProducts.fromJsonNode(json, NodePathRoot).expectSuccess()

            expectThat(actual).isEqualTo(value)

            val jsonStr = JProducts.toJson(value, pretty)

            expectThat(JProducts.fromJson(jsonStr).expectSuccess()).isEqualTo(value)
        }
    }


    @Test
    fun `render pretty Json Invoice`() {

        val invoice = Invoice(
            InvoiceId("abc-1234"),
            false,
            Company("Acme", TaxType.US),
            listOf(
                Product(1, "xs", "looooog desc", 123.45),
                Product(2, "xs2", "looooog desc2", null)
            ),
            125.toBigDecimal(),
            LocalDate.of(2024, Month.SEPTEMBER, 25),
            Instant.ofEpochSecond(1727257098)
        )
        val jsonPretty = JInvoice.toJson(invoice, pretty)

        expectThat(JInvoice.fromJson(jsonPretty).expectSuccess()).isEqualTo(invoice)

        expectThat(jsonPretty).isEqualTo(
            """|{
                |  "created_date": "2024-09-25",
                |  "customer": {
                |    "name": "Acme",
                |    "tax_type": "US",
                |    "type": "company"
                |  },
                |  "id": "abc-1234",
                |  "items": [
                |    {
                |      "id": 1,
                |      "long_description": "looooog desc",
                |      "price": 123.45,
                |      "short-desc": "xs"
                |    },
                |    {
                |      "id": 2,
                |      "long_description": "looooog desc2",
                |      "short-desc": "xs2"
                |    }
                |  ],
                |  "paid_datetime": 1727257098000,
                |  "total": 125,
                |  "vat-to-pay": false
                |}""".trimMargin()
        )
    }


    @Test
    fun `Json render flatten objects like fields`() {

        val selectedFile = SelectedFile(
            true, FileInfo(
                name = "filename",
                date = Instant.parse("2021-07-01T10:15:30Z"),
                isDir = false,
                size = 123,
                folderPath = "/tmp"
            )
        )

        val json = JSelectedFile.toJson(selectedFile, pretty)

        expectThat(json).isEqualTo(
            """{
              |  "creation_date": 1625134530000,
              |  "file_name": "filename",
              |  "folder_path": "/tmp",
              |  "is_dir": false,
              |  "selected": true,
              |  "size": 123
              |}""".trimMargin()
        )

    }

    @Test
    fun `Json render flatten maps like fields`() {

        val metadataFile = MetadataFile(
            filename = "myfile",
            metadata = mapOf("type" to "picture", "owner" to "uberto")
        )

        val json = JMetadataFile.toJson(metadataFile, pretty)

        expectThat(json).isEqualTo(
            """{
              |  "fileName": "myfile",
              |  "owner": "uberto",
              |  "type": "picture"
              |}""".trimMargin()
        )

    }


    @Test
    fun `Json SelectedFile`() {

        repeat(10) {

            val value = SelectedFile(Random.nextBoolean(), randomFileInfo())
            val json = JSelectedFile.toJsonNode(value)

            val actual = JSelectedFile.fromJsonNode(json, NodePathRoot).expectSuccess()

            expectThat(actual).isEqualTo(value)

            val jsonStr = JSelectedFile.toJson(value)

            expectThat(JSelectedFile.fromJson(jsonStr).expectSuccess()).isEqualTo(value)
        }
    }

    @Test
    fun `Json SelectedFile old format`() {
        repeat(10) {
            val value = SelectedFile(Random.nextBoolean(), randomFileInfo())
            val jsonStr = JSelectedFile.toJson(value)
            println("[DEBUG_LOG] JSelectedFile JSON (flattened): $jsonStr")
            val result = JSelectedFile.fromJson(jsonStr).expectSuccess()
            expectThat(result).isEqualTo(value)
        }
    }

    @Test
    fun `Json FileInfo new format`() {

        val fileInfo = FileInfo(
            "file name",
            Instant.parse("2021-07-01T10:15:30Z"),
            false,
            1234,
            "/home/"
        )

        val json = JFileInfoNew.toJson(fileInfo)

        val obj = JFileInfoNew.fromJson(json).expectSuccess()
        expectThat(obj).isEqualTo(fileInfo)

    }

    @Test
    fun `Json SelectedFile new format`() {
        repeat(10) {
            val value = SelectedFile(Random.nextBoolean(), randomFileInfo())
            val jsonStr = JSelectedFileNew.toJson(value)
            val result = JSelectedFileNew.fromJson(jsonStr).expectSuccess()
            expectThat(result).isEqualTo(value)
        }
    }

    @Test
    fun `Json with flatten map is rendered and parsed correctly`() {

        repeat(10) {

            val value = MetadataFile(randomString(lowercase, 3, 20), randomMetadata())
            val json = JMetadataFile.toJsonNode(value)

            val actual = JMetadataFile.fromJsonNode(json, NodePathRoot).expectSuccess()

            expectThat(actual).isEqualTo(value)

            val jsonStr = JMetadataFile.toJson(value)

            expectThat(JMetadataFile.fromJson(jsonStr).expectSuccess()).isEqualTo(value)
        }
    }

    @Test
    fun `Json with flatten map is pretty rendered correctly`() {

        val value = MetadataFile("myfile", mapOf("canWrite" to "N", "canRead" to "Y", "owner" to "adam"))
        val json = JMetadataFile.toJson(value, pretty)

        val expected = """{
              |  "canRead": "Y",
              |  "canWrite": "N",
              |  "fileName": "myfile",
              |  "owner": "adam"
              |}""".trimMargin()

        expectThat(json).isEqualTo(expected)

        val actual = JMetadataFile.fromJson(json).expectSuccess()

        expectThat(actual).isEqualTo(value)

    }

    @Test
    fun `Json TitleRequest`() {

        repeat(5) {

            val value = TitleRequest(randomString(lowercase, 5, 5), TitleType.values().random())
            val json = JTitleRequest.toJsonNode(value)

            val actual = JTitleRequest.fromJsonNode(json, NodePathRoot).expectSuccess()

            expectThat(actual).isEqualTo(value)

            val jsonStr = JTitleRequest.toJson(value)

            expectThat(JTitleRequest.fromJson(jsonStr).expectSuccess()).isEqualTo(value)
        }
    }

    @Test
    fun `Json With Errors to Null`() {

        repeat(5) {

            val value = TitleRequest(randomString(lowercase, 5, 5), TitleType.values().random())
            val json = JTitleRequest.toJsonNode(value)

            val actual = JTitleRequest.fromJsonNode(json, NodePathRoot).expectSuccess()

            expectThat(actual).isEqualTo(value)

            val jsonStr = JTitleRequest.toJson(value)

            expectThat(JTitleRequest.fromJson(jsonStr).expectSuccess()).isEqualTo(value)
        }
    }

    @Test
    fun `Json schema with fields called path and jsonNode`() {

        repeat(5) {
            val value = GraphNode(
                randomString(lowercase, 5, 5), randomString(lowercase, 5, 5), randomString(lowercase, 5, 5)
            )

            val json = JGraphNode.toJsonNode(value)

            val actual = JGraphNode.fromJsonNode(json, NodePathRoot).expectSuccess()

            expectThat(actual).isEqualTo(value)

            val jsonStr = JGraphNode.toJson(value)

            expectThat(JGraphNode.fromJson(jsonStr).expectSuccess()).isEqualTo(value)

        }
    }


    @Test
    fun `render object with jsonNode field and back`() {

        repeat(10) {
            val objWithDynamicAttr = randomObjectWithDynamicAttr()

            val json = JDynamicAttr.toJsonNode(objWithDynamicAttr)

            val actual = JDynamicAttr.fromJsonNode(json, NodePathRoot).expectSuccess()

            expectThat(actual).isEqualTo(objWithDynamicAttr)

            val jsonStr = JDynamicAttr.toJson(objWithDynamicAttr)

            expectThat(JDynamicAttr.fromJson(jsonStr).expectSuccess()).isEqualTo(objWithDynamicAttr)
        }
    }
}
