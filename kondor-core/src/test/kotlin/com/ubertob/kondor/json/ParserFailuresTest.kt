package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.JsonNodeObject
import com.ubertob.kondor.json.jsonnode.parseJsonNode
import com.ubertob.kondortools.expectFailure
import com.ubertob.kondortools.expectSuccess
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.startsWith

class ParserFailuresTest {

    @Test
    fun `parsing empty json node fails`() {
        val invalidJson = ""
        val error = parseJsonNode(invalidJson).expectFailure()

        expectThat(error.msg).isEqualTo("Error parsing node <[root]> at position 0: expected some valid Json but found end of file - invalid Json")
    }

    @Test
    fun `parsing illegal json node fails`() {
        val invalidJson = "BOOM"
        val error = parseJsonNode(invalidJson).expectFailure()

        expectThat(error.msg).isEqualTo("Error parsing node <[root]> at position 1: expected a Number but found 'BOOM' - For input string: \"BOOM\"")
    }


    @Test
    fun `parsing empty json fails`() {
        val invalidJson = ""
        val error = JPerson.fromJson(invalidJson).expectFailure()

        expectThat(error.msg).isEqualTo("Error parsing node <[root]> at position 0: expected OpeningCurly but found end of file - invalid Json")
    }

    @Test
    fun `parsing not complaint json fails`() {
        val illegalJson = "123 b"

        val error = JInt.fromJson(illegalJson).expectFailure()

        expectThat(error.msg).isEqualTo("Error parsing node <[root]> at position 5: expected EOF but found 'b' - json continue after end")
    }

    @Test
    fun `parsing illegal Boolean returns an error`() {
        val illegalJson = "False"

        val error = JBoolean.fromJson(illegalJson).expectFailure()

        expectThat(error.msg).isEqualTo("Error parsing node <[root]> at position 1: expected a Boolean but found 'False' - valid values: false, true")
    }

    @Test
    fun `thrown exception keep the json error`() {
        val illegalJson = "False"

        try {
            JBoolean.fromJson(illegalJson).orThrow()
        } catch (e: Exception) {
            expectThat(e.message).isEqualTo("Error parsing node <[root]> at position 1: expected a Boolean but found 'False' - valid values: false, true")
        }
    }

    @Test
    fun `parsing String missing close quotes returns an error`() {
        val illegalJson = """
            "unclosed string
            """.trim()

        val error = JString.fromJson(illegalJson).expectFailure()

        expectThat(error.msg).isEqualTo("Error parsing node <[root]> at position 2: expected ClosingQuotes but found end of file - invalid Json")
    }

    @Test
    fun `parsing String with unknown escape returns an error`() {
        val illegalJson = """
            "foo \ bar"
            """.trim()

        val error = JString.fromJson(illegalJson).expectFailure()

        expectThat(error.msg).isEqualTo("Error parsing node <[root]> at position 7: expected a valid Json but found wrongly escaped char '\\ ' inside a Json string after 'foo ' - Invalid Json")
    }

    @Test
    fun `correctly parse escaped String`() {
        val optionalEscape = """
            "foo \/ bar"
            """.trim()

        val text = JString.fromJson(optionalEscape).expectSuccess()

        expectThat(text).isEqualTo("""foo / bar""")
    }

    @Test
    fun `parsing illegal Long returns an error`() {
        val illegalJson = "123-234"

        val error = JLong.fromJson(illegalJson).expectFailure()

        expectThat(error.msg).startsWith("Error parsing node <[root]> at position 1: expected a Number but found '123-234'")
    }

    @Test
    fun `Json Long underflow`() {

        val error = JLong.fromJson("-9223372036854775809").expectFailure()

        expectThat(error.msg).isEqualTo("Error converting node <[root]> Caught exception: java.lang.ArithmeticException: Overflow")
    }

    @Test
    fun `Json Long overflow`() {

        val error = JLong.fromJson("9223372036854775808000000000000").expectFailure()

        expectThat(error.msg).isEqualTo("Error converting node <[root]> Caught exception: java.lang.ArithmeticException: Overflow")
    }

    @Test
    fun `Json Int underflow`() {

        val error = JInt.fromJson("-2147483649").expectFailure()

        expectThat(error.msg).isEqualTo("Error converting node <[root]> Caught exception: java.lang.ArithmeticException: Overflow")
    }

    @Test
    fun `Json Int overflow`() {

        val error = JInt.fromJson("2147483648").expectFailure()

        expectThat(error.msg).isEqualTo("Error converting node <[root]> Caught exception: java.lang.ArithmeticException: Overflow")
    }

    @Test
    fun `parsing Json with duplicate keys doesn't return an error`() {
        val illegalJson = """{ "id": 1, "id": 2, "name": "alice"}"""

        val person = JPerson.fromJson(illegalJson).expectSuccess()
        expectThat(person).isEqualTo(Person(2, "alice"))

    }

    @Test
    fun `parsing illegal Json without colon returns an error`() {
        val illegalJson = """{ "id" 2, "name": "alice"}"""

        val error = JPerson.fromJson(illegalJson).expectFailure()

        expectThat(error.msg).isEqualTo("Error parsing node <[root]> at position 8: expected Colon but found '2' - invalid Json")
    }

