package ru.sibwaf.inuyama.finance.rates

import ru.sibwaf.inuyama.common.api.ExchangeRateHostApi
import java.time.LocalDate

class ExchangeRateHostExchangeRateProvider(
    private val api : ExchangeRateHostApi,
    private val currencySetProvider: () -> Set<String>,
) : ExchangeRateProvider {

    override suspend fun getExchangeRate(fromCurrency: String, toCurrency: String, date: LocalDate): Double? {
        return api.getExchangeRates(fromCurrency, currencySetProvider() + toCurrency - fromCurrency, date)[toCurrency]
    }
}
