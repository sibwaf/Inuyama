package ru.sibwaf.inuyama.finance.rates

import java.time.LocalDate

class CrossExchangeRateProvider(
    private val mainCurrency: String,
    private val delegate: ExchangeRateProvider,
) : ExchangeRateProvider {

    override suspend fun getExchangeRate(fromCurrency: String, toCurrency: String, date: LocalDate): Double? {
        return when {
            fromCurrency == toCurrency -> 1.0

            fromCurrency == mainCurrency -> delegate.getExchangeRate(
                fromCurrency = fromCurrency,
                toCurrency = toCurrency,
                date = date,
            )

            toCurrency == mainCurrency -> delegate.getExchangeRate(
                fromCurrency = toCurrency,
                toCurrency = fromCurrency,
                date = date,
            )?.let { 1.0 / it }

            else -> {
                val mainToSource = delegate.getExchangeRate(
                    fromCurrency = mainCurrency,
                    toCurrency = fromCurrency,
                    date = date,
                ) ?: return null

                val mainToTarget = delegate.getExchangeRate(
                    fromCurrency = mainCurrency,
                    toCurrency = toCurrency,
                    date = date,
                ) ?: return null

                mainToTarget / mainToSource
            }
        }
    }
}
