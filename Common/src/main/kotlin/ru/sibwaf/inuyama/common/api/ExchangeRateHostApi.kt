package ru.sibwaf.inuyama.common.api

import java.time.LocalDate

interface ExchangeRateHostApi {
    suspend fun getExchangeRates(
        fromCurrency: String,
        toCurrencies: Set<String>,
        date: LocalDate,
    ): Map<String, Double>
}
