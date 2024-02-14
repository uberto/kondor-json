package com.ubertob.kondor.json

import com.ubertob.kondor.json.JsonStyle.Companion.appendText
import com.ubertob.kondor.json.jsonnode.JsonNode
import com.ubertob.kondor.json.jsonnode.JsonNodeObject
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
            val newPath = NodePathSegment(key, _path)
            keyConverter.cons(key) to
                    valueConverter.fromJsonNodeBase(value, newPath)
                        .failIfNull { ConverterJsonError(newPath, "Found null node in map!") }
                        .orThrow()
        }


    private fun valueAppender(propName: String, value: V?): PropertyAppender? =
        if (value == null) null
        else { style, off ->
            appendText(propName)
            style.appendValueSeparator(this)
            valueConverter.appendValue(this, style, off, value)
        }

    override fun fieldAppenders(valueObject: Map<K, V>): List<NamedAppender> =
        valueObject
            .map { (key, value) ->
                val propName = keyConverter.render(key)
                propName to valueAppender(propName, value)
            }
            .sortedBy { it.first }

    override fun convertFields(valueObject: Map<K, V>): Map<String, JsonNode> =
        valueObject
            .map { (key, value) ->
                val keyString = keyConverter.render(key)
                keyString to valueConverter.toJsonNode(value)
            }
            .sortedBy { it.first }
            .toMap()

}