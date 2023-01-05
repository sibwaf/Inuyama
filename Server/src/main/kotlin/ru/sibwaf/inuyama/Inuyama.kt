package ru.sibwaf.inuyama

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.zaxxer.hikari.HikariDataSource
import okhttp3.OkHttpClient
import org.kodein.di.Kodein
import org.kodein.di.direct
import org.kodein.di.generic.allInstances
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton
import org.slf4j.LoggerFactory
import ru.sibwaf.inuyama.backup.backupModule
import ru.sibwaf.inuyama.common.api.ExchangeRateHostApi
import ru.sibwaf.inuyama.common.utilities.gson.registerDateTimeAdapter
import ru.sibwaf.inuyama.common.utilities.gson.registerJavaTimeAdapters
import ru.sibwaf.inuyama.common.utilities.gson.withCaseInsensitiveEnums
import ru.sibwaf.inuyama.configuration.CombinedConfigurationSource
import ru.sibwaf.inuyama.configuration.CommandLineConfigurationSource
import ru.sibwaf.inuyama.configuration.EnvironmentConfigurationSource
import ru.sibwaf.inuyama.configuration.PropertiesConfigurationSource
import ru.sibwaf.inuyama.errors.pairedErrorModule
import ru.sibwaf.inuyama.finance.financeModule
import ru.sibwaf.inuyama.http.httpModule
import ru.sibwaf.inuyama.pairing.pairingModule
import ru.sibwaf.inuyama.torrent.torrentModule
import java.nio.file.Paths
import javax.sql.DataSource

fun main(args: Array<String>) {
    val configurationSource = CombinedConfigurationSource(
        PropertiesConfigurationSource(Paths.get("application.properties")),
        EnvironmentConfigurationSource(),
        CommandLineConfigurationSource(args.asList()),
    )

    val kodein = Kodein.lazy {
        bind<Gson>() with singleton {
            GsonBuilder()
                .registerJavaTimeAdapters()
                .registerDateTimeAdapter()
                .withCaseInsensitiveEnums()
                .create()
        }
        bind<JsonParser>() with singleton { JsonParser() }

        bind<InuyamaConfiguration>() with singleton { InuyamaConfiguration.from(configurationSource) }

        // TODO: provide "is database available property"

        bind<DataSource>() with singleton {
            HikariDataSource().apply {
                val configuration = instance<InuyamaConfiguration>().database!!
                jdbcUrl = configuration.url
                username = configuration.username
                password = configuration.password
            }
        }

        bind<KeyKeeper>() with singleton { KeyKeeper() }

        bind<OkHttpClient>() with singleton { OkHttpClient() }

        bind<ExchangeRateHostApi>() with singleton {
            ExchangeRateHostApi(
                httpClient = instance(),
                gson = instance(),
            )
        }

        import(pairingModule)
        import(httpModule)
        import(pairedErrorModule)
        import(torrentModule)
        import(backupModule)
        import(financeModule)
    }

    // TODO: data replication by marking "last update" on entities
    // TODO: handle deletion somehow

    val logger = LoggerFactory.getLogger("Main")

    val configuration by kodein.instance<InuyamaConfiguration>()
    if (configuration.database == null) {
        logger.warn("DB connection is not configured, some features won't be available")
    }

    for (module in kodein.direct.allInstances<Module>()) {
        module.install()
    }
}
