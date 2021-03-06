package com.ubertob.kondor.json

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


//bindings

@JvmName("bindingString")
fun <PT : Any> binding(binder: PT.() -> String) = JField(binder, JString)

@JvmName("bindingStringNull")
fun <PT : Any> binding(binder: PT.() -> String?) = JFieldMaybe(binder, JString)

@JvmName("bindingInt")
fun <PT : Any> binding(binder: PT.() -> Int) = JField(binder, JInt)

@JvmName("bindingIntNull")
fun <PT : Any> binding(binder: PT.() -> Int?) = JFieldMaybe(binder, JInt)

@JvmName("bindingDouble")
fun <PT : Any> binding(binder: PT.() -> Double) = JField(binder, JDouble)

@JvmName("bindingDoubleNull")
fun <PT : Any> binding(binder: PT.() -> Double?) = JFieldMaybe(binder, JDouble)

@JvmName("jBindEnum")
inline fun <PT : Any, reified E : Enum<E>> binding(noinline binder: PT.() -> E) = JField(binder, JEnum(::enumValueOf))

@JvmName("jBindEnumNull")
inline fun <PT : Any, reified E : Enum<E>> binding(noinline binder: PT.() -> E?) =
    JFieldMaybe(binder, JEnum(::enumValueOf))

