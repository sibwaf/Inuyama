package ru.dyatel.inuyama.screens

import android.content.Context
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.Menu
import android.view.View
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
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.RemoteService
import ru.dyatel.inuyama.layout.DIM_EXTRA_LARGE
import ru.dyatel.inuyama.layout.DIM_LARGE
import ru.dyatel.inuyama.layout.ModuleStateItem
import ru.dyatel.inuyama.layout.SP_MEDIUM
import ru.dyatel.inuyama.layout.State
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
    }

    val overseerState: TextView
    private val recyclerView: RecyclerView

    init {
        verticalLayout {
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

            recyclerView {
                lparams(width = matchParent, height = wrapContent) {
                    topMargin = DIM_LARGE
                }

                id = serviceRecyclerId
                layoutManager = LinearLayoutManager(context)

                overScrollMode = View.OVER_SCROLL_NEVER
            }
        }

        overseerState = find(overseerStateId)
        recyclerView = find(serviceRecyclerId)
    }

    fun bindAdapter(adapter: RecyclerView.Adapter<*>) {
        recyclerView.adapter = adapter
    }

}

class HomeScreen : Screen<HomeView>(), KodeinAware {

    override val kodein by closestKodein { activity }

    private val preferenceHelper by instance<PreferenceHelper>()

    private val services by allInstances<RemoteService>()
    private val checkers = mutableListOf<StateChecker>()

    private val adapter = ItemAdapter<ModuleStateItem>()
    private val fastAdapter = adapter.buildFastAdapter()

    private var overseerListener: OverseerListener? = null

    override fun createView(context: Context) = HomeView(context).apply { bindAdapter(fastAdapter) }

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
        }.also { it(OverseerWorker.isWorking) }

        for ((index, service) in services.withIndex()) {
            val item = ModuleStateItem(service.getName(context), State.PENDING)
            adapter.add(item)
            checkers += StateChecker(service) {
                item.state = it
                fastAdapter.notifyAdapterItemChanged(index)
            }
        }

        checkers.forEach { it.check() }
    }

    override fun onHide(context: Context) {
        overseerListener?.let { OverseerWorker.removeListener(it) }
        overseerListener = null

        checkers.forEach { it.cancel() }
        checkers.clear()
        adapter.clear()

        super.onHide(context)
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
