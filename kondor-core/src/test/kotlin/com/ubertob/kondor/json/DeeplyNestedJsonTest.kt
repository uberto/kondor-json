package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.ArrayNode
import com.ubertob.kondor.json.jsonnode.FieldsValues
import com.ubertob.kondor.json.jsonnode.JsonNodeObject
import com.ubertob.kondor.json.jsonnode.NodePath
import com.ubertob.kondor.json.jsonnode.NodePathRoot
import com.ubertob.kondor.json.jsonnode.ObjectNode
import com.ubertob.kondor.json.jsonnode.parseJsonNode
import com.ubertob.kondor.json.parser.KondorTokenizer
import com.ubertob.kondor.json.parser.TokensPath
import com.ubertob.kondor.outcome.Failure
import com.ubertob.kondor.outcome.bind
import com.ubertob.kondortools.expectFailure
import com.ubertob.kondortools.expectSuccess
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.fail
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEmpty
import strikt.assertions.isTrue
import java.io.ByteArrayInputStream

// parsing is recursive: a Json nested too deeply for the stack must be an error, not a StackOverflowError.
// The depth the stack allows is a few hundred levels, and it changes with the size of the stack and with how warm
// the JVM is (a cold converter fails around 275 here, a hot one goes over 1500), so the tests stay far from it:
// either much deeper, or a depth every stack can take. `a recursive converter never throws` accepts both results.
class DeeplyNestedJsonTest {

    private val tooDeepError = "Error parsing node <[root]> the Json is nested too deeply to be parsed"

    private fun nestedArrays(depth: Int) = "[".repeat(depth) + "1" + "]".repeat(depth)
    private fun nestedObjects(depth: Int) = """{"a":""".repeat(depth) + "1" + "}".repeat(depth)
    private fun nestedTrees(depth: Int) = """{"a":"x","children":[""".repeat(depth) +
            """{"a":"leaf","children":[]}""" + "]}".repeat(depth)

    @Test
    fun `a Json nested too deeply is an error for parseJsonNode`() {
        listOf(nestedArrays(10_000), nestedObjects(10_000)).forEach { json ->
            expectThat(parseFailure(json) { parseJsonNode(it) }.msg).isEqualTo(tooDeepError)
        }
    }

    @Test
    fun `a Json nested too deeply is an error for a NodeKind, parsing a String or the tokens`() {
        expectThat(parseFailure(nestedArrays(10_000)) { ArrayNode.fromJsonString(it) }.msg).isEqualTo(tooDeepError)
        expectThat(parseFailure(nestedObjects(10_000)) { ObjectNode.fromJsonString(it) }.msg).isEqualTo(tooDeepError)

        expectThat(parseFailure(nestedObjects(10_000)) { json ->
            KondorTokenizer.tokenize(json).bind { ObjectNode.parse(TokensPath(it, NodePathRoot)) }
        }.msg).isEqualTo(tooDeepError)
    }

    @Test
    fun `a Json nested too deeply is an error for a converter, from a String and from a stream`() {
        listOf(nestedArrays(10_000), nestedObjects(10_000)).forEach { json ->
            expectThat(parseFailure(json) { JJsonNode.fromJson(it) }.msg).isEqualTo(tooDeepError)
            expectThat(parseFailure(json) { JJsonNode.fromJson(it.byteInputStream()) }.msg).isEqualTo(tooDeepError)
        }
    }

    @Test
    fun `a deeply nested unknown field is an error for a JObj, from a String and from a stream`() {
        val json = """{"id": 1, "name": "Frank", "extra": ${nestedArrays(10_000)}}"""

        expectThat(parseFailure(json) { JPerson.fromJson(it) }.msg).isEqualTo(tooDeepError)
        expectThat(parseFailure(json) { JPerson.fromJson(it.byteInputStream()) }.msg).isEqualTo(tooDeepError)
    }

    @Test
    fun `a recursive converter never throws, at any depth, in a JAny and in a JObj`() {
        (100..2_000 step 100).forEach { depth ->
            val json = nestedTrees(depth)

            listOf(JNestedTreeAny, JNestedTreeObj).forEach { converter ->
                val outcome = try {
                    converter.fromJson(json)
                } catch (t: Throwable) {
                    fail("$t parsing a tree nested $depth deep with $converter")
                }
                if (outcome is Failure) {
                    expectThat(outcome.error.msg).describedAs("a tree nested $depth deep").isEqualTo(tooDeepError)
                }
            }
        }
    }

    @Test
    fun `a converter recursing on itself reports the same error, and fromJsonNode is not guarded`() {
        expectThat(parseFailure("""{"a": "x"}""") { JRecursingForever.fromJson(it) }.msg).isEqualTo(tooDeepError)
        expectThat(parseFailure("""{"a": "x"}""") { JRecursingForever.fromJson(it.byteInputStream()) }.msg)
            .isEqualTo(tooDeepError)

        assertThrows<StackOverflowError> {
            JRecursingForever.fromJsonNode(parseJsonNode("""{"a": "x"}""").expectSuccess() as JsonNodeObject, NodePathRoot)
        }
    }

    @Test
    fun `the helpers converting an exception let a stack overflow through`() {
        assertThrows<StackOverflowError> { tryWithPath<String>(NodePathRoot) { throw StackOverflowError() } }
        assertThrows<StackOverflowError> { tryFromNode<String>(NodePathRoot) { throw StackOverflowError() } }
    }

    @Test
    fun `the input stream is closed when the Json is nested too deeply`() {
        val stream = object : ByteArrayInputStream(nestedArrays(10_000).toByteArray()) {
            var closed = false
            override fun close() {
                closed = true
                super.close()
            }
        }

        expectThat(JJsonNode.fromJson(stream).expectFailure().msg).isEqualTo(tooDeepError)
        expectThat(stream.closed).isTrue()
    }

    @Test
    fun `a normally nested Json is still read`() {
        //far below the limit: a small stack, or a cold JVM, lowers it a lot
        val json = nestedArrays(120)

        expectThat(parseJsonNode(json).expectSuccess().render()).isEqualTo(json)
        expectThat(JJsonNode.fromJson(nestedObjects(120)).expectSuccess().render()).isEqualTo(nestedObjects(120))
        expectThat(JNestedTreeObj.fromJson(nestedTrees(30)).expectSuccess().children).isNotEmpty()
    }

    private fun parseFailure(json: String, parse: (String) -> JsonOutcome<*>): JsonError =
        try {
            parse(json).expectFailure()
        } catch (t: Throwable) {
            fail("$t parsing a Json nested ${json.count { it == '[' || it == '{' }} deep")
        }


    private data class NestedTree(val a: String, val children: List<NestedTree>)

    private object JNestedTreeAny : JAny<NestedTree>() {
        private val a by str(NestedTree::a)
        private val children by array(JNestedTreeAny, NestedTree::children)

        override fun JsonNodeObject.deserializeOrThrow() = NestedTree(+a, +children)
    }

    private object JNestedTreeObj : JObj<NestedTree>() {
        private val a by str(NestedTree::a)
        private val children by array(JNestedTreeObj, NestedTree::children)

        override fun FieldsValues.deserializeOrThrow(path: NodePath) = NestedTree(+a, +children)
    }

    // only to check how a StackOverflowError of a converter is reported: it recurses until the stack is over
    private object JRecursingForever : JObj<String>() {
        private val a by str(String::toString)

        override fun FieldsValues.deserializeOrThrow(path: NodePath): String = recurseForever(+a, 0)

        private fun recurseForever(text: String, depth: Int): String =
            if (depth < 0) text else recurseForever(text, depth + 1)
    }
}
