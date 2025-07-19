package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.JsonNode
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaType


/**
 * Abstract base class for JSON converters that automatically discover and register properties
 * of Kotlin data classes without requiring explicit field declarations.
 *
 * JDataClassAuto extends JDataClass and uses reflection to automatically map between JSON fields
 * and data class properties. This eliminates the need to manually declare property mappings.
 * The JSON field names will be identical to the property names in the data class.
 *
 * Usage:
 * ```
 * data class Person(val id: Int, val name: String)
 *
 * object PersonJson : JDataClassAuto<Person>(Person::class) {
 *     init {
 *         registerAllProperties()
 *     }
 * }
 * ```
 *
 * Note: This class uses reflection to discover properties, which may have performance implications.
 * For production use with performance-critical code, consider using JDataClass with explicit mappings.
 *
 * @param T The data class type this converter handles
 * @param klazz The Kotlin class reference for the data class
 */
abstract class JDataClassAuto<T : Any>(val klazz: KClass<T>) : JDataClass<T>(klazz) {

    fun registerAllProperties() {
        // Use Kotlin reflection to get properties in the correct order
        klazz.memberProperties.forEach { property ->
            @Suppress("UNCHECKED_CAST")


            // Check if the property is nullable
            if (property.returnType.isMarkedNullable) {
                val prop = property as KProperty1<T, Any?>
                val converter = getConverterForType(property.returnType.javaType as Class<*>)

                registerProperty(JsonPropOptional(property.name, converter)) { obj -> prop.get(obj) }
            } else {
                val prop = property as KProperty1<T, Any>
                val converter =
                    getConverterForType(property.returnType.javaType as Class<*>) as JsonConverter<Any, out JsonNode>

                registerProperty(JsonPropMandatory(property.name, converter)) { obj -> prop.get(obj) }
            }
        }
    }

    //converter for types should be different for nullable and not nullable !!!
    @Suppress("UNCHECKED_CAST")
    private fun getConverterForType(fieldType: Class<*>): JsonConverter<Any?, out JsonNode> {
        return when (fieldType) {
            Int::class.java, Integer::class.java -> JInt as JsonConverter<Any?, out JsonNode>
            Long::class.java -> JLong as JsonConverter<Any?, out JsonNode>
            Float::class.java -> JFloat as JsonConverter<Any?, out JsonNode>
            Double::class.java -> JDouble as JsonConverter<Any?, out JsonNode>
            String::class.java -> JString as JsonConverter<Any?, out JsonNode>
            Boolean::class.java -> JBoolean as JsonConverter<Any?, out JsonNode>
            else -> throw IllegalArgumentException("Unsupported field type: $fieldType. Please use explicit JDataClass mapping for complex types.")
        } //!!! add bigdecimal, big integer, enums, date, time, and other common types, plus use another JDataClassAuto for the data class types
    }
}
