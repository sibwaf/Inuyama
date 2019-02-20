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
import ru.dyatel.inuyama.pairing.DiscoverResponseListener
import ru.dyatel.inuyama.pairing.PairedApi
import ru.dyatel.inuyama.pairing.PairingManager
import ru.dyatel.inuyama.utilities.NoJson
import ru.dyatel.inuyama.utilities.PreferenceHelper
import ru.dyatel.inuyama.utilities.boxFor
import ru.sibwaf.inuyama.common.utilities.Cryptography
import java.security.PublicKey

class Application : Application(), KodeinAware {

    override val kodein = Kodein.lazy {
        import(androidModule(this@Application))

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
                    .registerTypeAdapter(PublicKey::class.java, object : TypeAdapter<PublicKey>() {
                        override fun write(writer: JsonWriter, value: PublicKey?) {
                            if (value == null) {
                                writer.nullValue()
                                return
                            }

                            writer.value(Cryptography.encodeRSAPublicKeyBase64(value))
                        }

                        override fun read(reader: JsonReader): PublicKey? {
                            val value = reader.nextString() ?: return null
                            return Cryptography.decodeRSAPublicKeyBase64(value)
                        }
                    })
                    .setPrettyPrinting()
                    .create()
        }
        bind<JsonParser>() with singleton { JsonParser() }

        bind<PreferenceHelper>() with singleton { PreferenceHelper(instance()) }
        bind<OverseerConfiguration>() with provider { instance<PreferenceHelper>().overseer }

        bind<Notifier>() with singleton { Notifier(kodein) }

        bind<NetworkManager>() with singleton { NetworkManager(kodein) }

        bind<DiscoverResponseListener>() with singleton { DiscoverResponseListener(kodein) }
        bind<PairingManager>() with singleton { PairingManager(kodein) }
        bind<PairedApi>() with singleton { PairedApi(kodein) }

        bind() from setBinding<ModuleScreenProvider>()
        import(rutrackerModule)
        import(nyaaModule)
        import(ruranobeModule)
        import(financeModule)
    }

}
