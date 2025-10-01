package com.ubertob.kondor.json

import com.ubertob.kondortools.expectSuccess
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class JDataClassAutoTest {

    object JPersonAuto : JDataClassAuto<Person>(Person::class)


    @Test
    fun `JDataClassAuto doesn't need the fields declaration`() {

        val alice = Person(123, "Alice")

        val json = JPersonAuto.toJson(alice)

        val parsed = JPersonAuto.fromJson(json)

        expectThat(parsed.expectSuccess()).isEqualTo(alice)

    }

    @Test
    fun `JDataClassAuto handle nulls and random values`() {
        JPersonAuto.toJson(randomPerson(), JsonStyle.prettyWithNulls)

        JPersonAuto.testParserAndRender(100) { randomPerson() }

    }
}

//!!! add test with more complex data examples (array, nested objects, etc)