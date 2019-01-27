package ru.dyatel.inuyama.screens

import android.content.Context
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.wealthfront.magellan.BaseScreenView
import io.objectbox.Box
import org.jetbrains.anko.appcompat.v7.tintedButton
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.padding
import org.jetbrains.anko.recyclerview.v7.recyclerView
import org.jetbrains.anko.verticalLayout
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.NetworkManager
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.layout.DIM_LARGE
import ru.dyatel.inuyama.layout.NetworkItem
import ru.dyatel.inuyama.model.Network
import ru.dyatel.inuyama.utilities.buildFastAdapter

class NetworkView(context: Context) : BaseScreenView<NetworkScreen>(context) {

    private lateinit var recyclerView: RecyclerView

    init {
        verticalLayout {
            lparams(width = matchParent, height = matchParent) {
                padding = DIM_LARGE
            }

            tintedButton(R.string.action_refresh) {
                setOnClickListener { screen.refresh() }
            }

            recyclerView = recyclerView {
                lparams(width = matchParent, height = matchParent)

                layoutManager = LinearLayoutManager(context)
                addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            }
        }
    }

    fun bindAdapter(adapter: RecyclerView.Adapter<*>) {
        recyclerView.adapter = adapter
    }

}

class NetworkScreen : InuScreen<NetworkView>(), KodeinAware {

    override val titleResource = R.string.screen_networks

    private val networkManager by instance<NetworkManager>()

    private val networkBox by instance<Box<Network>>()

    private val adapter = ItemAdapter<NetworkItem>()
    private val fastAdapter = adapter.buildFastAdapter()

    override fun createView(context: Context) = NetworkView(context).apply { bindAdapter(fastAdapter) }

    override fun onShow(context: Context) {
        super.onShow(context)

        refresh()
        observeChanges<Network>(::refresh)
    }

    fun refresh() {
        // TODO: causes unneeded updates
        networkManager.refreshNetworkList()

        val networks = networkBox.all
                .sortedBy { it.name }
                .map {
                    NetworkItem(it) {
                        networkBox.put(it) // ???
                    }
                }

        adapter.set(networks)
    }

}
