package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.*
import com.ubertob.kondor.outcome.asFailure
import com.ubertob.kondor.outcome.asSuccess
import com.ubertob.kondor.outcome.onFailure
import java.lang.reflect.Constructor
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

typealias ObjectFields = Map<String, Any?>

//!!! move everything that need reflection to a new module Kondor-reflection to simplify imports

abstract class JAnyAuto<T : Any>() : ObjectNodeConverterProperties<T>() {

    protected val _jsonProperties by lazy { getProperties() }

    override fun JsonNodeObject.deserializeOrThrow(): T? =
        error("Deprecated method! Override fromFieldMap if necessary.")

    override fun fromFieldNodeMap(fieldMap: FieldNodeMap, path: NodePath): JsonOutcome<T> {
        val args = _jsonProperties.associate { prop ->
            val propValue = prop.getter(fieldMap, path)
                .onFailure { return it.asFailure() }
            prop.propName to propValue
        }

        return buildInstance(args, path)
    }

    abstract fun buildInstance(args: ObjectFields, path: NodePath): JsonOutcome<T>
}

abstract class JDataClass<T : Any>(klazz: KClass<T>) : JAnyAuto<T>() {

    val clazz: Class<T> = klazz.java

    private val constructor: Constructor<T> by lazy {
        if (!klazz.isData)
            println("Warning! The class $klazz doesn't seem to be a DataClass!")

        clazz.constructors.first() as Constructor<T>
    }

    override fun buildInstance(args: ObjectFields, path: NodePath) =
        try {
            constructor.newInstance(*(args.values).toTypedArray())
                .asSuccess()
        } catch (t: Exception) {
            ConverterJsonError(
                path,
                "Error calling constructor with signature ${constructor.description()} using params $args"
            )
                .asFailure()
        }
}

private fun <T> Constructor<T>.description(): String =
    parameterTypes
        .map { it.simpleName }
        .joinToString(prefix = "[", postfix = "]")

fun <T : Any> JDataClass<T>.testParserAndRender(times: Int = 100, generator: (index: Int) -> T) {
    repeat(times) { index ->
        val value = generator(index)

        val jn = toJsonNode(value)

        val valFromNode = fromJsonNode(jn, NodePathRoot)
            .onFailure { throw AssertionError(it) }

        assert(value == valFromNode)

        val json = toJson(value)
        val valFromJson = fromJson(json)
            .onFailure { throw AssertionError(it) }

        assert(value == valFromJson)
    }
}

abstract class JDataClassReflect<T : Any>(val klazz: KClass<T>) : JDataClass<T>(klazz) {


    fun registerAllProperties() {
//assuming the declared fields are always in the construtor order we can fix the order problem in JDataClass


        klazz.members.filterIsInstance<KProperty1<Any,*>>() .forEach { property ->
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
                Int::class.java, Integer::class.java ->  JInt
                Long::class.java -> JLong
                Float::class.java -> JFloat
                Double::class.java -> JDouble
                String::class.java -> JString
                Boolean::class.java -> JBoolean
                else -> fieldType.classLoader.loadClass("J${fieldType.simpleName}") as JsonConverter<out Comparable<*>, out JsonNode> //how to get the object from a javaclass or get a Koltin class by name??!!!
            }

            //is there a way to detect nullable fields?

            val prop= JsonPropMandatory(field.name, converter)


            when (fieldType) {
                Int::class.java -> registerProperty( JsonPropMandatory(field.name, JInt)){ o -> field.getInt(o)}
                String::class.java -> registerProperty( JsonPropMandatory(field.name, JString)){ o -> field.get(o) as String}
            }

            registerPropertyHack(prop) {obj -> field. get(obj) }
        }

    }
}