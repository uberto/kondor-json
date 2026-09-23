package com.ubertob.kondor.json

import com.ubertob.kondor.json.JsonStyle.Companion.compact
import com.ubertob.kondor.json.JsonStyle.Companion.pretty
import com.ubertob.kondor.json.JsonStyle.Companion.prettyWithNulls
import com.ubertob.kondor.json.jsonnode.FieldsValues
import com.ubertob.kondor.json.jsonnode.JsonNodeObject
import com.ubertob.kondor.json.jsonnode.NodePath
import com.ubertob.kondor.json.jsonnode.NodePathRoot
import com.ubertob.kondortools.expectFailure
import com.ubertob.kondortools.expectSuccess
import com.ubertob.kondortools.isEquivalentJson
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import java.time.Instant

class EmptyJsonObjectTest {

    private val emptyStringMap = emptyMap<String, String>()

    @Test
    fun `empty JMap round-trips via json string`() {
        val jsonStr = JMap(JString).toJson(emptyStringMap)

        expectThat(jsonStr).isEqualTo("{}")
        expectThat(JMap(JString).fromJson(jsonStr).expectSuccess()).isEmpty()
        expectThat(JMap(JString).fromJson(JMap(JString).toJson(emptyStringMap, pretty)).expectSuccess()).isEmpty()
    }

    @Test
    fun `JMap parses empty json objects with any whitespace`() {
        listOf("{ }", " {\n\t} ", "{\r\n}").forEach { json ->
            expectThat(JMap(JString).fromJson(json).expectSuccess()).isEmpty()
        }
    }

    @Test
    fun `JMap parses empty json objects from an InputStream`() {
        listOf("{}", " { \n } ").forEach { json ->
            expectThat(JMap(JString).fromJson(json.byteInputStream()).expectSuccess()).isEmpty()
        }
    }

    @Test
    fun `empty JMap with custom key converter and object values round-trips`() {
        val empty = emptyMap<TaskId, Task>()

        expectThat(JTasks.fromJson(JTasks.toJson(empty)).expectSuccess()).isEqualTo(empty)
    }

    @Test
    fun `empty JMap of JObj values round-trips`() {
        val empty = emptyMap<String, Money>()

        expectThat(JMap(JMoney).fromJson(JMap(JMoney).toJson(empty)).expectSuccess()).isEqualTo(empty)
    }

    @Test
    fun `empty JMap as a field of a JObj followed by other fields round-trips`() {
        val value = Notes(Instant.parse("2024-01-01T10:00:00Z"), emptyMap())
        val json = """{"things_to_do": {}, "updated": "2024-01-01T10:00:00Z"}"""

        expectThat(JNotes.fromJson(json).expectSuccess()).isEqualTo(value)
        expectThat(JNotes.fromJson(JNotes.toJson(value)).expectSuccess()).isEqualTo(value)
        expectThat(JNotes.fromJson(JNotes.toJson(value, pretty)).expectSuccess()).isEqualTo(value)
        expectThat(JNotes.fromJson(json.byteInputStream()).expectSuccess()).isEqualTo(value)
    }

    @Test
    fun `errors after an empty JMap field keep the right path`() {
        val json = """{"things_to_do": {}, "updated": 42}"""

        val error = JNotes.fromJson(json).expectFailure()

        expectThat(error.msg).isEqualTo("Error parsing node </updated> at position 33: expected OpeningQuotes but found '42' - invalid Json")
    }

    @Test
    fun `list of empty JMaps round-trips`() {
        val value = listOf(emptyStringMap, mapOf("a" to "b"), emptyStringMap)
        val conv = JList(JMap(JString))

        expectThat(conv.fromJson(conv.toJson(value)).expectSuccess()).isEqualTo(value)
    }

    @Test
    fun `nested empty JMap round-trips`() {
        val value = mapOf("outer" to emptyStringMap)
        val conv = JMap(JMap(JString))

        expectThat(conv.fromJson(conv.toJson(value)).expectSuccess()).isEqualTo(value)
    }

    @Test
    fun `JObj with only optional fields parses an empty json object`() {
        expectThat(JOptionals.toJson(Optionals(null, null))).isEqualTo("{}")
        expectThat(JOptionals.fromJson("{}").expectSuccess()).isEqualTo(Optionals(null, null))
    }

    @Test
    fun `JObj with required fields reports the missing field, not a parsing error, on an empty json object`() {
        val error = JPerson.fromJson("{}").expectFailure()

        expectThat(error.msg).isEqualTo("Error reading property <id> of node <[root]> Not found key 'id'. Keys found: []")
    }

