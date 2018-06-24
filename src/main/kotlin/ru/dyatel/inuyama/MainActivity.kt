package ru.dyatel.inuyama

import android.Manifest
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.Menu
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.materialdrawer.DrawerBuilder
import com.mikepenz.materialdrawer.model.AbstractDrawerItem
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import com.wealthfront.magellan.Navigator
import com.wealthfront.magellan.support.SingleActivity
import org.jetbrains.anko.ctx
import org.jetbrains.anko.find
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.screens.MainScreen
import ru.dyatel.inuyama.screens.NetworkScreen

class MainActivity : SingleActivity(), KodeinAware {

    override val kodein by closestKodein()

    private val networkManager: NetworkManager by instance()

    override fun createNavigator() =
            Navigator
                    .withRoot(MainScreen())
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

        registerReceiver(networkManager, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
    }

    override fun onResume() {
        super.onResume()
        grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    override fun onDestroy() {
        unregisterReceiver(networkManager)
        super.onDestroy()
    }

    private fun DrawerBuilder.generateDrawerItems() {
        addDrawerItems(PrimaryDrawerItem()
                .withIcon(CommunityMaterial.Icon.cmd_glasses)
                .withName(R.string.screen_watches))

        addDrawerItems(PrimaryDrawerItem()
                .withIcon(CommunityMaterial.Icon.cmd_wifi)
                .withName(R.string.screen_networks)
                .withOnClickListener { getNavigator().replace(NetworkScreen()) })
    }

    private fun <T, VH : RecyclerView.ViewHolder> AbstractDrawerItem<T, VH>.withOnClickListener(listener: () -> Unit): T {
        return withOnDrawerItemClickListener { _, _, _ ->
            listener()
            false
        }
    }

    private fun createActionBarIcon(icon: IIcon) =
            IconicsDrawable(ctx).actionBar().icon(icon).colorRes(R.color.md_white_1000)

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        menu.findItem(R.id.refresh).icon = createActionBarIcon(CommunityMaterial.Icon.cmd_refresh)
        return super.onCreateOptionsMenu(menu)
    }
}
