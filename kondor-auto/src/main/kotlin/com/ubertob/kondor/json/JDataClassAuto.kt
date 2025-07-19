package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.JsonNode
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1


abstract class JDataClassAuto<T : Any>(val klazz: KClass<T>) : JDataClass<T>(klazz) {

// assuming you are ok that json fields are identical to the property names you can use this converter without having to define anything.

    fun registerAllProperties() {

        klazz.members.filterIsInstance<KProperty1<Any, *>>().forEach { property ->
            println(property.name)
            println(property.returnType)
            println("#")

            //this sees more promising...
        }
        klazz.java.declaredFields.forEach { field ->
            val fieldType: Class<*> = field.type
            val fieldTypeClass = field.type::class.java
            println(field.name)
            println(fieldType)
            println("-")

            val converter: JsonConverter<out Comparable<*>, out JsonNode> = when (fieldType) {
                Int::class.java, Integer::class.java -> JInt
                Long::class.java -> JLong
                Float::class.java -> JFloat
                Double::class.java -> JDouble
                String::class.java -> JString
                Boolean::class.java -> JBoolean
                else -> fieldType.classLoader.loadClass("J${fieldType.simpleName}") as JsonConverter<out Comparable<*>, out JsonNode> //how to get the object from a javaclass or get a Koltin class by name??
            }

            //is there a way to detect nullable fields?

            val prop = JsonPropMandatory(field.name, converter)


            when (fieldType) {
                Int::class.java -> registerProperty(JsonPropMandatory(field.name, JInt)) { o -> field.getInt(o) }
                String::class.java -> registerProperty(
                    JsonPropMandatory(
                        field.name,
                        JString
                    )
                ) { o -> field.get(o) as String }
            }

            registerPropertyHack(prop) { obj -> field.get(obj) }
        }

    }
}
