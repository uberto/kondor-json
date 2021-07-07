package com.ubertob.kondor.json

import com.ubertob.kondor.json.parser.pretty
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
              |          "type": "Array"
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
              |  "type": "object"
              |}""".trimMargin()
        )
    }

    @Test
    fun `schema for medium object`(){

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
              |  "type": "object"
              |}""".trimMargin()
        )
    }
}