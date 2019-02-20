package ru.dyatel.inuyama

import android.content.Context
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.typeface.IIcon
import com.wealthfront.magellan.Screen
import io.objectbox.Box
import io.objectbox.BoxStore
import org.jsoup.Connection
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.inSet
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton
import ru.dyatel.inuyama.model.FinanceAccount
import ru.dyatel.inuyama.model.FinanceCategory
import ru.dyatel.inuyama.model.FinanceOperation
import ru.dyatel.inuyama.model.NyaaTorrent
import ru.dyatel.inuyama.model.NyaaWatch
import ru.dyatel.inuyama.model.RuranobeProject
import ru.dyatel.inuyama.model.RuranobeVolume
import ru.dyatel.inuyama.model.RutrackerWatch
import ru.dyatel.inuyama.model.Update
import ru.dyatel.inuyama.nyaa.NyaaApi
import ru.dyatel.inuyama.nyaa.NyaaScreen
import ru.dyatel.inuyama.nyaa.NyaaWatcher
import ru.dyatel.inuyama.ruranobe.RuranobeApi
import ru.dyatel.inuyama.ruranobe.RuranobeScreen
import ru.dyatel.inuyama.ruranobe.RuranobeWatcher
import ru.dyatel.inuyama.rutracker.RutrackerApi
import ru.dyatel.inuyama.rutracker.RutrackerConfiguration
import ru.dyatel.inuyama.rutracker.RutrackerScreen
import ru.dyatel.inuyama.rutracker.RutrackerWatcher
import ru.dyatel.inuyama.utilities.PreferenceHelper
import ru.dyatel.inuyama.utilities.boxFor

interface RemoteService {

    val networkManager: NetworkManager

    fun getName(context: Context): String
    fun checkConnection(): Boolean

    fun createConnection(url: String, trustedOnly: Boolean): Connection {
        return networkManager.createJsoupConnection(url, trustedOnly)
    }
}

interface ProxyableRemoteService : RemoteService {

    val serviceId: Long

    override fun createConnection(url: String, trustedOnly: Boolean): Connection {
        return networkManager.createProxiedJsoupConnection(url, trustedOnly, serviceId)
    }
}

abstract class Watcher {

    private val listeners = mutableListOf<() -> Unit>()

    abstract fun checkUpdates(): List<String>
    abstract fun dispatchUpdates(dispatcher: UpdateDispatcher)
    abstract fun listUpdates(): List<Update>

    fun addUpdateListener(listener: () -> Unit) {
        listeners += listener
    }

    fun removeUpdateListener(listener: () -> Unit) {
        listeners -= listener
    }

    protected fun notifyListeners() {
        listeners.forEach { it() }
    }

}

abstract class ModuleScreenProvider {
    abstract fun getIcon(): IIcon
    abstract fun getTitle(context: Context): String
    abstract fun getScreenClass(): Class<out Screen<*>>
}

val rutrackerModule = Kodein.Module("rutracker") {
    bind<Box<RutrackerWatch>>() with singleton { instance<BoxStore>().boxFor<RutrackerWatch>() }
    bind<RutrackerConfiguration>() with singleton { instance<PreferenceHelper>().rutracker }
    bind<RutrackerApi>() with singleton { RutrackerApi(kodein) }
    bind<RutrackerWatcher>() with singleton { RutrackerWatcher(kodein) }

    bind<ModuleScreenProvider>().inSet() with singleton {
        object : ModuleScreenProvider() {
            override fun getIcon() = CommunityMaterial.Icon.cmd_database
            override fun getTitle(context: Context) = context.getString(R.string.module_rutracker)
            override fun getScreenClass() = RutrackerScreen::class.java
        }
    }
}

val nyaaModule = Kodein.Module("nyaa") {
    bind<Box<NyaaTorrent>>() with singleton { instance<BoxStore>().boxFor<NyaaTorrent>() }
    bind<Box<NyaaWatch>>() with singleton { instance<BoxStore>().boxFor<NyaaWatch>() }
    bind<NyaaApi>() with singleton { NyaaApi(kodein) }
    bind<NyaaWatcher>() with singleton { NyaaWatcher(kodein) }

    bind<ModuleScreenProvider>().inSet() with singleton {
        object : ModuleScreenProvider() {
            override fun getIcon() = CommunityMaterial.Icon.cmd_database
            override fun getTitle(context: Context) = context.getString(R.string.module_nyaa)
            override fun getScreenClass() = NyaaScreen::class.java
        }
    }
}

val ruranobeModule = Kodein.Module("ruranobe") {
    bind<Box<RuranobeProject>>() with singleton { instance<BoxStore>().boxFor<RuranobeProject>() }
    bind<Box<RuranobeVolume>>() with singleton { instance<BoxStore>().boxFor<RuranobeVolume>() }
    bind<RuranobeApi>() with singleton { RuranobeApi(kodein) }
    bind<RuranobeWatcher>() with singleton { RuranobeWatcher(kodein) }

    bind<ModuleScreenProvider>().inSet() with singleton {
        object : ModuleScreenProvider() {
            override fun getIcon() = CommunityMaterial.Icon.cmd_book
            override fun getTitle(context: Context) = context.getString(R.string.module_ruranobe)
            override fun getScreenClass() = RuranobeScreen::class.java
        }
    }
}

val financeModule = Kodein.Module("finance") {
    bind<Box<FinanceAccount>>() with singleton { instance<BoxStore>().boxFor<FinanceAccount>() }
    bind<Box<FinanceCategory>>() with singleton { instance<BoxStore>().boxFor<FinanceCategory>() }
    bind<Box<FinanceOperation>>() with singleton { instance<BoxStore>().boxFor<FinanceOperation>() }
}