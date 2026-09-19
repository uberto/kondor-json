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
        registerConstructorProperties()
    }

    @Deprecated("Properties are registered automatically, calling this is no longer needed", ReplaceWith(""))
    fun registerAllProperties() {
        // no-op: kept for compatibility with code written for 4.0.0 and 4.0.1
    }

    private fun registerConstructorProperties() {
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
        val kClassifier = kType.classifier as? KClass<*>
        val rawClass = kClassifier?.java
            ?: (kType.javaType as? Class<*>)
            ?: throw IllegalArgumentException("Unsupported Kotlin type: $kType")

        when (rawClass) {
            Int::class.java, Integer::class.java -> return JInt as JsonConverter<Any?, out JsonNode>
            Long::class.java, java.lang.Long::class.java -> return JLong as JsonConverter<Any?, out JsonNode>
            Float::class.java, java.lang.Float::class.java -> return JFloat as JsonConverter<Any?, out JsonNode>
            Double::class.java, java.lang.Double::class.java -> return JDouble as JsonConverter<Any?, out JsonNode>
            String::class.java, java.lang.String::class.java -> return JString as JsonConverter<Any?, out JsonNode>
            Boolean::class.java, java.lang.Boolean::class.java -> return JBoolean as JsonConverter<Any?, out JsonNode>
            java.math.BigDecimal::class.java -> return JBigDecimal as JsonConverter<Any?, out JsonNode>
            java.math.BigInteger::class.java -> return JBigInteger as JsonConverter<Any?, out JsonNode>
            java.time.LocalDate::class.java -> return com.ubertob.kondor.json.datetime.JLocalDate as JsonConverter<Any?, out JsonNode>
            java.time.LocalDateTime::class.java -> return com.ubertob.kondor.json.datetime.JLocalDateTime as JsonConverter<Any?, out JsonNode>
            java.time.LocalTime::class.java -> return com.ubertob.kondor.json.datetime.JLocalTime as JsonConverter<Any?, out JsonNode>
            java.time.Instant::class.java -> return com.ubertob.kondor.json.datetime.JInstant as JsonConverter<Any?, out JsonNode>
            java.time.Duration::class.java -> return com.ubertob.kondor.json.datetime.JDuration as JsonConverter<Any?, out JsonNode>
            java.time.ZoneId::class.java -> return com.ubertob.kondor.json.datetime.JZoneId as JsonConverter<Any?, out JsonNode>
        }

        // Enums
        if (rawClass.isEnum) {
            val enumK = kClassifier as KClass<out Enum<*>>
            return JEnumClass(enumK) as JsonConverter<Any?, out JsonNode>
        }

        // Collections: handle via KType classifier to preserve Kotlin types
        if (kClassifier == List::class || kClassifier == MutableList::class) {
            val arg = kType.arguments.firstOrNull()?.type
                ?: throw IllegalArgumentException("List has no element type: $kType")
            val elemConv = getConverterForKType(arg)
            val isElemNullable = arg.isMarkedNullable
            return if (isElemNullable)
                JNullableList(elemConv as JsonConverter<Any, out JsonNode>) as JsonConverter<Any?, out JsonNode>
            else
                JList(elemConv as JsonConverter<Any, out JsonNode>) as JsonConverter<Any?, out JsonNode>
        }
        if (kClassifier == Set::class || kClassifier == MutableSet::class) {
            val arg = kType.arguments.firstOrNull()?.type
                ?: throw IllegalArgumentException("Set has no element type: $kType")
            if (arg.isMarkedNullable) {
                throw IllegalArgumentException("Set with nullable elements is not supported: $kType")
            }
            val elemConv = getConverterForKType(arg)
            return JSet(elemConv as JsonConverter<Any, out JsonNode>) as JsonConverter<Any?, out JsonNode>
        }
        if (kClassifier == Map::class || kClassifier == MutableMap::class) {
            val keyType = kType.arguments.getOrNull(0)?.type
                ?: throw IllegalArgumentException("Map has no key type: $kType")
            val valueType = kType.arguments.getOrNull(1)?.type
                ?: throw IllegalArgumentException("Map has no value type: $kType")

            // Key: support String and Enums out of the box
            val keyRaw = (keyType.classifier as? KClass<*>)?.java
            val valueConv = getConverterForKType(valueType)

            return when {
                keyRaw == String::class.java || keyRaw == java.lang.String::class.java ->
                    JMap(valueConv as JsonConverter<Any, out JsonNode>) as JsonConverter<Any?, out JsonNode>

                keyRaw != null && keyRaw.isEnum -> {
                    val enumK = keyType.classifier as KClass<out Enum<*>>
                    JMap(JEnumClass(enumK), valueConv as JsonConverter<Any, out JsonNode>) as JsonConverter<Any?, out JsonNode>
                }

                else -> throw IllegalArgumentException("Unsupported Map key type: $keyType. Only String or Enum keys are supported by JDataClassAuto.")
            }
        }

        // Nested data classes: create an automatic converter
        if (kClassifier != null && kClassifier.isData) {
            @Suppress("UNCHECKED_CAST")
            val nestedK = kClassifier as KClass<Any>
            val auto = object : JDataClassAuto<Any>(nestedK) {}
            return auto as JsonConverter<Any?, out JsonNode>
        }

        throw IllegalArgumentException("Unsupported field type: $rawClass. Please use explicit JDataClass mapping for complex or custom types.")
    }
}
