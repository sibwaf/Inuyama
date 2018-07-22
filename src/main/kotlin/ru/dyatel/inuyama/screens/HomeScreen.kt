package ru.dyatel.inuyama.screens

import android.content.Context
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.Gravity
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.iconics.IconicsDrawable
import com.wealthfront.magellan.BaseScreenView
import com.wealthfront.magellan.Screen
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.cardview.v7.cardView
import org.jetbrains.anko.find
import org.jetbrains.anko.frameLayout
import org.jetbrains.anko.imageView
import org.jetbrains.anko.linearLayout
import org.jetbrains.anko.margin
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.recyclerview.v7.recyclerView
import org.jetbrains.anko.textResource
import org.jetbrains.anko.textView
import org.jetbrains.anko.verticalLayout
import org.jetbrains.anko.wrapContent
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.allInstances
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.DASHBOARD_UPDATE_COUNT
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.RemoteService
import ru.dyatel.inuyama.Watcher
import ru.dyatel.inuyama.layout.DIM_EXTRA_LARGE
import ru.dyatel.inuyama.layout.DIM_LARGE
import ru.dyatel.inuyama.layout.ModuleStateItem
import ru.dyatel.inuyama.layout.SP_MEDIUM
import ru.dyatel.inuyama.layout.State
import ru.dyatel.inuyama.layout.UpdateItem
import ru.dyatel.inuyama.overseer.OverseerListener
import ru.dyatel.inuyama.overseer.OverseerStarter
import ru.dyatel.inuyama.overseer.OverseerWorker
import ru.dyatel.inuyama.utilities.PreferenceHelper
import ru.dyatel.inuyama.utilities.asDate
import ru.dyatel.inuyama.utilities.buildFastAdapter
import ru.dyatel.inuyama.utilities.ctx
import ru.dyatel.inuyama.utilities.prettyTime

class HomeView(context: Context) : BaseScreenView<HomeScreen>(context) {

    private companion object {
        val overseerStateId = View.generateViewId()
        val serviceRecyclerId = View.generateViewId()
        val updateRecyclerId = View.generateViewId()
    }

    val overseerState: TextView
    private val serviceRecycler: RecyclerView
    private val updateRecycler: RecyclerView

    init {
        verticalLayout {
            lparams(width = matchParent, height = matchParent)

            cardView {
                lparams(width = matchParent, height = wrapContent)

                linearLayout {
                    lparams(width = matchParent, height = wrapContent) {
                        margin = DIM_EXTRA_LARGE
                    }

                    val icon = IconicsDrawable(context)
                            .icon(CommunityMaterial.Icon.cmd_update)
                            .sizeDp(24)
                            .colorRes(R.color.material_drawer_dark_secondary_text)

                    imageView(icon).lparams { rightMargin = DIM_EXTRA_LARGE }

                    textView {
                        id = overseerStateId
                        textSize = SP_MEDIUM
                    }
                }

                setOnClickListener {
                    screen.requestOverseerCheck()
                }
            }

            verticalLayout {
                lparams(width = matchParent, height = matchParent) {
                    topMargin = DIM_LARGE
                }

                container(R.string.container_services) {
                    recyclerView {
                        lparams(width = matchParent, height = wrapContent)

                        id = serviceRecyclerId
                        layoutManager = LinearLayoutManager(context)

                        overScrollMode = View.OVER_SCROLL_NEVER
                    }
                }

                container(R.string.container_update_list) {
                    recyclerView {
                        lparams(width = matchParent, height = wrapContent)

                        id = updateRecyclerId
                        layoutManager = LinearLayoutManager(context)

                        overScrollMode = View.OVER_SCROLL_NEVER
                    }
                }
            }
        }

        overseerState = find(overseerStateId)
        serviceRecycler = find(serviceRecyclerId)
        updateRecycler = find(updateRecyclerId)
    }

    private fun ViewGroup.container(titleResource: Int, init: ViewGroup.() -> Unit) {
        cardView {
            lparams(width = matchParent, height = wrapContent) {
                margin = DIM_LARGE
            }

            verticalLayout {
                lparams(width = matchParent, height = wrapContent) {
                    margin = DIM_EXTRA_LARGE
                }

                frameLayout {
                    lparams(width = matchParent, height = wrapContent) {
                        bottomMargin = DIM_LARGE
                    }

                    textView {
                        textResource = titleResource
                        gravity = Gravity.CENTER
                        textSize = SP_MEDIUM
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

}

class HomeScreen : Screen<HomeView>(), KodeinAware {

    override val kodein by closestKodein { activity }

    private val preferenceHelper by instance<PreferenceHelper>()

    private val services by allInstances<RemoteService>()
    private val watchers by allInstances<Watcher>()

    private val checkers = mutableListOf<StateChecker>()
    private var updateListener: (() -> Unit)? = null

    private val serviceAdapter = ItemAdapter<ModuleStateItem>()
    private val serviceFastAdapter = serviceAdapter.buildFastAdapter()

    private val updateAdapter = ItemAdapter<UpdateItem>()
    private val updateFastAdapter = updateAdapter.buildFastAdapter()

    private var overseerListener: OverseerListener? = null

    override fun createView(context: Context): HomeView {
        return HomeView(context).apply {
            bindServiceAdapter(serviceFastAdapter)
            bindUpdateAdapter(updateFastAdapter)
        }
    }

    override fun onShow(context: Context) {
        super.onShow(context)

        overseerListener = OverseerWorker.addListener { state ->
            if (state) {
                view.overseerState.textResource = R.string.label_checking
            } else {
                val lastCheckText = preferenceHelper.lastCheck
                        ?.let { prettyTime.format(it.asDate) }
                        ?: context.getString(R.string.const_never)
                view.overseerState.text = context.getString(R.string.label_last_check, lastCheckText)
            }
        }.also {
            it(OverseerWorker.isWorking)
        }

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
        }
    }

    fun requestOverseerCheck() {
        if (!OverseerWorker.isWorking) {
            view.overseerState.textResource = R.string.label_waiting_for_task
            OverseerStarter.start(ctx, true)
        }
    }

    override fun onUpdateMenu(menu: Menu) {
        menu.findItem(R.id.refresh).apply {
            isVisible = true
            setOnMenuItemClickListener { checkers.forEach { it.check() }; true }
        }
    }

    override fun getTitle(context: Context) = context.getString(R.string.screen_home)!!
}

private class StateChecker(private val service: RemoteService, private val onUpdate: (State) -> Unit) {

    private var coroutine: Job? = null

    fun check() {
        if (coroutine != null) {
            return
        }

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
