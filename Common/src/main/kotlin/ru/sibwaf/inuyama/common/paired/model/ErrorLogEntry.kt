package ru.sibwaf.inuyama.common.paired.model

import hirondelle.date4j.DateTime
import java.util.UUID

data class ErrorLogEntry(
    val timestamp: DateTime,
    val guid: UUID,
    val stacktrace: String,
)
