package ru.sibwaf.inuyama.finance.rates

import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import ru.sibwaf.inuyama.common.api.ExchangeRateHostApi
import java.time.Duration
import java.time.LocalDate

class ExchangeRateHostExchangeRateProvider(
    private val api: ExchangeRateHostApi,
    private val currencySetProvider: () -> Set<String>,
) : ExchangeRateProvider {

    private val availableCurrenciesCache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofHours(24))
        .buildAsync<Unit, Set<String>> { _, executor ->
            CoroutineScope(executor.asCoroutineDispatcher()).future {
                api.getAvailableCurrencies()
            }
        }

    override suspend fun getExchangeRate(fromCurrency: String, toCurrency: String, date: LocalDate): Double? {
        val now = LocalDate.now()
        if (date >= now) {
            return getExchangeRate(
                fromCurrency = fromCurrency,
                toCurrency = toCurrency,
                date = now.minusDays(1),
            )
        }

        val availableCurrencies = availableCurrenciesCache.get(Unit).await()

        if (fromCurrency !in availableCurrencies || toCurrency !in availableCurrencies) {
            return null
        }

        val toCurrencies = (currencySetProvider() + toCurrency - fromCurrency).intersect(availableCurrencies)
        if (toCurrencies.isEmpty()) {
            return null
        }

        return api.getExchangeRates(fromCurrency, toCurrencies, date)[toCurrency]
    }
}
