package com.ubertob.kondor.json

import com.ubertob.kondor.json.JsonStyle.Companion.appendText
import com.ubertob.kondor.json.jsonnode.*
import com.ubertob.kondor.json.schema.sealedSchema

abstract class PolymorphicConverter<T : Any> : JAny<T>() {
    abstract fun extractTypeName(obj: T): String
    abstract val subConverters: Map<String, ObjectNodeConverter<out T>>

    @Suppress("UNCHECKED_CAST")
    fun findSubTypeConverter(typeName: String): ObjectNodeConverter<T>? =
        subConverters[typeName] as? ObjectNodeConverter<T>

    protected open val discriminatorFieldName: String = "_type"

    protected open val defaultConverter: ObjectNodeConverter<out T>? = null

    override fun fromFieldMap(fieldMap: FieldMap, path: NodePath): JsonOutcome<T> =
        tryFromNode(path) {
            val discriminatorValue = fieldMap.getValue(discriminatorFieldName)
                ?: defaultConverter?.let { return@tryFromNode it.fromFieldMap(fieldMap, path).orThrow() }
                ?: throw JsonParsingException(
                    ConverterJsonError(
                        path,
                        "expected discriminator field \"$discriminatorFieldName\" not found"
                    )
                )

            val typeName = when (discriminatorValue) {
                is String -> discriminatorValue
                is JsonNodeString -> discriminatorValue.text
                else -> throw JsonParsingException(ConverterJsonError(path, "discriminator field must be a string"))
            }

            val converter = subConverters[typeName]
                ?: throw JsonParsingException(ConverterJsonError(path, "subtype not known $typeName"))

            converter.fromFieldMap(fieldMap, path).orThrow()
        }
}

abstract class JSealed<T : Any> : PolymorphicConverter<T>() {

    override open val discriminatorFieldName: String = "_type"

    override open val defaultConverter: ObjectNodeConverter<out T>? = null

    private fun discriminatorFieldNode(obj: T) =
        JsonNodeString(extractTypeName(obj))

    override fun JsonNodeObject.deserializeOrThrow(): T {
        val discriminatorNode = _fieldMap[discriminatorFieldName]
            ?: defaultConverter?.let { return it.fromFieldNodeMap(_fieldMap, _path).orThrow() }
            ?: throw JsonParsingException(
                ConverterJsonError(
                    _path,
                    "expected discriminator field \"$discriminatorFieldName\" not found"
                )
            )

        val typeName = JString.fromJsonNodeBase(discriminatorNode, _path).orThrow()
        val converter = subConverters[typeName]
            ?: throw JsonParsingException(ConverterJsonError(_path, "subtype not known $typeName"))
        return converter.fromFieldNodeMap(_fieldMap, _path).orThrow()
    }

    override fun convertFields(valueObject: T): Map<String, JsonNode> =
        extractTypeName(valueObject).let { typeName ->
            findSubTypeConverter(typeName)
                ?.toJsonNode(valueObject)
                ?._fieldMap
                ?.also { (it as MutableMap)[discriminatorFieldName] = discriminatorFieldNode(valueObject) }
                ?: error("subtype not known $typeName")
        }


    override fun fieldAppenders(valueObject: T): List<NamedAppender> =
        extractTypeName(valueObject).let { typeName ->
            mutableListOf(appendTypeName(discriminatorFieldName, typeName))
                .apply {
                    addAll(
                        converterFromTypename(typeName, valueObject)
                            ?: error("subtype not known $typeName")
                    )
                }
        }

    private fun converterFromTypename(typeName: String, valueObject: T) =
        findSubTypeConverter(typeName)?.fieldAppenders(valueObject)

    private fun appendTypeName(discriminatorFieldName: String, typeName: String): NamedAppender =
        discriminatorFieldName to { app: CharWriter, style: JsonStyle, _: Int ->
            app.appendText(typeName)
        }

    override fun schema() = sealedSchema(discriminatorFieldName, subConverters)

}
