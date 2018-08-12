package ru.dyatel.inuyama.screens

import android.content.Context
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.support.v7.widget.SimpleItemAnimator
import android.view.Menu
import android.view.View
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.utils.ComparableItemListImpl
import com.wealthfront.magellan.BaseScreenView
import com.wealthfront.magellan.Screen
import io.objectbox.Box
import io.objectbox.BoxStore
import io.objectbox.android.AndroidScheduler
import io.objectbox.reactive.DataSubscription
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.find
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.recyclerview.v7.recyclerView
import org.jetbrains.anko.support.v4.onRefresh
import org.jetbrains.anko.support.v4.swipeRefreshLayout
import org.jetbrains.anko.wrapContent
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.layout.RuranobeProjectItem
import ru.dyatel.inuyama.model.RuranobeProject
import ru.dyatel.inuyama.model.RuranobeVolume
import ru.dyatel.inuyama.ruranobe.RuranobeWatcher
import ru.dyatel.inuyama.utilities.buildFastAdapter

class RuranobeView(context: Context) : BaseScreenView<RuranobeScreen>(context) {

    private companion object {
        private val swipeRefreshId = View.generateViewId()
        private val recyclerViewId = View.generateViewId()
    }

    private val swipeRefresh: SwipeRefreshLayout
    private val recyclerView: RecyclerView

    var refreshing: Boolean
        get() = swipeRefresh.isRefreshing
        set(value) {
            swipeRefresh.isRefreshing = value
        }

    init {
        swipeRefreshLayout {
            id = swipeRefreshId

            onRefresh { screen.fetch() }

            recyclerView {
                lparams(width = matchParent, height = wrapContent)

                id = recyclerViewId
                layoutManager = LinearLayoutManager(context)

                (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
            }
        }

        swipeRefresh = find(swipeRefreshId)
        recyclerView = find(recyclerViewId)
    }

    fun bindAdapter(adapter: RecyclerView.Adapter<*>) {
        recyclerView.adapter = adapter
    }

}

class RuranobeScreen : Screen<RuranobeView>(), KodeinAware {

    override val kodein by closestKodein { activity }

    private val boxStore by instance<BoxStore>()
    private val projectBox by instance<Box<RuranobeProject>>()

    private var boxObserver: DataSubscription? = null

    private val watcher by instance<RuranobeWatcher>()

    private val adapter: ItemAdapter<RuranobeProjectItem>
    private val fastAdapter: FastAdapter<RuranobeProjectItem>

    private var fetchTask: Job? = null

    init {
        val comparableList = ComparableItemListImpl<RuranobeProjectItem> { item1, item2 ->
            val first = item1.project
            val second = item2.project

            if (first.watching && !second.watching) {
                return@ComparableItemListImpl -1
            }
            if (!first.watching && second.watching) {
                return@ComparableItemListImpl 1
            }

            if (!first.works && second.works) {
                return@ComparableItemListImpl -1
            }
            if (first.works && !second.works) {
                return@ComparableItemListImpl 1
            }

            first.title.compareTo(second.title)
        }

        adapter = ItemAdapter(comparableList)
        fastAdapter = adapter.buildFastAdapter()

        adapter.itemFilter.withFilterPredicate { item, constraint ->
            if (constraint == null) {
                return@withFilterPredicate true
            }

            val itemTokens = listOfNotNull(item.project.title, item.project.titleRomaji)
                    .flatMap { it.split(" ") }
                    .filter { it.isNotEmpty() }

            val constraintTokens = constraint.split(" ").filter { it.isNotEmpty() }

            for (token in constraintTokens) {
                if (itemTokens.none { it.contains(token, true) }) {
                    return@withFilterPredicate false
                }
            }

            return@withFilterPredicate true
        }
    }

    override fun createView(context: Context) = RuranobeView(context).apply { bindAdapter(fastAdapter) }

    override fun onShow(context: Context) {
        super.onShow(context)

        boxObserver = boxStore
                .subscribe()
                .on(AndroidScheduler.mainThread())
                .onlyChanges()
                .observer {
                    if (it != RuranobeVolume::class.java && it != RuranobeProject::class.java) {
                        return@observer
                    }

                    reload()
                }

        reload()
        if (adapter.adapterItemCount == 0) {
            fetch()
        }
    }

    override fun onHide(context: Context) {
        fetchTask?.cancel()

        boxObserver?.cancel()
        boxObserver = null

        super.onHide(context)
    }

    private fun reload() {
        val projects = projectBox.all.map {
            RuranobeProjectItem(it)
        }

        adapter.setNewList(projects)
    }

    fun fetch() {
        if (fetchTask != null) {
            return
        }

        fetchTask = launch(UI) {
            try {
                view.refreshing = true

                async {
                    watcher.syncProjects()

                    for (project in projectBox.all) {
                        watcher.syncVolumes(project)
                    }
                }.await()
            } finally {
                view?.refreshing = false
                fetchTask = null
            }
        }
    }

    override fun getTitle(context: Context) = context.getString(R.string.module_ruranobe)!!

    override fun onUpdateMenu(menu: Menu) {
        menu.findItem(R.id.search).apply {
            isVisible = true

            val actionView = actionView as SearchView

            actionView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    adapter.filter(query)
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    adapter.filter(newText)
                    return true
                }
            })
        }
    }
}
