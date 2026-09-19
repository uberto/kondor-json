package com.ubertob.kondor.json

import com.ubertob.kondortools.expectFailure
import com.ubertob.kondortools.expectSuccess
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
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

class JDataClassAutoRegistrationTest {

    private object JPersonAutoTwice : JDataClassAuto<Person>(Person::class)

    @Test
    fun `calling registerAllProperties again doesn't register the properties twice`() {
        @Suppress("DEPRECATION")
        JPersonAutoTwice.registerAllProperties()

        val alice = Person(123, "Alice")
        val json = JPersonAutoTwice.toJson(alice)

        expectThat(json).isEqualTo("""{"id": 123, "name": "Alice"}""")
        expectThat(JPersonAutoTwice.fromJson(json).expectSuccess()).isEqualTo(alice)
    }
}

data class Settings(val theme: String, val fontSize: Int = 12, val nickname: String?)

class JDataClassWithNamesTest {

    private object JSettings : JDataClassWithNames<Settings>(Settings::class) {
        val nickname by str(Settings::nickname)
        val theme by str(Settings::theme)
        val fontSize by num(Settings::fontSize)
    }

    private object JSettingsNoFontSize : JDataClassWithNames<Settings>(Settings::class) {
        val theme by str(Settings::theme)
        val nickname by str(Settings::nickname)
    }

    @Test
    fun `fields can be declared in any order`() {
        val settings = Settings("dark", 20, "bob")

        expectThat(JSettings.fromJson(JSettings.toJson(settings)).expectSuccess()).isEqualTo(settings)
    }

    @Test
    fun `missing fields use the default value or null`() {
        expectThat(JSettings.fromJson("""{"theme": "dark"}""").expectSuccess()).isEqualTo(Settings("dark", 12, null))
    }

    @Test
    fun `unknown fields are ignored`() {
        val json = """{"_id": {"oid": "x"}, "theme": "dark", "extra": [1, 2], "nickname": "bob"}"""

        expectThat(JSettings.fromJson(json).expectSuccess()).isEqualTo(Settings("dark", 12, "bob"))
    }

    @Test
    fun `a Json field matching a constructor parameter without a converter field is ignored`() {
        expectThat(JSettingsNoFontSize.fromJson("""{"theme": "dark", "fontSize": 99}""").expectSuccess())
            .isEqualTo(Settings("dark", 12, null))
    }

    @Test
    fun `missing field without a default fails`() {
        val error = JSettings.fromJson("""{"fontSize": 14}""").expectFailure()

        expectThat(error.msg).contains("Missing required parameter 'theme'")
    }
}

class JDataClassMissingFieldTest {

    @Test
    fun `JDataClass reports a missing mandatory field`() {
        val error = Person.Json.fromJson("""{"name": "Alice"}""").expectFailure()

        expectThat(error.msg).isEqualTo("Error reading property <id> of node <[root]> Not found key 'id'. Keys found: [name]")
    }
}
