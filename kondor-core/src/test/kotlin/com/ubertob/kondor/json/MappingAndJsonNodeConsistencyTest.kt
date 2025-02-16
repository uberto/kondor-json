package com.ubertob.kondor.json

import com.ubertob.kondor.json.JsonStyle.Companion.pretty
import com.ubertob.kondor.json.jsonnode.JsonNodeObject
import com.ubertob.kondor.json.jsonnode.parseJsonNode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MappingAndJsonNodeConsistencyTest {
    data class Tree(val id: String, val children: List<Tree> = emptyList())
    
    object JTree : JAny<Tree>() {
        val a by str(Tree::id)
        val children by array(JTree, Tree::children)
        
        override fun JsonNodeObject.deserializeOrThrow() = Tree(+a,+children)
    }
    
    @Test
    fun `serialising json node and mapping an object create the same json`() {
        val p = Tree("root", listOf(Tree("1", listOf(Tree("1.1"))), Tree("2")))
        
        val mappedObject = JTree.toJson(p, pretty)
        
        val nodes = parseJsonNode(mappedObject).orThrow()
        
        val serialisedNodes = nodes.render(pretty)
        
        assertEquals(mappedObject, serialisedNodes)
    }
}
