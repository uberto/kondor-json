package com.ubertob.kondortools

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.ubertob.kondor.json.JAny
import com.ubertob.kondor.json.JsonNodeObject
import java.io.StringWriter
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.createType
import kotlin.reflect.full.memberProperties

interface RemoveMe //only used to be removed later

fun generateConverterFileFor(vararg kClasses: KClass<*>): String {
    val fileBuilder = FileSpec.builder("", "KondorConverters")

    kClasses.forEach { fileBuilder.addType(createConverterFor(it)) }

    val sw = StringWriter()
    fileBuilder.build().writeTo(sw)
    val output = sw.toString()
        .replace(": RemoveMe", "")
        .replace("public object", "object")

    return output
}

private fun createConverterFor(kClass: KClass<*>): TypeSpec =
    TypeSpec.objectBuilder("J${kClass.simpleName}")
        .superclass(JAny::class.parameterizedBy(kClass))
        .addProperties(
            kClass.memberProperties.map { prop ->
                PropertySpec.builder(prop.name, RemoveMe::class, KModifier.PRIVATE)

                    .delegate(generateRightConverter(kClass, prop))
                    .build()
            }
        )
        .addFunction(
            FunSpec.builder("deserializeOrThrow")
                .receiver(JsonNodeObject::class)
                .addModifiers(KModifier.OVERRIDE)
                .returns(kClass)
                .addStatement(kClass.generateNamedParams())
                .build()
        )
        .build()

//this should allow for a callback so users can specialize it with their types
private fun generateRightConverter(
    kClass: KClass<*>,
    prop: KProperty1<out Any, *>
) = when (prop.returnType) {
    Boolean::class.createType() -> "bool(${kClass.simpleName}::${prop.name})"
    Double::class.createType() -> "num(${kClass.simpleName}::${prop.name})"
    Long::class.createType() -> "num(${kClass.simpleName}::${prop.name})"
    Int::class.createType() -> "num(${kClass.simpleName}::${prop.name})"
    String::class.createType() -> "str(${kClass.simpleName}::${prop.name})"
    else -> "obj(J${prop.simpleClassName()}, ${kClass.simpleName}::${prop.name})"
//enum, objects, collections...
}

private fun KProperty1<out Any, *>.simpleClassName() =
    ClassName.bestGuess(returnType.asTypeName().toString()).simpleName
// .also{   println("!!!${prop.name}  ${prop.returnType.asTypeName()}") }


private fun KClass<*>.generateNamedParams(): String =
    memberProperties.joinToString(
        separator = ",\n",
        prefix = "return \n    $simpleName(\n",
        postfix = "\n    )"
    ) { "      ${it.name} = +${it.name}" }
