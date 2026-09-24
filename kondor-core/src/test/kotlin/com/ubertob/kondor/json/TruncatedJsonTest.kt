package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.*
import com.ubertob.kondor.outcome.Failure
import com.ubertob.kondortools.expectFailure
import com.ubertob.kondortools.expectSuccess
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.endsWith
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.util.Currency

// top level numbers are not tested, because a prefix of a valid number is also valid (e.g. 1 for 12)
class TruncatedJsonTest {

    private val person = Person(1, "Frank \"the\" \\tank\\ \n è 😀")

    private val invoice = Invoice(
        id = InvoiceId("INV-1"),
        vat = true,
        customer = person,
        items = listOf(
            Product(1, "a", "desc a", 12.5),
            Product(2, "", "", null)
        ),
        total = BigDecimal("12.50"),
        created = java.time.LocalDate.of(2026, 9, 19),
        paid = Instant.ofEpochSecond(1_700_000_000)
    )

    private val expenseReport = ExpenseReport(
        person,
        mapOf("hotel" to Money(Currency.getInstance("EUR"), BigInteger.valueOf(123)), "empty" to Money(Currency.getInstance("USD"), BigInteger.ZERO))
    )

    @Test
    fun `truncated Json objects fail without exceptions`() {
        checkTruncations(JInvoice, invoice)
        checkTruncations(Invoice.Json, invoice)
        checkTruncations(JExpenseReport, expenseReport)
        checkTruncations(JPerson, person)
        checkTruncations(Person.Json, person)
        checkTruncations(JOptionalAddress, OptionalAddress(null, "street", null))
        checkTruncations(JOptionalAddressAny, OptionalAddress(null, "street", null))
        checkTruncations(JSelectedFileAny, SelectedFile(true, FileInfo("f", Instant.EPOCH, false, 1, "/a")))
        checkTruncations(JCustomer, person)
        checkTruncations(JCustomer, Company("ACME", TaxType.Domestic))
    }

    @Test
    fun `truncated Json with unknown fields fails without exceptions`() {
        val json = """{"id": 1, "extra": [1, {"a": [true, null, "x"]}, {}], "name": "a", "last": {"b": 2.5}}"""
        JPerson.fromJson(json).expectSuccess()

        json.properPrefixes().forEach { prefix ->
            expectFailureWithoutExceptions(prefix) { JPerson.fromJson(it) }
            expectFailureWithoutExceptions(prefix) { JPerson.fromJson(it.byteInputStream()) }
        }
    }

    @Test
    fun `truncated Json arrays and maps fail without exceptions`() {
        checkTruncations(JList(JPerson), listOf(person, Person(2, "")))
        checkTruncations(JList(Product.Json), listOf(Product(1, "a", "b", null)))
        checkTruncations(JList(JList(JString)), listOf(listOf("a", ""), emptyList(), listOf("[", "]")))
        checkTruncations(JMap(JString), mapOf("a" to "x", "{" to "}", "" to ""))
        checkTruncations(JMap(JList(JInt)), mapOf("a" to listOf(1, -2), "b" to emptyList()))
        checkTruncations(JStringList, listOf("a", "b"))
    }

    @Test
    fun `truncated Json values fail without exceptions`() {
        checkTruncations(JString, "a \"quoted\" \\ string \u0001 \t")
        checkTruncations(JBoolean, true)
        checkTruncations(JBoolean, false)
        checkTruncations(JList(JDouble), listOf(1.5, -0.0, 1e300))
    }

    @Test
    fun `truncated Json nodes fail without exceptions`() {
        val json = """{"a": [1, -2.5e10, "x\"y", true, false, null, {}, [], {"b": {"c": [[]]}}], "d": "è"}"""
        parseJsonNode(json).expectSuccess()

        json.properPrefixes().forEach { prefix ->
            expectFailureWithoutExceptions(prefix) { parseJsonNode(it) }
        }
    }

