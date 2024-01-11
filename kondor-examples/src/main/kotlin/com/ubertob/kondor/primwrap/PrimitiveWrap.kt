package com.ubertob.kondor.primwrap

sealed interface PrimitiveWrap<T> {
    val raw: T
    fun render(): String = raw.toString()
}

abstract class IntWrap(override val raw: Int) : PrimitiveWrap<Int>
abstract class LongWrap(override val raw: Long) : PrimitiveWrap<Long>
abstract class DoubleWrap(override val raw: Double) : PrimitiveWrap<Double>
abstract class StringWrap(override val raw: String) : PrimitiveWrap<String>
abstract class BooleanWrap(override val raw: Boolean) : PrimitiveWrap<Boolean>




