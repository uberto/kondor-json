package com.ubertob.kondor.json.schema

import com.ubertob.kondor.json.*
import com.ubertob.kondor.json.jsonnode.JsonNodeObject
import com.ubertob.kondor.json.jsonnode.JsonNodeString
import com.ubertob.kondor.json.jsonnode.NodeKind
import com.ubertob.kondor.json.jsonnode.NodePathRoot

//TODO
// enums
// mandatory/optional prop
// arrays
// etc.


fun NodeKind<*>. createSchema() = JsonNodeObject(mapOf("type" to JsonNodeString(desc, NodePathRoot)), NodePathRoot)

fun Iterable<JsonProperty<*>>.createSchema(): JsonNodeObject {

    val pmap = this.map { prop ->

        val converter = when (prop) { //todo add the info mandatory or not...
            is JsonPropMandatory<*, *> -> prop.converter
            is JsonPropMandatoryFlatten<*> -> prop.converter
            is JsonPropOptional<*, *> -> prop.converter
        }
        prop.propName to converter.schema()
    }.toMap()
    val propNode = JsonNodeObject(pmap, NodePathRoot)

    val map = mapOf(
        "type" to "object".asNode(),
        "properties" to propNode
    )

    return JsonNodeObject(map, NodePathRoot)
}
