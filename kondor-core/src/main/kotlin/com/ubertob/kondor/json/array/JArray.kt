package com.ubertob.kondor.json.array

import com.ubertob.kondor.json.*
import com.ubertob.kondor.json.jsonnode.*

abstract class JArray<T : Any, IterT : Iterable<T?>> : JArrayRepresentable<IterT, T, IterT>() {
    override val _nodeType = ArrayNode

    override val cons: (IterT) -> IterT = { it }
    override val binder: IterT.() -> IterT = { this }
    override fun convertToAny(from: Iterable<T?>) = convertToCollection(from)

    abstract fun convertToCollection(from: Iterable<T?>): IterT
}


data class JList<T : Any>(override val converter: JConverter<T>) : JArray<T, List<T>>() {
    override fun convertToCollection(from: Iterable<T?>): List<T> = from.filterNotNull()
}

data class JNullableList<T : Any>(override val converter: JConverter<T>) : JArray<T, List<T?>>() {

    override val jsonStyle = JsonStyle.singleLineWithNulls
    override fun convertToCollection(from: Iterable<T?>): List<T?> = from.toList()
    override val _nodeType = ArrayNode
}

data class JSet<T : Any>(override val converter: JConverter<T>) : JArray<T, Set<T>>() {
    override fun convertToCollection(from: Iterable<T?>): Set<T> = from.filterNotNull().toSet()
    override val _nodeType = ArrayNode
}
