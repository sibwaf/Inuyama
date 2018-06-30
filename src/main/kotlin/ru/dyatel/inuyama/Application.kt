package ru.dyatel.inuyama

import android.app.Application
import android.content.Context
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
import org.kodein.di.generic.singleton
import ru.dyatel.inuyama.model.Directory
import ru.dyatel.inuyama.model.MyObjectBox
import ru.dyatel.inuyama.model.Network
import ru.dyatel.inuyama.overseer.OverseerConfiguration
import ru.dyatel.inuyama.transmission.TorrentClient
import ru.dyatel.inuyama.transmission.TransmissionClient
import ru.dyatel.inuyama.transmission.TransmissionConfiguration

class Application : Application(), KodeinAware {

    override val kodein = Kodein.lazy {
        bind<Kodein>() with singleton { kodein }

        import(androidModule(this@Application))

        bind<NetworkManager>() with singleton { NetworkManager(instance()) }

        bind<BoxStore>() with singleton {
            MyObjectBox.builder()
                    .androidContext(instance<Context>())
                    .build()
        }

        bind<Box<Network>>() with singleton { instance<BoxStore>().boxFor<Network>() }
        bind<Box<Directory>>() with singleton { instance<BoxStore>().boxFor<Directory>() }

        bind<Gson>() with singleton { GsonBuilder().setPrettyPrinting().create() }
        bind<JsonParser>() with singleton { JsonParser() }

        bind<PreferenceHelper>() with singleton { PreferenceHelper(instance()) }
        bind<OverseerConfiguration>() with provider { instance<PreferenceHelper>().overseer }
        bind<TransmissionConfiguration>() with provider { instance<PreferenceHelper>().transmission }

        bind<Notifier>() with singleton { Notifier(instance()) }

        bind<TorrentClient>() with singleton { TransmissionClient(kodein) } // TODO: temporary fix

        import(rutrackerModule)
        import(nyaaModule)
    }

}
