package com.ubertob.kondor.json

sealed class NodePath()
object NodeRoot : NodePath()
data class Node(val nodeName: String, val parent: NodePath) : NodePath()


private val ROOT_NODE = "[root]"

fun NodePath.getPath(): String =
    when (this) {
        NodeRoot -> ROOT_NODE
        is Node -> parent.getPath() append nodeName
    }

private infix fun String.append(next: String): String = if (this == ROOT_NODE) "/$next" else "$this/$next"
