package ru.sibwaf.inuyama

import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.zaxxer.hikari.HikariDataSource
import org.kodein.di.Kodein
import org.kodein.di.direct
import org.kodein.di.generic.allInstances
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton
import org.slf4j.LoggerFactory
import ru.sibwaf.inuyama.backup.backupModule
import ru.sibwaf.inuyama.http.httpModule
import ru.sibwaf.inuyama.torrent.torrentModule
import java.nio.file.Files
import java.nio.file.Paths
import javax.sql.DataSource

private val kodein = Kodein.lazy {
    bind<Gson>() with singleton { Gson() }
    bind<JsonParser>() with singleton { JsonParser() }

    bind<InuyamaConfiguration>() with singleton {
        val gson = GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES)
            .create()

        val configurationPath = Paths.get("configuration.json")
        return@singleton if (Files.exists(configurationPath)) {
            val configurationText = Files.readAllLines(configurationPath).joinToString("\n")
            gson.fromJson(configurationText)
        } else {
            InuyamaConfiguration()
        }
    }

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
    bind<SessionManager>() with singleton { SessionManager() }
    bind<PairingManager>() with singleton { PairingManager(kodein) }

    import(httpModule)
    import(torrentModule)
    import(backupModule)
}

fun main() {
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

    kodein.direct.instance<PairingManager>().start()
}