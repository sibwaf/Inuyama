package ru.dyatel.inuyama.pairing

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.Button
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.wealthfront.magellan.BaseScreenView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.anko.appcompat.v7.tintedButton
import org.jetbrains.anko.hintResource
import org.jetbrains.anko.margin
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.padding
import org.jetbrains.anko.recyclerview.v7.recyclerView
import org.jetbrains.anko.support.v4.nestedScrollView
import org.jetbrains.anko.textResource
import org.jetbrains.anko.verticalLayout
import org.jetbrains.anko.wrapContent
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.NetworkManager
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.layout.DIM_LARGE
import ru.dyatel.inuyama.layout.PairingServerItem
import ru.dyatel.inuyama.layout.components.ElementPicker
import ru.dyatel.inuyama.layout.components.UniformTextInput
import ru.dyatel.inuyama.layout.components.uniformTextInput
import ru.dyatel.inuyama.layout.components.uniformTextView
import ru.dyatel.inuyama.screens.InuScreen
import ru.dyatel.inuyama.utilities.PreferenceHelper
import ru.dyatel.inuyama.utilities.buildFastAdapter
import ru.dyatel.inuyama.utilities.isVisible
import ru.sibwaf.inuyama.common.Pairing
import ru.sibwaf.inuyama.common.utilities.humanReadable

class PairingView(context: Context) : BaseScreenView<PairingScreen>(context) {

    lateinit var unbindButton: Button
        private set

    lateinit var discoveryPortView: UniformTextInput
        private set

    lateinit var untrustedNetworkView: View
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

                discoveryPortView = uniformTextInput {
                    hintResource = R.string.label_pairing_discovery_port
                }.lparams(width = matchParent, height = wrapContent) {
                    margin = DIM_LARGE
                }

                untrustedNetworkView = uniformTextView {
                    textResource = R.string.label_current_network_untrusted
                    isVisible = false
                }.lparams {
                    margin = DIM_LARGE
                    gravity = Gravity.CENTER_HORIZONTAL
                }

                recyclerView = recyclerView {
                    lparams(width = matchParent, height = wrapContent)
                    layoutManager = LinearLayoutManager(context)
                    itemAnimator = null
                }
            }
        }
    }

}

class PairingScreen : InuScreen<PairingView>() {

    override val titleResource = R.string.module_pairing

    private val networkManager by instance<NetworkManager>()

    private val preferenceHelper by instance<PreferenceHelper>()
    private val pairingManager by instance<PairingManager>()
    private val discoverResponseListener by instance<DiscoverResponseListener>()
    private lateinit var discoverListener: (DiscoveredServer) -> Unit

    private val serverAdapter = ItemAdapter<PairingServerItem>()
    private val serverFastAdapter = serverAdapter.buildFastAdapter()

    private val serverItemIds = mutableMapOf<DiscoveredServer, Long>()

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
            unbindButton.isVisible = pairingManager.pairedServer != null
            unbindButton.setOnClickListener {
                pairingManager.unbind()
                it.isVisible = false
            }

            val portPicker = ElementPicker(
                    discoveryPortView.editText!!,
                    generateSequence(Pairing.DEFAULT_DISCOVER_SERVER_PORT) { it + 1 }.take(100).toList(),
                    { preferenceHelper.discoveryPort = it }
            )
            portPicker.currentValue = preferenceHelper.discoveryPort
            discoveryPortView.editText!!.setOnClickListener { portPicker.showDialog(activity!!.supportFragmentManager) }

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
                val networkTrusted = networkManager.isNetworkTrusted

                withContext(Dispatchers.Main) {
                    view.discoveryPortView.isVisible = networkTrusted
                    view.untrustedNetworkView.isVisible = !networkTrusted
                }

                servers.removeAll { it !in aliveServers }
                aliveServers.clear()
                withContext(Dispatchers.Main) { refresh() }

                if (networkTrusted) {
                    pairingManager.sendDiscoverRequest()
                }

                delay(4000) // TODO: magic constants
            }
        }

        refresh()
    }

    private fun refresh() {
        val list = servers.sortedBy { it.key.humanReadable }
        serverAdapter.set(list.map {
            PairingServerItem(it, pairingManager.equalsToPaired(it)).apply {
                val id = serverItemIds.getOrPut(it) { (serverItemIds.values.max() ?: 0) + 1L }
                withIdentifier(id)
            }
        })
    }

    override fun onHide(context: Context) {
        discoverResponseListener.removeListener(discoverListener)
        super.onHide(context)
    }
}