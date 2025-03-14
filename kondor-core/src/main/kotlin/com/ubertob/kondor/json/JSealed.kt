package com.ubertob.kondor.json

import com.ubertob.kondor.json.JsonStyle.Companion.appendText
import com.ubertob.kondor.json.jsonnode.FieldNodeMap
import com.ubertob.kondor.json.jsonnode.JsonNodeObject
import com.ubertob.kondor.json.jsonnode.JsonNodeString
import com.ubertob.kondor.json.schema.sealedSchema

abstract class PolymorphicConverter<T : Any> : JAny<T>() {
    abstract fun extractTypeName(obj: T): String
    abstract val subConverters: Map<String, ObjectNodeConverter<out T>>

    @Suppress("UNCHECKED_CAST")
    fun findSubTypeConverter(typeName: String): ObjectNodeConverter<T>? =
        subConverters[typeName] as? ObjectNodeConverter<T>

}

abstract class JSealed<T : Any> : PolymorphicConverter<T>() {

    open val discriminatorFieldName: String = "_type"

    open val defaultConverter: ObjectNodeConverter<out T>? = null

    private fun discriminatorFieldNode(obj: T) =
        JsonNodeString(extractTypeName(obj))

    override fun JsonNodeObject.deserializeOrThrow(): T? {
        val discriminatorNode = _fieldMap.map[discriminatorFieldName]
            ?: defaultConverter?.let { return it.fromFieldNodeMap(_fieldMap, _path).orThrow() }
            ?: error("expected discriminator field \"$discriminatorFieldName\" not found")

        val typeName = JString.fromJsonNodeBase(discriminatorNode, _path).orThrow()
        val converter = subConverters[typeName] ?: error("subtype not known $typeName")
        return converter.fromFieldNodeMap(_fieldMap, _path).orThrow()
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
            mutableListOf(appendTypeName(typeName))
                .apply {
                    addAll(
                        converterFromTypename(typeName, valueObject)
                            ?: error("subtype not known $typeName")
                    )
                }
        }

    private fun converterFromTypename(typeName: String, valueObject: T) =
        findSubTypeConverter(typeName)?.fieldAppenders(valueObject)

    private fun appendTypeName(typeName: String): NamedAppender =
        discriminatorFieldName to { app: CharWriter, style: JsonStyle, _: Int ->
            app.appendText(typeName)
        }

    override fun schema() = sealedSchema(discriminatorFieldName, subConverters)

}