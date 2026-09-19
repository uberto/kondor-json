package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.*
import com.ubertob.kondor.json.parser.KondorSeparator
import com.ubertob.kondor.json.parser.TokensStream
import com.ubertob.kondor.json.parser.parseFields
import com.ubertob.kondor.json.parser.surrounded
import com.ubertob.kondor.outcome.Outcome
import com.ubertob.kondor.outcome.asFailure
import com.ubertob.kondor.outcome.asSuccess
import com.ubertob.kondor.outcome.bind
import com.ubertob.kondor.outcome.traverse

abstract class JObj<T : Any> : ObjectNodeConverterProperties<T>() {
    //this is the new JAny with faster parsing !!!better desc

    override fun fromTokens(
        tokens: TokensStream,
        path: NodePath,
    ): JsonOutcome<T> =
        surrounded(
            KondorSeparator.OpeningCurly,
            { t, p ->
                parseFields(t, p) { fieldName, tks, nodePath ->
                    val prop = getPropertyByName(fieldName)
                    if (prop == null) {
                        return@parseFields JsonPropertyError(
                            nodePath.parent(),
                            fieldName,
                            "Not found a property for the Json field '$fieldName'. Defined properties: ${
                                getProperties().map {
                                    it.propName
                                }
                            }",
                        ).asFailure()
                    }
                    @Suppress("UNCHECKED_CAST")
                    (prop as JsonProperty<Any?>).readFromTokens(tks, nodePath)
                }
            },
            KondorSeparator.ClosingCurly,
        )(tokens, path)
            .bind { fieldMap -> checkMandatoryFields(fieldMap, path) }
            .bind { fieldMap ->
                fromFieldValues(fieldMap, path)
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
