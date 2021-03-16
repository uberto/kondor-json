package com.ubertob.kondor.json

import java.math.BigDecimal
import java.math.BigInteger
import java.util.*
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

sealed class JFieldBase<T, PT : Any>
    : ReadOnlyProperty<JAny<PT>, JsonProperty<T>> {

    protected abstract val binder: (PT) -> T

    protected abstract fun buildJsonProperty(property: KProperty<*>): JsonProperty<T>

    operator fun provideDelegate(thisRef: JAny<PT>, prop: KProperty<*>): JFieldBase<T, PT> {
        val jp = buildJsonProperty(prop)
        thisRef.registerProperty(jp, binder)
        return this
    }

    override fun getValue(thisRef: JAny<PT>, property: KProperty<*>): JsonProperty<T> =
        buildJsonProperty(property)
}

class JField<T : Any, PT : Any>(
    override val binder: (PT) -> T,
    private val converter: JConverter<T>
) : JFieldBase<T, PT>() {

    override fun buildJsonProperty(property: KProperty<*>): JsonProperty<T> =
        JsonPropMandatory(property.name, converter)

}

class JFieldMaybe<T : Any, PT : Any>(
    override val binder: (PT) -> T?,
    private val converter: JConverter<T>
) : JFieldBase<T?, PT>() {

    override fun buildJsonProperty(property: KProperty<*>): JsonProperty<T?> =
        JsonPropOptional(property.name, converter)

}


//binding

@JvmName("bindBool")
fun <PT : Any> bool(binder: PT.() -> Boolean) = JField(binder, JBoolean)

@JvmName("bindBoolNull")
fun <PT : Any> bool(binder: PT.() -> Boolean?) = JFieldMaybe(binder, JBoolean)


@JvmName("bindInt")
fun <PT : Any> num(binder: PT.() -> Int) = JField(binder, JInt)

@JvmName("bindIntNull")
fun <PT : Any> num(binder: PT.() -> Int?) = JFieldMaybe(binder, JInt)

@JvmName("bindLong")
fun <PT : Any> num(binder: PT.() -> Long) = JField(binder, JLong)

@JvmName("bindLongNull")
fun <PT : Any> num(binder: PT.() -> Long?) = JFieldMaybe(binder, JLong)


@JvmName("bindDouble")
fun <PT : Any> num(binder: PT.() -> Double) = JField(binder, JDouble)

@JvmName("bindDoubleNull")
fun <PT : Any> num(binder: PT.() -> Double?) = JFieldMaybe(binder, JDouble)

@JvmName("bindBigDecimal")
fun <PT : Any> num(binder: PT.() -> BigDecimal) = JField(binder, JBigDecimal)

@JvmName("bindBigDecimal")
fun <PT : Any> num(binder: PT.() -> BigDecimal?) = JFieldMaybe(binder, JBigDecimal)

@JvmName("bindBigInteger")
fun <PT : Any> num(binder: PT.() -> BigInteger) = JField(binder, JBigInteger)

@JvmName("bindBigIntegerNull")
fun <PT : Any> num(binder: PT.() -> BigInteger?) = JFieldMaybe(binder, JBigInteger)


@JvmName("bindString")
fun <PT : Any> str(binder: PT.() -> String) = JField(binder, JString)

@JvmName("bindStringNull")
fun <PT : Any> str(binder: PT.() -> String?) = JFieldMaybe(binder, JString)

@JvmName("bindEnum")
inline fun <PT : Any, reified E : Enum<E>> str(noinline binder: PT.() -> E) = JField(binder, JEnum(::enumValueOf))

@JvmName("bindEnumNull")
inline fun <PT : Any, reified E : Enum<E>> str(noinline binder: PT.() -> E?) = JFieldMaybe(binder, JEnum(::enumValueOf))

@JvmName("bindWrappedString")
inline fun <PT : Any, reified E : StringWrapper> str(noinline cons: (String) -> E, noinline binder: PT.() -> E) = JField(binder, JStringWrapper(cons))

@JvmName("bindWrappedStringNull")
inline fun <PT : Any, reified E : StringWrapper> str(noinline cons: (String) -> E, noinline binder: PT.() -> E?) =
    JFieldMaybe(binder, JStringWrapper(cons))

@JvmName("bindCurrency")
fun <PT : Any> str(binder: PT.() -> Currency) = JField(binder, JCurrency)

@JvmName("bindCurrencyNull")
fun <PT : Any> str(binder: PT.() -> Currency?) =
    JFieldMaybe(binder, JCurrency)


@JvmName("bindSet")
inline fun <PT : Any, reified E : Any> array(converter: JConverter<E>, noinline binder: PT.() -> Set<E>) =
    JField(binder, JSet(converter))

@JvmName("bindSetNull")
inline fun <PT : Any, reified E : Any> array(converter: JConverter<E>, noinline binder: PT.() -> Set<E>?) =
    JFieldMaybe(binder, JSet(converter))

@JvmName("bindList")
inline fun <PT : Any, reified E : Any> array(converter: JConverter<E>, noinline binder: PT.() -> List<E>) =
    JField(binder, JList(converter))

@JvmName("bindListNull")
inline fun <PT : Any, reified E : Any> array(converter: JConverter<E>, noinline binder: PT.() -> List<E>?) =
    JFieldMaybe(binder, JList(converter))

@JvmName("bindObject")
inline fun <PT : Any, reified T : Any> obj(converter: JConverter<T>, noinline binder: PT.() -> T) =
    JField(binder, converter)

@JvmName("bindObjectNull")
inline fun <PT : Any, reified T : Any> obj(converter: JConverter<T>, noinline binder: PT.() -> T?) =
    JFieldMaybe(binder, converter)


//continue with Instant etc....