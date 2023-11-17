package ru.sibwaf.inuyama.finance

import kotlinx.coroutines.runBlocking
import ru.sibwaf.inuyama.finance.rates.CrossExchangeRateProvider
import ru.sibwaf.inuyama.finance.rates.ExchangeRateProvider
import strikt.api.expect
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import java.time.LocalDate
import kotlin.test.Test

private const val CURRENCY_A = "A"
private const val CURRENCY_B = "B"
private const val CURRENCY_C = "C"

private const val TOLERANCE = 0.001

class CrossExchangeRateProviderTest {

    @Test
    fun `Test main=A, B to B`() = runBlocking {
        val mockRateProvider = MockExchangeRateProvider()
        val rateProvider = CrossExchangeRateProvider(CURRENCY_A, mockRateProvider)

        val rate = rateProvider.getExchangeRate(
            fromCurrency = CURRENCY_B,
            toCurrency = CURRENCY_B,
            date = LocalDate.now(),
        )

        expect {
            that(rate).isNotNull().isEqualTo(1.0, TOLERANCE)
            that(mockRateProvider.calls).isEmpty()
        }
    }

    @Test
    fun `Test main=A, A to B`() = runBlocking {
        val mockRateProvider = MockExchangeRateProvider()
        val rateProvider = CrossExchangeRateProvider(CURRENCY_A, mockRateProvider)

        val rate = rateProvider.getExchangeRate(
            fromCurrency = CURRENCY_A,
            toCurrency = CURRENCY_B,
            date = LocalDate.now(),
        )

        expect {
            that(rate).isNotNull().isEqualTo(2.0, TOLERANCE)
            that(mockRateProvider.calls).containsExactlyInAnyOrder(CURRENCY_A to CURRENCY_B)
        }
    }

    @Test
    fun `Test main=A, B to A`() = runBlocking {
        val mockRateProvider = MockExchangeRateProvider()
        val rateProvider = CrossExchangeRateProvider(CURRENCY_A, mockRateProvider)

        val rate = rateProvider.getExchangeRate(
            fromCurrency = CURRENCY_B,
            toCurrency = CURRENCY_A,
            date = LocalDate.now(),
        )

        expect {
            that(rate).isNotNull().isEqualTo(0.5, TOLERANCE)
            that(mockRateProvider.calls).containsExactlyInAnyOrder(CURRENCY_A to CURRENCY_B)
        }
    }

    @Test
    fun `Test main=A, B to C`() = runBlocking {
        val mockRateProvider = MockExchangeRateProvider()
        val rateProvider = CrossExchangeRateProvider(CURRENCY_A, mockRateProvider)

        val rate = rateProvider.getExchangeRate(
            fromCurrency = CURRENCY_B,
            toCurrency = CURRENCY_C,
            date = LocalDate.now(),
        )

        expect {
            that(rate).isNotNull().isEqualTo(2.0, TOLERANCE)
            that(mockRateProvider.calls).containsExactlyInAnyOrder(
                CURRENCY_A to CURRENCY_B,
                CURRENCY_A to CURRENCY_C,
            )
        }
    }
}

private class MockExchangeRateProvider : ExchangeRateProvider {

    private val _calls = mutableListOf<Pair<String, String>>()
    val calls get() = _calls

    override suspend fun getExchangeRate(fromCurrency: String, toCurrency: String, date: LocalDate): Double {
        calls += fromCurrency to toCurrency
        return when {
            fromCurrency == CURRENCY_A && toCurrency == CURRENCY_A -> 1.0
            fromCurrency == CURRENCY_A && toCurrency == CURRENCY_B -> 2.0
            fromCurrency == CURRENCY_A && toCurrency == CURRENCY_C -> 4.0

            fromCurrency == CURRENCY_B && toCurrency == CURRENCY_A -> 0.5
            fromCurrency == CURRENCY_B && toCurrency == CURRENCY_B -> 1.0
            fromCurrency == CURRENCY_B && toCurrency == CURRENCY_C -> 2.0

            fromCurrency == CURRENCY_C && toCurrency == CURRENCY_A -> 0.25
            fromCurrency == CURRENCY_C && toCurrency == CURRENCY_B -> 0.5
            fromCurrency == CURRENCY_C && toCurrency == CURRENCY_C -> 1.0

            else -> throw IllegalArgumentException("Unsupported conversion $fromCurrency -> $toCurrency")
        }
    }
}
