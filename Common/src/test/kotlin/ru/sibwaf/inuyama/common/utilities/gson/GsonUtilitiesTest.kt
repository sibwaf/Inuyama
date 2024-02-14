package ru.sibwaf.inuyama.common.utilities.gson

import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.util.concurrent.Executors

class GsonUtilitiesTest {

    @Test
    fun `check toJsonReader short`() {
        val obj = mapOf(
            "a" to "1",
            "b" to "2",
        )

        val gson = Gson()

        val result = Executors.newSingleThreadExecutor().asCoroutineDispatcher().use { dispatcher ->
            val reader = gson.toJsonReader(obj, CoroutineScope(dispatcher))
            gson.fromJson<Map<String, String>>(reader)
        }

        expectThat(result).isEqualTo(obj)
    }

    @Test
    fun `check toJsonReader long`() {
        val obj = (0 until 100_000).map { "$it" }

        val gson = Gson()

        val result = Executors.newSingleThreadExecutor().asCoroutineDispatcher().use { dispatcher ->
            val reader = gson.toJsonReader(obj, CoroutineScope(dispatcher))
            gson.fromJson<List<String>>(reader)
        }

        expectThat(result).isEqualTo(obj)
    }
}
