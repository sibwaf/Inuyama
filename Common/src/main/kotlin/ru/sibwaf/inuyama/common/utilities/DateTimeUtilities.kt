package ru.sibwaf.inuyama.common.utilities

import hirondelle.date4j.DateTime
import java.util.Date
import java.util.TimeZone

val Date.asDateTime
    get() = DateTime.forInstant(time, TimeZone.getDefault())!!

val DateTime.asDate
    get() = Date(getMilliseconds(TimeZone.getDefault()))

fun DateTime.minusMonths(months: Int): DateTime = minus(0, months, 0, 0, 0, 0, 0, DateTime.DayOverflow.Spillover)

infix fun DateTime.withTimeFrom(other: DateTime): DateTime {
    return DateTime(year, month, day, other.hour, other.minute, other.second, other.nanoseconds)
}

fun DateTime.toDateOnly(): DateTime = DateTime.forDateOnly(year, month, day)

fun DateTime.toTimeOnly(): DateTime = DateTime.forTimeOnly(hour, minute, second, nanoseconds)