    @Test
    fun `empty JMap renders as empty json object in every style`() {
        expectThat(JMap(JString).toJson(emptyStringMap)).isEqualTo("{}")
        expectThat(JMap(JString).toJson(emptyStringMap, compact)).isEqualTo("{}")
        JMap(JString).toJson(emptyStringMap, pretty).isEquivalentJson("{}").expectSuccess()
        JMap(JString).toJson(emptyStringMap, prettyWithNulls).isEquivalentJson("{}").expectSuccess()
    }

    @Test
    fun `empty JMap round-trips via JsonNode`() {
        val node = JMap(JString).toJsonNode(emptyStringMap)
        expectThat(JMap(JString).fromJsonNode(node, NodePathRoot).expectSuccess()).isEmpty()

        val empty = emptyMap<TaskId, Task>()
        expectThat(JTasks.fromJsonNode(JTasks.toJsonNode(empty), NodePathRoot).expectSuccess()).isEqualTo(empty)
    }

    @Test
    fun `empty JMap as a field of a JAny round-trips`() {
        val value = Labels("empty", emptyMap())

        val jsonStr = JLabels.toJson(value)
        jsonStr.isEquivalentJson("""{"labels": {}, "name": "empty"}""").expectSuccess()
        expectThat(JLabels.fromJson(jsonStr).expectSuccess()).isEqualTo(value)
        expectThat(JLabels.fromJsonNode(JLabels.toJsonNode(value), NodePathRoot).expectSuccess()).isEqualTo(value)
    }

    @Test
    fun `empty flattened JMap round-trips`() {
        val value = MetadataFile("file.txt", emptyMap())

        val jsonStr = JMetadataFileAny.toJson(value)
        jsonStr.isEquivalentJson("""{"fileName": "file.txt"}""").expectSuccess()
        expectThat(JMetadataFileAny.fromJson(jsonStr).expectSuccess()).isEqualTo(value)
        expectThat(JMetadataFileAny.fromJsonNode(JMetadataFileAny.toJsonNode(value), NodePathRoot).expectSuccess())
            .isEqualTo(value)
    }

    @Test
    fun `empty JMap as a field of a JObj round-trips via JsonNode`() {
        val value = Notes(Instant.parse("2024-01-01T10:00:00Z"), emptyMap())

        expectThat(JNotes.fromJsonNode(JNotes.toJsonNode(value), NodePathRoot).expectSuccess()).isEqualTo(value)
    }

    @Test
    fun `JMap on an empty array fails with a clear error`() {
        val error = JMap(JString).fromJson("[]").expectFailure()

        expectThat(error.msg).isEqualTo("Error parsing node <[root]> at position 1: expected OpeningCurly but found OpeningBracket - invalid Json")
    }

    @Test
    fun `malformed objects around the empty case are still rejected`() {
        mapOf(
            "{" to "Error parsing node <[root]> at position 1: expected OpeningQuotes but found end of file - invalid Json",
            "{}}" to "Error parsing node <[root]> at position 3: expected EOF but found ClosingCurly - json continue after end",
            "{,}" to "Error parsing node <[root]> at position 2: expected OpeningQuotes but found Comma - invalid Json",
            """{,"a": "b"}""" to "Error parsing node <[root]> at position 2: expected OpeningQuotes but found Comma - invalid Json",
            """{"a": "b",}""" to "Error parsing node <[root]> at position 11: expected OpeningQuotes but found ClosingCurly - invalid Json",
        ).forEach { (json, expectedError) ->
            expectThat(JMap(JString).fromJson(json).expectFailure().msg).describedAs(json).isEqualTo(expectedError)
        }
    }
}

private data class Optionals(val name: String?, val nick: String?)

private object JOptionals : JObj<Optionals>() {
    private val name by str(Optionals::name)
    private val nick by str(Optionals::nick)

    override fun FieldsValues.deserializeOrThrow(path: NodePath) =
        Optionals(
            name = +name,
            nick = +nick
        )
}

private data class Labels(val name: String, val labels: Map<String, String>)

private object JLabels : JAny<Labels>() {
    private val name by str(Labels::name)
    private val labels by obj(JMap(JString), Labels::labels)

    override fun JsonNodeObject.deserializeOrThrow() =
        Labels(
            name = +name,
            labels = +labels
        )
}
