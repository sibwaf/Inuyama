package ru.sibwaf.inuyama.finance

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.sibwaf.inuyama.common.api.ExchangeRateHostApi
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.SortedMap
import java.util.concurrent.ConcurrentHashMap

class CurrencyConverter(private val exchangeRateHostApi: ExchangeRateHostApi) {

    private val cache = ConcurrentHashMap<Pair<String, String>, SortedMap<LocalDate, Double>>()
    private val mutex = Mutex()

    suspend fun prepareCache(currencies: Set<String>, start: OffsetDateTime, end: OffsetDateTime) {
        val startDate = start.toLocalDate()
        val endDate = end.toLocalDate()
        val currentDate = LocalDate.now()

        val timeline = generateSequence(startDate) { it.plusDays(1) }
            .takeWhile { it <= endDate && it < currentDate }
            .toList()

        val availableCurrencies = exchangeRateHostApi.fetchAvailableCurrencies()

        for (fromCurrency in currencies) {
            if (fromCurrency !in availableCurrencies) {
                continue
            }

            for (toCurrency in currencies - fromCurrency) {
                if (toCurrency !in availableCurrencies) {
                    continue
                }

                val pair = fromCurrency to toCurrency

                // todo: no concurrency at all, should have a striped lock
                mutex.withLock {
                    val exchangeRates = cache[pair].orEmpty()

                    if (timeline.any { it !in exchangeRates }) {
                        cache[pair] = exchangeRateHostApi.fetchExchangeRates(
                            fromCurrency = fromCurrency,
                            toCurrency = toCurrency,
                            start = timeline.first(),
                            end = timeline.last(),
                        ).toSortedMap()
                    }
                }
            }
        }
    }

    fun convert(amount: Double, fromCurrency: String, toCurrency: String, datetime: OffsetDateTime): Double {
        if (fromCurrency == toCurrency) {
            return amount
        }

        val date = datetime.toLocalDate()

        val exchangeRates = cache[fromCurrency to toCurrency] ?: return 0.0
        val exchangeRate = exchangeRates[date] ?: run {
            val previousExchangeRate = exchangeRates.headMap(date).takeIf { it.isNotEmpty() }?.let { exchangeRates[it.lastKey()] }
            val nextExchangeRate = exchangeRates.tailMap(date).takeIf { it.isNotEmpty() }?.let { exchangeRates[it.firstKey()] }

            listOfNotNull(previousExchangeRate, nextExchangeRate)
                .average()
                .takeIf { it.isFinite() }
                ?: 0.0
        }

        return amount * exchangeRate
    }
}
