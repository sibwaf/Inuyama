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
import ru.dyatel.inuyama.model.RuranobeProject
import ru.dyatel.inuyama.model.RuranobeVolume
import ru.dyatel.inuyama.model.RutrackerWatch
import ru.dyatel.inuyama.model.Update
import ru.dyatel.inuyama.nyaa.NyaaApi
import ru.dyatel.inuyama.nyaa.NyaaWatcher
import ru.dyatel.inuyama.ruranobe.RuranobeApi
import ru.dyatel.inuyama.ruranobe.RuranobeWatcher
import ru.dyatel.inuyama.rutracker.RutrackerApi
import ru.dyatel.inuyama.rutracker.RutrackerConfiguration
import ru.dyatel.inuyama.rutracker.RutrackerWatcher
import ru.dyatel.inuyama.screens.NyaaScreen
import ru.dyatel.inuyama.screens.RuranobeScreen
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
    fun listUpdates(): List<Update>
    fun addUpdateListener(listener: () -> Unit)
    fun removeUpdateListener(listener: () -> Unit)
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
    bind<RutrackerApi>() with singleton { RutrackerApi(kodein) }
    bind<RutrackerWatcher>() with singleton { RutrackerWatcher(kodein) }

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
    bind<NyaaApi>() with singleton { NyaaApi(kodein) }
    bind<NyaaWatcher>() with singleton { NyaaWatcher(kodein) }

    bind<ModuleScreenProvider<NyaaScreen>>() with singleton {
        object : ModuleScreenProvider<NyaaScreen>() {
            override fun getIcon() = CommunityMaterial.Icon.cmd_database
            override fun getTitle(context: Context) = context.getString(R.string.module_nyaa)!!
            override fun getScreen() = NyaaScreen()
        }
    }
}

val ruranobeModule = Kodein.Module {
    bind<Box<RuranobeProject>>() with singleton { instance<BoxStore>().boxFor<RuranobeProject>() }
    bind<Box<RuranobeVolume>>() with singleton { instance<BoxStore>().boxFor<RuranobeVolume>() }
    bind<RuranobeApi>() with singleton { RuranobeApi(kodein) }
    bind<RuranobeWatcher>() with singleton { RuranobeWatcher(kodein) }

    bind<ModuleScreenProvider<RuranobeScreen>>() with singleton {
        object : ModuleScreenProvider<RuranobeScreen>() {
            override fun getIcon() = CommunityMaterial.Icon.cmd_book
            override fun getTitle(context: Context) = context.getString(R.string.module_ruranobe)!!
            override fun getScreen() = RuranobeScreen()
        }
    }
}
