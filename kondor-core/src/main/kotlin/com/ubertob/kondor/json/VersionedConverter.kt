package com.ubertob.kondor.json

import com.ubertob.kondor.json.JsonStyle.Companion.appendText
import com.ubertob.kondor.json.jsonnode.*
import com.ubertob.kondor.outcome.asFailure
import com.ubertob.kondor.outcome.asSuccess
import com.ubertob.kondor.outcome.bind
import com.ubertob.kondor.outcome.failIfNull


private const val defaultVersionProperty = "@version"


abstract class VersionedConverter<T : Any> : ObjectNodeConverter<T> {
    open val versionProperty = defaultVersionProperty
    open val defaultVersion: String? = null

    abstract fun converterForVersion(version: String): ObjectNodeConverter<T>?

    abstract val outputVersion: String

    open val outputVersionConverter: ObjectNodeConverter<T> get() =
        converterForVersion(outputVersion) ?: error("no converter for version $outputVersion")

    override fun fromFieldMap(fieldMap: FieldMap, path: NodePath): JsonOutcome<T> {
        val jsonVersion = (fieldMap.getValue(versionProperty) as? String)
            ?: defaultVersion
            ?: return missingVersionError(path).asFailure()

        return converterForVersion(jsonVersion).asSuccess()
            .failIfNull { unsupportedVersionError(path, jsonVersion) }
            .bind { it.fromFieldMap(fieldMap, path) }
    }

    override fun fromFieldNodeMap(fieldNodeMap: FieldNodeMap, path: NodePath): JsonOutcome<T> {
        val jsonVersion = fieldNodeMap[versionProperty].asStringValue()
            ?: defaultVersion
            ?: return missingVersionError(path).asFailure()

        return converterForVersion(jsonVersion).asSuccess()
            .failIfNull { unsupportedVersionError(path, jsonVersion) }
            .bind { it.fromFieldNodeMap(fieldNodeMap, path) }
    }

    override fun fieldAppenders(valueObject: T): List<NamedAppender> {
        return outputVersionConverter.fieldAppenders(valueObject) +
            (versionProperty to { s, _ ->
                appendText(outputVersion)
                this
            })
    }

    override fun toJsonNode(value: T): JsonNodeObject =
        outputVersionConverter.toJsonNode(value).let {
            it.copy(_fieldMap = it._fieldMap + (versionProperty to JsonNodeString(outputVersion)))
        }

    private fun missingVersionError(path: NodePath) =
        JsonPropertyError(
            path,
            versionProperty, "missing $versionProperty property"
        )

    private fun unsupportedVersionError(path: NodePath, version: String) =
        JsonPropertyError(path + versionProperty, versionProperty, "unsupported format version $version")
}

data class VersionMapConverter<T: Any>(
    override val versionProperty: String = defaultVersionProperty,
    override val defaultVersion: String? = null,
    val versionConverters: Map<String,ObjectNodeConverter<T>>,
    override val outputVersion: String = versionConverters.keys.max()
) : VersionedConverter<T>() {
    override fun converterForVersion(version: String): ObjectNodeConverter<T>? =
        versionConverters[version]
}
