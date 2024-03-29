package ru.sibwaf.inuyama.api

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import ru.sibwaf.inuyama.common.api.ExchangeRateHostApi
import ru.sibwaf.inuyama.fromJson
import ru.sibwaf.inuyama.toJson
import java.nio.file.Path
import java.time.LocalDate
import kotlin.io.path.createDirectories

class FilesystemCachedExchangeRateHostApi(
    private val delegate: ExchangeRateHostApi,
    private val fetchAllCurrencies: Boolean,
    private val cachePath: Path,
    private val gson: Gson,
) : ExchangeRateHostApi {

    private val mutex = Mutex()

    init {
        cachePath.createDirectories()
    }

    override suspend fun getExchangeRates(
        fromCurrency: String,
        toCurrencies: Set<String>,
        date: LocalDate,
    ): Map<String, Double> {
        val path = cachePath.resolve("$date-$fromCurrency.json")
        return mutex.withLock {
            val cache = try {
                withContext(Dispatchers.IO) { gson.fromJson<CacheEntry>(path) }
            } catch (e: Exception) {
                CacheEntry(emptySet(), emptyMap())
            }

            if (!cache.currencies.containsAll(toCurrencies)) {
                val rates = delegate.getExchangeRates(
                    fromCurrency = fromCurrency,
                    toCurrencies = toCurrencies.takeUnless { fetchAllCurrencies }.orEmpty(),
                    date = date,
                )
                withContext(Dispatchers.IO) { gson.toJson(CacheEntry(rates.keys, rates), path) }
                rates
            } else {
                cache.values
            }
        }
    }

    override suspend fun getAvailableCurrencies(): Set<String> {
        val path = cachePath.resolve("currencies.json")
        return mutex.withLock {
            val cache = try {
                withContext(Dispatchers.IO) { gson.fromJson<Set<String>>(path) }
            } catch (e: Exception) {
                null
            }

            if (cache == null) {
                val currencies = delegate.getAvailableCurrencies()
                withContext(Dispatchers.IO) { gson.toJson(currencies, path) }
                currencies
            } else {
                cache
            }
        }
    }
}

private data class CacheEntry(
    val currencies: Set<String>,
    val values: Map<String, Double>,
)
