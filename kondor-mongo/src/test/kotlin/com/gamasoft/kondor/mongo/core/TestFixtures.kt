package com.gamasoft.kondor.mongo.core

import com.ubertob.kondor.json.*
import com.ubertob.kondor.json.datetime.str
import com.ubertob.kondor.json.jsonnode.JsonNodeObject
import java.time.Instant


sealed interface SealedClass
data class SmallClass(val string: String, val int: Int, val double: Double, val boolean: Boolean) : SealedClass
data class ClassWithArray(val name: String, val values: List<String>) : SealedClass
data class NestedClass(val instant: Instant, val smallClass: SmallClass) : SealedClass


object JSmallClass : JAny<SmallClass>() {
    val string by str(SmallClass::string)
    val int by num(SmallClass::int)
    val double by num(SmallClass::double)
    val boolean by bool(SmallClass::boolean)

    override fun JsonNodeObject.deserializeOrThrow() =
        SmallClass(
            string = +string,
            int = +int,
            double = +double,
            boolean = +boolean
        )
}

object JClassWithArray : JAny<ClassWithArray>() {
    val name by str(ClassWithArray::name)
    val values by array(JString, ClassWithArray::values)
    override fun JsonNodeObject.deserializeOrThrow() = ClassWithArray(
        name= +name,
        values = +values
    )
}

object JNestedClass : JAny<NestedClass>() {
    val instant by str(NestedClass::instant)
    val small_class by obj(JSmallClass, NestedClass::smallClass)
    override fun JsonNodeObject.deserializeOrThrow() = NestedClass(
        instant = +instant,
        smallClass = +small_class
    )
}

object JSealedClass : JSealed<SealedClass>() {

    override val discriminatorFieldName = "kind"

    override val subConverters = mapOf(
        "SMALL" to JSmallClass,
        "NESTED" to JNestedClass,
        "ARRAY" to JClassWithArray
    )

    override fun extractTypeName(obj: SealedClass) =
        when (obj) {
            is ClassWithArray -> "ARRAY"
            is SmallClass -> "SMALL"
            is NestedClass -> "NESTED"
        }

}

fun buildSealedClass(index: Int): SealedClass =
    when(index % 3){
        0 -> SmallClass("SmallClass$index", index, index.toDouble(), index % 2 == 0)
        1 -> NestedClass(Instant.now(), SmallClass("Nested$index", index, index.toDouble(), index % 2 == 0))
        else -> ClassWithArray(name = "ClassWithArray$index", (0..index).map { it.toString() })
    }

