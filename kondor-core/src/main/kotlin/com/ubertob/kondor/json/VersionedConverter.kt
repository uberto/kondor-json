package com.ubertob.kondor.json

import com.ubertob.kondor.json.JsonStyle.Companion.appendText
import com.ubertob.kondor.json.jsonnode.*
import com.ubertob.kondor.outcome.*


private const val defaultVersionProperty = "@version"


abstract class VersionedConverter<T : Any> : ObjectNodeConverter<T> {
    open val versionProperty = defaultVersionProperty
    open val defaultVersion: String? = null
    open val unversionedConverters: List<ObjectNodeConverter<T>> = emptyList()

    abstract fun converterForVersion(version: String): ObjectNodeConverter<T>?

    abstract val outputVersion: String?
    private val nullCheckedOutputVersion get() = outputVersion ?: error("output version is null")

    open val outputVersionConverter: ObjectNodeConverter<T>
        get() =
            outputVersion?.let { converterForVersion(nullCheckedOutputVersion) } ?: unversionedConverters.firstOrNull()
            ?: error("no converter for version $outputVersion")

    override fun fromFieldNodeMap(fieldMap: FieldNodeMap, path: NodePath): JsonOutcome<T> {
        val jsonVersion = fieldMap[versionProperty].asStringValue() ?: defaultVersion

        val converters = if (jsonVersion != null) {

            val defaultConverter = converterForVersion(jsonVersion)

            if (defaultConverter != null) {
                listOf(defaultConverter) + unversionedConverters
            } else {
                unversionedConverters
            }
        } else {
            unversionedConverters
        }

        return ChainedConverter(converters).fromFieldNodeMap(fieldMap, path)
    }

    override fun fieldAppenders(valueObject: T): List<NamedAppender> {
        return if (outputVersion == null) {
            outputVersionConverter.fieldAppenders(valueObject)
        } else {
            outputVersionConverter.fieldAppenders(valueObject) +
                    (versionProperty to { s, _ ->
                        appendText(nullCheckedOutputVersion)
                        this
                    })
        }
    }

    override fun toJsonNode(value: T): JsonNodeObject =
        outputVersionConverter.toJsonNode(value).let {
            if (outputVersion == null) {
                it
            } else {
                it.copy(_fieldMap = it._fieldMap + (versionProperty to JsonNodeString(nullCheckedOutputVersion)))
            }
        }

    private fun missingVersionError(path: NodePath) =
        JsonPropertyError(
            path,
            versionProperty, "missing $versionProperty property"
        )

    private fun unsupportedVersionError(path: NodePath, version: String) =
        JsonPropertyError(path + versionProperty, versionProperty, "unsupported format version $version")
}

data class VersionMapConverter<T : Any>(
    override val versionProperty: String = defaultVersionProperty,
    override val defaultVersion: String? = null,
    val versionConverters: Map<String, ObjectNodeConverter<T>> = emptyMap(),
    override val unversionedConverters: List<ObjectNodeConverter<T>> = emptyList(),
    override val outputVersion: String? = versionConverters.keys.maxOrNull(),
) : VersionedConverter<T>() {
    override fun converterForVersion(version: String): ObjectNodeConverter<T>? =
        versionConverters[version]
}

private class ChainedConverter<T : Any>(
    val konvertors: List<ObjectNodeConverter<T>>,
) : JAny<T>() {
    override fun toJsonNode(value: T): JsonNodeObject = konvertors.first().toJsonNode(value)

    override fun JsonNodeObject.deserializeOrThrow(): T {
        val initialFailure = ConverterJsonError(_path, "no valid versioned convertor").asFailure()

        return konvertors.fold<ObjectNodeConverter<T>, JsonOutcome<T>>(initialFailure) { outcome, converter ->
            outcome.bindFailure { converter.fromJsonNode(this, _path) }
        }.orThrow()
    }
}
