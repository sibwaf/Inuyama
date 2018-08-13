package ru.dyatel.inuyama.screens

import android.content.Context
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.Switch
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.iconics.IconicsDrawable
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
import org.jetbrains.anko.alignParentRight
import org.jetbrains.anko.cardview.v7.cardView
import org.jetbrains.anko.centerVertically
import org.jetbrains.anko.find
import org.jetbrains.anko.imageView
import org.jetbrains.anko.leftOf
import org.jetbrains.anko.margin
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.recyclerview.v7.recyclerView
import org.jetbrains.anko.relativeLayout
import org.jetbrains.anko.rightOf
import org.jetbrains.anko.support.v4.onRefresh
import org.jetbrains.anko.support.v4.swipeRefreshLayout
import org.jetbrains.anko.switch
import org.jetbrains.anko.textResource
import org.jetbrains.anko.textView
import org.jetbrains.anko.verticalLayout
import org.jetbrains.anko.wrapContent
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.layout.DIM_EXTRA_LARGE
import ru.dyatel.inuyama.layout.RuranobeVolumeItem
import ru.dyatel.inuyama.layout.SP_MEDIUM
import ru.dyatel.inuyama.model.RuranobeProject
import ru.dyatel.inuyama.model.RuranobeVolume
import ru.dyatel.inuyama.model.RuranobeVolume_
import ru.dyatel.inuyama.ruranobe.RuranobeWatcher
import ru.dyatel.inuyama.utilities.buildFastAdapter
import ru.dyatel.inuyama.utilities.subscribeFor

class RuranobeProjectView(context: Context) : BaseScreenView<RuranobeProjectScreen>(context) {

    private companion object {
        val iconViewId = View.generateViewId()
        val watchingSwitchId = View.generateViewId()
        val recyclerViewId = View.generateViewId()
    }

    private val swipeRefresh: SwipeRefreshLayout
    private val watchingSwitch: Switch
    private val recyclerView: RecyclerView

    var refreshing: Boolean
        get() = swipeRefresh.isRefreshing
        set(value) {
            swipeRefresh.isRefreshing = value
        }

    var watching: Boolean
        get() = watchingSwitch.isChecked
        set(value) {
            watchingSwitch.isChecked = value
        }

    init {
        swipeRefresh = swipeRefreshLayout {
            onRefresh { screen.fetch() }

            verticalLayout {
                lparams(width = matchParent, height = wrapContent)

                cardView {
                    lparams(width = matchParent, height = wrapContent)

                    relativeLayout {
                        lparams(width = matchParent, height = wrapContent) {
                            margin = DIM_EXTRA_LARGE
                        }

                        val icon = IconicsDrawable(context)
                                .icon(CommunityMaterial.Icon.cmd_glasses)
                                .sizeDp(24)
                                .colorRes(R.color.material_drawer_dark_secondary_text)

                        imageView(icon) {
                            id = iconViewId
                        }.lparams {
                            rightMargin = DIM_EXTRA_LARGE
                            centerVertically()
                        }

                        textView {
                            textResource = R.string.label_watching
                            textSize = SP_MEDIUM
                        }.lparams {
                            centerVertically()
                            rightOf(iconViewId)
                            leftOf(watchingSwitchId)
                        }

                        switch {
                            id = watchingSwitchId
                            setOnCheckedChangeListener { _, checked -> screen.switchWatching(checked) }
                        }.lparams {
                            centerVertically()
                            alignParentRight()
                        }
                    }
                }

                recyclerView {
                    lparams(width = matchParent, height = wrapContent)

                    id = recyclerViewId
                    layoutManager = LinearLayoutManager(context)
                }
            }
        }

        watchingSwitch = find(watchingSwitchId)
        recyclerView = find(recyclerViewId)
    }

    fun bindAdapter(adapter: RecyclerView.Adapter<*>) {
        recyclerView.adapter = adapter
    }

}

class RuranobeProjectScreen(private val project: RuranobeProject) : Screen<RuranobeProjectView>(), KodeinAware {

    override val kodein by closestKodein { activity }

    private val boxStore by instance<BoxStore>()
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

    override fun createView(context: Context) = RuranobeProjectView(context).apply { bindAdapter(fastAdapter) }

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

    fun reload() {
        val items = volumeQuery.find().map {
            RuranobeVolumeItem(it)
        }

        adapter.set(items)
    }

    fun fetch() {
        if (fetchTask != null) {
            return
        }

        fetchTask = launch(UI) {
            try {
                view.refreshing = true

                async {
                    watcher.syncVolumes(project)
                }.await()
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

    override fun getTitle(context: Context?) = project.title

}
