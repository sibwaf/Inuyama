package ru.dyatel.inuyama

import android.content.Context
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.typeface.IIcon
import com.wealthfront.magellan.Screen
import io.objectbox.Box
import io.objectbox.BoxStore
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton
import ru.dyatel.inuyama.model.NyaaTorrent
import ru.dyatel.inuyama.model.NyaaWatch
import ru.dyatel.inuyama.model.RutrackerWatch
import ru.dyatel.inuyama.nyaa.NyaaApi
import ru.dyatel.inuyama.nyaa.NyaaWatcher
import ru.dyatel.inuyama.rutracker.RutrackerApi
import ru.dyatel.inuyama.rutracker.RutrackerConfiguration
import ru.dyatel.inuyama.rutracker.RutrackerWatcher
import ru.dyatel.inuyama.screens.NyaaScreen
import ru.dyatel.inuyama.screens.RutrackerScreen
import ru.dyatel.inuyama.utilities.PreferenceHelper
import ru.dyatel.inuyama.utilities.boxFor

interface RemoteService {
    fun getName(context: Context): String
    fun checkConnection(): Boolean
}

interface Watcher {
    fun checkUpdates(): List<String>
    fun dispatchUpdates()
}

@Suppress("FINITE_BOUNDS_VIOLATION_IN_JAVA")
abstract class ModuleScreenProvider<T : Screen<*>> {
    abstract fun getIcon(): IIcon
    abstract fun getTitle(context: Context): String
    abstract fun getScreen(): T
}

val rutrackerModule = Kodein.Module {
    bind<Box<RutrackerWatch>>() with singleton { instance<BoxStore>().boxFor<RutrackerWatch>() }
    bind<RutrackerConfiguration>() with singleton { instance<PreferenceHelper>().rutracker }
    bind<RutrackerApi>() with singleton { RutrackerApi(instance()) }
    bind<RutrackerWatcher>() with singleton { RutrackerWatcher(instance()) }

    bind<ModuleScreenProvider<RutrackerScreen>>() with singleton {
        object : ModuleScreenProvider<RutrackerScreen>() {
            override fun getIcon() = CommunityMaterial.Icon.cmd_database
            override fun getTitle(context: Context) = context.getString(R.string.module_rutracker)!!
            override fun getScreen() = RutrackerScreen()
        }
    }
}

val nyaaModule = Kodein.Module {
    bind<Box<NyaaTorrent>>() with singleton { instance<BoxStore>().boxFor<NyaaTorrent>() }
    bind<Box<NyaaWatch>>() with singleton { instance<BoxStore>().boxFor<NyaaWatch>() }
    bind<NyaaApi>() with singleton { NyaaApi(instance()) }
    bind<NyaaWatcher>() with singleton { NyaaWatcher(instance()) }

    bind<ModuleScreenProvider<NyaaScreen>>() with singleton {
        object : ModuleScreenProvider<NyaaScreen>() {
            override fun getIcon() = CommunityMaterial.Icon.cmd_database
            override fun getTitle(context: Context) = context.getString(R.string.module_nyaa)!!
            override fun getScreen() = NyaaScreen()
        }
    }
}