    @Test
    fun `parsing illegal Json Object with double comma returns an error`() {
        val illegalJson = """{ "id": 1,, "name": "alice" }"""

        val error = JPerson.fromJson(illegalJson).expectFailure()

        expectThat(error.msg).isEqualTo("Error parsing node <[root]> at position 11: expected a valid key but found Comma - key missing in object field")
    }

    @Test
    fun `parsing illegal Json Object without quoted keys returns an error`() {
        val illegalJson = """{ "id": 1, name: "alice" }"""

        val error = JPerson.fromJson(illegalJson).expectFailure()

        expectThat(error.msg).isEqualTo("Error parsing node <[root]> at position 12: expected a valid key but found 'name' - key missing in object field")
    }

    @Test
    fun `allows illegal Json Array with trailing comma`() {
        val illegalJson = """[ "a", "b",]"""

        val jsonStringArray = JList(JString)
        jsonStringArray.fromJson(illegalJson).expectSuccess()
    }

    @Test
    fun `illegal Json Array with double trailing comma returns an error`() {
        val illegalJson = """[ "a", "b",,]"""

        val jsonStringArray = JList(JString)
        val error = jsonStringArray.fromJson(illegalJson).expectFailure()

        expectThat(error.msg).isEqualTo("Error parsing node </[2]> at position 12: expected a new node but found Comma - Comma in wrong position")

    }

    @Test
    fun `parsing illegal Json Array starting with comma returns an error`() {
        val illegalJson = """[,"a", "b"]"""

        val jsonStringArray = JList(JString)
        val error = jsonStringArray.fromJson(illegalJson).expectFailure()

        expectThat(error.msg).isEqualTo("Error parsing node </[0]> at position 2: expected a new node but found Comma - Comma in wrong position")
    }

    @Test
    fun `parsing illegal Json Array with non matching brackets returns an error`() {
        val illegalJson = """[ "a", "b" }"""

        val jsonStringArray = JList(JString)
        val error = jsonStringArray.fromJson(illegalJson).expectFailure()

        expectThat(error.msg).isEqualTo("Error parsing node <[root]> at position 12: expected ClosingBracket but found ClosingCurly - invalid Json")
    }


    @Test
    fun `parsing json without a field returns an error`() {
        val jsonWithDifferentField =
            """{ "id": 1, "fullname": "alice" }"""

        val error = JPerson.fromJson(jsonWithDifferentField).expectFailure()

        expectThat(error.msg).isEqualTo("Error reading property <name> of node <[root]> Not found key 'name'. Keys found: [fullname, id]")
    }

    @Test
    fun `parsing invalid json gives an error`() {
        val invalidJson = "BOOM"
        val error = JPerson.fromJson(invalidJson).expectFailure()

        expectThat(error.msg).isEqualTo("Error parsing node <[root]> at position 1: expected OpeningCurly but found 'BOOM' - invalid Json")
    }

    @Test
    fun `parsing nested invalid json gives an error with the correct position`() {
        val invalidJson = """{ "obj": {"num": 123, "nestedObj": { BOOOM} } }"""
        val error = JPerson.fromJson(invalidJson).expectFailure()

        expectThat(error.msg).isEqualTo("Error parsing node </obj/nestedObj> at position 38: expected a valid key but found 'BOOOM' - key missing in object field")
    }

    @Test
    fun `parsing sealed class json without the discriminator field returns an error`() {
        val jsonWithDifferentField =
            """
 {
  "id": "1001",
  "vat-to-pay": true,
  "customer": {
    "id": 1,
    "name": "ann"
  },
  "items": [
    {
      "id": 1001,
      "short-desc": "toothpaste",
      "long_description": "toothpaste \"whiter than white\"",
      "price": 125
    },
    {
      "id": 10001,
      "short_desc": "special offer"
    }
  ],
  "total": 123.45
}  """

        val error = JInvoice.fromJson(jsonWithDifferentField).expectFailure()

        expectThat(error.msg).isEqualTo("Error converting node </customer> expected discriminator field \"type\" not found")
        //if an object is valid json but fail the parser it should use a different error with the node reference
    }


    @Test
    fun `parsing json with different type of fields returns an error`() {
        val jsonWithDifferentField =
            """
 {
  "id": "1001",
  "vat-to-pay": true,
  "customer": {
    "type": "private",
    "id": 1,
    "name": "ann"
  },
  "items": [
    {
      "id": 1001,
      "short-desc": "toothpaste",
      "long_description": "toothpaste \"whiter than white\"",
      "price": "a string"
    },
    {
      "id": 10001,
      "short-desc": "special offer"
    }
  ],
  "total": "123.45"
}  """.trimIndent()

        val error = JInvoice.fromJson(jsonWithDifferentField).expectFailure()

        expectThat(error.msg).isEqualTo("Error converting node </items/[0]/price> expected a Number or NaN but found 'a string'")
    }

    object JPersonIncomplete : JAny<Person>() {

        private val id by JField(Person::id, JInt)
        private val name by JField(Person::name, JString)

        override fun JsonNodeObject.deserializeOrThrow() = error("not finished yet!")
    }

    @Test
    fun `error in parsing Json is returned correctly`() {

        val error = JPersonIncomplete.fromJson(JPerson.toJson(randomPerson())).expectFailure()

        expectThat(error.msg).isEqualTo("Error converting node <[root]> not finished yet!")
    }


    //add tests for... wrong enum, jmap with mixed node types, Double instead of Long
}
