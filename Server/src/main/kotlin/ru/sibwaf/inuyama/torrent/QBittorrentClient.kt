package ru.sibwaf.inuyama.torrent

import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.sibwaf.inuyama.TorrentClientConfiguration
import ru.sibwaf.inuyama.common.utilities.await
import ru.sibwaf.inuyama.common.utilities.successOrThrow

class QBittorrentClient(
    private val configuration: TorrentClientConfiguration,
    private val httpClient: OkHttpClient
) : TorrentClient {

    override suspend fun download(magnet: String, directory: String) {
        val request = Request.Builder()
            .url(configuration.url)
            .post(
                FormBody.Builder()
                    .addEncoded("urls", magnet)
                    .addEncoded("savepath", directory)
                    .build()
            )
            .build()

        httpClient
            .newCall(request)
            .await()
            .successOrThrow()
    }
}
