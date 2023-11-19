package ru.sibwaf.inuyama.finance.rates

import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import java.time.Duration
import java.time.LocalDate

class MemoryCachedExchangeRateProvider(
    private val delegate: ExchangeRateProvider,
    cacheExpiration: Duration,
) : ExchangeRateProvider {

    private data class CacheKey(
        val fromCurrency: String,
        val toCurrency: String,
        val date: LocalDate,
    )

    private val cache = Caffeine.newBuilder()
        .expireAfterWrite(cacheExpiration)
        .buildAsync<CacheKey, Double> { key, executor ->
            CoroutineScope(executor.asCoroutineDispatcher()).future {
                delegate.getExchangeRate(
                    fromCurrency = key.fromCurrency,
                    toCurrency = key.toCurrency,
                    date = key.date,
                )
            }
        }

    override suspend fun getExchangeRate(fromCurrency: String, toCurrency: String, date: LocalDate): Double? {
        val key = CacheKey(
            fromCurrency = fromCurrency,
            toCurrency = toCurrency,
            date = date,
        )

        return cache.get(key).await()
    }
}
