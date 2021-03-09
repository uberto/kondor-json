package com.ubertob.kondor.json

import java.math.BigDecimal
import java.time.*


object JZoneId : JStringRepresentable<ZoneId>() {
    override val cons: (String) -> ZoneId = ZoneId::of
    override val render: (ZoneId) -> String = ZoneId::toString
}

object JLocalTime : JStringRepresentable<LocalTime>() {
    override val cons: (String) -> LocalTime = LocalTime::parse
    override val render: (LocalTime) -> String = LocalTime::toString
}

object JLocalDateTime : JStringRepresentable<LocalDateTime>() {
    override val cons: (String) -> LocalDateTime = LocalDateTime::parse
    override val render: (LocalDateTime) -> String = LocalDateTime::toString
}

object JDuration : JStringRepresentable<Duration>() {
    override val cons: (String) -> Duration = Duration::parse
    override val render: (Duration) -> String = Duration::toString
}

object JLocalDate : JStringRepresentable<LocalDate>() {
    override val cons: (String) -> LocalDate = LocalDate::parse
    override val render: (LocalDate) -> String = LocalDate::toString
}


//instant as date string
object JInstant : JStringRepresentable<Instant>() {
    override val cons: (String) -> Instant = Instant::parse
    override val render: (Instant) -> String = Instant::toString
}

//instant as epoch millis
object JInstantEpoch : JNumRepresentable<Instant>() {
    override val cons: (BigDecimal) -> Instant = { Instant.ofEpochMilli(it.toLong()) }
    override val render: (Instant) -> BigDecimal = { it.toEpochMilli().toBigDecimal() }
}


//binding

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