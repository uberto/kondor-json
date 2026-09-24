package com.ubertob.kondor.json

import com.ubertob.kondor.json.JsonStyle.Companion.pretty
import com.ubertob.kondor.json.jsonnode.FieldsValues
import com.ubertob.kondor.json.jsonnode.JsonNodeObject
import com.ubertob.kondor.json.jsonnode.NodePath
import com.ubertob.kondor.json.jsonnode.NodePathRoot
import com.ubertob.kondor.json.jsonnode.parseJsonNode
import com.ubertob.kondortools.expectSuccess
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class MappingAndJsonNodeConsistencyTest {
    data class Tree(val id: String, val children: List<Tree> = emptyList())

    object JTree : JObj<Tree>() {
        val a by str(Tree::id)
        val children by array(JTree, Tree::children)

        override fun FieldsValues.deserializeOrThrow(path: NodePath) = Tree(+a, +children)
    }

    object JTreeAny : JAny<Tree>() {
        val a by str(Tree::id)
        val children by array(JTreeAny, Tree::children)

        override fun JsonNodeObject.deserializeOrThrow() = Tree(+a, +children)
    }

    private val tree = Tree("root", listOf(Tree("1", listOf(Tree("1.1"))), Tree("2")))

    @Test
    fun `serialising json node and mapping an object create the same json`() {
        listOf(JTree, JTreeAny).forEach { converter ->
            val mappedObject = converter.toJson(tree, pretty)

            val nodes = parseJsonNode(mappedObject).expectSuccess()

            expectThat(nodes.render(pretty)).isEqualTo(mappedObject)
            expectThat(converter.toJsonNode(tree).render(pretty)).isEqualTo(mappedObject)
        }
    }

    @Test
    fun `the Json and its JsonNode are read back as the same object`() {
        listOf(JTree, JTreeAny).forEach { converter ->
            val mappedObject = converter.toJson(tree, pretty)

            expectThat(converter.fromJson(mappedObject).expectSuccess()).isEqualTo(tree)
            expectThat(converter.fromJsonNode(parseJsonNode(mappedObject).expectSuccess() as JsonNodeObject, NodePathRoot)
                .expectSuccess()).isEqualTo(tree)
        }
    }
}
