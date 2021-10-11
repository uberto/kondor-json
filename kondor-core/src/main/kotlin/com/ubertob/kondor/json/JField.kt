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

class JFieldFlatten<T : Any, PT : Any>(
    override val binder: (PT) -> T,
    private val converter: ObjectNodeConverter<T>,
    private val parent: JAny<PT>
) : JFieldBase<T, PT>() {

    override fun buildJsonProperty(property: KProperty<*>): JsonProperty<T> =
        JsonPropMandatoryFlatten(property.name, converter, parent)

}

class JFieldMaybe<T, PT : Any>(
    override val binder: (PT) -> T?,
    private val converter: JConverter<T>
) : JFieldBase<T?, PT>() {

    override fun buildJsonProperty(property: KProperty<*>): JsonProperty<T?> =
        JsonPropOptional(property.name, converter)

}

