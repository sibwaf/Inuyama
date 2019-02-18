package ru.dyatel.inuyama.pairing

import android.content.Context
import android.widget.Button
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.wealthfront.magellan.BaseScreenView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.anko.appcompat.v7.tintedButton
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.padding
import org.jetbrains.anko.recyclerview.v7.recyclerView
import org.jetbrains.anko.support.v4.nestedScrollView
import org.jetbrains.anko.verticalLayout
import org.jetbrains.anko.wrapContent
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.NetworkManager
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.layout.DIM_LARGE
import ru.dyatel.inuyama.layout.PairingServerItem
import ru.dyatel.inuyama.screens.InuScreen
import ru.dyatel.inuyama.utilities.buildFastAdapter
import ru.dyatel.inuyama.utilities.isVisible
import ru.sibwaf.inuyama.common.utilities.humanReadable

class PairingView(context: Context) : BaseScreenView<PairingScreen>(context) {

    lateinit var unbindButton: Button
        private set

    lateinit var recyclerView: RecyclerView
        private set

    init {
        nestedScrollView {
            lparams(width = matchParent, height = matchParent)

            verticalLayout {
                lparams(width = matchParent, height = wrapContent) {
                    padding = DIM_LARGE
                }

                unbindButton = tintedButton(R.string.action_unbind)

                recyclerView = recyclerView {
                    lparams(width = matchParent, height = wrapContent)
                    layoutManager = LinearLayoutManager(context)
                }
            }
        }
    }

}

class PairingScreen : InuScreen<PairingView>() {

    override val titleResource = R.string.module_pairing

    private val networkManager by instance<NetworkManager>()

    private val pairingManager by instance<PairingManager>()
    private val discoverResponseListener by instance<DiscoverResponseListener>()
    private lateinit var discoverListener: (DiscoveredServer) -> Unit

    private val serverAdapter = ItemAdapter<PairingServerItem>()
    private val serverFastAdapter = serverAdapter.buildFastAdapter()

    private val servers = mutableSetOf<DiscoveredServer>()
    private val aliveServers = mutableSetOf<DiscoveredServer>()

    init {
        serverFastAdapter.withOnClickListener { _, _, item, _ ->
            navigator.goTo(PairingServerScreen(item.server))
            return@withOnClickListener true
        }
    }

    override fun createView(context: Context): PairingView {
        return PairingView(context).apply {
            unbindButton.setOnClickListener {
                pairingManager.unbind()
                refresh()
            }

            recyclerView.adapter = serverFastAdapter
        }
    }

    override fun onShow(context: Context) {
        super.onShow(context)

        discoverListener = discoverResponseListener.addListener { server ->
            servers.add(server)
            aliveServers.add(server)

            launchJob { refresh() }
        }

        launchJob(Dispatchers.Default) {
            while (true) {
                servers.removeAll { it !in aliveServers }
                aliveServers.clear()
                withContext(Dispatchers.Main) { refresh() }

                if (networkManager.isNetworkTrusted) {
                    pairingManager.sendDiscoverRequest()
                }
                // TODO: display warning for untrusted networks

                delay(10000) // TODO: magic constants
            }
        }

        refresh()
    }

    private fun refresh() {
        view.unbindButton.isVisible = pairingManager.pairedServer != null

        // TODO: stop this awful blinking
        val list = servers.sortedBy { it.key.humanReadable }
        serverAdapter.set(list.map {
            PairingServerItem(it, pairingManager.equalsToPaired(it))
        })
    }

    override fun onHide(context: Context) {
        discoverResponseListener.removeListener(discoverListener)
        super.onHide(context)
    }
}