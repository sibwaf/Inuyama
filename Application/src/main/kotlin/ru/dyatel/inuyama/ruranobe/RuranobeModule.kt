package ru.dyatel.inuyama.ruranobe

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
import ru.dyatel.inuyama.model.RuranobeProject
import ru.dyatel.inuyama.model.RuranobeVolume
import ru.dyatel.inuyama.utilities.boxFor
import sibwaf.inuyama.app.common.ModuleScreenProvider

val ruranobeModule = Kodein.Module("ruranobe") {
    bind<Box<RuranobeProject>>() with singleton { instance<BoxStore>().boxFor() }
    bind<Box<RuranobeVolume>>() with singleton { instance<BoxStore>().boxFor() }
    bind<RuranobeApi>() with singleton {
        RuranobeApi(
            networkManager = instance(),
            gson = instance(),
            jsonParser = instance(),
        )
    }
    bind<RuranobeWatcher>() with singleton {
        RuranobeWatcher(
            api = instance(),
            boxStore = instance(),
            projectBox = instance(),
            volumeBox = instance(),
        )
    }

    bind<ModuleScreenProvider>().inSet() with singleton {
        object : ModuleScreenProvider() {
            override fun getIcon() = CommunityMaterial.Icon.cmd_book
            override fun getTitle(context: Context) = context.getString(R.string.module_ruranobe)
            override fun getScreenClass() = RuranobeScreen::class.java
        }
    }
}