    @Test
    fun `truncated Json reports end of file with the path`() {
        expectMessage(JList(JInt), "[1, 2", "Error parsing node <[root]> at position 5: expected ClosingBracket but found end of file - invalid Json")
        expectMessage(JPerson, """{"id": """, "Error parsing node </id> at position 6: expected a Number but found end of file - invalid Json")
        expectMessage(JPerson, """{"id": 1, "name": "a""", "Error parsing node </name> at position 20: expected ClosingQuotes but found end of file - invalid Json")
        expectMessage(JPerson, """{"id": 1, "extra": """, "Error parsing node </extra> at position 18: expected a valid node but found end of file - invalid Json")
        expectMessage(JProduct, """{"id": 1, "price": """, "Error parsing node </price> at position 18: expected a Number but found end of file - invalid Json")
        expectMessage(Product.Json, """{"id": 1, "price": """, "Error parsing node </price> at position 18: expected a valid node but found end of file - invalid Json")
        expectMessage(JCustomer, """{"type": "private", "name": [""", "Error parsing node </name> at position 28: expected ClosingBracket but found end of file - invalid Json")
        expectMessage(JMap(JString), """{"a": """, "Error parsing node </a> at position 5: expected OpeningQuotes but found end of file - invalid Json")
        expectMessage(JBoolean, "", "Error parsing node <[root]> at position 0: expected a Boolean but found end of file - invalid Json")
        expectMessage(JString, "\"abc", "Error parsing node <[root]> at position 4: expected ClosingQuotes but found end of file - invalid Json")
        expectThat(parseJsonNode("""{"a": [true""").expectFailure().msg)
            .isEqualTo("Error parsing node </a> at position 11: expected ClosingBracket but found end of file - invalid Json")
    }

    @Test
    fun `empty Json reports end of file for every node kind`() {
        listOf(ArrayNode, BooleanNode, NullNode, NumberNode, ObjectNode, StringNode).forEach { nodeKind ->
            val error = try {
                nodeKind.fromJsonString("").expectFailure()
            } catch (e: Exception) {
                fail("Exception parsing an empty ${nodeKind.desc}", e)
            }
            expectThat(error.msg).contains("but found end of file")
        }
        expectThat(NullNode.fromJsonString("").expectFailure().msg)
            .isEqualTo("Error parsing node <[root]> at position 0: expected a Null but found end of file - invalid Json")
    }

    @Test
    fun `truncated Json longer than the stream buffer fails without exceptions`() {
        val people = (1..2_000).map { Person(it, "name $it") }
        val json = JList(JPerson).toJson(people)
        JList(JPerson).fromJson(json.byteInputStream()).expectSuccess()

        // every cut around the buffer boundaries, so that each kind of token is split between two reads
        listOf(8192, 16384, 24576).flatMap { boundary -> (boundary - 12..boundary + 12) }.forEach { length ->
            val prefix = json.take(length)
            expectFailureWithoutExceptions(prefix) { JList(JPerson).fromJson(it.byteInputStream()) }
            expectThat(JList(JPerson).fromJson(prefix.byteInputStream()).expectFailure().msg)
                .isEqualTo(JList(JPerson).fromJson(prefix).expectFailure().msg)
        }
    }

    private fun <T : Any> expectMessage(converter: JsonConverter<T, *>, json: String, expected: String) {
        expectThat(converter.fromJson(json).expectFailure().msg).isEqualTo(expected)
        expectThat(converter.fromJson(json.byteInputStream()).expectFailure().msg).isEqualTo(expected)
    }

    private fun <T : Any> checkTruncations(converter: JsonConverter<T, *>, value: T) {
        listOf(converter.toJson(value), converter.toJson(value, JsonStyle.pretty)).forEach { json ->
            converter.fromJson(json).expectSuccess()

            json.properPrefixes().forEach { prefix ->
                expectFailureWithoutExceptions(prefix) { converter.fromJson(it) }
                expectFailureWithoutExceptions(prefix) { converter.fromJson(it.byteInputStream()) }
            }
        }
    }

    private fun String.properPrefixes(): List<String> = indices.map { take(it) }

    // an exception caught and wrapped by a converter would also be a Failure, so the error must come from the parser
    private fun expectFailureWithoutExceptions(json: String, parse: (String) -> Any) {
        val result = try {
            parse(json)
        } catch (e: Exception) {
            fail("Exception parsing the truncated Json: $json", e)
        }
        expectThat(result).describedAs("parsing the truncated Json: $json").isA<Failure<*>>()
            .get { (this as Failure<*>).error.msg }
            .and {
                not().contains("EndOfCollection")
                not().contains("IllegalState")
                not().endsWith(" - ") // an exception without message, caught by parseNumber
                not().endsWith("> ") // an exception without message, caught by tryFromNode
            }
    }
}
