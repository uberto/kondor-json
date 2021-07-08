package com.ubertob.kondor.json

import com.ubertob.kondor.json.parser.pretty
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class JsonSchemaTest {

    @Test
    fun `schema for simple object`(){

        val schema = JPerson.schema().pretty(false, 2)

        expectThat(schema).isEqualTo(
            """{
              |  "properties": {
              |      "id": {
              |          "type": "Number"
              |        },
              |      "name": {
              |          "type": "String"
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
    fun `schema for complex object`(){

        val schema = JInvoice.schema().pretty(false, 2)

        expectThat(schema).isEqualTo(
            """{
              |  "properties": {
              |      "created_date": {
              |          "type": "String"
              |        },
              |      "customer": {
              |          "type": "Object"
              |        },
              |      "id": {
              |          "type": "String"
              |        },
              |      "items": {
              |          "items": {
              |              "properties": {
              |                  "id": {
              |                      "type": "Number"
              |                    },
              |                  "long_description": {
              |                      "type": "String"
              |                    },
              |                  "price": {
              |                      "type": "Number"
              |                    },
              |                  "short-desc": {
              |                      "type": "String"
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
              |          "type": "Number"
              |        },
              |      "total": {
              |          "type": "Number"
              |        },
              |      "vat-to-pay": {
              |          "type": "Boolean"
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
    fun `schema for object with optional fields`(){

        val schema = JProduct.schema().pretty(false, 2)

        expectThat(schema).isEqualTo(
            """{
              |  "properties": {
              |      "id": {
              |          "type": "Number"
              |        },
              |      "long_description": {
              |          "type": "String"
              |        },
              |      "price": {
              |          "type": "Number"
              |        },
              |      "short-desc": {
              |          "type": "String"
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

    @Disabled
    @Test
    fun `schema for object with enums fields`(){

        val schema = JCompany.schema().pretty(false, 2)

        //making it work with Enum
        expectThat(schema).isEqualTo(
            """{
              |  "properties": {
              |      "name": {
              |          "type": "String"
              |        },
              |      "tax_type": {
              |          "enum": [Domestic, Exempt, EU, US, Other]
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
}