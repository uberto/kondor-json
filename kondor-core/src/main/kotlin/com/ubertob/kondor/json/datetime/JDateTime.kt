package com.ubertob.kondor.json.datetime

import com.ubertob.kondor.json.JNumRepresentable
import com.ubertob.kondor.json.JStringRepresentable
import com.ubertob.kondor.json.JsonOutcome
import com.ubertob.kondor.outcome.asSuccess
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*

object JZoneId : JStringRepresentable<ZoneId>() {
    override val cons: (String) -> ZoneId = ZoneId::of
    override val render: (ZoneId) -> String = ZoneId::toString
}

object JLocalTime : JStringRepresentable<LocalTime>() {
    override val cons: (String) -> LocalTime = LocalTime::parse
    override val render: (LocalTime) -> String = LocalTime::toString

    fun withFormatter(formatter: DateTimeFormatter): JStringRepresentable<LocalTime> = Custom(formatter)

    fun withPattern(pattern: String, locale: Locale = Locale.ENGLISH): JStringRepresentable<LocalTime> =
        Custom(DateTimeFormatter.ofPattern(pattern).withLocale(locale))

    private class Custom(private val formatter: DateTimeFormatter) : JStringRepresentable<LocalTime>() {
        override val cons: (String) -> LocalTime = { LocalTime.parse(it, formatter) }
        override val render: (LocalTime) -> String = { formatter.format(it) }
    }
}

object JLocalDateTime : JStringRepresentable<LocalDateTime>() {
    override val cons: (String) -> LocalDateTime = LocalDateTime::parse
    override val render: (LocalDateTime) -> String = LocalDateTime::toString

    fun withFormatter(formatter: DateTimeFormatter): JStringRepresentable<LocalDateTime> = Custom(formatter)

    fun withPattern(pattern: String, locale: Locale = Locale.ENGLISH): JStringRepresentable<LocalDateTime> =
        Custom(DateTimeFormatter.ofPattern(pattern).withLocale(locale))

    private class Custom(private val formatter: DateTimeFormatter) : JStringRepresentable<LocalDateTime>() {
        override val cons: (String) -> LocalDateTime = { LocalDateTime.parse(it, formatter) }
        override val render: (LocalDateTime) -> String = { formatter.format(it) }
    }
}

object JDuration : JStringRepresentable<Duration>() {
    override val cons: (String) -> Duration = Duration::parse
    override val render: (Duration) -> String = Duration::toString
}

object JLocalDate : JStringRepresentable<LocalDate>() {
    override val cons: (String) -> LocalDate = LocalDate::parse
    override val render: (LocalDate) -> String = LocalDate::toString

    fun withFormatter(formatter: DateTimeFormatter): JStringRepresentable<LocalDate> = Custom(formatter)

    fun withPattern(pattern: String, locale: Locale = Locale.ENGLISH): JStringRepresentable<LocalDate> =
        Custom(DateTimeFormatter.ofPattern(pattern).withLocale(locale))

    private class Custom(private val formatter: DateTimeFormatter) : JStringRepresentable<LocalDate>() {
        override val cons: (String) -> LocalDate = { LocalDate.parse(it, formatter) }
        override val render: (LocalDate) -> String = { formatter.format(it) }
    }
}

//instant as date string
object JInstant : JStringRepresentable<Instant>() {
    override val cons: (String) -> Instant = Instant::parse
    override val render: (Instant) -> String = Instant::toString
}

//instant as epoch millis
object JInstantEpoch : JNumRepresentable<Instant>() {
    override val cons: (Number) -> Instant = { Instant.ofEpochMilli(it.toLong()) }
    override val render: (Instant) -> Number = { it.toEpochMilli() }
    override fun parser(value: String): JsonOutcome<Long> = value.toLong().asSuccess()
}
