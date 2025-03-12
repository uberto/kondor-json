package com.ubertob.kondor.json

import com.ubertob.kondor.json.JsonStyle.Companion.appendText
import com.ubertob.kondor.json.jsonnode.FieldNodeMap
import com.ubertob.kondor.json.jsonnode.FieldsValues
import com.ubertob.kondor.json.jsonnode.JsonNodeString
import com.ubertob.kondor.json.jsonnode.NodePath
import com.ubertob.kondor.json.schema.sealedSchema

abstract class PolymorphicConverter<T : Any> : JObj<T>() {  //!!!!!!!!!! thiis is broken now
    abstract fun extractTypeName(obj: T): String
    abstract val subConverters: Map<String, ObjectNodeConverter<out T>>

    @Suppress("UNCHECKED_CAST")
    fun findSubTypeConverter(typeName: String): ObjectNodeConverter<T>? =
        subConverters[typeName] as? ObjectNodeConverter<T>

    protected open val discriminatorFieldName: String = "_type"

    protected open val defaultConverter: ObjectNodeConverter<out T>? = null

    override fun fromFieldValues(fieldValues: FieldsValues, path: NodePath): JsonOutcome<T> =
        tryFromNode(path) {
            val discriminatorValue = fieldValues.getValue(discriminatorFieldName)
                ?: defaultConverter?.let { return@tryFromNode it.fromFieldValues(fieldValues, path).orThrow() }
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

            converter.fromFieldValues(fieldValues, path).orThrow()
        }
}

abstract class JSealed<T : Any> : PolymorphicConverter<T>() {

    override val discriminatorFieldName: String = "_type"

    override val defaultConverter: ObjectNodeConverter<out T>? = null

    private fun discriminatorFieldNode(obj: T) =
        JsonNodeString(extractTypeName(obj))

    override fun FieldsValues.deserializeOrThrow(path: NodePath): T {
        val typeName = getValue(discriminatorFieldName)
            ?: defaultConverter?.let { return it.fromFieldValues(this, path).orThrow() }
            ?: throw JsonParsingException(
                ConverterJsonError(path, "expected discriminator field \"$discriminatorFieldName\" not found")
            )

        val converter = subConverters[typeName]
            ?: throw JsonParsingException(ConverterJsonError(path, "subtype not known $typeName"))
        return converter.fromFieldValues(this, path).orThrow()
    }

    override fun convertFields(valueObject: T): FieldNodeMap =
        extractTypeName(valueObject).let { typeName ->
            findSubTypeConverter(typeName)
                ?.toJsonNode(valueObject)
                ?._fieldMap
                ?.also { (it.map as MutableMap)[discriminatorFieldName] = discriminatorFieldNode(valueObject) }
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
