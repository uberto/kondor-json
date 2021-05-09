package com.ubertob.kondortools

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.ubertob.kondor.json.JAny
import com.ubertob.kondor.json.jsonnode.JsonNodeObject
import java.io.StringWriter
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.memberProperties

interface RemoveMe //only used to be removed later

fun generateConverterFileFor(vararg kClasses: KClass<*>): String {
    val fileBuilder = FileSpec.builder("", "KondorConverters")

    kClasses.forEach { fileBuilder.addType(createConverterFor(it)) }

    val sw = StringWriter()
    fileBuilder.build().writeTo(sw)
    val output = sw.toString()
        .replace(": RemoveMe", "")
        .replace("import com.ubertob.kondortools.RemoveMe\n", "")
        .replace("import com.ubertob.kondor.json.(.*)\n".toRegex(), "")
        .insert("import com.ubertob.kondor.json.*\n")
        .replace("public object", "object")
        .replace("public override", "override")

    return output
}

private fun String.insert(prefix: String): String =
    prefix + this

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
) = when {
    prop.returnType == Boolean::class.createType() -> "bool(${kClass.simpleName}::${prop.name})"
    prop.returnType.isSubtypeOf(Number::class.createType()) -> "num(${kClass.simpleName}::${prop.name})"
    prop.returnType.isSubtypeOf(CharSequence::class.createType()) -> "str(${kClass.simpleName}::${prop.name})"
    prop.returnType.isSubtypeOf(Enum::class.createType(listOf(KTypeProjection.STAR))) -> "str(${kClass.simpleName}::${prop.name})"
    prop.returnType.isSubtypeOf(Iterable::class.createType(listOf(KTypeProjection.STAR))) -> "array(J${prop.genericClassName()}, ${kClass.simpleName}::${prop.name})"
    else -> "obj(J${prop.simpleClassName()}, ${kClass.simpleName}::${prop.name})"
}

private fun KProperty1<out Any, *>.simpleClassName() =
    try {
        ClassName.bestGuess(returnType.asTypeName().toString()).simpleName
    } catch (t: Throwable) {
        "xxx"
    }

private fun KProperty1<out Any, *>.genericClassName() =
    try {
        ClassName.bestGuess(returnType.arguments.first().type?.asTypeName().toString()).simpleName
    } catch (t: Throwable) {
        "xxx"
    }

private fun KClass<*>.generateNamedParams(): String =
    memberProperties.joinToString(
        separator = ",\n",
        prefix = "return \n    $simpleName(\n",
        postfix = "\n    )"
    ) { "      ${it.name} = +${it.name}" }
