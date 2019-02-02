package ru.dyatel.inuyama.ruranobe

import android.content.Context
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.wealthfront.magellan.BaseScreenView
import io.objectbox.Box
import io.objectbox.kotlin.query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.anko.appcompat.v7.tintedButton
import org.jetbrains.anko.browse
import org.jetbrains.anko.horizontalMargin
import org.jetbrains.anko.margin
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.recyclerview.v7.recyclerView
import org.jetbrains.anko.support.v4.nestedScrollView
import org.jetbrains.anko.support.v4.onRefresh
import org.jetbrains.anko.support.v4.swipeRefreshLayout
import org.jetbrains.anko.verticalLayout
import org.jetbrains.anko.verticalMargin
import org.jetbrains.anko.wrapContent
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.layout.DIM_LARGE
import ru.dyatel.inuyama.layout.RuranobeVolumeItem
import ru.dyatel.inuyama.layout.components.DirectorySelector
import ru.dyatel.inuyama.layout.components.StatusBar
import ru.dyatel.inuyama.layout.components.directorySelector
import ru.dyatel.inuyama.layout.components.statusBar
import ru.dyatel.inuyama.layout.components.uniformTextView
import ru.dyatel.inuyama.model.Directory
import ru.dyatel.inuyama.model.RuranobeProject
import ru.dyatel.inuyama.model.RuranobeVolume
import ru.dyatel.inuyama.model.RuranobeVolume_
import ru.dyatel.inuyama.screens.InuScreen
import ru.dyatel.inuyama.utilities.buildFastAdapter
import ru.dyatel.inuyama.utilities.hideIf

class RuranobeProjectView(context: Context) : BaseScreenView<RuranobeProjectScreen>(context) {

    private val swipeRefresh: SwipeRefreshLayout

    private lateinit var statusBar: StatusBar

    private lateinit var romajiView: TextView
    private lateinit var statusView: TextView
    private lateinit var issueStatusView: TextView
    private lateinit var translationStatusView: TextView

    private lateinit var directorySelector: DirectorySelector

    private lateinit var recyclerView: RecyclerView

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

                statusBar = statusBar {
                    icon = CommunityMaterial.Icon.cmd_glasses
                    textResource = R.string.label_watching

                    switchEnabled = true
                    onSwitchChange { screen.switchWatching(it) }
                }

                nestedScrollView {
                    lparams(width = matchParent, height = matchParent)

                    descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS

                    verticalLayout {
                        lparams(width = matchParent, height = wrapContent) {
                            margin = DIM_LARGE
                        }

                        tintedButton(R.string.ruranobe_button_browse_project) {
                            setOnClickListener { screen.browseProject() }
                        }

                        verticalLayout {
                            lparams(width = matchParent, height = wrapContent) {
                                horizontalMargin = DIM_LARGE
                            }

                            romajiView = uniformTextView {
                                gravity = Gravity.CENTER_HORIZONTAL
                            }.lparams(width = matchParent) {
                                verticalMargin = DIM_LARGE
                            }

                            statusView = uniformTextView()
                            issueStatusView = uniformTextView()
                            translationStatusView = uniformTextView()

                            directorySelector = directorySelector {
                                onItemSelected { screen.changeDirectory(it) }
                            }.lparams(width = matchParent) {
                                topMargin = DIM_LARGE
                            }
                        }

                        recyclerView = recyclerView {
                            lparams(width = matchParent, height = wrapContent) {
                                topMargin = DIM_LARGE
                            }

                            layoutManager = LinearLayoutManager(context)
                            isNestedScrollingEnabled = false
                        }
                    }
                }
            }
        }
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

        translationStatusView.text = context.getString(R.string.ruranobe_label_status_translation, project.translationStatus)
        translationStatusView.hideIf { it.text.isNullOrBlank() }

        directorySelector.selected = project.directory.target
    }

    fun bindAdapter(adapter: RecyclerView.Adapter<*>) {
        recyclerView.adapter = adapter
    }

}

class RuranobeProjectScreen(private val project: RuranobeProject) : InuScreen<RuranobeProjectView>(), KodeinAware {

    override val titleText = project.title

    private val directoryBox by instance<Box<Directory>>()
    private val projectBox by instance<Box<RuranobeProject>>()
    private val volumeBox by instance<Box<RuranobeVolume>>()

    private val ruranobeApi by instance<RuranobeApi>()

    private val watcher by instance<RuranobeWatcher>()

    private val adapter = ItemAdapter<RuranobeVolumeItem>()
    private val fastAdapter = adapter.buildFastAdapter()

    private var fetchJobId = generateJobId()

    private val volumeQuery by lazy {
        volumeBox.query {
            equal(RuranobeVolume_.projectId, project.id)
            order(RuranobeVolume_.order)
        }
    }

    init {
        fastAdapter.withOnClickListener { _, _, item, _ ->
            val url = ruranobeApi.getVolumePageUrl(item.volume)
            context!!.browse(url)
            true
        }
    }

    override fun createView(context: Context): RuranobeProjectView {
        return RuranobeProjectView(context).apply {
            bindDirectories(directoryBox.all)
            bindProjectInfo(project)
            bindAdapter(fastAdapter)
        }
    }

    override fun onShow(context: Context) {
        super.onShow(context)

        view.watching = project.watching

        reload()
        observeChanges<RuranobeVolume>(::reload)
    }

    private fun reload() {
        val items = volumeQuery.find().map {
            RuranobeVolumeItem(it)
        }

        adapter.set(items)
    }

    fun fetch() {
        launchJob(id = fetchJobId) {
            try {
                view?.refreshing = true

                withContext(Dispatchers.Default) {
                    watcher.syncVolumes(project)
                }
            } finally {
                view?.refreshing = false
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

    fun browseProject() {
        val url = ruranobeApi.getProjectPageUrl(project)
        context!!.browse(url)
    }

}
