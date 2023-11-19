package ru.sibwaf.inuyama.common.api

import com.google.gson.Gson
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.sibwaf.inuyama.common.utilities.await
import ru.sibwaf.inuyama.common.utilities.gson.fromJson
import ru.sibwaf.inuyama.common.utilities.successOrThrow
import java.time.LocalDate

class ExchangeRateHostApiImpl(
    private val httpClient: OkHttpClient,
    private val gson: Gson,
    private val token: String,
) : ExchangeRateHostApi {

    override suspend fun getExchangeRates(
        fromCurrency: String,
        toCurrencies: Set<String>,
        date: LocalDate,
    ): Map<String, Double> {
        val normalizedFromCurrency = fromCurrency.trim().uppercase()
        val normalizedToCurrencies = toCurrencies.map { it.trim().uppercase() }

        val url = HttpUrl.Builder()
            .scheme("http")
            .host("api.exchangerate.host")
            .addPathSegment("historical")
            .addQueryParameter("date", date.toString())
            .addQueryParameter("source", normalizedFromCurrency)
            .addQueryParameter("currencies", normalizedToCurrencies.joinToString(","))
            .addQueryParameter("access_key", token)
            .build()

        val request = Request.Builder()
            .get()
            .url(url)
            .build()

        return httpClient.newCall(request).await().use { response ->
            response.successOrThrow()

            val body = gson.fromJson<HistoricalResponseDto>(response.body!!.charStream())
            if (!body.success) {
                throw ExchangeRateHostApiException(
                    code = body.error?.code ?: ExchangeRateHostApiException.UNKNOWN,
                    message = body.error?.info ?: "Request failed, but no error info was provided",
                )
            }

            body.quotes.mapKeys { (key, _) -> key.removePrefix(fromCurrency) }
        }
    }

    override suspend fun getAvailableCurrencies(): Set<String> {
        val url = HttpUrl.Builder()
            .scheme("http")
            .host("api.exchangerate.host")
            .addPathSegment("list")
            .addQueryParameter("access_key", token)
            .build()

        val request = Request.Builder()
            .get()
            .url(url)
            .build()

        return httpClient.newCall(request).await().use { response ->
            response.successOrThrow()

            val body = gson.fromJson<ListResponseDto>(response.body!!.charStream())
            if (!body.success) {
                throw ExchangeRateHostApiException(
                    code = body.error?.code ?: ExchangeRateHostApiException.UNKNOWN,
                    message = body.error?.info ?: "Request failed, but no error info was provided",
                )
            }

            body.currencies.keys
        }
    }
}

private data class HistoricalResponseDto(
    val success: Boolean,
    val quotes: Map<String, Double>,
    val error: ErrorDto?,
)

private data class ListResponseDto(
    val success: Boolean,
    val currencies: Map<String, String>,
    val error: ErrorDto?,
)

private data class ErrorDto(
    val code: Int,
    val info: String,
)

class ExchangeRateHostApiException(
    val code: Int,
    message: String,
) : RuntimeException(message) {

    companion object ErrorCode {
        const val UNKNOWN = -1
    }
}
