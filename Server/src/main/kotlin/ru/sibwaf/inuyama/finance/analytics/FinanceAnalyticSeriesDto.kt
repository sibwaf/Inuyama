package ru.sibwaf.inuyama.finance.analytics

import java.time.OffsetDateTime

data class FinanceAnalyticSeriesDto(
    val timeline: List<OffsetDateTime>,
    val data: Map<String, List<Double>>
)
