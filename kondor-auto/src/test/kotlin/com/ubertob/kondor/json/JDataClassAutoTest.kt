package com.ubertob.kondor.json

import org.junit.jupiter.api.Test

class JDataClassAutoTest {

    object PersonRefl : JDataClassAuto<Person>(Person::class)

    @Test
    fun `JDataClassAuto doesn't need the fields declaration`() {

        PersonRefl.registerAllProperties() //temp hack

        PersonRefl.toJson(randomPerson(), JsonStyle.prettyWithNulls)

        PersonRefl.testParserAndRender(100) { randomPerson() }

    }
}