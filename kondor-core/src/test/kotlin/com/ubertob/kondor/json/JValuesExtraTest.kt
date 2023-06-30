package com.ubertob.kondor.json

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

            val jsonStr = JNotes.toJson(value, JsonRenderer.pretty)

//            println(jsonStr)

            expectThat(JNotes.fromJson(jsonStr).expectSuccess()).isEqualTo(value)
        }
    }

    @Test
    fun `Json Tasks`() {
        repeat(10) {
            val value = randomTasks()

            val json = JTasks.toJsonNode(value, NodePathRoot)

            val actual = JTasks.fromJsonNode(json).expectSuccess()

            expectThat(actual).isEqualTo(value)

            val jsonStr = JTasks.toJson(value, JsonRenderer.pretty)

            expectThat(JTasks.fromJson(jsonStr).expectSuccess()).isEqualTo(value)
        }
    }

    @Test
    fun `JMap schema is valid`() {
        val value = randomTasks()

        val json = JTasks.toJson(value)
        val schemaJson = JTasks.schema().pretty()

        validateJsonAgainstSchema(schemaJson, json)
    }

    @Test
    fun `JMaps without a specified keyConverter do not double quote`() {
        val map = mapOf(
            "foo1" to "bar1",
            "foo2" to "bar2"
        )

        expectThat(JMap(JString).toJson(map, JsonRenderer.pretty)).isEqualTo(
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
            val json = JProducts.toJsonNode(value, NodePathRoot)

            val actual = JProducts.fromJsonNode(json).expectSuccess()

            expectThat(actual).isEqualTo(value)

            val jsonStr = JProducts.toJson(value, JsonRenderer.pretty)

//            println(jsonStr)

            expectThat(JProducts.fromJson(jsonStr).expectSuccess()).isEqualTo(value)
        }
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

        val json = JSelectedFile.toJson(selectedFile, JsonRenderer.pretty)

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

        val json = JMetadataFile.toJson(metadataFile, JsonRenderer.pretty)

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
            val json = JSelectedFile.toJsonNode(value, NodePathRoot)

            val actual = JSelectedFile.fromJsonNode(json).expectSuccess()

            expectThat(actual).isEqualTo(value)

            val jsonStr = JSelectedFile.toJson(value)

            expectThat(JSelectedFile.fromJson(jsonStr).expectSuccess()).isEqualTo(value)
        }
    }

    @Test
    fun `Json MetadataFile`() {

        repeat(10) {

            val value = MetadataFile(randomString(lowercase, 3, 20), randomMetadata())
            val json = JMetadataFile.toJsonNode(value, NodePathRoot)

            val actual = JMetadataFile.fromJsonNode(json).expectSuccess()

            expectThat(actual).isEqualTo(value)

            val jsonStr = JMetadataFile.toJson(value)

            expectThat(JMetadataFile.fromJson(jsonStr).expectSuccess()).isEqualTo(value)
        }
    }


    @Test
    fun `Json TitleRequest`() {

        repeat(5) {

            val value = TitleRequest(randomString(lowercase, 5, 5), TitleType.values().random())
            val json = JTitleRequest.toJsonNode(value, NodePathRoot)

            val actual = JTitleRequest.fromJsonNode(json).expectSuccess()

            expectThat(actual).isEqualTo(value)

            val jsonStr = JTitleRequest.toJson(value)

//            println( jsonStr)

            expectThat(JTitleRequest.fromJson(jsonStr).expectSuccess()).isEqualTo(value)
        }
    }

    @Test
    fun `Json With Errors to Null`() {

        repeat(5) {

            val value = TitleRequest(randomString(lowercase, 5, 5), TitleType.values().random())
            val json = JTitleRequest.toJsonNode(value, NodePathRoot)

            val actual = JTitleRequest.fromJsonNode(json).expectSuccess()

            expectThat(actual).isEqualTo(value)

            val jsonStr = JTitleRequest.toJson(value)

//            println( jsonStr)

            expectThat(JTitleRequest.fromJson(jsonStr).expectSuccess()).isEqualTo(value)
        }
    }

    @Test
    fun `Json schema with fields called path and jsonNode`() {

        repeat(5) {
            val value =
                GraphNode(randomString(lowercase, 5, 5), randomString(lowercase, 5, 5), randomString(lowercase, 5, 5),)
            val json = JGraphNode.toJsonNode(value, NodePathRoot)

            val actual = JGraphNode.fromJsonNode(json).expectSuccess()

            expectThat(actual).isEqualTo(value)

            val jsonStr = JGraphNode.toJson(value)

//            println(jsonStr)

            expectThat(JGraphNode.fromJson(jsonStr).expectSuccess()).isEqualTo(value)

        }
    }


    @Test
    fun `render object with jsonNode field and back`() {

        repeat(10) {
            val objWithDynamicAttr = randomObjectWithDynamicAttr()

            val json = JDynamicAttr.toJsonNode(objWithDynamicAttr, NodePathRoot)

            val actual = JDynamicAttr.fromJsonNode(json).expectSuccess()

            expectThat(actual).isEqualTo(objWithDynamicAttr)

            val jsonStr = JDynamicAttr.toJson(objWithDynamicAttr)

            expectThat(JDynamicAttr.fromJson(jsonStr).expectSuccess()).isEqualTo(objWithDynamicAttr)
        }
    }
}


