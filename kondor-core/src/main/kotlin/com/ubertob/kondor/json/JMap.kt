package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.JsonNode
import com.ubertob.kondor.json.jsonnode.JsonNodeObject
import com.ubertob.kondor.json.jsonnode.NodePathRoot
import com.ubertob.kondor.outcome.failIfNull

class JMap<K : Any, V : Any>(
    private val keyConverter: JStringRepresentable<K>,
    private val valueConverter: JConverter<V>
) : ObjectNodeConverterBase<Map<K, V>>() {
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

    override fun JsonNodeObject.deserializeOrThrow() =
        _fieldMap.entries.associate { (key, value) ->
            val path = NodePathRoot //!!!
            keyConverter.cons(key) to
                    valueConverter.fromJsonNodeBase(value, path)
                        .failIfNull { ConverterJsonError(path, "Found null node in map!") }
                        .orThrow()
        }

    override fun convertFields(valueObject: Map<K, V>): Map<String, JsonNode> =
        valueObject
            .map { (key, value) ->
                val keyString = keyConverter.render(key)
                keyString to valueConverter.toJsonNode(value)
            }
            .sortedBy { it.first }
            .toMap()

}