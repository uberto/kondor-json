package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.*
import com.ubertob.kondor.outcome.failIfNull

class JMap<K : Any, V : Any>(
    private val keyConverter: JStringRepresentable<K>,
    private val valueConverter: JConverter<V>
) : JAny<Map<K, V>>() {
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

    private fun convertEntries(entries: Set<Map.Entry<String, Any?>>, path: NodePath): Map<K, V> =
        entries.associate { (key, value) ->
            val newPath = NodePathSegment(key, path)
            val jsonNode = when (value) {
                null -> JsonNodeNull
                is String -> JsonNodeString(value)
                is Number -> JsonNodeNumber(value)
                is Boolean -> JsonNodeBoolean(value)
                is JsonNode -> value
                else -> throw JsonParsingException(ConverterJsonError(path, "Unsupported type: ${value::class}"))
            }
            keyConverter.cons(key) to
                    valueConverter.fromJsonNodeBase(jsonNode, newPath)
                        .failIfNull { ConverterJsonError(newPath, "Found null node in map!") }
                        .orThrow()
        }

    override fun JsonNodeObject.deserializeOrThrow(): Map<K, V> =
        convertEntries(_fieldMap.entries, _path)

    override fun fromFieldMap(fieldMap: Map<String, Any?>, path: NodePath): JsonOutcome<Map<K, V>> =
        tryFromNode(path) {
            convertEntries(fieldMap.entries, path)
        }


    private fun valueAppender(value: V?): ValueAppender? =
        if (value == null) null
        else { style, off ->
            valueConverter.appendValue(this, style, off, value)
        }

    override fun fieldAppenders(valueObject: Map<K, V>): List<NamedAppender> =
        valueObject
            .map { (key, value) ->
                keyConverter.render(key) to valueAppender(value)
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
