package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.*
import com.ubertob.kondor.json.schema.objectSchema
import com.ubertob.kondor.outcome.asSuccess
import com.ubertob.kondor.outcome.failIfNull

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

    override fun fromFieldValues(fieldValues: FieldsValues, path: NodePath): JsonOutcome<Map<K, V>> =
        tryFromNode(path) {
            fieldValues.deserializeOrThrow(path)
        }

    @Suppress("UNCHECKED_CAST")
    override fun FieldsValues.deserializeOrThrow(path: NodePath): Map<K, V> =
        when (this) {
            is FieldMap -> map.entries.associate { (key, value) ->
                val mapKey = keyConverter.cons(key)
                mapKey to value as V
            }

            is FieldNodeMap -> map.entries.associate { (key, node) ->
                val keyPath = NodePathSegment(key, path)
                keyConverter.cons(key) to valueConverter.fromJsonNodeBase(node, keyPath)
                    .failIfNull { ConverterJsonError(keyPath, "Null value found for key: $key") }.orThrow()
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

    override fun convertFields(valueObject: Map<K, V>): FieldNodeMap =
        FieldNodeMap(
            valueObject
            .map { (key, value) ->
                val keyString = keyConverter.render(key)
                keyString to valueConverter.toJsonNode(value)
            }
            .sortedBy { it.first }
            .toMap()
        )
}
