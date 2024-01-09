package com.ubertob.kondor.json

import com.ubertob.kondor.json.JsonStyle.Companion.appendText
import com.ubertob.kondor.json.jsonnode.JsonNode
import com.ubertob.kondor.json.jsonnode.JsonNodeObject
import com.ubertob.kondor.json.jsonnode.NodePath
import com.ubertob.kondor.json.jsonnode.NodePathSegment
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
            keyConverter.cons(key) to
                    valueConverter.fromJsonNodeBase(value)
                        .failIfNull { ConverterJsonError(_path, "Found null node in map!") }
                        .orThrow()
        }


    private fun valueAppender(propName: String, value: V?): PropertyAppender? =
        if (value == null) null
        else { js, off ->
            appendText(propName)
                .append(js.valueSeparator)
            valueConverter.appendValue(this, js, off, value)
        }

    override fun fieldAppenders(valueObject: Map<K, V>): Map<String, PropertyAppender?> =
        valueObject
            .map { (key, value) ->
                val propName = keyConverter.render(key)
                propName to valueAppender(propName, value)
            }
            .sortedBy { it.first }
            .toMap()

    override fun convertFields(valueObject: Map<K, V>, path: NodePath): Map<String, JsonNode> =
        valueObject
            .map { (key, value) ->
                val keyString = keyConverter.render(key)
                keyString to valueConverter.toJsonNode(value, NodePathSegment(keyString, path))
            }
            .sortedBy { it.first }
            .toMap()

}