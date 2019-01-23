package ru.dyatel.inuyama.screens

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.wealthfront.magellan.BaseScreenView
import com.wealthfront.magellan.Screen
import io.objectbox.Box
import io.objectbox.BoxStore
import io.objectbox.android.AndroidScheduler
import io.objectbox.reactive.DataSubscription
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.find
import org.jetbrains.anko.margin
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.recyclerview.v7.recyclerView
import org.jetbrains.anko.support.v4.nestedScrollView
import org.jetbrains.anko.support.v4.onRefresh
import org.jetbrains.anko.support.v4.swipeRefreshLayout
import org.jetbrains.anko.verticalLayout
import org.jetbrains.anko.wrapContent
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.layout.DIM_EXTRA_LARGE
import ru.dyatel.inuyama.layout.DIM_LARGE
import ru.dyatel.inuyama.layout.DirectorySelector
import ru.dyatel.inuyama.layout.RuranobeVolumeItem
import ru.dyatel.inuyama.layout.StatusBar
import ru.dyatel.inuyama.layout.directorySelector
import ru.dyatel.inuyama.layout.statusBar
import ru.dyatel.inuyama.layout.uniformTextView
import ru.dyatel.inuyama.model.Directory
import ru.dyatel.inuyama.model.RuranobeProject
import ru.dyatel.inuyama.model.RuranobeVolume
import ru.dyatel.inuyama.model.RuranobeVolume_
import ru.dyatel.inuyama.ruranobe.RuranobeWatcher
import ru.dyatel.inuyama.utilities.buildFastAdapter
import ru.dyatel.inuyama.utilities.hideIf
import ru.dyatel.inuyama.utilities.subscribeFor

class RuranobeProjectView(context: Context) : BaseScreenView<RuranobeProjectScreen>(context) {

    private companion object {
        val statusBarId = View.generateViewId()

        val romajiViewId = View.generateViewId()
        val statusViewId = View.generateViewId()
        val issueStatusViewId = View.generateViewId()
        val translationStatusViewId = View.generateViewId()

        val directorySelectorId = View.generateViewId()

        val recyclerViewId = View.generateViewId()
    }

    private val swipeRefresh: SwipeRefreshLayout

    private val statusBar: StatusBar

    private val romajiView: TextView
    private val statusView: TextView
    private val issueStatusView: TextView
    private val translationStatusView: TextView

    private val directorySelector: DirectorySelector

    private val recyclerView: RecyclerView

    var refreshing: Boolean
        get() = swipeRefresh.isRefreshing
        set(value) {
            swipeRefresh.isRefreshing = value
        }

    var watching: Boolean
        get() = statusBar.switchState
        set(value) {
            statusBar.switchState = value
        }

    init {
        swipeRefresh = swipeRefreshLayout {
            onRefresh { screen.fetch() }

            verticalLayout {
                lparams(width = matchParent, height = wrapContent)

                statusBar {
                    id = statusBarId

                    icon = CommunityMaterial.Icon.cmd_glasses
                    textResource = R.string.label_watching

                    switchEnabled = true
                    onSwitchChange { screen.switchWatching(it) }
                }

                nestedScrollView {
                    lparams(width = matchParent, height = matchParent)

                    descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS

                    verticalLayout {
                        lparams(width = matchParent, height = wrapContent)

                        verticalLayout {
                            lparams(width = matchParent, height = wrapContent) {
                                margin = DIM_LARGE
                                leftMargin = DIM_EXTRA_LARGE
                                rightMargin = DIM_EXTRA_LARGE
                            }

                            uniformTextView {
                                id = romajiViewId
                                gravity = Gravity.CENTER_HORIZONTAL
                            }.lparams(width = matchParent) {
                                bottomMargin = DIM_LARGE
                            }

                            uniformTextView { id = statusViewId }
                            uniformTextView { id = issueStatusViewId }
                            uniformTextView { id = translationStatusViewId }

                            directorySelector {
                                id = directorySelectorId
                                onItemSelected { screen.changeDirectory(it) }
                            }.lparams(width = matchParent) {
                                topMargin = DIM_LARGE
                            }
                        }

                        recyclerView {
                            lparams(width = matchParent, height = wrapContent)

                            id = recyclerViewId
                            layoutManager = LinearLayoutManager(context)

                            isNestedScrollingEnabled = false
                        }
                    }
                }
            }
        }

        statusBar = find(statusBarId)

        romajiView = find(romajiViewId)
        statusView = find(statusViewId)
        issueStatusView = find(issueStatusViewId)
        translationStatusView = find(translationStatusViewId)

        directorySelector = find(directorySelectorId)

        recyclerView = find(recyclerViewId)
    }

