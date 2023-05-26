package com.ubertob.kondor.json.jsonnode

import com.ubertob.kondor.json.parser.TokensPath
import com.ubertob.kondor.json.parser.TokensStream

sealed class NodePath() {
    operator fun plus(nodePath: String): NodePath =
        NodePathSegment(nodePath, this)

}

object NodePathRoot : NodePath() {
    override fun toString(): String = ROOT_NODE
}

data class NodePathSegment(val nodeName: String, val parent: NodePath) : NodePath()

private val ROOT_NODE = "[root]"

fun NodePath.getPath(): String =
    when (this) {
        NodePathRoot -> ROOT_NODE
        is NodePathSegment -> parent.getPath() append nodeName
    }


fun NodePath.parent(): NodePath =
    when (this) {
        NodePathRoot -> this
        is NodePathSegment -> parent
    }

private infix fun String.append(next: String): String = if (this == ROOT_NODE) "/$next" else "$this/$next"


fun TokensStream.onRoot(): TokensPath = TokensPath(this, NodePathRoot)

