package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.FieldsValues
import com.ubertob.kondor.json.jsonnode.JsonNodeObject
import com.ubertob.kondor.json.jsonnode.NodePath
import com.ubertob.kondor.json.jsonnode.NodePathRoot
import com.ubertob.kondor.json.jsonnode.parseJsonNode
import com.ubertob.kondortools.expectFailure
import com.ubertob.kondortools.expectSuccess
import com.ubertob.kondortools.isEquivalentJson
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import java.time.Instant

class JObjUnknownFieldsTest {

    private val product = Product(id = 42, shortDesc = "short", longDesc = "long", price = 12.5)

    private val productWithUnknownFields = """{
        "_id": {"${'$'}oid": "5f1d7c3e9b1e8a2f4c6d8e0a"},
        "id": 42,
        "tags": ["a", {"nested": [1, 2, {"deep": null}]}, [], {}],
        "short-desc": "short",
        "note": "with \"quotes\", {braces} and [brackets] è \n",
        "empty_obj": {},
        "empty_arr": [],
        "long_description": "long",
        "flag": true,
        "nothing": null,
        "big": -1.5e300,
        "price": 12.5,
        "last": {"a": {"b": {"c": [[[]]]}}}
    }"""

    @Test
    fun `JObj and JAny ignore unknown fields of any kind`() {
        expectThat(JProduct.fromJson(productWithUnknownFields).expectSuccess()).isEqualTo(product)
        expectThat(Product.Json.fromJson(productWithUnknownFields).expectSuccess()).isEqualTo(product)
    }

    @Test
    fun `JObj ignores unknown fields parsing from an InputStream`() {
        expectThat(JProduct.fromJson(productWithUnknownFields.byteInputStream()).expectSuccess()).isEqualTo(product)
    }

    @Test
    fun `JObj ignores unknown fields parsing from a JsonNode`() {
        val node = parseJsonNode(productWithUnknownFields).expectSuccess() as JsonNodeObject

        expectThat(JProduct.fromJsonNode(node, NodePathRoot).expectSuccess()).isEqualTo(product)
    }

    @Test
    fun `JObj ignores unknown fields in nested objects and arrays`() {
        val json = """[
            {"user": {"id": 1, "name": "Frank", "age": 30},
             "file": {"selected": true, "file_name": "f", "creation_date": 0, "folder_path": "/a", "is_dir": false, "size": 1, "extra": [1]},
             "unknown": {"user": {"id": "not a number"}}}
        ]"""

        val expected = UserFile(
            Person(1, "Frank"),
            SelectedFile(true, FileInfo("f", Instant.EPOCH, false, 1, "/a"))
        )
        expectThat(JList(JUserFile).fromJson(json).expectSuccess()).isEqualTo(listOf(expected))
    }

    @Test
    fun `JObj and JAny with only unknown fields report the first missing mandatory field`() {
        val json = """{"zeta": 1, "alpha": {"id": 3}}"""

        val objError = JPerson.fromJson(json).expectFailure()
        val anyError = JGraphNode.fromJson(json).expectFailure()

        expectThat(objError.msg).isEqualTo("Error reading property <id> of node <[root]> Not found key 'id'. Keys found: [alpha, zeta]")
        expectThat(anyError.msg).isEqualTo("Error reading property <name> of node <[root]> Not found key 'name'. Keys found: [alpha, zeta]")
    }

    @Test
    fun `JObj and JAny missing field errors list the unknown keys`() {
        val json = """{"zeta": 0, "id": 1, "short-desc": "s", "price": null, "alpha": [], "long-description": "typo"}"""

        val objError = JProduct.fromJson(json).expectFailure()
        val anyError = Product.Json.fromJson(json).expectFailure()

        expectThat(objError.msg).isEqualTo("Error reading property <long_description> of node <[root]> Not found key 'long_description'. Keys found: [alpha, id, long-description, price, short-desc, zeta]")
        expectThat(objError.msg).isEqualTo(anyError.msg)
    }

    @Test
    fun `invalid Json inside an unknown field is still an error`() {
        val invalidJsons = listOf(
            """{"id": 1, "extra": [1,,2], "name": "a"}""",
            """{"id": 1, "extra": {"a" 1}, "name": "a"}""",
            """{"id": 1, "extra": , "name": "a"}""",
        )

        invalidJsons.forEach { json ->
            JPerson.fromJson(json).expectFailure()
            JPerson.fromJson(json.byteInputStream()).expectFailure()
        }
    }

    @Test
    fun `a missing value in an unknown field reports the path`() {
        val error = JPerson.fromJson("""{"id": 1, "extra": }""").expectFailure()

        expectThat(error.msg).isEqualTo("Error parsing node </extra> at position 18: expected a valid node but found nothing - invalid Json")
    }

    @Test
    fun `invalid Json inside an unknown field reports the path`() {
        val error = JPerson.fromJson("""{"id": 1, "extra": {"a": [1,,2]}, "name": "a"}""").expectFailure()

        expectThat(error.msg).isEqualTo("Error parsing node </extra/a> at position 28: expected a new node but found Comma - Comma in wrong position")
    }

