package ru.sibwaf.inuyama.common.utilities

import hirondelle.date4j.DateTime
import java.util.Date
import java.util.TimeZone

val Date.asDateTime
    get() = DateTime.forInstant(time, TimeZone.getDefault())!!

val DateTime.asDate
    get() = Date(getMilliseconds(TimeZone.getDefault()))