package ru.dyatel.inuyama.screens

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.wealthfront.magellan.BaseScreenView
import io.objectbox.Box
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.anko.backgroundColorResource
import org.jetbrains.anko.bottomPadding
import org.jetbrains.anko.cardview.v7.cardView
import org.jetbrains.anko.frameLayout
import org.jetbrains.anko.margin
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.padding
import org.jetbrains.anko.recyclerview.v7.recyclerView
import org.jetbrains.anko.support.v4.nestedScrollView
import org.jetbrains.anko.textResource
import org.jetbrains.anko.topPadding
import org.jetbrains.anko.verticalLayout
import org.jetbrains.anko.wrapContent
import org.kodein.di.KodeinAware
import org.kodein.di.generic.allInstances
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.BuildConfig
import ru.dyatel.inuyama.DASHBOARD_UPDATE_COUNT
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.Watcher
import ru.dyatel.inuyama.layout.ProxySelector
import ru.dyatel.inuyama.layout.RemoteServiceStateItem
import ru.dyatel.inuyama.layout.UpdateItem
import ru.dyatel.inuyama.layout.proxySelector
import ru.dyatel.inuyama.model.Proxy
import ru.dyatel.inuyama.model.ProxyBinding
import ru.dyatel.inuyama.overseer.OverseerListener
import ru.dyatel.inuyama.overseer.OverseerStarter
import ru.dyatel.inuyama.overseer.OverseerWorker
import ru.dyatel.inuyama.utilities.PreferenceHelper
import ru.dyatel.inuyama.utilities.buildFastAdapter
import ru.dyatel.inuyama.utilities.prettyTime
import ru.sibwaf.inuyama.common.utilities.asDate
import sibwaf.inuyama.app.common.DIM_EXTRA_LARGE
import sibwaf.inuyama.app.common.DIM_LARGE
import sibwaf.inuyama.app.common.DIM_MEDIUM
import sibwaf.inuyama.app.common.ProxyableRemoteService
import sibwaf.inuyama.app.common.RemoteService
import sibwaf.inuyama.app.common.components.StatusBar
import sibwaf.inuyama.app.common.components.statusBar
import sibwaf.inuyama.app.common.components.uniformTextView

class HomeScreenView(context: Context) : BaseScreenView<HomeScreen>(context) {

    lateinit var statusBar: StatusBar
        private set

    private lateinit var serviceRecycler: RecyclerView
    private lateinit var noUpdatesMarker: View
    private lateinit var updateRecycler: RecyclerView

    init {
        verticalLayout {
            lparams(width = matchParent, height = matchParent)

            statusBar = statusBar {
                icon = CommunityMaterial.Icon2.cmd_update
                switchEnabled = false

                setOnClickListener { screen.requestOverseerCheck() }
            }

            nestedScrollView {
                lparams(width = matchParent, height = matchParent)

                verticalLayout {
                    lparams(width = matchParent, height = wrapContent) {
                        topPadding = DIM_LARGE
                        bottomPadding = DIM_LARGE
                    }

                    container(R.string.container_services) {
                        serviceRecycler = recyclerView {
                            lparams(width = matchParent, height = wrapContent)

                            layoutManager = LinearLayoutManager(context)
                            isNestedScrollingEnabled = false
                        }

                        setOnClickListener {
                            screen.requestServiceCheck()
                        }
                    }

                    container(R.string.container_update_list) {
                        noUpdatesMarker = uniformTextView {
                            textResource = R.string.label_no_updates
                        }

                        updateRecycler = recyclerView {
                            lparams(width = matchParent, height = wrapContent)

                            layoutManager = LinearLayoutManager(context)
                            isNestedScrollingEnabled = false
                        }
                    }
                }
            }
        }
    }

    private fun ViewGroup.container(titleResource: Int, init: ViewGroup.() -> Unit) {
        cardView {
            lparams(width = matchParent, height = wrapContent) {
                margin = DIM_LARGE
            }

            verticalLayout {
                lparams(width = matchParent, height = wrapContent) {
                    padding = DIM_EXTRA_LARGE
                }

                frameLayout {
                    lparams(width = matchParent, height = wrapContent) {
                        bottomMargin = DIM_LARGE
                        padding = DIM_MEDIUM
                    }

                    backgroundColorResource = R.color.color_primary

                    uniformTextView {
                        textResource = titleResource
                        gravity = Gravity.CENTER
                    }
                }

                init()
            }
        }
    }

    fun bindServiceAdapter(adapter: RecyclerView.Adapter<*>) {
        serviceRecycler.adapter = adapter
    }

    fun bindUpdateAdapter(adapter: RecyclerView.Adapter<*>) {
        updateRecycler.adapter = adapter
    }

