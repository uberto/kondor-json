package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.JsonNode
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
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

    init {
        // Auto-register all properties on creation so users don't need to call register manually
        registerAllProperties()
    }

    fun registerAllProperties() {
        // Register properties in the exact order of constructor parameters
        val constructor = klazz.primaryConstructor ?: klazz.constructors.firstOrNull()
        ?: throw IllegalStateException("No accessible constructor found for $klazz")

        val propertiesByName: Map<String, KProperty1<T, *>> = klazz.memberProperties
            .associateBy { it.name }

        constructor.parameters.forEach { param ->
            val name = param.name
                ?: throw IllegalStateException("Constructor parameter without a name in $klazz")

            val property = propertiesByName[name]
                ?: throw IllegalStateException("Property '$name' not found in $klazz for constructor parameter")

            @Suppress("UNCHECKED_CAST")
            if (param.type.isMarkedNullable) {
                val prop = property as KProperty1<T, Any?>
                val converter = getConverterForKType(param.type)
                registerProperty(JsonPropOptional(name, converter)) { obj -> prop.get(obj) }
            } else {
                val prop = property as KProperty1<T, Any>
                val converter = getConverterForKType(param.type) as JsonConverter<Any, out JsonNode>
                registerProperty(JsonPropMandatory(name, converter)) { obj -> prop.get(obj) }
            }
        }
    }

    // Basic mapping from Kotlin types to built-in converters
    @Suppress("UNCHECKED_CAST")
    private fun getConverterForKType(kType: KType): JsonConverter<Any?, out JsonNode> {
        val rawClass = (kType.classifier as? KClass<*>)?.java
            ?: (kType.javaType as? Class<*>)
            ?: throw IllegalArgumentException("Unsupported Kotlin type: $kType")

        return when (rawClass) {
            Int::class.java, Integer::class.java -> JInt as JsonConverter<Any?, out JsonNode>
            Long::class.java, java.lang.Long::class.java -> JLong as JsonConverter<Any?, out JsonNode>
            Float::class.java, java.lang.Float::class.java -> JFloat as JsonConverter<Any?, out JsonNode>
            Double::class.java, java.lang.Double::class.java -> JDouble as JsonConverter<Any?, out JsonNode>
            String::class.java, java.lang.String::class.java -> JString as JsonConverter<Any?, out JsonNode>
            Boolean::class.java, java.lang.Boolean::class.java -> JBoolean as JsonConverter<Any?, out JsonNode>
            else -> throw IllegalArgumentException("Unsupported field type: $rawClass. Please use explicit JDataClass mapping for complex or custom types.")
        } // TODO: add BigDecimal, BigInteger, enums, date/time, nested data classes, collections, etc.
    }
}
