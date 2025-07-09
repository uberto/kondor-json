package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.*
import com.ubertob.kondor.outcome.asSuccess

class JMap<K : Any, V : Any>(
    private val keyConverter: JStringRepresentable<K>,
    private val valueConverter: JConverter<V>
) : JObj<Map<K, V>>() {

    // always return type:object assuming the map is representing an object. We don't know its properties.
    override fun schema(): JsonNodeObject =
        JsonNodeObject(FieldNodeMap(mapOf("type" to JsonNodeString("object"))))

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


    override fun FieldsValues.deserializeOrThrow(path: NodePath): Map<K, V> =
        getMap()
            .entries.associate { (key, value) ->
                val keyValue = keyConverter.cons(key)
                keyValue to (value as V)
            }

    override fun fromFieldNodeMap(fieldNodeMap: FieldNodeMap, path: NodePath): JsonOutcome<Map<K, V>> =
        tryFromNode(path) {
            fieldNodeMap.map.entries.associate { (key, jsonNode) ->
                val newPath = NodePathSegment(key, path)

                val value = valueConverter.fromJsonNodeBase(jsonNode, newPath)
                    .orThrow() as V
                keyConverter.cons(key) to value
            }
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
        val result = FieldNodeMap(
            valueObject
                .map { (key, value) ->
                    val keyString = keyConverter.render(key)
                    val jsonNode = valueConverter.toJsonNode(value)
                    keyString to jsonNode
                }
                .sortedBy { it.first }
                .toMap()
        )
        return result
    }

}
