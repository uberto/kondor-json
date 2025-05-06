package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.ArrayNode
import com.ubertob.kondor.json.jsonnode.NodePathRoot
import com.ubertob.kondortools.expectSuccess
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

/**
 * This test demonstrates how to create a JArray converter for a custom collection
 * that doesn't inherit from Iterable<T> but takes a list as a constructor parameter.
 */
class JCustomCollectionTest {

    // Custom collection that doesn't inherit from Iterable<T>
    // but takes a list as a constructor parameter
    data class StringBox(val strings: List<String>) {
        // Some utility methods to demonstrate functionality
        fun count(): Int = strings.size
        fun concatenate(): String = strings.joinToString("")
        fun isEmpty(): Boolean = strings.isEmpty()
    }

    // JArray converter for StringBox
    object JStringBox : JArray<String, StringBox> {
        override val converter = JString
        override val _nodeType = ArrayNode

        // Convert from Iterable<String?> to StringBox
        override fun convertToCollection(from: Iterable<String?>): StringBox =
            StringBox(from.filterNotNull().toList())

        // Convert from StringBox to Iterable<String?>
        override fun convertFromCollection(collection: StringBox): Iterable<String?> =
            collection.strings

    }

    @Test
    fun `convert StringBox to JSON and back`() {
        // Create a StringBox with some test data
        val stringBox = StringBox(listOf("hello", "world", "test"))

        // Convert to JSON node
        val jsonNode = JStringBox.toJsonNode(stringBox)

        // Convert back to StringBox
        val result = JStringBox.fromJsonNode(jsonNode, NodePathRoot).expectSuccess()

        // Verify the result matches the original
        expectThat(result).isEqualTo(stringBox)

        // Convert to JSON string
        val jsonString = JStringBox.toJson(stringBox)

        // Convert back to StringBox
        val resultFromString = JStringBox.fromJson(jsonString).expectSuccess()

        // Verify the result matches the original
        expectThat(resultFromString).isEqualTo(stringBox)
    }

    @Test
    fun `convert empty StringBox to JSON and back`() {
        // Create an empty StringBox
        val emptyStringBox = StringBox(emptyList())

        // Convert to JSON node
        val jsonNode = JStringBox.toJsonNode(emptyStringBox)

        // Convert back to StringBox
        val result = JStringBox.fromJsonNode(jsonNode, NodePathRoot).expectSuccess()

        // Verify the result matches the original
        expectThat(result).isEqualTo(emptyStringBox)

        // Convert to JSON string
        val jsonString = JStringBox.toJson(emptyStringBox)

        // Convert back to StringBox
        val resultFromString = JStringBox.fromJson(jsonString).expectSuccess()

        // Verify the result matches the original
        expectThat(resultFromString).isEqualTo(emptyStringBox)
    }
}
