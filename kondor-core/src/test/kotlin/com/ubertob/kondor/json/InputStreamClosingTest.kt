package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.FieldsValues
import com.ubertob.kondor.json.jsonnode.NodePath
import com.ubertob.kondor.json.parser.KondorTokenizer
import com.ubertob.kondortools.expectFailure
import com.ubertob.kondortools.expectSuccess
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import strikt.api.expectThat
import com.ubertob.kondor.outcome.Failure
import strikt.assertions.isA
import strikt.assertions.isEmpty
import strikt.assertions.isFalse
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThan
import strikt.assertions.isTrue
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream

class InputStreamClosingTest {

    private class TrackedInputStream(json: String) : ByteArrayInputStream(json.toByteArray()) {
        var closed: Boolean = false
            private set

        override fun close() {
            closed = true
            super.close()
        }
    }

    private class FailingInputStream : InputStream() {
        var closed: Boolean = false
            private set

        override fun read(): Int = throw IOException("boom")

        override fun close() {
            closed = true
        }
    }

    companion object {
        private val longJson = JList(JPerson).toJson((1..20_000).map { Person(it, "name $it") })
    }

    @Test
    fun `the input stream is closed after reading a value`() {
        val stream = TrackedInputStream("""{"id": 1, "name": "Frank"}""")

        expectThat(JPerson.fromJson(stream).expectSuccess()).isEqualTo(Person(1, "Frank"))
        expectThat(stream.closed).isTrue()
    }

    @Test
    fun `a long input stream is closed after reading it to the end`() {
        val stream = TrackedInputStream(longJson)

        expectThat(JList(JPerson).fromJson(stream).expectSuccess().size).isEqualTo(20_000)
        expectThat(stream.closed).isTrue()
        expectThat(stream.available()).isEqualTo(0)
    }

    @Test
    fun `the input stream is closed when the parsing fails`() {
        val notClosed = mapOf(
            "missing value" to """{"id": 1, "name": }""",
            "truncated" to """{"id": 1, "name": "Frank"""",
            "invalid escape" to """{"id": 1, "name": "Fr\qnk"}""",
            "wrong type" to """{"id": "not a number", "name": "Frank"}""",
            "missing field" to """{"id": 1}""",
            "trailing content" to """{"id": 1, "name": "Frank"} {"id": 2, "name": "Ann"}""",
            "empty" to "",
        ).filterNot { (name, json) ->
            val stream = TrackedInputStream(json)
            expectThat(JPerson.fromJson(stream)).describedAs("parsing the $name Json").isA<Failure<*>>()
            stream.closed
        }

        expectThat(notClosed.keys).isEmpty()
    }

    @Test
    fun `the input stream is closed when a converter throws`() {
        val stream = TrackedInputStream("""{"id": 1, "name": "Frank"}""")

        JThrowingPerson.fromJson(stream).expectFailure()

        expectThat(stream.closed).isTrue()
    }

    @Test
    fun `the input stream is closed when reading it fails`() {
        val stream = FailingInputStream()

        assertThrows<IOException> { JPerson.fromJson(stream) }

        expectThat(stream.closed).isTrue()
    }

    @Test
    fun `the input stream is closed when only a part of it is read`() {
        val stream = TrackedInputStream(longJson)

        JPerson.fromJson(stream).expectFailure()

        expectThat(stream.closed).isTrue()
        expectThat(stream.available()).isGreaterThan(0)
    }

    @Test
    fun `closing the tokens of a Json in memory changes nothing`() {
        val json = """{"id": 1, "name": "Frank"}"""
        val tokens = KondorTokenizer.tokenize(json).expectSuccess()

        tokens.close()

        expectThat(tokens.toList()).isEqualTo(KondorTokenizer.tokenize(json).expectSuccess().toList())
        expectThat(JPerson.fromJson("""{"id": 1, "name": "Frank"}""").expectSuccess()).isEqualTo(Person(1, "Frank"))
    }

    @Test
    fun `there are no more tokens after closing a lazy stream`() {
        val tokens = KondorTokenizer.tokenize(longJson.byteInputStream()).expectSuccess()
        tokens.next()

        tokens.close()

        expectThat(tokens.hasNext()).isFalse()
        expectThat(tokens.toList()).isEmpty()
    }

    @Test
    fun `a token already read ahead does not survive closing a lazy stream`() {
        val tokens = KondorTokenizer.tokenize(longJson.byteInputStream()).expectSuccess()
        tokens.next()
        tokens.peek()

        tokens.close()

        expectThat(tokens.hasNext()).isFalse()
        expectThat(tokens.toList()).isEmpty()
    }
}

private object JThrowingPerson : JObj<Person>() {
    private val id by num(Person::id)
    private val name by str(Person::name)

    override fun FieldsValues.deserializeOrThrow(path: NodePath): Person = error("converter failure")
}
