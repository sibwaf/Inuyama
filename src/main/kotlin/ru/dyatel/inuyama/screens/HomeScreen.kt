package ru.dyatel.inuyama.screens

import android.content.Context
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.wealthfront.magellan.BaseScreenView
import com.wealthfront.magellan.Screen
import io.objectbox.Box
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.backgroundColorResource
import org.jetbrains.anko.bottomPadding
import org.jetbrains.anko.cardview.v7.cardView
import org.jetbrains.anko.find
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
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.allInstances
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.BuildConfig
import ru.dyatel.inuyama.DASHBOARD_UPDATE_COUNT
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.RemoteService
import ru.dyatel.inuyama.Watcher
import ru.dyatel.inuyama.layout.DIM_EXTRA_LARGE
import ru.dyatel.inuyama.layout.DIM_LARGE
import ru.dyatel.inuyama.layout.DIM_MEDIUM
import ru.dyatel.inuyama.layout.ModuleStateItem
import ru.dyatel.inuyama.layout.ProxySelector
import ru.dyatel.inuyama.layout.State
import ru.dyatel.inuyama.layout.StatusBar
import ru.dyatel.inuyama.layout.UpdateItem
import ru.dyatel.inuyama.layout.proxySelector
import ru.dyatel.inuyama.layout.statusBar
import ru.dyatel.inuyama.layout.uniformTextView
import ru.dyatel.inuyama.model.Proxy
import ru.dyatel.inuyama.model.ProxyBinding
import ru.dyatel.inuyama.overseer.OverseerListener
import ru.dyatel.inuyama.overseer.OverseerStarter
import ru.dyatel.inuyama.overseer.OverseerWorker
import ru.dyatel.inuyama.utilities.PreferenceHelper
import ru.dyatel.inuyama.utilities.asDate
import ru.dyatel.inuyama.utilities.buildFastAdapter
import ru.dyatel.inuyama.utilities.ctx
import ru.dyatel.inuyama.utilities.hideIf
import ru.dyatel.inuyama.utilities.prettyTime

class HomeScreenView(context: Context) : BaseScreenView<HomeScreen>(context) {

    private companion object {
        val statusBarId = View.generateViewId()
        val serviceRecyclerId = View.generateViewId()
        val noUpdatesMarkerId = View.generateViewId()
        val updateRecyclerId = View.generateViewId()
    }

    val statusBar: StatusBar
    private val serviceRecycler: RecyclerView
    private val noUpdatesMarker: View
    private val updateRecycler: RecyclerView

    init {
        verticalLayout {
            lparams(width = matchParent, height = matchParent)

            statusBar {
                id = statusBarId

                icon = CommunityMaterial.Icon.cmd_update
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
                        recyclerView {
                            lparams(width = matchParent, height = wrapContent)

                            id = serviceRecyclerId
                            layoutManager = LinearLayoutManager(context)

                            isNestedScrollingEnabled = false
                        }

                        setOnClickListener {
                            screen.requestServiceCheck()
                        }
                    }

                    container(R.string.container_update_list) {
                        uniformTextView {
                            id = noUpdatesMarkerId
                            textResource = R.string.label_no_updates
                        }

                        recyclerView {
                            lparams(width = matchParent, height = wrapContent)

                            id = updateRecyclerId
                            layoutManager = LinearLayoutManager(context)

                            isNestedScrollingEnabled = false
                        }
                    }
                }
            }
        }

        statusBar = find(statusBarId)
        serviceRecycler = find(serviceRecyclerId)
        noUpdatesMarker = find(noUpdatesMarkerId)
        updateRecycler = find(updateRecyclerId)
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
        val hasUpdates = updateRecycler.adapter.itemCount > 0

        noUpdatesMarker.hideIf(hasUpdates)
        updateRecycler.hideIf(!hasUpdates)
    }

}

class HomeScreen : Screen<HomeScreenView>(), KodeinAware {

    private companion object {
        val proxySelectorId = View.generateViewId()
    }

    override val kodein by closestKodein { activity }

    private val preferenceHelper by instance<PreferenceHelper>()

