package com.ubertob.kondor.json

import com.ubertob.kondor.json.JsonStyle.Companion.appendText
import com.ubertob.kondor.json.jsonnode.JsonNodeObject
import com.ubertob.kondor.json.jsonnode.JsonNodeString
import com.ubertob.kondor.json.jsonnode.NodePath
import com.ubertob.kondor.json.jsonnode.asStringValue
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
    
    override fun fromJsonNode(node: JsonNodeObject): JsonOutcome<T> {
        val jsonVersion = node._fieldMap[versionProperty].asStringValue()
            ?: defaultVersion
            ?: return missingVersionError(node).asFailure()
        
        return converterForVersion(jsonVersion).asSuccess()
            .failIfNull { unsupportedVersionError(node, jsonVersion) }
            .bind { it.fromJsonNode(node) }
    }
    
    override fun fieldAppenders(valueObject: T): List<NamedAppender> {
        return outputVersionConverter.fieldAppenders(valueObject) +
            (versionProperty to { s, _ ->
                s.appendValueSeparator(this)
                appendText(outputVersion)
                this
            })
    }
    
    override fun toJsonNode(value: T, path: NodePath): JsonNodeObject =
        outputVersionConverter.toJsonNode(value, path).let {
            it.copy(_fieldMap = it._fieldMap + (versionProperty to JsonNodeString(outputVersion, path + versionProperty)))
        }
    
    private fun missingVersionError(node: JsonNodeObject) =
        JsonPropertyError(
            node._path,
            versionProperty, "missing $versionProperty property"
        )
    
    private fun unsupportedVersionError(node: JsonNodeObject, version: String) =
        JsonPropertyError(node._path + versionProperty, versionProperty, "unsupported format version $version")
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
