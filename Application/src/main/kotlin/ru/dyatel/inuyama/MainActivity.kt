package ru.dyatel.inuyama

//import androidx.biometric.BiometricManager
import android.Manifest
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
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
import com.wealthfront.magellan.ScreenLifecycleListener
import com.wealthfront.magellan.support.SingleActivity
import io.objectbox.BoxStore
import io.objectbox.android.AndroidObjectBrowser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.contentView
import org.jetbrains.anko.design.indefiniteSnackbar
import org.jetbrains.anko.design.snackbar
import org.jetbrains.anko.find
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.android.subKodein
import org.kodein.di.direct
import org.kodein.di.generic.bind
import org.kodein.di.generic.eagerSingleton
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.backup.BackupService
import ru.dyatel.inuyama.finance.FinanceDashboardScreen
import ru.dyatel.inuyama.finance.FinanceStatisticsScreen
import ru.dyatel.inuyama.pairing.PairingScreen
import ru.dyatel.inuyama.screens.DirectoryScreen
import ru.dyatel.inuyama.screens.HomeScreen
import ru.dyatel.inuyama.screens.NetworkScreen
import ru.dyatel.inuyama.screens.ProxyScreen
import ru.dyatel.inuyama.utilities.debugOnly
import ru.dyatel.inuyama.utilities.grantPermissions
import sibwaf.inuyama.app.common.ModuleScreenProvider
import sibwaf.inuyama.app.common.components.showConfirmationDialog
import java.util.concurrent.atomic.AtomicLong

class MainActivity : SingleActivity(), KodeinAware {

    override val kodein by subKodein(kodein()) {
        bind<QrReader>() with eagerSingleton { QrReader(this@MainActivity) }
    }

    private val backgroundServiceManager by lazy { kodein.direct.instance<BackgroundServiceManager>() }
    private val backupService by instance<BackupService>()

    private val menuIdGenerator = AtomicLong(1)
    private val menuItemRegistry = mutableMapOf<Class<out Screen<*>>, Long>()
    private lateinit var drawer: Drawer

    private lateinit var searchView: SearchView

    override fun createNavigator(): Navigator =
        Navigator
            .withRoot(HomeScreen())
            .build()
            .apply {
                addLifecycleListener(
                    object : ScreenLifecycleListener {
                        override fun onShow(screen: Screen<*>) = syncNavigation()
                        override fun onHide(screen: Screen<*>) {
                            closeSearchView()
                            searchView.isVisible = false
                        }
                    }
                )
            }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity)

        val toolbar = find<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        searchView = find<SearchView>(R.id.search).apply {
            queryHint = getString(R.string.action_search)
            isVisible = false
        }

        drawer = DrawerBuilder(this)
            .withToolbar(toolbar)
            .withSavedInstance(savedInstanceState)
            .apply { generateDrawerItems() }
            .build()

        debugOnly {
            val boxStore by instance<BoxStore>()
            AndroidObjectBrowser(boxStore).start(applicationContext)
        }
    }

    override fun onStart() {
        super.onStart()
        backgroundServiceManager.onActivityStart(applicationContext)

//        QrFeature(this, supportFragmentManager,
//                onNeedToRequestPermissions = {  grantPermissions(requestPermissions(it, 0))},
//        onScanResult =         {            toast("Scanned: $it")        }
//        )

//        val biometric = BiometricManager.from(this)
    }

    override fun onResume() {
        super.onResume()
        grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(drawer.saveInstanceState(outState))
    }

    override fun onStop() {
        backgroundServiceManager.onActivityStop(applicationContext)
        super.onStop()
    }

    override fun onBackPressed() {
        if (!searchView.isIconified) {
            closeSearchView()
            return
        }

        super.onBackPressed()
    }

    private fun closeSearchView() {
        searchView.setQuery("", false)
        searchView.isIconified = true
    }

    private fun syncNavigation() {
        // TODO: find nearest existing screen
        val id = menuItemRegistry[getNavigator().currentScreen().javaClass] ?: return
        drawer.setSelection(id)
    }

    private fun navigate(screen: Screen<*>) {
        getNavigator().navigate({
            while (it.size > 1) {
                it.pop()
            }

            if (it.last.javaClass != screen.javaClass) {
                it.addFirst(screen)
            }
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

        createMenuItem<FinanceDashboardScreen>(CommunityMaterial.Icon2.cmd_wallet, getString(R.string.screen_finance_dashboard))
        createMenuItem<FinanceStatisticsScreen>(CommunityMaterial.Icon.cmd_chart_histogram, getString(R.string.screen_finance_statistics))

        addDrawerItems(DividerDrawerItem())

        createMenuItem<NetworkScreen>(CommunityMaterial.Icon2.cmd_wifi, getString(R.string.screen_networks))
        createMenuItem<PairingScreen>(CommunityMaterial.Icon2.cmd_monitor_cellphone, getString(R.string.module_pairing))
        createMenuItem<ProxyScreen>(CommunityMaterial.Icon.cmd_cloud, getString(R.string.screen_proxy))
        createMenuItem<DirectoryScreen>(CommunityMaterial.Icon.cmd_folder, getString(R.string.screen_directories))

        addDrawerItems(DividerDrawerItem())

        // todo: a separate screen with checkboxes?
        addDrawerItems(PrimaryDrawerItem()
            .withIdentifier(-1000)
            .withIcon(CommunityMaterial.Icon.cmd_backup_restore)
            .withName(R.string.action_restore_backup)
            .withSelectable(false)
            .withOnDrawerItemClickListener { _, _, _ ->
                showConfirmationDialog(
                    title = getString(R.string.dialog_restore_backup_title),
                    message = getString(R.string.dialog_restore_backup_full_message),
                    action = getString(R.string.ok)
                ) {
                    // todo: no-leave waiting screen
                    CoroutineScope(Dispatchers.Default).launch {
                        val progressSnackbar = withContext(Dispatchers.Main) {
                            contentView!!.indefiniteSnackbar(R.string.message_backup_restore_in_progress)
                        }

                        try {
                            Log.i("MainActivity", "Restoring all modules from backups")
                            backupService.restoreEverything()
                            Log.i("MainActivity", "Restore from backup completed")
                        } finally {
                            withContext(Dispatchers.Main) {
                                progressSnackbar.dismiss()
                            }
                        }

                        withContext(Dispatchers.Main) {
                            contentView!!.snackbar(R.string.message_backup_restore_finished)
                        }
                    }
                }

                true
            }
        )
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
        menu.findItem(R.id.add).icon = createActionBarIcon(CommunityMaterial.Icon2.cmd_plus)
        menu.findItem(R.id.settings).icon = createActionBarIcon(CommunityMaterial.Icon2.cmd_settings)
        return super.onCreateOptionsMenu(menu)
    }
}
