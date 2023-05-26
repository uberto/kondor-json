package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.JsonNodeObject
import com.ubertob.kondor.json.jsonnode.NodePathSegment
import com.ubertob.kondor.outcome.failIfNull

class JMap<K : Any, V : Any>(
    private val keyConverter: JStringRepresentable<K>,
    private val valueConverter: JConverter<V>
) : ObjectNodeConverterWriters<Map<K, V>>() {

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

    override fun getWriters(value: Map<K, V>): List<NodeWriter<Map<K, V>>> =
        value
            .map { (key, value) -> keyConverter.render(key) to value }
            .sortedBy { it.first }
            .map { (key, value) ->
                { jno: JsonNodeObject, _: Map<K, V> ->
                    jno.copy(
                        _fieldMap = jno._fieldMap +
                                (key to valueConverter.toJsonNode(value, NodePathSegment(key, jno._path)))
                    )
                }
            }

}