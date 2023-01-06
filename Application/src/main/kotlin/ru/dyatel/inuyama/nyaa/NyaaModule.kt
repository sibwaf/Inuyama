package ru.dyatel.inuyama.nyaa

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
import ru.dyatel.inuyama.model.NyaaTorrent
import ru.dyatel.inuyama.model.NyaaWatch
import ru.dyatel.inuyama.utilities.boxFor
import sibwaf.inuyama.app.common.ModuleScreenProvider

val nyaaModule = Kodein.Module("nyaa") {
    bind<Box<NyaaTorrent>>() with singleton { instance<BoxStore>().boxFor() }
    bind<Box<NyaaWatch>>() with singleton { instance<BoxStore>().boxFor() }
    bind<NyaaApiService>() with singleton {
        NyaaApiService(
            networkManager = instance(),
        )
    }
    bind<NyaaWatcher>() with singleton {
        NyaaWatcher(
            api = instance(),
            boxStore = instance(),
            torrentBox = instance(),
            watchBox = instance(),
        )
    }

    bind<NyaaBackupHandler>() with singleton {
        NyaaBackupHandler(
            watchRepository = instance(),
            torrentRepository = instance(),
            gson = instance()
        )
    }

    bind<ModuleScreenProvider>().inSet() with singleton {
        object : ModuleScreenProvider() {
            override fun getIcon() = CommunityMaterial.Icon.cmd_database
            override fun getTitle(context: Context) = context.getString(R.string.module_nyaa)
            override fun getScreenClass() = NyaaScreen::class.java
        }
    }
}
