package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.JsonNodeObject
import com.ubertob.kondor.json.jsonnode.NodePathRoot
import com.ubertob.kondor.json.jsonnode.parseJsonNode
import com.ubertob.kondortools.expectFailure
import com.ubertob.kondortools.expectSuccess
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

// JObj is the converter to use, and most tests use it. JAny parses through a JsonNode, which JSealed and the JsonNode
// converters still need: these few tests cover it end to end, from a String, a stream and a JsonNode.
class JAnyTest {

    private val invoice = Invoice(
        id = InvoiceId("INV-42"),
        vat = true,
        customer = Company("ACME", TaxType.EU),
        items = listOf(Product(1, "pen", "a blue pen", 1.5), Product(2, "gift", "free", null)),
        total = BigDecimal("1.50"),
        created = LocalDate.of(2026, 9, 23),
        paid = null
    )

    @Test
    fun `a JAny round trips from a String, a stream and a JsonNode`() {
        val json = Invoice.Json.toJson(invoice)

        expectThat(Invoice.Json.fromJson(json).expectSuccess()).isEqualTo(invoice)
        expectThat(Invoice.Json.fromJson(json.byteInputStream()).expectSuccess()).isEqualTo(invoice)
        expectThat(
            Invoice.Json.fromJsonNode(parseJsonNode(json).expectSuccess() as JsonNodeObject, NodePathRoot).expectSuccess()
        ).isEqualTo(invoice)
        expectThat(Invoice.Json.fromJsonNode(Invoice.Json.toJsonNode(invoice), NodePathRoot).expectSuccess())
            .isEqualTo(invoice)
    }

    @Test
    fun `a JAny reads absent and null optional fields`() {
        val withoutPrice = Product(3, "box", "empty", null)

        expectThat(Product.Json.fromJson("""{"id": 3, "short-desc": "box", "long_description": "empty"}""").expectSuccess())
            .isEqualTo(withoutPrice)
        expectThat(Product.Json.fromJson("""{"id": 3, "short-desc": "box", "long_description": "empty", "price": null}""")
            .expectSuccess()).isEqualTo(withoutPrice)
    }

    @Test
    fun `a JAny reports a missing field and a wrong type with their path`() {
        expectThat(Invoice.Json.fromJson("""{"id": "INV-42", "vat-to-pay": true}""").expectFailure().msg)
            .isEqualTo("Error reading property <customer> of node <[root]> Not found key 'customer'. Keys found: [id, vat-to-pay]")
        expectThat(Product.Json.fromJson("""{"id": "one", "short-desc": "s", "long_description": "l"}""").expectFailure().msg)
            .isEqualTo("Error converting node </id> expected a Number but found String 'one'")
    }

    @Test
    fun `a JAny flattens an object, a map and a JsonNodeObject`() {
        val selected = SelectedFile(true, FileInfo("notes.txt", Instant.ofEpochMilli(1_000), false, 42, "/home"))
        val metadata = MetadataFile("photo.jpg", mapOf("camera" to "x100", "iso" to "200"))
        val dynamic = DynamicAttr(7, "dyn", parseJsonNode("""{"color": "red", "size": 3}""").expectSuccess() as JsonNodeObject)

        expectThat(JSelectedFileAny.fromJson(JSelectedFileAny.toJson(selected)).expectSuccess()).isEqualTo(selected)
        expectThat(JMetadataFileAny.fromJson(JMetadataFileAny.toJson(metadata)).expectSuccess()).isEqualTo(metadata)
        expectThat(JDynamicAttrAny.fromJson(JDynamicAttrAny.toJson(dynamic)).expectSuccess()).isEqualTo(dynamic)
        expectThat(JSelectedFileAny.toJson(selected))
            .isEqualTo("""{"selected": true, "file_name": "notes.txt", "creation_date": 1000, "is_dir": false, "size": 42, "folder_path": "/home"}""")
    }

    @Test
    fun `a JSealed reads its JAny subtypes by the discriminator`() {
        val variants = listOf(VariantString("s", "text"), VariantInt("i", 42))

        variants.forEach { variant ->
            val json = JVariant.toJson(variant)

            expectThat(JVariant.fromJson(json).expectSuccess()).isEqualTo(variant)
            expectThat(JVariant.fromJson(json.byteInputStream()).expectSuccess()).isEqualTo(variant)
        }
        //without the discriminator the default converter is used
        expectThat(JVariant.fromJson("""{"name": "n", "value": "v"}""").expectSuccess()).isEqualTo(VariantString("n", "v"))
    }

    @Test
    fun `a JAny ignores the fields it does not declare`() {
        val json = """{"_id": 1, "name": "n", "nodeType": "file", "extra": [1, {"a": null}], "path": "/a"}"""

        expectThat(JGraphNodeAny.fromJson(json).expectSuccess()).isEqualTo(GraphNode("n", "file", "/a"))
    }
}
