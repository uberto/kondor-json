package com.ubertob.kondor.json

import com.ubertob.kondor.json.JsonStyle.Companion.pretty
import com.ubertob.kondor.validateJsonAgainstSchema
import com.ubertob.kondortools.printIt
import org.junit.jupiter.api.Test
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure


class JsonSchemaTest {

    @Test
    fun `schema for simple object`() {

        val schema = JPerson.schema().render(pretty)

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

        val schema = pretty.render(JProduct.schema())

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

        val schema = JCompany.schema().render(pretty)

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

        val schema = JProducts.schema().render(pretty)

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

        val schema = JNotes.schema().render(pretty)

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
    fun `schema for non-string keyed object map`() {
        val schema = JTasks.schema().render(pretty)

        expectThat(schema).isEqualTo(
            """{
              |  "type": "object"
              |}""".trimMargin()
        )
    }


    @Test
    fun `schema for a flattened object`() {

        val schema = JSelectedFile.schema().render(pretty)

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
    fun `schema for polymorphic object`() {
        val schema = JCustomer.schema().render(pretty)

        expectThat(schema).isEqualTo(
            """{
                |  "description": "discriminant field: type",
                |  "oneOf": [
                |      {
                |          "properties": {
                |              "id": {
                |                  "type": "number"
                |                },
                |              "name": {
                |                  "type": "string"
                |                },
                |              "type": {
                |                  "const": "private",
                |                  "type": "string"
                |                }
                |            },
                |          "required": [
                |              "id",
                |              "name"
                |            ]
                |        },
                |      {
                |          "properties": {
                |              "name": {
                |                  "type": "string"
                |                },
                |              "tax_type": {
                |                  "enum": [
                |                      "Domestic",
                |                      "Exempt",
                |                      "EU",
                |                      "US",
                |                      "Other"
                |                    ]
                |                },
                |              "type": {
                |                  "const": "company",
                |                  "type": "string"
                |                }
                |            },
                |          "required": [
                |              "name",
                |              "tax_type"
                |            ]
                |        },
                |      {
                |          "properties": {
                |              "type": {
                |                  "const": "anonymous",
                |                  "type": "string"
                |                }
                |            }
                |        }
                |    ],
                |  "type": "object"
                |}""".trimMargin()
        )
    }

    @Test
    fun `schema for complex object`() {

        val schema = JInvoice.schema().render(pretty)

        expectThat(schema).isEqualTo(
            """{
                |  "properties": {
                |      "created_date": {
                |          "type": "string"
                |        },
                |      "customer": {
                |          "description": "discriminant field: type",
                |          "oneOf": [
                |              {
                |                  "properties": {
                |                      "id": {
                |                          "type": "number"
                |                        },
                |                      "name": {
                |                          "type": "string"
                |                        },
                |                      "type": {
                |                          "const": "private",
                |                          "type": "string"
                |                        }
                |                    },
                |                  "required": [
                |                      "id",
                |                      "name"
                |                    ]
                |                },
                |              {
                |                  "properties": {
                |                      "name": {
                |                          "type": "string"
                |                        },
                |                      "tax_type": {
                |                          "enum": [
                |                              "Domestic",
                |                              "Exempt",
                |                              "EU",
                |                              "US",
                |                              "Other"
                |                            ]
                |                        },
                |                      "type": {
                |                          "const": "company",
                |                          "type": "string"
                |                        }
                |                    },
                |                  "required": [
                |                      "name",
                |                      "tax_type"
                |                    ]
                |                },
                |              {
                |                  "properties": {
                |                      "type": {
                |                          "const": "anonymous",
                |                          "type": "string"
                |                        }
                |                    }
                |                }
                |            ],
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

        val schema = JInvoice.schema().render().printIt("!!!!!!")
        repeat(100) {
            val json = JInvoice.toJson(randomInvoice())

            validateJsonAgainstSchema(schema, json)
        }
    }

    @Test
    fun `validating an Json against another Schema will give a failure`() {

        val schemaJson = JPerson.schema().render()
        val json = JCompany.toJson(randomCompany())

        expectCatching {
            validateJsonAgainstSchema(schemaJson, json)
        }.isFailure()
            .get { message.orEmpty() }.contains("The object must have a property whose name is \"id\"")
    }


}