package com.ubertob.kondor.json

import com.ubertob.kondor.json.parser.pretty
import com.ubertob.kondor.json.parser.render
import org.junit.jupiter.api.Test
import org.leadpony.justify.api.JsonSchema
import org.leadpony.justify.api.JsonValidationService
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure


class JsonSchemaTest {

    @Test
    fun `schema for simple object`() {

        val schema = JPerson.schema().pretty()

        expectThat(schema).isEqualTo(
            """{
              |  "properties": {
              |      "id": {
              |          "type": "number"
              |        },
              |      "name": {
              |          "type": "string"
              |        }
              |    },
              |  "required": [
              |      "id",
              |      "name"
              |    ],
              |  "type": "object"
              |}""".trimMargin()
        )
    }


    @Test
    fun `schema for object with optional fields`() {

        val schema = JProduct.schema().pretty()

        expectThat(schema).isEqualTo(
            """{
              |  "properties": {
              |      "id": {
              |          "type": "number"
              |        },
              |      "long_description": {
              |          "type": "string"
              |        },
              |      "price": {
              |          "type": "number"
              |        },
              |      "short-desc": {
              |          "type": "string"
              |        }
              |    },
              |  "required": [
              |      "id",
              |      "long_description",
              |      "short-desc"
              |    ],
              |  "type": "object"
              |}""".trimMargin()
        )
    }

    @Test
    fun `schema for object with enums fields`() {

        val schema = JCompany.schema().pretty()

        expectThat(schema).isEqualTo(
            """{
              |  "properties": {
              |      "name": {
              |          "type": "string"
              |        },
              |      "tax_type": {
              |          "enum": [
              |              "Domestic",
              |              "Exempt",
              |              "EU",
              |              "US",
              |              "Other"
              |            ]
              |        }
              |    },
              |  "required": [
              |      "name",
              |      "tax_type"
              |    ],
              |  "type": "object"
              |}""".trimMargin()
        )
    }

    @Test
    fun `schema for array of objects`() {

        val schema = JProducts.schema().pretty()

        expectThat(schema).isEqualTo(
            """{
              |  "items": {
              |      "properties": {
              |          "id": {
              |              "type": "number"
              |            },
              |          "long_description": {
              |              "type": "string"
              |            },
              |          "price": {
              |              "type": "number"
              |            },
              |          "short-desc": {
              |              "type": "string"
              |            }
              |        },
              |      "required": [
              |          "id",
              |          "long_description",
              |          "short-desc"
              |        ],
              |      "type": "object"
              |    },
              |  "type": "array"
              |}""".trimMargin()
        )
    }


    @Test
    fun `schema for object map`() {

        val schema = JNotes.schema().pretty()

        expectThat(schema).isEqualTo(
            """{
              |  "properties": {
              |      "things_to_do": {
              |          "type": "object"
              |        },
              |      "updated": {
              |          "type": "string"
              |        }
              |    },
              |  "required": [
              |      "updated",
              |      "things_to_do"
              |    ],
              |  "type": "object"
              |}""".trimMargin()
        )
    }


    @Test
    fun `schema for a flattened object`() {

        val schema = JSelectedFile.schema().pretty()

        expectThat(schema).isEqualTo(
            """{
              |  "properties": {
              |      "creation_date": {
              |          "type": "number"
              |        },
              |      "file_name": {
              |          "type": "string"
              |        },
              |      "folder_path": {
              |          "type": "string"
              |        },
              |      "is_dir": {
              |          "type": "boolean"
              |        },
              |      "selected": {
              |          "type": "boolean"
              |        },
              |      "size": {
              |          "type": "number"
              |        }
              |    },
              |  "required": [
              |      "selected",
              |      "file_name",
              |      "creation_date",
              |      "is_dir",
              |      "size",
              |      "folder_path"
              |    ],
              |  "type": "object"
              |}""".trimMargin()
        )
    }


    @Test
    fun `schema for complex object`() {

        val schema = JInvoice.schema().pretty(false, 2)

        expectThat(schema).isEqualTo(
            """{
              |  "properties": {
              |      "created_date": {
              |          "type": "string"
              |        },
              |      "customer": {
              |          "type": "object"
              |        },
              |      "id": {
              |          "type": "string"
              |        },
              |      "items": {
              |          "items": {
              |              "properties": {
              |                  "id": {
              |                      "type": "number"
              |                    },
              |                  "long_description": {
              |                      "type": "string"
              |                    },
              |                  "price": {
              |                      "type": "number"
              |                    },
              |                  "short-desc": {
              |                      "type": "string"
              |                    }
              |                },
              |              "required": [
              |                  "id",
              |                  "long_description",
              |                  "short-desc"
              |                ],
              |              "type": "object"
              |            },
              |          "type": "array"
              |        },
              |      "paid_datetime": {
              |          "type": "number"
              |        },
              |      "total": {
              |          "type": "number"
              |        },
              |      "vat-to-pay": {
              |          "type": "boolean"
              |        }
              |    },
              |  "required": [
              |      "id",
              |      "vat-to-pay",
              |      "customer",
              |      "items",
              |      "total",
              |      "created_date"
              |    ],
              |  "type": "object"
              |}""".trimMargin()
        )
    }

    @Test
    fun `validate an Json against its Schema`() {

        val schema = JInvoice.schema().render()
        val schemaJson = schemaService(schema)
        repeat(10) {
            val json = JInvoice.toJson(randomInvoice())

            validateJsonAgainstSchema(schemaJson, json)
        }
    }

    @Test
    fun `validating an Json against another Schema will give a failure`() {

        val schema = JPerson.schema().render()
        val schemaJson = schemaService(schema)
        val json = JCompany.toJson(randomCompany())

        expectCatching {
            validateJsonAgainstSchema(schemaJson, json)
        }.isFailure()
            .get { message.orEmpty() }.contains("The object must have a property whose name is \"id\"")
    }

    val service = JsonValidationService.newInstance()

    private fun schemaService(schemaJson: String): JsonSchema =
        service.readSchema(schemaJson.byteInputStream())


    private fun validateJsonAgainstSchema(jsonConfig: JsonSchema, json: String) {
        val handler = service.createProblemPrinter { error("Schema validation error: $it") }
        service.createReader(json.byteInputStream(), jsonConfig, handler)
            .use { reader -> reader.readValue() }
    }
}