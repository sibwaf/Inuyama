package ru.dyatel.inuyama.rutracker

import android.content.Context
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import io.objectbox.Box
import io.objectbox.BoxStore
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.inSet
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.model.RutrackerWatch
import ru.dyatel.inuyama.utilities.PreferenceHelper
import ru.dyatel.inuyama.utilities.boxFor
import sibwaf.inuyama.app.common.ModuleScreenProvider

val rutrackerModule = Kodein.Module("rutracker") {
    bind<Box<RutrackerWatch>>() with singleton { instance<BoxStore>().boxFor() }
    bind<RutrackerConfiguration>() with singleton { instance<PreferenceHelper>().rutracker }
    bind<RutrackerApiService>() with singleton { RutrackerApiService(kodein) }
    bind<RutrackerWatcher>() with singleton { RutrackerWatcher(kodein) }

    bind<ModuleScreenProvider>().inSet() with singleton {
        object : ModuleScreenProvider() {
            override fun getIcon() = CommunityMaterial.Icon.cmd_database
            override fun getTitle(context: Context) = context.getString(R.string.module_rutracker)
            override fun getScreenClass() = RutrackerScreen::class.java
        }
    }
}
