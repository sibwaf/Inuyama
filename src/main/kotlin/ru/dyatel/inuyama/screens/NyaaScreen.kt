package ru.dyatel.inuyama.screens

import android.app.AlertDialog
import android.content.Context
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.Menu
import android.view.View
import android.widget.EditText
import android.widget.Spinner
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.wealthfront.magellan.BaseScreenView
import com.wealthfront.magellan.Screen
import io.objectbox.Box
import io.objectbox.BoxStore
import io.objectbox.android.AndroidScheduler
import io.objectbox.reactive.DataSubscription
import org.jetbrains.anko.appcompat.v7.tintedSpinner
import org.jetbrains.anko.find
import org.jetbrains.anko.hintResource
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.padding
import org.jetbrains.anko.recyclerview.v7.recyclerView
import org.jetbrains.anko.verticalLayout
import org.jetbrains.anko.wrapContent
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.layout.DIM_EXTRA_LARGE
import ru.dyatel.inuyama.layout.DatePicker
import ru.dyatel.inuyama.layout.NyaaWatchItem
import ru.dyatel.inuyama.layout.uniformTextInput
import ru.dyatel.inuyama.model.Directory
import ru.dyatel.inuyama.model.NyaaTorrent
import ru.dyatel.inuyama.model.NyaaTorrent_
import ru.dyatel.inuyama.model.NyaaWatch
import ru.dyatel.inuyama.utilities.DirectorySelector
import ru.dyatel.inuyama.utilities.buildFastAdapter
import ru.dyatel.inuyama.utilities.ctx

class NyaaView(context: Context) : BaseScreenView<NyaaScreen>(context) {

    private val recyclerView: RecyclerView

    init {
        recyclerView = recyclerView {
            lparams(width = matchParent, height = matchParent)

            layoutManager = LinearLayoutManager(context)
        }
    }

    fun bindAdapter(adapter: RecyclerView.Adapter<*>) {
        recyclerView.adapter = adapter
    }

}

class NyaaScreen : Screen<NyaaView>(), KodeinAware {

    private companion object {
        val queryEditorId = View.generateViewId()
        val startDateSelectorId = View.generateViewId()
        val descriptionEditorId = View.generateViewId()
        val collectPathEditorId = View.generateViewId()
        val directorySelectorId = View.generateViewId()
    }

    override val kodein by closestKodein { activity }

    private val boxStore by instance<BoxStore>()
    private val directoryBox by instance<Box<Directory>>()
    private val watchBox by instance<Box<NyaaWatch>>()
    private val torrentBox by instance<Box<NyaaTorrent>>()

    private var boxObserver: DataSubscription? = null

    private val adapter = ItemAdapter<NyaaWatchItem>()
    private val fastAdapter = adapter.buildFastAdapter()

    override fun createView(context: Context) = NyaaView(context).apply { bindAdapter(fastAdapter) }

    override fun onShow(context: Context) {
        super.onShow(context)

        reload()
        boxObserver = watchBox.store
                .subscribe(NyaaWatch::class.java)
                .on(AndroidScheduler.mainThread())
                .onlyChanges()
                .observer { reload() }
    }

    override fun onHide(context: Context) {
        boxObserver?.cancel()
        boxObserver = null

        super.onHide(context)
    }

    private fun reload() {
        val watches = watchBox.all.map {
            NyaaWatchItem(it, { showEditDialog(it) }, {
                boxStore.runInTx {
                    torrentBox.query()
                            .equal(NyaaTorrent_.watchId, it.id)
                            .build()
                            .remove()
                    watchBox.remove(it)
                }
            })
        }

        adapter.set(watches)
    }

    override fun getTitle(context: Context) = context.getString(R.string.module_nyaa)!!

    private fun showEditDialog(watch: NyaaWatch) {
        val directories = directoryBox.all

        val view = ctx.verticalLayout {
            lparams(width = matchParent, height = wrapContent) {
                padding = DIM_EXTRA_LARGE
            }

            uniformTextInput {
                id = queryEditorId
                hintResource = R.string.hint_nyaa_watch_query

                setText(watch.query)
            }

            uniformTextInput {
                id = startDateSelectorId
                hintResource = R.string.hint_nyaa_watch_start_date
            }

            uniformTextInput {
                id = descriptionEditorId
                hintResource = R.string.hint_nyaa_watch_description

                setText(watch.description)
            }

            uniformTextInput {
                id = collectPathEditorId
                hintResource = R.string.hint_nyaa_watch_collect_path

                setText(watch.collectPath)
            }

            tintedSpinner {
                id = directorySelectorId
            }
        }

        val queryEditor = view.find<EditText>(queryEditorId)
        val startDateSelector = view.find<EditText>(startDateSelectorId).let {
            val picker = DatePicker(it, watch.startDatetime)
            it.setOnClickListener { picker.showDialog(activity.fragmentManager) }
            picker
        }
        val descriptionEditor = view.find<EditText>(descriptionEditorId)
        val collectPathEditor = view.find<EditText>(collectPathEditorId)
        val directorySelector = view.find<Spinner>(directorySelectorId).let {
            DirectorySelector(it, directories, watch.directory.target)
        }

        AlertDialog.Builder(activity)
                .setTitle(R.string.dialog_add_watch)
                .setView(view)
                .setPositiveButton(R.string.action_save) { _, _ ->
                    watch.query = queryEditor.text.toString()
                    watch.startDatetime = startDateSelector.date
                    watch.description = descriptionEditor.text.toString()
                    watch.collectPath = collectPathEditor.text.toString()
                    watch.directory.target = directorySelector.directory

                    watchBox.put(watch)
                }
                .setNegativeButton(R.string.action_cancel) { _, _ -> }
                .show()
    }

    override fun onUpdateMenu(menu: Menu) {
        menu.findItem(R.id.add).apply {
            isVisible = true
            setOnMenuItemClickListener { showEditDialog(NyaaWatch()); true }
        }
    }
}
