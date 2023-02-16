package ru.sibwaf.inuyama.common.api

import com.google.gson.GsonBuilder
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Test
import ru.sibwaf.inuyama.common.utilities.gson.registerJavaTimeAdapters
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.containsKeys
import strikt.assertions.hasSize
import strikt.assertions.isGreaterThan
import strikt.assertions.isNotEmpty
import strikt.assertions.withValue
import java.time.LocalDate

class ExchangeRateHostApiTest {

    private val httpClient = OkHttpClient()
    private val gson = GsonBuilder()
        .registerJavaTimeAdapters()
        .create()

    @Test
    fun fetchExchangeRates() {
        val api = ExchangeRateHostApi(httpClient, gson)

        val fromCurrency = "USD"
        val toCurrency = "EUR"
        val start = LocalDate.of(2022, 1, 1)
        val end = LocalDate.of(2022, 1, 5)

        val expectedDates = generateSequence(start) { it.plusDays(1) }.takeWhile { it <= end }.toList()

        val rates = runBlocking {
            api.fetchExchangeRates(
                fromCurrency = fromCurrency,
                toCurrency = toCurrency,
                start = start,
                end = end,
            )
        }

        expectThat(rates) {
            hasSize(expectedDates.size)
            containsKeys(*expectedDates.toTypedArray())
            for (date in expectedDates) {
                withValue(date) { isGreaterThan(0.0) }
            }
        }
    }

    @Test
    fun fetchAvailableCurrencies() {
        val api = ExchangeRateHostApi(httpClient, gson)
        val currencies = runBlocking { api.fetchAvailableCurrencies() }

        expectThat(currencies) {
            isNotEmpty()
            contains("USD")
            contains("EUR")
        }
    }
}
