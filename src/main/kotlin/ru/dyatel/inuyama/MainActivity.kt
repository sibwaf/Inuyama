package ru.dyatel.inuyama

import android.Manifest
import android.os.Bundle
import android.view.Menu
import androidx.appcompat.widget.Toolbar
import androidx.work.WorkManager
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.DrawerBuilder
import com.mikepenz.materialdrawer.model.DividerDrawerItem
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import com.wealthfront.magellan.NavigationType
import com.wealthfront.magellan.Navigator
import com.wealthfront.magellan.Screen
import com.wealthfront.magellan.support.SingleActivity
import io.objectbox.BoxStore
import io.objectbox.android.AndroidObjectBrowser
import org.jetbrains.anko.find
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.overseer.OverseerStarter
import ru.dyatel.inuyama.screens.DirectoryScreen
import ru.dyatel.inuyama.screens.HomeScreen
import ru.dyatel.inuyama.screens.NetworkScreen
import ru.dyatel.inuyama.screens.ProxyScreen
import ru.dyatel.inuyama.screens.TransmissionScreen
import ru.dyatel.inuyama.utilities.grantPermissions
import java.util.concurrent.atomic.AtomicLong

class MainActivity : SingleActivity(), KodeinAware {

    override val kodein by closestKodein()

    private val menuIdGenerator = AtomicLong(1)
    private val menuItemRegistry = mutableMapOf<Class<out Screen<*>>, Long>()
    private lateinit var drawer: Drawer

    override fun createNavigator() =
            Navigator
                    .withRoot(HomeScreen())
                    .build()!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity)

        val toolbar = find<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawer = DrawerBuilder(this)
                .withToolbar(toolbar)
                .withSavedInstance(savedInstanceState)
                .apply { generateDrawerItems() }
                .build()

        if (BuildConfig.DEBUG) {
            val boxStore by instance<BoxStore>()
            AndroidObjectBrowser(boxStore).start(applicationContext)
        }
    }

    override fun onStart() {
        super.onStart()
        OverseerStarter.start(applicationContext, false)
    }

    override fun onResume() {
        super.onResume()
        grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(drawer.saveInstanceState(outState))
    }

    override fun onStop() {
        if (BuildConfig.DEBUG) {
            WorkManager.getInstance().cancelUniqueWork(WORK_NAME_OVERSEER)
        }

        super.onStop()
    }

    fun syncNavigation() {
        // TODO: find nearest existing screen
        val id = menuItemRegistry[getNavigator().currentScreen().javaClass] ?: return
        drawer.setSelection(id)
    }

    private fun navigate(screen: Screen<*>) {
        getNavigator().navigate({
            while (it.size > 1) {
                it.pop()
            }

            it.addFirst(screen)
        }, NavigationType.GO)
    }

    private fun DrawerBuilder.generateDrawerItems() {
        createMenuItem<HomeScreen>(CommunityMaterial.Icon2.cmd_home, getString(R.string.screen_home))

        addDrawerItems(DividerDrawerItem())

        val moduleScreens by instance<Set<ModuleScreenProvider>>()
        for (provider in moduleScreens.sortedBy { it.getTitle(this@MainActivity) }) {
            createMenuItem(provider.getIcon(), provider.getTitle(this@MainActivity), provider.getScreenClass())
        }

        addDrawerItems(DividerDrawerItem())

        createMenuItem<NetworkScreen>(CommunityMaterial.Icon2.cmd_wifi, getString(R.string.screen_networks))
        createMenuItem<ProxyScreen>(CommunityMaterial.Icon.cmd_cloud, getString(R.string.screen_proxy))
        createMenuItem<DirectoryScreen>(CommunityMaterial.Icon.cmd_folder, getString(R.string.screen_directories))
        createMenuItem<TransmissionScreen>(CommunityMaterial.Icon2.cmd_inbox_arrow_down, getString(R.string.screen_transmission))
    }

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T : Screen<*>> DrawerBuilder.createMenuItem(icon: IIcon, name: String) =
            createMenuItem(icon, name, T::class.java as Class<Screen<*>>)

    private fun DrawerBuilder.createMenuItem(icon: IIcon, name: String, c: Class<out Screen<*>>) {
        val id = menuIdGenerator.getAndIncrement()

        menuItemRegistry[c] = id

        addDrawerItems(PrimaryDrawerItem()
                .withIdentifier(id)
                .withIcon(icon)
                .withName(name)
                .withOnDrawerItemClickListener { view, _, _ ->
                    if (view != null && !c.isInstance(getNavigator().currentScreen())) {
                        navigate(c.newInstance())
                    }

                    false
                })
    }

    private fun createActionBarIcon(icon: IIcon) =
            IconicsDrawable(this).actionBar().icon(icon).colorRes(R.color.material_drawer_dark_primary_text)

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        menu.findItem(R.id.search).icon = createActionBarIcon(CommunityMaterial.Icon2.cmd_magnify)
        menu.findItem(R.id.add).icon = createActionBarIcon(CommunityMaterial.Icon2.cmd_plus)
        menu.findItem(R.id.refresh).icon = createActionBarIcon(CommunityMaterial.Icon2.cmd_refresh)
        menu.findItem(R.id.settings).icon = createActionBarIcon(CommunityMaterial.Icon2.cmd_settings)
        return super.onCreateOptionsMenu(menu)
    }
}