    // flatten on JObj

    @Test
    fun `JObj with a flatten JAny field round-trips`() {
        val value = SelectedFile(true, FileInfo("f.txt", Instant.ofEpochMilli(1234), false, 99, "/a/b"))

        checkRoundTrip(JSelectedFileFlat, value)
        (JSelectedFileFlat.toJson(value) isEquivalentJson JSelectedFile.toJson(value)).expectSuccess()
    }

    @Test
    fun `JObj with a flatten JObj field round-trips`() {
        val value = SelectedFile(false, FileInfo("g", Instant.ofEpochMilli(-5), true, 0, ""))

        checkRoundTrip(JSelectedFileFlatObj, value)
    }

    @Test
    fun `JObj with a flatten field ignores fields unknown to the flattened converter`() {
        val json = """{"selected": true, "_id": 3, "file_name": "f", "creation_date": 0, "folder_path": "/a", "is_dir": false, "size": 1}"""

        val expected = SelectedFile(true, FileInfo("f", Instant.EPOCH, false, 1, "/a"))
        expectThat(JSelectedFileFlat.fromJson(json).expectSuccess()).isEqualTo(expected)
        expectThat(JSelectedFileFlatObj.fromJson(json).expectSuccess()).isEqualTo(expected)
        expectThat(JSelectedFile.fromJson(json).expectSuccess()).isEqualTo(expected)
    }

    @Test
    fun `JObj with a flatten field reports a missing flattened field like JAny`() {
        val json = """{"selected": true, "creation_date": 0, "folder_path": "/a", "is_dir": false, "size": 1}"""

        val objError = JSelectedFileFlat.fromJson(json).expectFailure()
        val anyError = JSelectedFile.fromJson(json).expectFailure()

        expectThat(objError.msg).isEqualTo("Error reading property <file_name> of node <[root]> Not found key 'file_name'. Keys found: [creation_date, folder_path, is_dir, size]")
        expectThat(objError.msg).isEqualTo(anyError.msg)
    }

    @Test
    fun `JObj with a flatten field reports a missing own field`() {
        val json = """{"file_name": "f", "creation_date": 0, "folder_path": "/a", "is_dir": false, "size": 1}"""

        val error = JSelectedFileFlat.fromJson(json).expectFailure()

        expectThat(error.msg).isEqualTo("Error reading property <selected> of node <[root]> Not found key 'selected'. Keys found: [creation_date, file_name, folder_path, is_dir, size]")
    }

    @Test
    fun `JObj with a flatten JMap field round-trips`() {
        val value = MetadataFile("f.txt", mapOf("author" to "me", "tag" to "x", "empty" to ""))
        checkRoundTrip(JMetadataFileObj, value)

        val empty = MetadataFile("f.txt", emptyMap())
        checkRoundTrip(JMetadataFileObj, empty)
    }

    @Test
    fun `JObj with a flatten JMap field collects all unknown fields like JAny`() {
        val json = """{"z": "last", "fileName": "f", "a": "first"}"""

        val expected = MetadataFile("f", mapOf("a" to "first", "z" to "last"))
        expectThat(JMetadataFileObj.fromJson(json).expectSuccess()).isEqualTo(expected)
        expectThat(JMetadataFile.fromJson(json).expectSuccess()).isEqualTo(expected)
    }

    @Test
    fun `JObj with a flatten JsonNodeObject field round-trips`() {
        val attributes = parseJsonNode("""{"color": "red", "size": 3, "tags": ["a", "b"], "sub": {"x": 1.5}}""")
            .expectSuccess() as JsonNodeObject
        val value = DynamicAttr(1, "dyn", attributes)

        checkRoundTrip(JDynamicAttrObj, value)
        (JDynamicAttrObj.toJson(value) isEquivalentJson JDynamicAttr.toJson(value)).expectSuccess()
    }

    @Test
    fun `JObj with the flatten field declared first reads only the undeclared fields`() {
        val value = MetadataFile("f.txt", mapOf("author" to "me"))
        checkRoundTrip(JMetadataFileFlattenFirst, value)

        val json = """{"fileName": "f", "k": "v"}"""
        val node = parseJsonNode(json).expectSuccess() as JsonNodeObject
        val expected = MetadataFile("f", mapOf("k" to "v"))
        expectThat(JMetadataFileFlattenFirst.fromJson(json).expectSuccess()).isEqualTo(expected)
        expectThat(JMetadataFileFlattenFirst.fromJsonNode(node, NodePathRoot).expectSuccess()).isEqualTo(expected)
    }

    @Test
    fun `JObj and JAny report a null unknown field read by a flatten JMap`() {
        val json = """{"fileName": "f", "k": null}"""

        val objError = JMetadataFileObj.fromJson(json).expectFailure()
        val anyError = JMetadataFile.fromJson(json).expectFailure()

        expectThat(objError.msg).isEqualTo("Error reading property <k> of node </k> Found null for non-nullable")
        expectThat(objError.msg).isEqualTo(anyError.msg)
    }

