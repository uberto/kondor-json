package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.*
import com.ubertob.kondor.json.schema.objectSchema
import com.ubertob.kondor.outcome.asSuccess

class JMap<K : Any, V : Any>(
    private val keyConverter: JStringRepresentable<K>,
    private val valueConverter: JConverter<V>
) : JObj<Map<K, V>>() {

    override fun schema(): JsonNodeObject =
        //!!! we shouldn't need this override, investigate
        if (keyConverter is JStringRepresentable<*> && keyConverter.cons === { it: String -> it })
            objectSchema(emptyList()) // for string-keyed maps, return empty object schema with type
        else
            JsonNodeObject(FieldNodeMap(mapOf("type" to JsonNodeString("object")))) // for non-string keys, just return type:object

    companion object {
        operator fun <V : Any> invoke(valueConverter: JConverter<V>): JMap<String, V> =
            JMap(
                object : JStringRepresentable<String>() {
                    override val cons: (String) -> String = { it }
                    override val render: (String) -> String = { it }
                },
                valueConverter
            )

        operator fun invoke(): JMap<String, String> =
            JMap(JString)
    }

    override fun resolveConverter(fieldName: String, nodePath: NodePath): JsonOutcome<JsonConverter<*, *>> =
        valueConverter.asSuccess()


    @Suppress("UNCHECKED_CAST")
    override fun FieldsValues.deserializeOrThrow(path: NodePath): Map<K, V> =
        getMap().entries.associate { (key, value) ->
            val mapKey = keyConverter.cons(key)
            mapKey to value as V
        }


    private fun valueAppender(value: V?): ValueAppender? =
        if (value == null)
            null
        else { style, off ->
            valueConverter.appendValue(this, style, off, value)
        }

    override fun fieldAppenders(valueObject: Map<K, V>): List<NamedAppender> =
        valueObject
            .map { (key, value) ->
                keyConverter.render(key) to valueAppender(value)
            }
            .sortedBy { it.first }

    override fun convertFields(valueObject: Map<K, V>): FieldNodeMap {
        println("[DEBUG_LOG] JMap.convertFields valueObject=$valueObject")
        val result = FieldNodeMap(
            valueObject
                .map { (key, value) ->
                    val keyString = keyConverter.render(key)
                    val jsonNode = valueConverter.toJsonNode(value)
                    println("[DEBUG_LOG] JMap.convertFields key=$key, keyString=$keyString, value=$value, jsonNode=$jsonNode")
                    keyString to jsonNode
                }
                .sortedBy { it.first }
                .toMap()
        )
        println("[DEBUG_LOG] JMap.convertFields result=$result")
        return result
    }
}
