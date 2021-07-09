package com.ubertob.kondor.json

import com.ubertob.kondor.json.parser.pretty
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class JsonSchemaTest {

    @Test
    fun `schema for simple object`(){

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
    fun `schema for object with optional fields`(){

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
    fun `schema for object with enums fields`(){

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
    fun `schema for array of objects`(){

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
    fun `schema for object map`(){

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
    fun `schema for complex object`(){

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
}