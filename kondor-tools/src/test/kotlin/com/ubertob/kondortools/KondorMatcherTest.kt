package com.ubertob.kondortools

import com.ubertob.kondor.outcome.onFailure
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test

internal class KondorMatcherTest {

    @Test
    fun `isSameJsonObject check for equivalent jsons`() {

        val expected = """
            {
            "name" : "Frank",
            "age" : 32,
            "married" : true,
            "children" : ["Ann", "Bob", "Cathy"] 
            }
        """.trimIndent()

        val actual = """{"age" : 32,"name" : "Frank", "children": [  "Ann", "Bob","Cathy"] , "married": true} """

        val res = actual isSameJsonObject expected
        res.onFailure { fail(it.msg) }

    }
}