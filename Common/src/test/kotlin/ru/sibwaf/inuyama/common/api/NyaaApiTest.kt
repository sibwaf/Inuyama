package ru.sibwaf.inuyama.common.api

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.all
import strikt.assertions.containsIgnoringCase
import strikt.assertions.isNotEmpty
import strikt.assertions.startsWith

class NyaaApiTest {

    private val httpClient by lazy { OkHttpClient() }

    @Test
    fun testQuery() {
        runBlocking {
            val api = NyaaApi()

            val torrents = api.query("horriblesubs okaa-san online 1080p", httpClient)

            expectThat(torrents) {
                isNotEmpty()

                all {
                    get { title }.and {
                        containsIgnoringCase("horriblesubs")
                        containsIgnoringCase("okaa-san")
                        containsIgnoringCase("1080p")
                    }
                }
            }
        }
    }

    @Test
    fun testMagnetExtraction() {
        runBlocking {
            val api = NyaaApi()

            val torrents = api.query("", httpClient)
            val magnet = api.getMagnet(torrents.first(), httpClient)

            expectThat(magnet) { startsWith("magnet:") }
        }
    }
}