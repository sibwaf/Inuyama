package ru.sibwaf.inuyama

import ru.sibwaf.inuyama.common.Pairing
import ru.sibwaf.inuyama.torrent.TorrentClientType

data class InuyamaConfiguration(
    val discoveryPort: Int = Pairing.DEFAULT_DISCOVER_SERVER_PORT,
    val serverPort: Int = Pairing.DEFAULT_DISCOVER_SERVER_PORT + 1,
    val database: DbConfiguration? = null,
    val webAuth: WebAuthConfiguration? = null,
    val torrent: TorrentClientConfiguration? = null
)

data class DbConfiguration(
    val url: String,
    val username: String,
    val password: String? = null
)

data class WebAuthConfiguration(
    val username: String,
    val password: String
)

data class TorrentClientConfiguration(
    val type: TorrentClientType,
    val url: String,
    val username: String,
    val password: String
)
