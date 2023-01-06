package ru.dyatel.inuyama

import android.app.Application
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import io.objectbox.Box
import io.objectbox.BoxStore
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.androidXModule
import org.kodein.di.generic.allInstances
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.on
import org.kodein.di.generic.provider
import org.kodein.di.generic.setBinding
import org.kodein.di.generic.singleton
import ru.dyatel.inuyama.backup.BackupService
import ru.dyatel.inuyama.backup.MainBackupHandler
import ru.dyatel.inuyama.errors.ErrorLogManager
import ru.dyatel.inuyama.errors.errorModule
import ru.dyatel.inuyama.finance.financeModule
import ru.dyatel.inuyama.model.Directory
import ru.dyatel.inuyama.model.MyObjectBox
import ru.dyatel.inuyama.model.Network
import ru.dyatel.inuyama.model.Proxy
import ru.dyatel.inuyama.model.ProxyBinding
import ru.dyatel.inuyama.nyaa.nyaaModule
import ru.dyatel.inuyama.overseer.OverseerConfiguration
import ru.dyatel.inuyama.overseer.OverseerService
import ru.dyatel.inuyama.overseer.UpdateDispatchExecutor
import ru.dyatel.inuyama.pairing.DiscoveryService
import ru.dyatel.inuyama.pairing.PairedApi
import ru.dyatel.inuyama.pairing.PairedConnectionHolder
import ru.dyatel.inuyama.pairing.PairingManager
import ru.dyatel.inuyama.rutracker.rutrackerModule
import ru.dyatel.inuyama.utilities.PreferenceHelper
import ru.dyatel.inuyama.utilities.boxFor
import ru.sibwaf.inuyama.common.utilities.gson.registerDateTimeAdapter
import ru.sibwaf.inuyama.common.utilities.gson.registerPublicKeyAdapter
import ru.sibwaf.inuyama.common.utilities.gson.withNoJsonAnnotationSupport
import sibwaf.inuyama.app.common.ModuleScreenProvider
import sibwaf.inuyama.app.common.NetworkManager

class Application : Application(), KodeinAware {

    override val kodein = Kodein.lazy {
        import(androidXModule(this@Application))

        bind<BoxStore>() with singleton {
            MyObjectBox.builder()
                .androidContext(applicationContext)
                .build()
        }

        bind<Box<Network>>() with singleton { instance<BoxStore>().boxFor<Network>() }
        bind<Box<Proxy>>() with singleton { instance<BoxStore>().boxFor<Proxy>() }
        bind<Box<ProxyBinding>>() with singleton { instance<BoxStore>().boxFor<ProxyBinding>() }
        bind<Box<Directory>>() with singleton { instance<BoxStore>().boxFor<Directory>() }

        bind<MigrationRunner>() with singleton {
            MigrationRunner(
                store = instance(),
                preferenceHelper = instance(),
            )
        }

        bind<Gson>() with singleton {
            GsonBuilder()
                .withNoJsonAnnotationSupport()
                .registerDateTimeAdapter()
                .registerPublicKeyAdapter()
                .setPrettyPrinting()
                .create()
        }
        bind<JsonParser>() with singleton { JsonParser() }

        bind<PreferenceHelper>() with singleton {
            PreferenceHelper(
                context = applicationContext,
                gson = instance(),
            )
        }
        bind<OverseerConfiguration>() with provider { instance<PreferenceHelper>().overseer }

        bind<MainBackupHandler>() with singleton {
            MainBackupHandler(
                preferenceHelper = instance(),
                proxyRepository = instance(),
                proxyBindingRepository = instance(),
                directoryRepository = instance(),
                gson = instance(),
            )
        }

        bind<BackgroundServiceManager>() with singleton { BackgroundServiceManager() }

        bind<Notifier>() with singleton { Notifier(applicationContext) }

        bind<NetworkManager>() with singleton {
            NetworkManagerImpl(
                wifiManager = on(applicationContext).instance(),
                networkBox = instance(),
                proxyBindingBox = instance(),
            )
        }

        bind<DiscoveryService>() with singleton {
            DiscoveryService(
                networkManager = instance(),
                preferenceHelper = instance(),
            )
        }
        bind<PairingManager>() with singleton {
            PairingManager(
                discoveryService = instance(),
                preferenceHelper = instance(),
            )
        }
        bind<PairedConnectionHolder>() with singleton {
            PairedConnectionHolder(
                networkManager = instance(),
                pairingManager = instance(),
                gson = instance(),
            )
        }
        bind<PairedApi>() with singleton {
            PairedApi(
                pairedConnectionHolder = instance(),
                networkManager = instance(),
                gson = instance(),
            )
        }

        bind<UpdateDispatchExecutor>() with singleton { UpdateDispatchExecutor(instance()) }

        bind<BackupService>() with singleton {
            BackupService(
                pairedApi = instance(),
                backupHandlers = allInstances()
            )
        }
        bind<OverseerService>() with singleton {
            OverseerService(
                preferenceHelper = instance(),
                notifier = instance(),
                updateDispatchExecutor = instance(),
                watchers = allInstances()
            )
        }

        bind() from setBinding<ModuleScreenProvider>()
        import(errorModule)
        import(rutrackerModule)
        import(nyaaModule)
//        import(ruranobeModule)
        import(financeModule)
    }

    override fun onCreate() {
        super.onCreate()

        val errorLogManager by instance<ErrorLogManager>()
        errorLogManager.setupExceptionHandler()

        val migrationRunner by instance<MigrationRunner>()
        migrationRunner.migrate()

        val backgroundServiceManager by instance<BackgroundServiceManager>()
        backgroundServiceManager.onApplicationStart(applicationContext)
    }
}
