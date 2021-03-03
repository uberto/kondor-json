package com.ubertob.kondor.json

import com.ubertob.kondor.expectFailure
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class ParserFailuresTest {

    @Test
    fun `parsing json not completely gives us precise errors`() {
        val illegalJson = "123 b"

        val error = JInt.fromJson(illegalJson).expectFailure()

        expectThat(error.msg).isEqualTo("error on <[root]> at position 5: expected EOF but found 'b'")
    }

    @Test
    fun `parsing illegal Boolean gives us precise errors`() {
        val illegalJson = "False"

        val error = JBoolean.fromJson(illegalJson).expectFailure()

        expectThat(error.msg).isEqualTo("error on <[root]> at position 5: expected a Boolean but found 'False'")
    }

    @Test
    fun `parsing illegal String gives us precise errors`() {
        val illegalJson = """
            "unclosed string
            """

        val error = JString.fromJson(illegalJson).expectFailure()

        expectThat(error.msg).isEqualTo("error on <[root]> at position 0: expected a String but found 'EOF'")
    }

    @Test
    fun `parsing illegal Long gives us precise errors`() {
        val illegalJson = "123-234"

        val error = JLong.fromJson(illegalJson).expectFailure()

        expectThat(error.msg).isEqualTo("error on <[root]> at position 0: expected a Number but found 'Character - is neither a decimal digit number, decimal point, nor \"e\" notation exponential mark.'")
    }


    @Test
    fun `parsing json without a field return precise errors`() {
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

        expectThat(error.msg).isEqualTo("error on </customer> expected field _type not found!")
    }


    @Test
    fun `parsing json with different type of fields return precise errors`() {
        val jsonWithDifferentField =
            """
 {
  "id": "1001",
  "vat-to-pay": true,
  "customer": {
    "_type": "private",
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

        expectThat(error.msg).isEqualTo("error on </items/0/price> expected a Number but found String")
    }

    object JPersonIncomplete : JAny<Person>() {

        private val id by JField(Person::id, JInt)
        private val name by JField(Person::name, JString)

        override fun JsonNodeObject.deserializeOrThrow() = TODO("not finished yet!")
    }

    @Test
    fun `error in parsing Json is reported`() {

        val error = JPersonIncomplete.fromJson(JPerson.toJson(randomPerson())).expectFailure()

        expectThat(error.msg).isEqualTo("error on <[root]> Caught exception: kotlin.NotImplementedError: An operation is not implemented: not finished yet!")
    }



    //add tests for... wrong enum, jmap with mixed node types, Double instead of Long
}
