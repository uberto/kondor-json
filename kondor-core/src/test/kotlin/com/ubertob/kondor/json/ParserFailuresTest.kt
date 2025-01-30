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

        expectThat(error.msg).isEqualTo("Error parsing node <[root]> at position 0: expected a Number or NaN but found 'BOOM' - For input string: \"BOOM\"")
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
    fun `parsing wrong enum returns an error`() {
        val illegalJson = """
            {"id": "72825", "vat-to-pay": true, "customer": {"type": "company", "name": "acme lim", "tax_type": "Coyote"}, "items": [], "total": 0, "created_date": "2024-05-21", "paid_datetime": 1716303968000}
        """.trimIndent()

        val error = JInvoice.fromJson(illegalJson).expectFailure()

        expectThat(error.msg).isEqualTo("Error converting node </customer/tax_type> not found Coyote among [Domestic, Exempt, EU, US, Other]")
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

        expectThat(error.msg).startsWith("Error parsing node <[root]> at position 0: expected a Number or NaN but found '123-234'")
    }

    @Test
    fun `Json Long underflow`() {

        val error = JLong.fromJson("-9223372036854775809").expectFailure()
        expectThat(error.msg).startsWith("Error parsing node <[root]> at position 0: expected a Number")

//!!!        expectThat(error.msg).isEqualTo("Error converting node <[root]> Wrong number format: Overflow")
    }

    @Test
    fun `Json Long overflow`() {

        val error = JLong.fromJson("9223372036854775808000000000000").expectFailure()

        expectThat(error.msg).startsWith("Error parsing node <[root]> at position 0: expected a Number")
// !!!       expectThat(error.msg).isEqualTo("Error converting node <[root]> Wrong number format: Overflow")
    }

    @Test
    fun `Json Int underflow`() {

        val error = JInt.fromJson("-2147483649").expectFailure()
        expectThat(error.msg).startsWith("Error parsing node <[root]> at position 0: expected a Number")

// !!!       expectThat(error.msg).isEqualTo("Error converting node <[root]> Wrong number format: Overflow")
    }

    @Test
    fun `Json Int overflow`() {

        val error = JInt.fromJson("2147483648").expectFailure()

        expectThat(error.msg).startsWith("Error parsing node <[root]> at position 0: expected a Number")
//!!!        expectThat(error.msg).isEqualTo("Error converting node <[root]> Wrong number format: Overflow")
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

        expectThat(error.msg).isEqualTo("Error parsing node <[root]> at position 10: expected a valid key but found Comma - key missing in object field")
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

        expectThat(error.msg).isEqualTo("Error parsing node </[2]> at position 11: expected a new json value but found Comma - Comma in wrong position")

    }

    @Test
    fun `parsing illegal Json Array starting with comma returns an error`() {
        val illegalJson = """[,"a", "b"]"""

        val jsonStringArray = JList(JString)
        val error = jsonStringArray.fromJson(illegalJson).expectFailure()

        expectThat(error.msg).isEqualTo("Error parsing node </[0]> at position 1: expected a new json value but found Comma - Comma in wrong position")
    }

    @Test
    fun `parsing illegal Json Array with non matching brackets returns an error`() {
        val illegalJson = """[ "a", "b" }"""

        val jsonStringArray = JList(JString)
        val error = jsonStringArray.fromJson(illegalJson).expectFailure()

        expectThat(error.msg).isEqualTo("Error parsing node <[root]> at position 11: expected ClosingBracket but found ClosingCurly - invalid Json")
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

    @Test
    fun `nested object parsing error report correct path`() {

        val wrongjson = """{
  "file": {
      "creation_date": -3951020977374450952,
      "file_name": "myfile",
      "folder_path": "/a/b/c",
      "is_dir": false,
      "selected": true,
      "size": 123
    },
  "user": {
      "id": 597,
      "name??": "Frank"
    }
}"""
        val error = JUserFile.fromJson(wrongjson).expectFailure()

        expectThat(error.msg).isEqualTo("Error reading property <name> of node </user> Not found key 'name'. Keys found: [id, name??]")
    }

    @Test
    fun `flatten field parsing error report correct path`() {

        val wrongjson = """{
  "file": {
      "creation_date": -3951020977374450952,
      "file_name??": "myfile",
      "folder_path": "/a/b/c",
      "is_dir": false,
      "selected": true,
      "size": 123
    },
  "user": {
      "id": 597,
      "name": "Frank"
    }
}"""
        val error = JUserFile.fromJson(wrongjson).expectFailure()

        expectThat(error.msg).isEqualTo("Error reading property <file_name> of node </file> Not found key 'file_name'. Keys found: [creation_date, file_name??, folder_path, is_dir, size]")
    }

    @Test
    fun `array parsing error on key report correct path`() {

        val wrongjson = """
  [
   { 
    "file": { "creation_date": -3951020977374450952, "file_name": "myfile", "folder_path": "/a/b/c", "is_dir": false, "selected": true, "size": 123 },
    "user": { "id": 597, "name": "Frank" } 
   },
   { 
    "file": { "creation_date": -3951020977374450952, "file_name??": "myfile", "folder_path": "/a/b/c", "is_dir": false, "selected": true, "size": 123 },
    "user": { "id": 597, "name": "Frank" } 
   }
  ]"""
        val error = JList(JUserFile).fromJson(wrongjson).expectFailure()

        expectThat(error.msg).isEqualTo("Error reading property <file_name> of node </[1]/file> Not found key 'file_name'. Keys found: [creation_date, file_name??, folder_path, is_dir, size]")
    }

    @Test
    fun `array parsing error on numeric precision report correct path`() {

        val wrongjson = """
  [
   { 
    "file": { "creation_date": -3951020977374450952, "file_name": "myfile", "folder_path": "/a/b/c", "is_dir": false, "selected": true, "size": 123 },
    "user": { "id": 597, "name": "Frank" } 
   },
   { 
    "file": { "creation_date": -3951020977374450952, "file_name": "myfile", "folder_path": "/a/b/c", "is_dir": false, "selected": true, "size": 123 },
    "user": { "id": 597.7, "name": "Frank" } 
   }
  ]"""
        val error = JList(JUserFile).fromJson(wrongjson).expectFailure()

        expectThat(error.msg).isEqualTo("Error converting node </[1]/user/id> Wrong number format: Rounding necessary")
    }

    @Test
    fun `array parsing error on value type report correct path`() {

        val wrongjson = """
  [
   { 
    "file": { "creation_date": -3951020977374450952, "file_name": "myfile", "folder_path": "/a/b/c", "is_dir": false, "selected": true, "size": 123 },
    "user": { "id": 597, "name": "Frank" } 
   },
   { 
    "file": { "creation_date": -3951020977374450952, "file_name": "myfile", "folder_path": "/a/b/c", "is_dir": false, "selected": true, "size": 123 },
    "user": { "id": "id-123", "name": "Frank" } 
   }
  ]"""
        val error = JList(JUserFile).fromJson(wrongjson).expectFailure()

        expectThat(error.msg).isEqualTo("Error converting node </[1]/user/id> expected a Number or NaN but found 'id-123'")
    }

    //add test for jmap with mixed node types
}
