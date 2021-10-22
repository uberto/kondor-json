package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.JsonNodeObject
import com.ubertob.kondortools.expectFailure
import com.ubertob.kondortools.expectSuccess
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class ParserFailuresTest {

    @Test
    fun `parsing json not completely returns an error`() {
        val illegalJson = "123 b"

        val error = JInt.fromJson(illegalJson).expectFailure()

        expectThat(error.msg).isEqualTo("error on <[root]> at position 5: expected EOF but found 'b' - json continue after end")
    }

    @Test
    fun `parsing illegal Boolean returns an error`() {
        val illegalJson = "False"

        val error = JBoolean.fromJson(illegalJson).expectFailure()

        expectThat(error.msg).isEqualTo("error on <[root]> at position 1: expected a Boolean but found 'False' - valid values: false, true")
    }

    @Test
    fun `thrown exception keep the json error`() {
        val illegalJson = "False"

        try {
            JBoolean.fromJson(illegalJson).orThrow()
        } catch (e: Exception) {
            expectThat(e.message).isEqualTo("error on <[root]> at position 1: expected a Boolean but found 'False' - valid values: false, true")
        }
    }

    @Test
    fun `parsing String missing close quotes returns an error`() {
        val illegalJson = """
            "unclosed string
            """.trim()

        val error = JString.fromJson(illegalJson).expectFailure()

        expectThat(error.msg).isEqualTo("error on <[root]> at position 2: expected a valid Json but found 'unclosed string' - Unexpected end of file - Invalid Json")
    }

    @Test
    fun `parsing String with unknown escape returns an error`() {
        val illegalJson = """
            "foo \ bar"
            """.trim()

        val error = JString.fromJson(illegalJson).expectFailure()

        expectThat(error.msg).isEqualTo("error on <[root]> at position 7: expected a valid Json but found wrongly escaped char '\\ ' in Json string after 'foo ' - Invalid Json")
    }

    @Test
    fun `parsing illegal Long returns an error`() {
        val illegalJson = "123-234"

        val error = JLong.fromJson(illegalJson).expectFailure()

        expectThat(error.msg).isEqualTo("error on <[root]> at position 1: expected a Number but found '123-234' - Character - is neither a decimal digit number, decimal point, nor \"e\" notation exponential mark.")
    }

    @Test
    fun `parsing illegal Json with duplicate keys returns an error`() {
        val illegalJson = """{ "id": 1, "id": 2, "name": "alice"}"""

        //decided to ignore duplicate keys
//        val error = JPerson.fromJson(illegalJson).expectFailure()
//        expectThat(error.msg).isEqualTo("error on <[root]> at position 15: expected a unique key but found 'id' - duplicated key")

        val person = JPerson.fromJson(illegalJson).expectSuccess()
        expectThat(person).isEqualTo(Person(2, "alice"))

    }

    @Test
    fun `parsing illegal Json without colon returns an error`() {
        val illegalJson = """{ "id" 2, "name": "alice"}"""

        val error = JPerson.fromJson(illegalJson).expectFailure()

        expectThat(error.msg).isEqualTo("error on <[root]> at position 8: expected Colon but found '2' - invalid Json")
    }

    @Test
    fun `parsing illegal Json Object with double comma returns an error`() {
        val illegalJson = """{ "id": 1,, "name": "alice" }"""

        val error = JPerson.fromJson(illegalJson).expectFailure()

        expectThat(error.msg).isEqualTo("error on <[root]> at position 11: expected a valid key but found Comma - key missing in object field")
    }

    @Test
    fun `parsing illegal Json Object with empty key returns an error`() {
        val illegalJson = """{ "": 1, "name": "alice" }"""

        val error = JPerson.fromJson(illegalJson).expectFailure()

        expectThat(error.msg).isEqualTo("error on <[root]> Not found key 'id'")
    }

    @Test
    fun `parsing illegal Json Object without quoted keys returns an error`() {
        val illegalJson = """{ "id": 1, name: "alice" }"""

        val error = JPerson.fromJson(illegalJson).expectFailure()

        expectThat(error.msg).isEqualTo("error on <[root]> at position 12: expected a valid key but found 'name' - key missing in object field")
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

        expectThat(error.msg).isEqualTo("error on </[2]> at position 12: expected a new node but found Comma - Comma in wrong position")

    }

    @Test
    fun `parsing illegal Json Array starting with comma returns an error`() {
        val illegalJson = """[,"a", "b"]"""

        val jsonStringArray = JList(JString)
        val error = jsonStringArray.fromJson(illegalJson).expectFailure()

        expectThat(error.msg).isEqualTo("error on </[0]> at position 2: expected a new node but found Comma - Comma in wrong position")
    }

    @Test
    fun `parsing illegal Json Array with non matching brackets returns an error`() {
        val illegalJson = """[ "a", "b" }"""

        val jsonStringArray = JList(JString)
        val error = jsonStringArray.fromJson(illegalJson).expectFailure()

        expectThat(error.msg).isEqualTo("error on <[root]> at position 12: expected ClosingBracket but found ClosingCurly - invalid Json")
    }


    @Test
    fun `parsing json without a field returns an error`() {
        val jsonWithDifferentField =
            """
 "{ "id": 1, "fullname": "alice" }
 """

        val error = JPerson.fromJson(jsonWithDifferentField).expectFailure()

        expectThat(error.msg).isEqualTo("error on <[root]> Not found key 'name'. Found ['id','fullname']")
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

        expectThat(error.msg).isEqualTo("error on </customer> expected discriminator field \"type\" not found. Fields[]")
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

        expectThat(error.msg).isEqualTo("error on </items/[0]/price> expected a Number but found String")
    }

    object JPersonIncomplete : JAny<Person>() {

        private val id by JField(Person::id, JInt)
        private val name by JField(Person::name, JString)

        override fun JsonNodeObject.deserializeOrThrow() = TODO("not finished yet!")
    }

    @Test
    fun `error in parsing Json is returned correctly`() {

        val error = JPersonIncomplete.fromJson(JPerson.toJson(randomPerson())).expectFailure()

        expectThat(error.msg).isEqualTo("error on <[root]> Caught exception: kotlin.NotImplementedError: An operation is not implemented: not finished yet!")
    }



    //add tests for... wrong enum, jmap with mixed node types, Double instead of Long
}
