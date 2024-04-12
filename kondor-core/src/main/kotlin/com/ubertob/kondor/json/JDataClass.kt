package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.JsonNodeObject
import com.ubertob.kondor.json.jsonnode.NodePath
import com.ubertob.kondor.json.jsonnode.NodePathRoot


//experimental converter for data classes without any boilerplate WIP!!
abstract class JDataClass<T : Any> : JAny<T>() {

    abstract val clazz: Class<T>

    val constructor by lazy { clazz.constructors.first() }

    internal var _currentPath: NodePath = NodePathRoot //hack to get the current path during parsing without breaking changes.

    override fun JsonNodeObject.deserializeOrThrow(): T? {

        //using ksp to get info about the T parameter names and order
//        class A : Store<B, C> { }
//
//        So to get the type of B and C
//
//        val B = it.superTypes.first().resolve().arguments.first().type?.resolve()?.declaration
//        print(B?.qualifiedName?.asString())
//
//        val C = it.superTypes.first().resolve().arguments.elementAtOrNull(1)?.type?.resolve()?.declaration
//        print(C?.qualifiedName?.asString())

        //todo can we check that is a Kotlin data class? we can also compare constructors args and Json fields

//        val map: Map<String, Any?> = getProperties().associate {
//            it.propName to it.getter(this).orThrow()
//        }
//
//        println("properties map ${map.keys}") //json names
//        val args = mutableListOf<Any?>()
////        first translate all props in objects values, then pass to the constructor
//        val consParams = constructor.parameters
//        println("found ${consParams.size} cons params")
//
//        val consParamNames = consParams.map { it.annotatedType } //just arg1 arg2...
//        println("consParamNames $consParamNames")
//
//        for (param in consParams) {
//            val field = map[param.name]
//            println("cons param ${param.name}  $field")
//            args.add(field)
//        }


        //using asm we can create a unnamed class with a single method that deserialize json based on the converter fields and then get the method handler and call it here !!!

        //this work assuming the JConverter has fields in the same exact order then the data class constructor

        val args: List<Any?> = getProperties().map { it.getter(_fieldMap).orThrow() }

        @Suppress("UNCHECKED_CAST")
        return constructor.newInstance(*args.toTypedArray()) as T
    }


//
//    override fun fromJson(json: String): JsonOutcome<T> =
//
//        JsonLexerEager(json).tokenize().bind {
//
//            val tp = TokensPath(it, NodePathRoot) //root??
// //asm generated method handler that know the tokens to expect and what to extract
//
//            tp.toObjectFields(getProperties())
//        }.transform {
//            val args = it.values
//            @Suppress("UNCHECKED_CAST")
//            constructor.newInstance(*args.toTypedArray()) as T
//
//        }


}