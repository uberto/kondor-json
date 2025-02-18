package com.ubertob.kondor.primwrap

import com.ubertob.kondor.json.JField
import com.ubertob.kondor.json.JIntRepresentable
import com.ubertob.kondor.json.JLongRepresentable
import com.ubertob.kondor.json.JStringRepresentable

data class JStringWrap<T : StringWrap>(override val cons: (String) -> T) : JStringRepresentable<T>() {
    override val render: (T) -> String = { it.raw }
}

data class JIntWrap<T : IntWrap>(val fromInt: (Int) -> T) : JIntRepresentable<T>() {
    override val cons: (Int) -> T = { fromInt(it) }
    override val render: (T) -> Int = { it.raw }
}

data class JLongWrap<T : LongWrap>(val fromLong: (Long) -> T) : JLongRepresentable<T>() {
    override val cons: (Long) -> T = { fromLong(it) }
    override val render: (T) -> Long = { it.raw }
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
