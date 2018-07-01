package ru.dyatel.inuyama

import android.Manifest
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.Menu
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.materialdrawer.DrawerBuilder
import com.mikepenz.materialdrawer.model.AbstractDrawerItem
import com.mikepenz.materialdrawer.model.DividerDrawerItem
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import com.wealthfront.magellan.Navigator
import com.wealthfront.magellan.support.SingleActivity
import io.objectbox.BoxStore
import io.objectbox.android.AndroidObjectBrowser
import org.jetbrains.anko.ctx
import org.jetbrains.anko.find
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.allInstances
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.overseer.OverseerStarter
import ru.dyatel.inuyama.screens.DirectoryScreen
import ru.dyatel.inuyama.screens.HomeScreen
import ru.dyatel.inuyama.screens.NetworkScreen
import ru.dyatel.inuyama.screens.TransmissionScreen
import ru.dyatel.inuyama.utilities.grantPermissions

class MainActivity : SingleActivity(), KodeinAware {

    override val kodein by closestKodein()

    private val moduleScreens by allInstances<ModuleScreenProvider<*>>()

    override fun createNavigator() =
            Navigator
                    .withRoot(HomeScreen())
                    .build()!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity)

        val toolbar = find<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        DrawerBuilder(this)
                .withToolbar(toolbar)
                .apply { generateDrawerItems() }
                .build()

        if (BuildConfig.DEBUG) {
            val boxStore by instance<BoxStore>()
            AndroidObjectBrowser(boxStore).start(applicationContext)
        }

        OverseerStarter.start(applicationContext, false)
    }

    override fun onResume() {
        super.onResume()
        grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    private fun DrawerBuilder.generateDrawerItems() {
        addDrawerItems(PrimaryDrawerItem()
                .withIcon(CommunityMaterial.Icon.cmd_home)
                .withName(R.string.screen_home)
                .withOnClickListener { getNavigator().replace(HomeScreen()) })

        addDrawerItems(DividerDrawerItem())

        for (provider in moduleScreens) {
            addDrawerItems(PrimaryDrawerItem()
                    .withIcon(provider.getIcon())
                    .withName(provider.getTitle(ctx))
                    .withOnClickListener { getNavigator().replace(provider.getScreen()) }
            )
        }

        addDrawerItems(DividerDrawerItem())

        addDrawerItems(PrimaryDrawerItem()
                .withIcon(CommunityMaterial.Icon.cmd_wifi)
                .withName(R.string.screen_networks)
                .withOnClickListener { getNavigator().replace(NetworkScreen()) })

        addDrawerItems(PrimaryDrawerItem()
                .withIcon(CommunityMaterial.Icon.cmd_folder)
                .withName(R.string.screen_directories)
                .withOnClickListener { getNavigator().replace(DirectoryScreen()) })

        addDrawerItems(PrimaryDrawerItem()
                .withIcon(CommunityMaterial.Icon.cmd_inbox_arrow_down)
                .withName(R.string.screen_transmission)
                .withOnClickListener { getNavigator().replace(TransmissionScreen()) })
    }

    private fun <T, VH : RecyclerView.ViewHolder> AbstractDrawerItem<T, VH>.withOnClickListener(listener: () -> Unit): T {
        return withOnDrawerItemClickListener { _, _, _ ->
            listener()
            false
        }
    }

    private fun createActionBarIcon(icon: IIcon) =
            IconicsDrawable(ctx).actionBar().icon(icon).colorRes(R.color.material_drawer_dark_primary_text)

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        menu.findItem(R.id.add).icon = createActionBarIcon(CommunityMaterial.Icon.cmd_plus)
        menu.findItem(R.id.refresh).icon = createActionBarIcon(CommunityMaterial.Icon.cmd_refresh)
        menu.findItem(R.id.settings).icon = createActionBarIcon(CommunityMaterial.Icon.cmd_settings)
        return super.onCreateOptionsMenu(menu)
    }
}
