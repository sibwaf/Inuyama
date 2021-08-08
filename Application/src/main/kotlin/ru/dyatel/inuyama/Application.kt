package ru.dyatel.inuyama

import android.app.Application
import android.content.Context
import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import io.objectbox.Box
import io.objectbox.BoxStore
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.androidXModule
import org.kodein.di.generic.allInstances
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.provider
import org.kodein.di.generic.setBinding
import org.kodein.di.generic.singleton
import ru.dyatel.inuyama.model.Directory
import ru.dyatel.inuyama.model.MyObjectBox
import ru.dyatel.inuyama.model.Network
import ru.dyatel.inuyama.model.Proxy
import ru.dyatel.inuyama.model.ProxyBinding
import ru.dyatel.inuyama.overseer.OverseerConfiguration
import ru.dyatel.inuyama.overseer.OverseerService
import ru.dyatel.inuyama.overseer.UpdateDispatchExecutor
import ru.dyatel.inuyama.pairing.DiscoveryService
import ru.dyatel.inuyama.pairing.PairedApi
import ru.dyatel.inuyama.pairing.PairingManager
import ru.dyatel.inuyama.utilities.NoJson
import ru.dyatel.inuyama.utilities.PreferenceHelper
import ru.dyatel.inuyama.utilities.boxFor
import ru.sibwaf.inuyama.common.utilities.Encoding
import sibwaf.inuyama.app.common.ModuleScreenProvider
import sibwaf.inuyama.app.common.NetworkManager
import java.security.PublicKey

class Application : Application(), KodeinAware {

    override val kodein = Kodein.lazy {
        import(androidXModule(this@Application))

        bind<BoxStore>() with singleton {
            MyObjectBox.builder()
                .androidContext(instance<Context>())
                .build()
        }

        bind<Box<Network>>() with singleton { instance<BoxStore>().boxFor<Network>() }
        bind<Box<Proxy>>() with singleton { instance<BoxStore>().boxFor<Proxy>() }
        bind<Box<ProxyBinding>>() with singleton { instance<BoxStore>().boxFor<ProxyBinding>() }
        bind<Box<Directory>>() with singleton { instance<BoxStore>().boxFor<Directory>() }

        bind<MigrationRunner>() with singleton { MigrationRunner(kodein) }

        bind<Gson>() with singleton {
            GsonBuilder()
                .setExclusionStrategies(object : ExclusionStrategy {
                    override fun shouldSkipClass(clazz: Class<*>) = false
                    override fun shouldSkipField(f: FieldAttributes) = f.getAnnotation(NoJson::class.java) != null
                })
                .registerTypeAdapter(PublicKey::class.java, object : TypeAdapter<PublicKey>() {
                    override fun write(writer: JsonWriter, value: PublicKey?) {
                        if (value == null) {
                            writer.nullValue()
                            return
                        }

                        writer.value(Encoding.encodeBase64(Encoding.encodeRSAPublicKey(value)))
                    }

                    override fun read(reader: JsonReader): PublicKey? {
                        val value = reader.nextString() ?: return null
                        return Encoding.decodeRSAPublicKey(Encoding.decodeBase64(value))
                    }
                })
                .setPrettyPrinting()
                .create()
        }
        bind<JsonParser>() with singleton { JsonParser() }

        bind<PreferenceHelper>() with singleton { PreferenceHelper(instance()) }
        bind<OverseerConfiguration>() with provider { instance<PreferenceHelper>().overseer }

        bind<BackgroundServiceManager>() with singleton { BackgroundServiceManager() }

        bind<Notifier>() with singleton { Notifier(kodein) }

        bind<NetworkManager>() with singleton { NetworkManagerImpl(kodein) }

        bind<DiscoveryService>() with singleton { DiscoveryService(kodein) }
        bind<PairingManager>() with singleton { PairingManager(kodein) }
        bind<PairedApi>() with singleton { PairedApi(kodein) }

        bind<UpdateDispatchExecutor>() with singleton { UpdateDispatchExecutor(instance()) }

        bind<OverseerService>() with singleton {
            OverseerService(
                preferenceHelper = instance(),
                notifier = instance(),
                updateDispatchExecutor = instance(),
                watchers = allInstances()
            )
        }

        bind() from setBinding<ModuleScreenProvider>()
        import(rutrackerModule)
        import(nyaaModule)
//        import(ruranobeModule)
        import(financeModule)
    }

    override fun onCreate() {
        super.onCreate()

        val migrationRunner by instance<MigrationRunner>()
        migrationRunner.migrate()
    }
}
