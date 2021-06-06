package com.ubertob.kondor.json.datetime

import com.ubertob.kondor.json.JField
import com.ubertob.kondor.json.JFieldMaybe
import java.time.*


@JvmName("bindLocalTime")
fun <PT : Any> str(binder: PT.() -> LocalTime) = JField(binder, JLocalTime)

@JvmName("bindLocalTimeNull")
fun <PT : Any> str(binder: PT.() -> LocalTime?) = JFieldMaybe(binder, JLocalTime)

@JvmName("bindLocalDateTime")
fun <PT : Any> str(binder: PT.() -> LocalDateTime) = JField(binder, JLocalDateTime)

@JvmName("bindLocalDateTimeNull")
fun <PT : Any> str(binder: PT.() -> LocalDateTime?) = JFieldMaybe(binder, JLocalDateTime)

@JvmName("bindLocalDate")
fun <PT : Any> str(binder: PT.() -> LocalDate) = JField(binder, JLocalDate)

@JvmName("bindLocalDateNull")
fun <PT : Any> str(binder: PT.() -> LocalDate?) = JFieldMaybe(binder, JLocalDate)

@JvmName("bindLocalDateWithPattern")
fun <PT : Any> localDate(pattern: String, binder: PT.() -> LocalDate) = JField(binder, JLocalDate.withFormat(pattern))

@JvmName("bindZoneId")
fun <PT : Any> str(binder: PT.() -> ZoneId) = JField(binder, JZoneId)

@JvmName("bindZoneIdNull")
fun <PT : Any> str(binder: PT.() -> ZoneId?) = JFieldMaybe(binder, JZoneId)

@JvmName("bindInstant")
fun <PT : Any> str(binder: PT.() -> Instant) = JField(binder, JInstant)

@JvmName("bindInstantNull")
fun <PT : Any> str(binder: PT.() -> Instant?) = JFieldMaybe(binder, JInstant)

@JvmName("bindInstantEpoch")
fun <PT : Any> num(binder: PT.() -> Instant) = JField(binder, JInstantEpoch)

@JvmName("bindInstantEpochNull")
fun <PT : Any> num(binder: PT.() -> Instant?) = JFieldMaybe(binder, JInstantEpoch)