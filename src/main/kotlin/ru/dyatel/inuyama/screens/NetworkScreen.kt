package ru.dyatel.inuyama.screens

import android.content.Context
import android.view.Menu
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.wealthfront.magellan.BaseScreenView
import io.objectbox.Box
import io.objectbox.android.AndroidScheduler
import io.objectbox.reactive.DataSubscription
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.recyclerview.v7.recyclerView
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.NetworkManager
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.layout.NetworkItem
import ru.dyatel.inuyama.model.Network
import ru.dyatel.inuyama.utilities.buildFastAdapter
import ru.dyatel.inuyama.utilities.subscribeFor

class NetworkView(context: Context) : BaseScreenView<NetworkScreen>(context) {

    private val recyclerView: RecyclerView

    init {
        recyclerView = recyclerView {
            lparams(width = matchParent, height = matchParent)

            layoutManager = LinearLayoutManager(context)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
    }

    fun bindAdapter(adapter: RecyclerView.Adapter<*>) {
        recyclerView.adapter = adapter
    }

}

class NetworkScreen : NavigatableScreen<NetworkView>(), KodeinAware {

    override val kodein by closestKodein { activity }

    private val networkManager by instance<NetworkManager>()

    private val networkBox by instance<Box<Network>>()
    private var networkBoxObserver: DataSubscription? = null

    private val adapter = ItemAdapter<NetworkItem>()
    private val fastAdapter = adapter.buildFastAdapter()

    override fun createView(context: Context) = NetworkView(context).apply { bindAdapter(fastAdapter) }

    override fun onShow(context: Context) {
        super.onShow(context)

        networkManager.isNetworkTrusted()
        refresh()

        networkBoxObserver = networkBox.store
                .subscribeFor<Network>()
                .on(AndroidScheduler.mainThread())
                .onlyChanges()
                .observer { refresh() }
    }

    override fun onHide(context: Context) {
        networkBoxObserver?.cancel()
        networkBoxObserver = null

        super.onHide(context)
    }

    private fun refresh() {
        val networks = networkBox.all
                .sortedBy { it.name }
                .map {
                    NetworkItem(it) {
                        networkBox.put(it)
                    }
                }

        adapter.set(networks)
    }

    override fun onUpdateMenu(menu: Menu) {
        menu.findItem(R.id.refresh).apply {
            isVisible = true
            setOnMenuItemClickListener { networkManager.isNetworkTrusted(); true }
        }
    }

    override fun getTitle(context: Context) = context.getString(R.string.screen_networks)!!

}