    fun refreshUpdateList() {
        val hasUpdates = updateRecycler.adapter!!.itemCount > 0

        noUpdatesMarker.isVisible = !hasUpdates
        updateRecycler.isVisible = hasUpdates
    }

}

class HomeScreen : InuScreen<HomeScreenView>(), KodeinAware {

    private val preferenceHelper by instance<PreferenceHelper>()

    private val proxyBox by instance<Box<Proxy>>()
    private val proxyBindingBox by instance<Box<ProxyBinding>>()

    private val services by allInstances<RemoteService>()
    private val serviceCheckerIds by lazy { services.map { it to generateJobId() }.toMap() }

    private val serviceAdapter = ItemAdapter<RemoteServiceStateItem>()
    private val serviceFastAdapter = serviceAdapter.buildFastAdapter()

    private val watchers by allInstances<Watcher>()

    private val updateAdapter = ItemAdapter<UpdateItem>()
    private val updateFastAdapter = updateAdapter.buildFastAdapter()

    private var overseerListener: OverseerListener? = null

    init {
        serviceFastAdapter
                .withOnClickListener { _, _, _, _ ->
                    requestServiceCheck()
                    true
                }
                .withOnLongClickListener { _, _, item, _ ->
                    if (item.service is ProxyableRemoteService) {
                        editProxy(item.service)
                        true
                    } else {
                        false
                    }
                }
    }

    override fun createView(context: Context): HomeScreenView {
        return HomeScreenView(context).apply {
            bindServiceAdapter(serviceFastAdapter)
            bindUpdateAdapter(updateFastAdapter)
        }
    }

    override fun onShow(context: Context) {
        super.onShow(context)

        overseerListener = OverseerWorker.addListener { state ->
            if (state) {
                view.statusBar.textResource = R.string.label_checking
            } else {
                val lastCheckText = preferenceHelper.lastCheck
                        ?.let { prettyTime.format(it.asDate) }
                        ?: context.getString(R.string.const_never)
                view.statusBar.text = context.getString(R.string.label_last_check, lastCheckText)
            }
        }.also {
            it(OverseerWorker.isWorking)
        }

        serviceAdapter.set(services
                .sortedBy { it.getName(context) }
                .map { RemoteServiceStateItem(it, R.color.color_pending) })
        requestServiceCheck()

        for (watcher in watchers) {
            watcher.addUpdateListener(::refreshUpdates)
        }
        refreshUpdates()
    }

    override fun onHide(context: Context) {
        overseerListener?.let {
            OverseerWorker.removeListener(it)
            overseerListener = null
        }

        for (watcher in watchers) {
            watcher.removeUpdateListener(::refreshUpdates)
        }

        super.onHide(context)
    }

    private fun refreshUpdates() {
        val updates = watchers
                .flatMap { it.listUpdates() }
                .sortedByDescending { it.timestamp }
                .take(DASHBOARD_UPDATE_COUNT)
                .map { UpdateItem(it) }

        // Updates can come from background threads
        launchJob {
            updateAdapter.set(updates)
            view?.refreshUpdateList()
        }
    }

    private fun editProxy(service: ProxyableRemoteService) {
        val binding = proxyBindingBox[service.serviceId] ?: ProxyBinding(service.serviceId)

        lateinit var proxySelectorView: ProxySelector

        val view = context!!.frameLayout {
            lparams(width = matchParent, height = wrapContent) {
                padding = DIM_EXTRA_LARGE
            }

            proxySelectorView = proxySelector {
                bindItems(proxyBox.all)
                selected = binding.proxy.target
            }
        }

        AlertDialog.Builder(context!!)
                .setTitle(R.string.dialog_select_proxy)
                .setView(view)
                .setPositiveButton(R.string.action_save) { _, _ ->
                    binding.proxy.target = proxySelectorView.selected
                    proxyBindingBox.put(binding)
                }
                .setNegativeButton(R.string.action_cancel) { _, _ -> }
                .show()
    }

    fun requestOverseerCheck() {
        if (!OverseerWorker.isWorking) {
            view.statusBar.textResource = R.string.label_waiting_for_task
            OverseerStarter.start(context!!, true)
        }
    }

    fun requestServiceCheck() {
        for ((index, item) in serviceAdapter.adapterItems.withIndex()) {
            val service = item.service

            launchJob(id = serviceCheckerIds.getValue(service), replacing = true) {
                item.markerColor = R.color.color_pending
                serviceFastAdapter.notifyAdapterItemChanged(index)

                val color = withContext(Dispatchers.Default) {
                    if (service.checkConnection()) R.color.color_ok else R.color.color_fail
                }

                item.markerColor = color
                serviceFastAdapter.notifyAdapterItemChanged(index)
            }
        }
    }

    override fun getTitle(context: Context): String {
        var title = context.getString(R.string.screen_home)
        if (BuildConfig.DEBUG) {
            title += " / Debug"
        }
        return title
    }
}
