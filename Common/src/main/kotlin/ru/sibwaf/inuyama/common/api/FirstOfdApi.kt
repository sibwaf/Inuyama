package ru.sibwaf.inuyama.common.api

import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.sibwaf.inuyama.common.utilities.await
import ru.sibwaf.inuyama.common.utilities.successOrThrow

data class TicketItem(
    val name: String,
    val price: Double
)

class FirstOfdApi(
    val host: String = "https://consumer.1-ofd.ru",
) {

    private val gson = Gson()

    suspend fun getTicketItemsByQr(qrText: String, http: OkHttpClient): List<TicketItem> {
        val request = Request.Builder()
            .url("$host/api/tickets/ticket/$qrText")
            .build()

        return http
            .newCall(request)
            .await()
            .use { response ->
                response.successOrThrow()

                gson.fromJson(response.body!!.charStream(), TicketResponse::class.java)
                    .ticket
                    .items
                    .map {
                        TicketItem(
                            name = it.name,
                            price = it.sum / 100.0
                        )
                    }
            }
    }
}

private data class TicketResponse(
    val ticket: TicketResponseTicket
)

private data class TicketResponseTicket(
    val items: List<TicketResponseTicketItem>
)

private data class TicketResponseTicketItem(
    val name: String,
    val sum: Int
)