    fun bindDirectories(directories: List<Directory>) {
        directorySelector.bindItems(directories)
    }

    fun bindProjectInfo(project: RuranobeProject) {
        romajiView.text = project.titleRomaji
        romajiView.hideIf { it.text.isNullOrBlank() }

        statusView.text = context.getString(R.string.ruranobe_label_status, project.status)
        statusView.hideIf { it.text.isNullOrBlank() }

        issueStatusView.text = context.getString(R.string.ruranobe_label_status_issue, project.issueStatus)
        issueStatusView.hideIf { it.text.isNullOrBlank() }

        translationStatusView.text =
                context.getString(R.string.ruranobe_label_status_translation, project.translationStatus)
        translationStatusView.hideIf { it.text.isNullOrBlank() }

        directorySelector.selected = project.directory.target
    }

    fun bindAdapter(adapter: RecyclerView.Adapter<*>) {
        recyclerView.adapter = adapter
    }

}

class RuranobeProjectScreen(private val project: RuranobeProject) : Screen<RuranobeProjectView>(), KodeinAware {

    override val kodein by closestKodein { activity }

    private val boxStore by instance<BoxStore>()
    private val directoryBox by instance<Box<Directory>>()
    private val projectBox by instance<Box<RuranobeProject>>()
    private val volumeBox by instance<Box<RuranobeVolume>>()

    private var boxObserver: DataSubscription? = null

    private val watcher by instance<RuranobeWatcher>()

    private val adapter = ItemAdapter<RuranobeVolumeItem>()
    private val fastAdapter = adapter.buildFastAdapter()

    private var fetchTask: Job? = null

    private val volumeQuery by lazy {
        volumeBox.query()
                .equal(RuranobeVolume_.projectId, project.id)
                .order(RuranobeVolume_.order)
                .build()
    }

    override fun createView(context: Context): RuranobeProjectView {
        return RuranobeProjectView(context).apply {
            bindDirectories(directoryBox.all)
            bindProjectInfo(project)
            bindAdapter(fastAdapter)
        }
    }

    override fun onShow(context: Context?) {
        super.onShow(context)

        view.watching = project.watching

        reload()
        boxObserver = boxStore
                .subscribeFor<RuranobeVolume>()
                .on(AndroidScheduler.mainThread())
                .onlyChanges()
                .observer { reload() }
    }

    override fun onHide(context: Context?) {
        fetchTask?.cancel()

        boxObserver?.cancel()
        boxObserver = null

        super.onHide(context)
    }

    private fun reload() {
        val items = volumeQuery.find().map {
            RuranobeVolumeItem(it)
        }

        adapter.set(items)
    }

    fun fetch() {
        if (fetchTask != null) {
            return
        }

        fetchTask = GlobalScope.launch(Dispatchers.Main) {
            try {
                view.refreshing = true

                withContext(Dispatchers.Default) {
                    watcher.syncVolumes(project)
                }
            } finally {
                view?.refreshing = false
                fetchTask = null
            }
        }
    }

    fun switchWatching(state: Boolean) {
        if (state == project.watching) {
            return
        }

        project.watching = state
        projectBox.put(project)
    }

    fun changeDirectory(directory: Directory?) {
        if (directory == project.directory.target) {
            return
        }

        project.directory.target = directory
        projectBox.put(project)
    }

    override fun getTitle(context: Context?) = project.title

}
