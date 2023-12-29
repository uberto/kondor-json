package com.ubertob.kondor.primwrap

import kotlin.reflect.KClass


internal val intWrappersMap = mutableMapOf<KClass<out PrimitiveWrap<Int>>, PrimitiveWrapCompanion<Int>>()
private val longWrappersMap = mutableMapOf<KClass<out PrimitiveWrap<Long>>, PrimitiveWrapCompanion<Long>>()
private val doubleWrappersMap = mutableMapOf<KClass<out PrimitiveWrap<Double>>, PrimitiveWrapCompanion<Double>>()
private val stringWrappersMap = mutableMapOf<KClass<out PrimitiveWrap<String>>, PrimitiveWrapCompanion<String>>()
private val booleanWrappersMap = mutableMapOf<KClass<out PrimitiveWrap<Boolean>>, PrimitiveWrapCompanion<Boolean>>()

fun <T : PrimitiveWrap<Long>> registeredLongCons(kClass: KClass<out PrimitiveWrap<Long>>, primitive: Long): T =
    (longWrappersMap[kClass]?.fromPrimitive(primitive) as? T) ?: error("Not registered! $kClass")

fun <T : PrimitiveWrap<Int>> registeredIntCons(kClass: KClass<out PrimitiveWrap<Int>>, primitive: Int): T =
    (intWrappersMap[kClass]?.fromPrimitive(primitive) as? T) ?: error("Not registered! $kClass")

fun <T : PrimitiveWrap<String>> registeredStringCons(kClass: KClass<out PrimitiveWrap<String>>, primitive: String): T =
    (stringWrappersMap[kClass]?.fromPrimitive(primitive) as? T) ?: error("Not registered! $kClass")


inline fun <reified T : PrimitiveWrap<String>> createTinyTypeString(x: String): T {

//    return registeredStringCons(T::class, x)

    val methods = T::class.java.declaredClasses.firstOrNull { it.name.endsWith("Companion") }!!.methods

//    methods.map { println("!!!! ${it.name}") }
    val xx = methods
        .firstOrNull { it.name == "fromPrimitive" }
        ?: error("Not found method by reflection! ${T::class.java.name} ")

//    return xx.invoke(x)  as T

    return registeredStringCons(T::class, x)
}


inline fun <reified T : IntWrap> createTinyTypeInt(x: Int): T = registeredIntCons(T::class, x)
inline fun <reified T : LongWrap> createTinyTypeLong(x: Long): T = registeredLongCons(T::class, x)


sealed interface PrimitiveWrapCompanion<T> {
    fun fromPrimitive(primitive: T): PrimitiveWrap<T>
}

abstract class IntWrapCompanion(wrappingClass: KClass<out PrimitiveWrap<Int>>) : PrimitiveWrapCompanion<Int> {

    init {
        this.also { intWrappersMap[wrappingClass] = it }
    }
}

abstract class LongWrapCompanion(wrappingClass: KClass<out PrimitiveWrap<Long>>) : PrimitiveWrapCompanion<Long> {
    init {
        this.also { longWrappersMap[wrappingClass] = it }
    }
}

abstract class DoubleWrapCompanion(wrappingClass: KClass<out PrimitiveWrap<Double>>) : PrimitiveWrapCompanion<Double> {
    init {
        this.also { doubleWrappersMap[wrappingClass] = it }
    }
}

abstract class StringWrapCompanion(wrappingClass: KClass<out PrimitiveWrap<String>>) : PrimitiveWrapCompanion<String> {
    init {
        this.also { stringWrappersMap[wrappingClass] = it }
    }
}

abstract class BooleanWrapCompanion(wrappingClass: KClass<out PrimitiveWrap<Boolean>>) :
    PrimitiveWrapCompanion<Boolean> {
    init {
        this.also { booleanWrappersMap[wrappingClass] = it }
    }
}