    private val proxyBox by instance<Box<Proxy>>()
    private val proxyBindingBox by instance<Box<ProxyBinding>>()

    private val services by allInstances<RemoteService>()
    private val watchers by allInstances<Watcher>()

    private val checkers = mutableListOf<StateChecker>()
    private var updateListener: (() -> Unit)? = null

    private val serviceAdapter = ItemAdapter<ModuleStateItem>()
    private val serviceFastAdapter = serviceAdapter.buildFastAdapter()

    private val updateAdapter = ItemAdapter<UpdateItem>()
    private val updateFastAdapter = updateAdapter.buildFastAdapter()

    private var overseerListener: OverseerListener? = null

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

        val services = services.sortedBy { it.getName(context) }
        for ((index, service) in services.withIndex()) {
            val item = ModuleStateItem(service.getName(context), State.PENDING)
            serviceAdapter.add(item)

            checkers += StateChecker(service) {
                item.state = it
                serviceFastAdapter.notifyAdapterItemChanged(index)
            }.also {
                it.check()
            }
        }

        serviceFastAdapter.withOnClickListener { _, _, _, _ ->
            requestServiceCheck()
            true
        }
        serviceFastAdapter.withOnLongClickListener { _, _, _, position ->
            editProxy(services[position])
            true
        }

        updateListener = {
            reloadUpdates()
        }.also { listener ->
            watchers.forEach {
                it.addUpdateListener(listener)
            }

            listener()
        }
    }

    override fun onHide(context: Context) {
        overseerListener?.let {
            OverseerWorker.removeListener(it)
            overseerListener = null
        }

        checkers.forEach { it.cancel() }
        checkers.clear()

        updateListener?.let { listener ->
            watchers.forEach {
                it.removeUpdateListener(listener)
            }
            updateListener = null
        }

        serviceAdapter.clear()
        updateAdapter.clear()

        super.onHide(context)
    }

    private fun reloadUpdates() {
        val updates = watchers
                .flatMap { it.listUpdates() }
                .sortedByDescending { it.timestamp }
                .take(DASHBOARD_UPDATE_COUNT)
                .map { UpdateItem(it) }

        launch(UI) {
            updateAdapter.set(updates)
            view.refreshUpdateList()
        }
    }

    private fun editProxy(service: RemoteService) {
        val binding = proxyBindingBox[service.serviceId] ?: ProxyBinding(service.serviceId)

        val view = ctx.frameLayout {
            lparams(width = matchParent, height = wrapContent) {
                padding = DIM_EXTRA_LARGE
            }

            proxySelector {
                id = proxySelectorId

                bindItems(proxyBox.all)
                selected = binding.proxy.target
            }
        }

        AlertDialog.Builder(activity)
                .setTitle(R.string.dialog_select_proxy)
                .setView(view)
                .setPositiveButton(R.string.action_save) { _, _ ->
                    binding.proxy.target = view.find<ProxySelector>(proxySelectorId).selected
                    proxyBindingBox.put(binding)
                }
                .setNegativeButton(R.string.action_cancel) { _, _ -> }
                .show()
    }

    fun requestOverseerCheck() {
        if (!OverseerWorker.isWorking) {
            view.statusBar.textResource = R.string.label_waiting_for_task
            OverseerStarter.start(ctx, true)
        }
    }

    fun requestServiceCheck() {
        for (checker in checkers) {
            checker.check()
        }
    }

    override fun getTitle(context: Context): String {
        var title = context.getString(R.string.screen_home);
        if (BuildConfig.DEBUG) {
            title += " / Debug"
        }
        return title
    }
}

private class StateChecker(private val service: RemoteService, private val onUpdate: (State) -> Unit) {

    private var coroutine: Job? = null

    fun check() {
        cancel()

        coroutine = launch(UI) {
            onUpdate(State.PENDING)

            val state = async {
                if (service.checkConnection()) State.OK else State.FAIL
            }.await()

            onUpdate(state)

            coroutine = null
        }
    }

    fun cancel() {
        coroutine?.cancel()
        coroutine = null
    }

}
