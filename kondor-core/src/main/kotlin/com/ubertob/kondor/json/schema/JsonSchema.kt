package com.ubertob.kondor.json.schema

import com.ubertob.kondor.json.*
import com.ubertob.kondor.json.jsonnode.*

//TODO
// flatten, sealed class, JMap etc...


internal fun valueSchema(nodeKind: NodeKind<*>): JsonNodeObject =
    mapOf(
        "type" to nodeKind.desc.lowercase().asNode()
    ).asNode()


internal fun <E : Enum<E>> enumSchema(values: List<E>): JsonNodeObject =

    mapOf(
        if (values.isEmpty())
            "type" to "string".asNode()
        else
            "enum" to values.map { it.name }.asNode()
    ).asNode()


internal fun arraySchema(itemsConverter: JsonConverter<*, *>): JsonNodeObject =
    mapOf(
        "type" to "array".asNode(),
        "items" to itemsConverter.schema()
    ).asNode()

internal fun objectSchema(properties: Iterable<JsonProperty<*>>): JsonNodeObject {

    val reqProp = mutableListOf<String>()
    val pmap = properties.map { prop ->

        val converter = when (prop) { //todo add the info mandatory or not...
            is JsonPropMandatory<*, *> -> prop.converter.also { reqProp.add(prop.propName) }
            is JsonPropMandatoryFlatten<*> -> prop.converter //this must be flatten
            is JsonPropOptional<*, *> -> prop.converter
        }
        prop.propName to converter.schema()
    }.toMap()
    val propNode = JsonNodeObject(pmap, NodePathRoot)

    val map = mapOf(
        "type" to "object".asNode(),
        "properties" to propNode,
        "required" to reqProp.asNode()
    )

    return JsonNodeObject(map, NodePathRoot)
}


internal fun String.asNode() = JsonNodeString(this, NodePathRoot)
internal fun List<String>.asNode() = JsonNodeArray(this.map { it.asNode() }, NodePathRoot)
internal fun Map<String, JsonNode>.asNode() = JsonNodeObject(this, NodePathRoot)
