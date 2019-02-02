package ru.dyatel.inuyama.ruranobe

import android.content.Context
import android.view.View
import android.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.wealthfront.magellan.BaseScreenView
import io.objectbox.Box
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.anko.find
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.recyclerview.v7.recyclerView
import org.jetbrains.anko.support.v4.onRefresh
import org.jetbrains.anko.support.v4.swipeRefreshLayout
import org.jetbrains.anko.wrapContent
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.layout.RuranobeProjectItem
import ru.dyatel.inuyama.model.RuranobeProject
import ru.dyatel.inuyama.model.RuranobeVolume
import ru.dyatel.inuyama.screens.InuScreen
import ru.dyatel.inuyama.utilities.buildFastAdapter
import ru.dyatel.inuyama.utilities.isVisible

class RuranobeView(context: Context) : BaseScreenView<RuranobeScreen>(context) {

    private companion object {
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
        swipeRefresh = swipeRefreshLayout {
            onRefresh { screen.fetch() }

            recyclerView {
                lparams(width = matchParent, height = wrapContent)

                id = recyclerViewId
                layoutManager = LinearLayoutManager(context)

                (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
            }
        }

        recyclerView = find(recyclerViewId)
    }

    fun bindAdapter(adapter: RecyclerView.Adapter<*>) {
        recyclerView.adapter = adapter
    }

}

class RuranobeScreen : InuScreen<RuranobeView>(), KodeinAware {

    private companion object {
        val projectComparator = Comparator<RuranobeProject> { first, second ->
            if (first.watching && !second.watching) {
                return@Comparator -1
            }
            if (!first.watching && second.watching) {
                return@Comparator 1
            }

            if (!first.works && second.works) {
                return@Comparator -1
            }
            if (first.works && !second.works) {
                return@Comparator 1
            }

            first.title.compareTo(second.title)
        }
    }

    override val titleResource = R.string.module_ruranobe

    private val projectBox by instance<Box<RuranobeProject>>()

    private val watcher by instance<RuranobeWatcher>()

    private val adapter = ItemAdapter<RuranobeProjectItem>()
    private val fastAdapter = adapter.buildFastAdapter()

    private var fetchJobId = generateJobId()

    init {
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

        activity!!.searchView.apply {
            isVisible = true

            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
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

        observeChanges<RuranobeProject>(::refresh)
        observeChanges<RuranobeVolume>(::refresh)

        refresh()
        if (adapter.adapterItemCount == 0) {
            fetch()
        }
    }

    private fun refresh() {
        val projects = projectBox.all
                .sortedWith(projectComparator)
                .map {
                    RuranobeProjectItem(it)
                            .withOnItemClickListener { _, _, item, _ ->
                                navigator.goTo(RuranobeProjectScreen(item.project))
                                true
                            }
                }

        adapter.set(projects)
    }

    fun fetch() {
        launchJob(id = fetchJobId) {
            try {
                view?.refreshing = true

                withContext(Dispatchers.Default) {
                    watcher.syncProjects()

                    for (project in projectBox.all) {
                        watcher.syncVolumes(project)
                    }
                }
            } finally {
                view?.refreshing = false
            }
        }
    }

}
