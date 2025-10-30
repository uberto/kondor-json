package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.*
import com.ubertob.kondor.json.parser.TokensStream
import com.ubertob.kondor.json.parser.sameValueAs
import com.ubertob.kondor.outcome.Outcome
import com.ubertob.kondor.outcome.asFailure
import com.ubertob.kondor.outcome.asSuccess
import com.ubertob.kondor.outcome.failIfNull

typealias MutableFieldMap = MutableMap<String, JsonNode>
typealias PropertySetter = (MutableFieldMap) -> MutableFieldMap
typealias ValueAppender = CharWriter.(JsonStyle, Int) -> CharWriter
typealias NamedAppender = Pair<String, ValueAppender?>

sealed class JsonProperty<T> {
    abstract val propName: String
    abstract fun setter(value: T): PropertySetter
    abstract fun appender(value: T): List<NamedAppender>
    abstract fun getter(fieldMap: FieldNodeMap, path: NodePath): JsonOutcome<T>

    /**
     * Property-level token read hook.
     *
     * Important: This method does NOT parse the JSON value itself. Parsing remains a responsibility of the
     * underlying JsonConverter for the value type. The property only applies field policy (e.g. nullable vs mandatory)
     * and then delegates to [converter.fromTokens]. In other words: properties decide "can/must this be null?",
     * converters decide "how to parse a non-null value from tokens".
     *
     * Having this hook here avoids duplicating null/missing handling in object converters and keeps responsibilities
     * separated: Object-level code dispatches to the right property, the property enforces field rules, and the
     * converter performs actual value parsing.
     */
    abstract fun readFromTokens(
        tokens: TokensStream,
        path: NodePath,
    ): JsonOutcome<Any?>
}

data class JsonParsingException(
    val error: JsonError,
) : RuntimeException()

data class JsonPropMandatory<T : Any, JN : JsonNode>(
    override val propName: String,
    val converter: JsonConverter<T, JN>
) : JsonProperty<T>() {

    override fun getter(fieldMap: FieldNodeMap, path: NodePath): Outcome<JsonError, T> =
        fieldMap.map[propName]
            ?.let { converter.fromJsonNodeBase(it, NodePathSegment(propName, path)) }
            ?.failIfNull { JsonPropertyError(NodePathSegment(propName, path), propName, "Found null for non-nullable") }
            ?: JsonPropertyError(
                path, propName, "Not found key '$propName'. Keys found: [${fieldMap.map.keys.joinToString()}]"
            ).asFailure()

    override fun setter(value: T): PropertySetter = { fm ->
        fm.apply {
            put(propName, converter.toJsonNode(value))
        }
    }

    override fun appender(value: T): List<NamedAppender> = listOf(propName to { style, off ->
        appendValue(style, off, value)
    })

    fun CharWriter.appendValue(style: JsonStyle, offset: Int, value: T): CharWriter =
        converter.appendValue(this, style, offset, value)

    override fun readFromTokens(
        tokens: TokensStream,
        path: NodePath,
    ): Outcome<JsonError, Any?> =
        if (tokens.peek().sameValueAs("null")) {
            JsonPropertyError(path, propName, "Found null for non-nullable").asFailure()
        } else {
            converter.fromTokens(tokens, path).transform { it as Any? }
        }
}


data class JsonPropOptional<T, JN : JsonNode>(
    override val propName: String,
    val converter: JsonConverter<T, JN>
) : JsonProperty<T?>() {

    override fun getter(fieldMap: FieldNodeMap, path: NodePath): Outcome<JsonError, T?> =
        fieldMap.map[propName]
            ?.let { converter.fromJsonNodeBase(it, NodePathSegment(propName, path)) }
            ?: null.asSuccess()

    override fun readFromTokens(
        tokens: TokensStream,
        path: NodePath,
    ): Outcome<JsonError, Any?> =
        if (tokens.peek().sameValueAs("null")) {
            tokens.next() // consume the null token
            null.asSuccess()
        } else {
            converter.fromTokens(tokens, path).transform { it as Any? }
        }

    override fun setter(value: T?): PropertySetter = { fm ->
        fm.apply {
            put(
                propName,
                value?.let { converter.toJsonNode(it) }
                    ?: JsonNodeNull
            )
        }
    }

    override fun appender(value: T?): List<NamedAppender> =
        if (value == null)
            listOf(propName to null)
        else listOf(propName to { style, off ->
            appendValue(style, off, value)
        })

    fun CharWriter.appendValue(style: JsonStyle, offset: Int, value: T): CharWriter =
        converter.appendValue(this, style, offset, value)
}

data class JsonPropMandatoryFlatten<T : Any>(
    override val propName: String,
    val converter: ObjectNodeConverter<T>,
    val parent: ObjectNodeConverterProperties<*>,
) : JsonProperty<T>() {
    private val parentProperties = parent.getProperties().map { it.propName }

    override fun appender(value: T): List<NamedAppender> = converter.fieldAppenders(value)

    override fun getter(fieldNodeMap: FieldNodeMap, path: NodePath): Outcome<JsonError, T> =
        converter.fromFieldNodeMap(fieldNodeMap.removeFieldsFromParent(), path)
            .failIfNull { JsonPropertyError(path, propName, "Found null for non-nullable") }

    private fun FieldNodeMap.removeFieldsFromParent(): FieldNodeMap =
        FieldNodeMap(map.filterKeys { key -> !parentProperties.contains(key) })

    override fun setter(value: T): PropertySetter = { fm ->
        fm.apply {
            fm.putAll(converter.toJsonNode(value)._fieldMap.map)
        }
    }

    override fun readFromTokens(
        tokens: TokensStream,
        path: NodePath,
    ): Outcome<JsonError, Any?> =
        JsonPropertyError(path, propName, "Flattened properties cannot be read from tokens directly").asFailure()
}
