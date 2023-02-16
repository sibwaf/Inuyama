package ru.sibwaf.inuyama.common.api

import com.google.gson.Gson
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.sibwaf.inuyama.common.utilities.await
import ru.sibwaf.inuyama.common.utilities.gson.fromJson
import ru.sibwaf.inuyama.common.utilities.successOrThrow
import java.time.LocalDate

class ExchangeRateHostApi(
    private val httpClient: OkHttpClient,
    private val gson: Gson,
) {

    @Suppress("NewApi")
    suspend fun fetchExchangeRates(fromCurrency: String, toCurrency: String, start: LocalDate, end: LocalDate): Map<LocalDate, Double> {
        val normalizedFromCurrency = fromCurrency.trim().uppercase()
        val normalizedToCurrency = toCurrency.trim().uppercase()

        val url = HttpUrl.Builder()
            .scheme("https")
            .host("api.exchangerate.host")
            .addPathSegment("timeseries")
            .addQueryParameter("start_date", start.toString())
            .addQueryParameter("end_date", end.toString())
            .addQueryParameter("base", normalizedFromCurrency)
            .addQueryParameter("symbols", normalizedToCurrency)
            .build()

        val request = Request.Builder()
            .get()
            .url(url)
            .build()

        return httpClient.newCall(request).await().use { response ->
            response.successOrThrow()

//            {
//                ...,
//                "rates": {
//                    "2020-01-01": {
//                        "USD": 0.016158
//                    },
//                    "2020-01-02": {
//                        "USD": 0.016265
//                    },
//                    "2020-01-03": {
//                        "USD": 0.016207
//                    }
//                }
//            }

            gson.fromJson<TimeseriesResponseDto>(response.body!!.charStream())
                .rates
                .asSequence()
                .flatMap { (date, values) ->
                    values.asSequence()
                        .filter { (currency, _) -> currency == normalizedToCurrency }
                        .map { (_, exchangeRate) -> date to exchangeRate }
                }
                .toMap()
        }
    }

    suspend fun fetchAvailableCurrencies(): Set<String> {
        val request = Request.Builder()
            .get()
            .url("https://api.exchangerate.host/symbols")
            .build()

        return httpClient.newCall(request).await().use { response ->
            response.successOrThrow()

            gson.fromJson<SymbolsResponseDto>(response.body!!.charStream())
                .symbols
                .keys
                .toSet()
        }
    }
}

private data class TimeseriesResponseDto(
    val rates: Map<LocalDate, Map<String, Double>>,
)

private data class SymbolsResponseDto(
    val symbols: Map<String, Any>
)
