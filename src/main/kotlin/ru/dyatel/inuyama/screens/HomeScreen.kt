package ru.dyatel.inuyama.screens

import android.content.Context
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.Menu
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.wealthfront.magellan.BaseScreenView
import com.wealthfront.magellan.Screen
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.recyclerview.v7.recyclerView
import org.jetbrains.anko.wrapContent
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.allInstances
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.RemoteService
import ru.dyatel.inuyama.utilities.buildFastAdapter
import ru.dyatel.inuyama.layout.ModuleStateItem
import ru.dyatel.inuyama.layout.State

class HomeView(context: Context) : BaseScreenView<HomeScreen>(context) {

    private val recyclerView: RecyclerView

    init {
        recyclerView = recyclerView {
            lparams(width = matchParent, height = wrapContent)

            layoutManager = LinearLayoutManager(context)
        }
    }

    fun bindAdapter(adapter: RecyclerView.Adapter<*>) {
        recyclerView.adapter = adapter
    }

}

class HomeScreen : Screen<HomeView>(), KodeinAware {

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

    override val kodein by closestKodein { activity }

    private val services by allInstances<RemoteService>()
    private val checkers = mutableListOf<StateChecker>()

    private val adapter = ItemAdapter<ModuleStateItem>()
    private val fastAdapter = adapter.buildFastAdapter()

    override fun createView(context: Context) = HomeView(context).apply { bindAdapter(fastAdapter) }

    override fun onShow(context: Context) {
        super.onShow(context)

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
        checkers.forEach { it.cancel() }
        checkers.clear()
        adapter.clear()

        super.onHide(context)
    }

    override fun onUpdateMenu(menu: Menu) {
        menu.findItem(R.id.refresh).apply {
            isVisible = true
            setOnMenuItemClickListener { checkers.forEach { it.check() }; true }
        }
    }

    override fun getTitle(context: Context) = context.getString(R.string.screen_home)!!
}
