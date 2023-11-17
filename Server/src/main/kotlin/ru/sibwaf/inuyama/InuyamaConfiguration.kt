package ru.sibwaf.inuyama

import ru.sibwaf.inuyama.common.Pairing
import ru.sibwaf.inuyama.configuration.ConfigurationSource
import ru.sibwaf.inuyama.configuration.wrapped
import ru.sibwaf.inuyama.torrent.TorrentClientType

data class InuyamaConfiguration(
    val discoveryPort: Int = Pairing.DEFAULT_DISCOVER_SERVER_PORT,
    val serverPort: Int = Pairing.DEFAULT_DISCOVER_SERVER_PORT + 1,
    val database: DbConfiguration? = null,
    val webAuth: WebAuthConfiguration? = null,
    val torrent: TorrentClientConfiguration? = null,
    val exchangeRateHostToken: String,
) {

    companion object {
        fun from(configuration: ConfigurationSource): InuyamaConfiguration {
            val configurationWrapper = configuration.wrapped()

            val databaseConfiguration = configurationWrapper.getString("inuyama.database.url")?.let { url ->
                DbConfiguration(
                    url = url,
                    username = configurationWrapper.requireString("inuyama.database.username"),
                    password = configurationWrapper.getString("inuyama.database.password"),
                )
            }

            val torrentConfiguration = configurationWrapper.getEnum<TorrentClientType>("inuyama.torrent.type")?.let { type ->
                TorrentClientConfiguration(
                    type = type,
                    url = configurationWrapper.requireString("inuyama.torrent.url"),
                    username = configurationWrapper.requireString("inuyama.torrent.username"),
                    password = configurationWrapper.requireString("inuyama.torrent.password"),
                )
            }

            val authenticationConfiguration = configurationWrapper.getString("inuyama.authentication.username")?.let { username ->
                WebAuthConfiguration(
                    username = username,
                    password = configurationWrapper.requireString("inuyama.authentication.password"),
                )
            }

            return InuyamaConfiguration(
                discoveryPort = configurationWrapper.getInt("inuyama.server.discovery-port") ?: Pairing.DEFAULT_DISCOVER_SERVER_PORT,
                serverPort = configurationWrapper.getInt("inuyama.server.api-port") ?: (Pairing.DEFAULT_DISCOVER_SERVER_PORT + 1),
                database = databaseConfiguration,
                torrent = torrentConfiguration,
                webAuth = authenticationConfiguration,
                exchangeRateHostToken = configurationWrapper.requireString("inuyama.exchangeratehost.token")
            )
        }
    }

}

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
