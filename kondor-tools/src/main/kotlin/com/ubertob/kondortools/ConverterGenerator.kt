package com.ubertob.kondortools

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.ubertob.kondor.json.JAny
import com.ubertob.kondor.json.JsonNodeObject
import java.io.StringWriter
import kotlin.reflect.KClass
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

                    .delegate("xxx(${kClass.simpleName}::${prop.name})")
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

private fun KClass<*>.generateNamedParams(): String =
    memberProperties.joinToString(
        separator = ",\n",
        prefix = "return \n    $simpleName(\n",
        postfix = "\n    )"
    ) { "      ${it.name} = +${it.name}" }

//KotlinPoet snippets examples:
//
//val flux = FunSpec.constructorBuilder()
//    .addParameter("greeting", String::class)
//    .build()
//
//val classHW = TypeSpec.classBuilder("HelloWorld")
//    .primaryConstructor(flux)
//    .addProperty(
//        PropertySpec.builder("greeting", String::class)
//            .initializer("greeting")
//            .addModifiers(KModifier.PRIVATE)
//            .build()
//    )
//    .build()
//
//
//val helloClass = ClassName("com.example.hello", "Hello")
//val worldFunction: MemberName = helloClass.member("world")
//val byeProperty: MemberName = helloClass.nestedClass("World").member("bye")
//
//val factoriesFun = FunSpec.builder("factories")
//    .addStatement("val hello = %L", helloClass.constructorReference())
//    .addStatement("val world = %L", worldFunction.reference())
//    .addStatement("val bye = %L", byeProperty.reference())
//    .build()