    @Test
    fun `JObj and JAny report a wrong type in a flattened field with its path`() {
        val json = """{"selected": true, "file_name": "f", "creation_date": 0, "folder_path": "/a", "is_dir": false, "size": "big"}"""

        val objError = JSelectedFileFlat.fromJson(json).expectFailure()
        val anyError = JSelectedFile.fromJson(json).expectFailure()

        expectThat(objError.msg).contains("</size>")
        expectThat(objError.msg).isEqualTo(anyError.msg)
    }

    @Test
    fun `JObj with two flatten fields and no other fields passes all fields to both`() {
        val json = """{"id": 1, "name": "n", "file_name": "f", "creation_date": 0, "folder_path": "/a", "is_dir": false, "size": 1}"""

        val (fileInfo, attributes) = JTwoFlattens.fromJson(json).expectSuccess()

        expectThat(fileInfo).isEqualTo(FileInfo("f", Instant.EPOCH, false, 1, "/a"))
        expectThat(attributes._fieldMap.map.keys).isEqualTo(setOf("id", "name", "file_name", "creation_date", "folder_path", "is_dir", "size"))
    }

    @Test
    fun `JObj with a flatten field and an absent or null optional field`() {
        val json = """{"z": "last", "fileName": null, "a": "first"}"""

        expectThat(JOptionalNameMetadata.fromJson(json).expectSuccess())
            .isEqualTo(OptionalNameMetadata(null, mapOf("a" to "first", "z" to "last")))
        expectThat(JOptionalNameMetadata.fromJson("""{"a": "first"}""").expectSuccess())
            .isEqualTo(OptionalNameMetadata(null, mapOf("a" to "first")))
    }

    private fun <T : Any> checkRoundTrip(converter: JObj<T>, value: T) {
        val jsonNode = converter.toJsonNode(value)
        expectThat(converter.fromJsonNode(jsonNode, NodePathRoot).expectSuccess()).isEqualTo(value)

        val json = converter.toJson(value)
        expectThat(converter.fromJson(json).expectSuccess()).isEqualTo(value)
        expectThat(converter.fromJson(json.byteInputStream()).expectSuccess()).isEqualTo(value)
    }
}

object JSelectedFileFlat : JObj<SelectedFile>() {
    val selected by bool(SelectedFile::selected)
    val file_info by flatten(JFileInfo, SelectedFile::file)

    override fun FieldsValues.deserializeOrThrow(path: NodePath) =
        SelectedFile(
            selected = +selected,
            file = +file_info,
        )
}

object JSelectedFileFlatObj : JObj<SelectedFile>() {
    val selected by bool(SelectedFile::selected)
    val file_info by flatten(JFileInfoNew, SelectedFile::file)

    override fun FieldsValues.deserializeOrThrow(path: NodePath) =
        SelectedFile(
            selected = +selected,
            file = +file_info,
        )
}

object JMetadataFileObj : JObj<MetadataFile>() {
    val fileName by str(MetadataFile::filename)
    val metadata by flatten(JMap(), MetadataFile::metadata)

    override fun FieldsValues.deserializeOrThrow(path: NodePath) =
        MetadataFile(
            filename = +fileName,
            metadata = +metadata
        )
}

object JDynamicAttrObj : JObj<DynamicAttr>() {
    private val id by num(DynamicAttr::id)
    private val name by str(DynamicAttr::name)
    private val attributes by flatten(DynamicAttr::attributes)

    override fun FieldsValues.deserializeOrThrow(path: NodePath) = DynamicAttr(
        id = +id,
        name = +name,
        attributes = +attributes
    )
}

object JMetadataFileFlattenFirst : JObj<MetadataFile>() {
    val metadata by flatten(JMap(), MetadataFile::metadata)
    val fileName by str(MetadataFile::filename)

    override fun FieldsValues.deserializeOrThrow(path: NodePath) =
        MetadataFile(
            filename = +fileName,
            metadata = +metadata
        )
}

// only for reading: rendering it would write the FileInfo fields twice, as they are also in the attributes
object JTwoFlattens : JObj<Pair<FileInfo, JsonNodeObject>>() {
    val file by flatten(JFileInfo, Pair<FileInfo, JsonNodeObject>::first)
    val attributes by flatten(Pair<FileInfo, JsonNodeObject>::second)

    override fun FieldsValues.deserializeOrThrow(path: NodePath) = +file to +attributes
}

data class OptionalNameMetadata(val name: String?, val metadata: Map<String, String>)

object JOptionalNameMetadata : JObj<OptionalNameMetadata>() {
    val fileName by str(OptionalNameMetadata::name)
    val metadata by flatten(JMap(), OptionalNameMetadata::metadata)

    override fun FieldsValues.deserializeOrThrow(path: NodePath) =
        OptionalNameMetadata(
            name = +fileName,
            metadata = +metadata
        )
}
