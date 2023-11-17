package ru.sibwaf.inuyama.finance.rates

import java.time.LocalDate

interface ExchangeRateProvider {
    suspend fun getExchangeRate(fromCurrency: String, toCurrency: String, date: LocalDate): Double?
}
