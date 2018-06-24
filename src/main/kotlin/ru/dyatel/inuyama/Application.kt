package ru.dyatel.inuyama

import android.app.Application
import android.content.Context
import io.objectbox.Box
import io.objectbox.BoxStore
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.androidModule
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton
import ru.dyatel.inuyama.model.MyObjectBox
import ru.dyatel.inuyama.model.Network

class Application : Application(), KodeinAware {

    override val kodein = Kodein.lazy {
        import(androidModule(this@Application))

        bind<NetworkManager>() with singleton { NetworkManager(kodein) }

        bind<BoxStore>() with singleton {
            MyObjectBox.builder()
                    .androidContext(instance<Context>())
                    .build()
        }

        bind<Box<Network>>() with singleton { instance<BoxStore>().boxFor(Network::class.java) }
    }

}
