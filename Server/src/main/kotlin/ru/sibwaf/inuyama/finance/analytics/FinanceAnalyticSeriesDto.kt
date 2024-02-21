package ru.sibwaf.inuyama.finance.analytics

import java.time.OffsetDateTime

data class FinanceAnalyticSeriesDto(
    val timeline: List<OffsetDateTime>,
    val data: Map<String, List<Double>>
) {
    constructor(timeline: List<OffsetDateTime>, transposedData: List<Map<String, Double>>) : this(timeline, transpose(transposedData))
}

private fun <K, V> transpose(data: List<Map<K, V>>): Map<K, List<V>> {
    val result = mutableMapOf<K, MutableList<V>>()
    for (column in data) {
        for ((key, value) in column) {
            result.getOrPut(key) { mutableListOf() }.add(value)
        }
    }
    return result
}
