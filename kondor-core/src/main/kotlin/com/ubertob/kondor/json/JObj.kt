package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.*
import com.ubertob.kondor.json.parser.KondorSeparator
import com.ubertob.kondor.json.parser.TokensPath
import com.ubertob.kondor.json.parser.TokensStream
import com.ubertob.kondor.json.parser.parseFields
import com.ubertob.kondor.json.parser.parseNewNode
import com.ubertob.kondor.json.parser.parsingFailure
import com.ubertob.kondor.json.parser.surrounded
import com.ubertob.kondor.outcome.Outcome
import com.ubertob.kondor.outcome.asFailure
import com.ubertob.kondor.outcome.asSuccess
import com.ubertob.kondor.outcome.bind
import com.ubertob.kondor.outcome.traverse

private class UnknownField(val node: JsonNode)

/**
 * Object converter parsing directly from the Json tokens, without building a [JsonNodeObject] first.
 * Like [JAny], it ignores the Json fields not declared in the converter, and passes them to the `flatten` fields.
 */
abstract class JObj<T : Any> : ObjectNodeConverterProperties<T>() {

    override fun fromTokens(
        tokens: TokensStream,
        path: NodePath,
    ): JsonOutcome<T> =
        surrounded(
            KondorSeparator.OpeningCurly,
            { t, p -> parseFields(t, p, ::readField) },
            KondorSeparator.ClosingCurly,
        )(tokens, path)
            .bind { fieldMap -> checkMandatoryFields(fieldMap, path) }
            .bind { fieldMap -> resolveUnknownFields(fieldMap, path) }
            .bind { fieldMap ->
                fromFieldValues(fieldMap, path)
            }

    // like JAny, fields without a property are ignored, but their value is still parsed, to validate the Json and
    // to pass it to the flatten properties
    private fun readField(fieldName: String, tokens: TokensStream, path: NodePath): JsonOutcome<Any?> =
        getPropertyByName(fieldName)
            ?.let {
                @Suppress("UNCHECKED_CAST")
                (it as JsonProperty<Any?>).readFromTokens(tokens, path)
            }
            ?: parseUnknownField(tokens, path)

    private fun parseUnknownField(tokens: TokensStream, path: NodePath): JsonOutcome<UnknownField> =
        (TokensPath(tokens, path).parseNewNode()
            ?: parsingFailure("a valid node", "nothing", tokens.lastPosRead(), path, "invalid Json"))
            .transform(::UnknownField)

    private val flattenProperties: List<JsonPropMandatoryFlatten<*>> by lazy {
        getProperties().filterIsInstance<JsonPropMandatoryFlatten<*>>()
    }

    // the flatten properties read the fields not declared by this converter, as in JsonPropMandatoryFlatten.getter
    private fun resolveUnknownFields(fieldValues: FieldsValuesMap, path: NodePath): JsonOutcome<FieldsValuesMap> =
        when {
            flattenProperties.isNotEmpty() -> {
                val (unknown, known) = fieldValues.getMap().entries.partition { it.value is UnknownField }
                val unknownNodes = FieldNodeMap(unknown.associate { it.key to (it.value as UnknownField).node })
                flattenProperties
                    .traverse { prop -> prop.getter(unknownNodes, path).transform { prop.propName to it } }
                    .transform { flattened -> FieldsValuesMap(known.associate { it.key to it.value } + flattened) }
            }

            fieldValues.getMap().values.any { it is UnknownField } ->
                FieldsValuesMap(fieldValues.getMap().filterValues { it !is UnknownField }).asSuccess()

            else -> fieldValues.asSuccess()
        }

    /**
     * When true (the default), parsing fails if a mandatory field is missing from the Json, before calling
     * [deserializeOrThrow]. Converters that handle missing fields themselves (e.g. using constructor default values)
     * can override it to false.
     */
    protected open val failOnMissingMandatoryFields: Boolean = true

    // lazy because properties are registered after the constructor of the base class has run
    private val mandatoryPropNames: List<String> by lazy {
        getProperties().filterIsInstance<JsonPropMandatory<*, *>>().map { it.propName }
    }

    // same error as JsonPropMandatory.getter on the JsonNode path, where keys are sorted by the JsonNode parser
    private fun checkMandatoryFields(fieldValues: FieldsValuesMap, path: NodePath): JsonOutcome<FieldsValuesMap> =
        mandatoryPropNames
            .takeIf { failOnMissingMandatoryFields }
            ?.firstOrNull { !fieldValues.getMap().containsKey(it) }
            ?.let { missing ->
                JsonPropertyError(
                    path,
                    missing,
                    "Not found key '$missing'. Keys found: [${fieldValues.getMap().keys.sorted().joinToString()}]"
                ).asFailure()
            }
            ?: fieldValues.asSuccess()

    fun fromFieldValues(fieldValues: FieldsValues, path: NodePath): JsonOutcome<T> =
        tryFromNode(path) {
            fieldValues.deserializeOrThrow(path)
        }

    override fun fromFieldNodeMap(fieldNodeMap: FieldNodeMap, path: NodePath): Outcome<JsonError, T> =
        getProperties().traverse { property ->
            property.getter(fieldNodeMap, path)
                .transform { property.propName to it }
        }.bind {
            fromFieldValues(FieldsValuesMap(it.toMap()), path)
        }

    abstract fun FieldsValues.deserializeOrThrow(path: NodePath): T //this is the method that concrete converter will have to implement
}
