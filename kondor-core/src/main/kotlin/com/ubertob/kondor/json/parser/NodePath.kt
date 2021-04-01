package com.ubertob.kondor.json

sealed class NodePath()
object NodePathRoot : NodePath()
data class NodePathSegment(val nodeName: String, val parent: NodePath) : NodePath()


private val ROOT_NODE = "[root]"

fun NodePath.getPath(): String =
    when (this) {
        NodePathRoot -> ROOT_NODE
        is NodePathSegment -> parent.getPath() append nodeName
    }

private infix fun String.append(next: String): String = if (this == ROOT_NODE) "/$next" else "$this/$next"
