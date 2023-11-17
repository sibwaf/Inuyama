package ru.sibwaf.inuyama.finance.rates

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

class MemoryCachedExchangeRateProvider(private val delegate: ExchangeRateProvider) : ExchangeRateProvider {

    private data class CacheKey(
        val fromCurrency: String,
        val toCurrency: String,
        val date: LocalDate,
    )

    private val cache = ConcurrentHashMap<CacheKey, Double>()
    private val mutex = Mutex()

    override suspend fun getExchangeRate(fromCurrency: String, toCurrency: String, date: LocalDate): Double? {
        val key = CacheKey(
            fromCurrency = fromCurrency,
            toCurrency = toCurrency,
            date = date,
        )

        return cache[key] ?: mutex.withLock {
            var result = cache[key]
            if (result == null) {
                result = delegate.getExchangeRate(
                    fromCurrency = fromCurrency,
                    toCurrency = toCurrency,
                    date = date,
                )
            }
            result
        }
    }
}
