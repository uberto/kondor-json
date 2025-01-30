package com.ubertob.kondor.primwrap

import com.ubertob.kondor.json.*

data class JStringWrap<T : StringWrap>(override val cons: (String) -> T) : JStringRepresentable<T>() {
    override val render: (T) -> String = { it.raw }
}

data class JIntWrap<T : IntWrap>(val fromInt: (Int) -> T) : JNumRepresentable<T>() {
    override val cons: (Number) -> T = { fromInt(it.toInt()) }
    override val render: (T) -> Number = { it.raw }
    override fun parser(value: String): JsonOutcome<Number> = JInt.parser(value)
}

data class JLongWrap<T : LongWrap>(val fromLong: (Long) -> T) : JNumRepresentable<T>() {
    override val cons: (Number) -> T = { fromLong(it.toLong()) }
    override val render: (T) -> Number = { it.raw }
    override fun parser(value: String): JsonOutcome<Number> = JLong.parser(value)
}

inline fun <PT : Any, reified PRIMWRAP : StringWrap> strW(noinline binder: PT.() -> PRIMWRAP): JField<PRIMWRAP, PT> =
    JField(binder, JStringWrap(::createTinyTypeString))

@JvmName("bindIntWrap")
inline fun <PT : Any, reified PRIMWRAP : IntWrap> numW(noinline binder: PT.() -> PRIMWRAP): JField<PRIMWRAP, PT> =
    JField(binder, JIntWrap(::createTinyTypeInt))

@JvmName("bindLongWrap")
inline fun <PT : Any, reified PRIMWRAP : LongWrap> numW(noinline binder: PT.() -> PRIMWRAP): JField<PRIMWRAP, PT> =
    JField(binder, JLongWrap(::createTinyTypeLong))
//fun <PT : Any, PRIMWRAP: DoubleWrap> numW(binder: PT.() -> PRIMWRAP) = JField(binder, JDoubleConverter)
//fun <PT : Any, PRIMWRAP: BooleanWrap> boolW(binder: PT.() -> PRIMWRAP) = JField(binder, JBooleanConverter)
