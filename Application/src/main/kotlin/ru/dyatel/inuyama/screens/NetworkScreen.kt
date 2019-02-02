package ru.dyatel.inuyama.screens

import android.content.Context
import android.widget.Button
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.wealthfront.magellan.BaseScreenView
import io.objectbox.Box
import org.jetbrains.anko.appcompat.v7.tintedButton
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.padding
import org.jetbrains.anko.recyclerview.v7.recyclerView
import org.jetbrains.anko.support.v4.nestedScrollView
import org.jetbrains.anko.verticalLayout
import org.jetbrains.anko.wrapContent
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.NetworkManager
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.layout.DIM_LARGE
import ru.dyatel.inuyama.layout.NetworkItem
import ru.dyatel.inuyama.model.Network
import ru.dyatel.inuyama.utilities.buildFastAdapter

class NetworkView(context: Context) : BaseScreenView<NetworkScreen>(context) {

    lateinit var refreshButton: Button
        private set

    lateinit var recyclerView: RecyclerView
        private set

    init {
        nestedScrollView {
            verticalLayout {
                lparams(width = matchParent, height = wrapContent) {
                    padding = DIM_LARGE
                }

                refreshButton = tintedButton(R.string.action_refresh)

                recyclerView = recyclerView {
                    lparams(width = matchParent, height = wrapContent)
                    layoutManager = LinearLayoutManager(context)
                }
            }
        }
    }
}

class NetworkScreen : InuScreen<NetworkView>(), KodeinAware {

    override val titleResource = R.string.screen_networks

    private val networkManager by instance<NetworkManager>()

    private val networkBox by instance<Box<Network>>()

    private val adapter = ItemAdapter<NetworkItem>()
    private val fastAdapter = adapter.buildFastAdapter()

    override fun createView(context: Context): NetworkView {
        return NetworkView(context).apply {
            refreshButton.setOnClickListener {
                networkManager.refreshNetworkList()
            }

            recyclerView.adapter = fastAdapter
        }
    }

    override fun onShow(context: Context) {
        super.onShow(context)

        networkManager.refreshNetworkList()

        refresh()
        observeChanges<Network>(::refresh)
    }

    private fun refresh() {
        adapter.set(networkBox.all
                .sortedBy { it.name }
                .map { network ->
                    NetworkItem(network) {
                        network.trusted = it
                        networkBox.put(network)
                    }
                })
    }

}
