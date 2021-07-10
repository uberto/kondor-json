package com.ubertob.kondortools

import org.junit.jupiter.api.Test

internal class KondorMatcherTest {

    @Test
    fun `isEquivalentJson check for equivalent jsons`() {

        val json1 = """
            {
            "name" : "Frank",
            "age" : 32,
            "married" : true,
            "children" : ["Ann", "Bob", "Cathy"] 
            }
        """.trimIndent()

        val json2 = """{"age": 32,"name": "Frank", "children": [  "Ann", "Bob","Cathy"], "married": true} """

        json2.isEquivalentJson(json1).expectSuccess()

    }

    @Test
    fun `isEquivalentJson works on json arrays`() {

        val json1 = """
            [
              {
                  "age": 21,
                  "name": "Greg"
                },
              {
                  "age": 32,
                  "children": [
                      
                    ],
                  "married": false,
                  "name": "Ann"
                },
              {
                  "age": 32,
                  "children": [
                      "Ann",
                      "Bob",
                      "Cathy"
                    ],
                  "married": true,
                  "name": "Frank"
                }
            ]
        """.trimIndent()


        val json2 =
            """[{"age": 21, "name": "Greg"}, {"age": 32, "children": [], "married": false, "name": "Ann"}, {"age": 32, "children": ["Ann", "Bob", "Cathy"], "married": true, "name": "Frank"}]"""

        json2.isEquivalentJson(json1).expectSuccess()

    }
}