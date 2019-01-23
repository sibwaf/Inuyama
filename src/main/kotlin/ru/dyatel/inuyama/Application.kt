package ru.dyatel.inuyama

import android.app.Application
import android.content.Context
import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import io.objectbox.Box
import io.objectbox.BoxStore
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.androidModule
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
import ru.dyatel.inuyama.transmission.TorrentClient
import ru.dyatel.inuyama.transmission.TransmissionClient
import ru.dyatel.inuyama.transmission.TransmissionConfiguration
import ru.dyatel.inuyama.utilities.NoJson
import ru.dyatel.inuyama.utilities.PreferenceHelper
import ru.dyatel.inuyama.utilities.boxFor

class Application : Application(), KodeinAware {

    override val kodein = Kodein.lazy {
        import(androidModule(this@Application))

        bind<NetworkManager>() with singleton { NetworkManager(kodein) }

        bind<BoxStore>() with singleton {
            MyObjectBox.builder()
                    .androidContext(instance<Context>())
                    .build()
        }

        bind<Box<Network>>() with singleton { instance<BoxStore>().boxFor<Network>() }
        bind<Box<Proxy>>() with singleton { instance<BoxStore>().boxFor<Proxy>() }
        bind<Box<ProxyBinding>>() with singleton { instance<BoxStore>().boxFor<ProxyBinding>() }
        bind<Box<Directory>>() with singleton { instance<BoxStore>().boxFor<Directory>() }

        bind<Gson>() with singleton {
            GsonBuilder()
                    .setExclusionStrategies(object : ExclusionStrategy {
                        override fun shouldSkipClass(clazz: Class<*>) = false
                        override fun shouldSkipField(f: FieldAttributes) = f.getAnnotation(NoJson::class.java) != null
                    })
                    .setPrettyPrinting()
                    .create()
        }
        bind<JsonParser>() with singleton { JsonParser() }

        bind<PreferenceHelper>() with singleton { PreferenceHelper(instance()) }
        bind<OverseerConfiguration>() with provider { instance<PreferenceHelper>().overseer }
        bind<TransmissionConfiguration>() with singleton { instance<PreferenceHelper>().transmission }

        bind<Notifier>() with singleton { Notifier(kodein) }

        bind<TorrentClient>() with singleton { TransmissionClient(kodein) }

        bind() from setBinding<ModuleScreenProvider>()
        import(rutrackerModule)
        import(nyaaModule)
        import(ruranobeModule)
    }

}
