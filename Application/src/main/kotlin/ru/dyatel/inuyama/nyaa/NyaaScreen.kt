package ru.dyatel.inuyama.nyaa

import android.app.AlertDialog
import android.content.Context
import android.view.Menu
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.wealthfront.magellan.BaseScreenView
import io.objectbox.Box
import org.jetbrains.anko.hintResource
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.padding
import org.jetbrains.anko.recyclerview.v7.recyclerView
import org.jetbrains.anko.verticalLayout
import org.jetbrains.anko.wrapContent
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.layout.DirectorySelector
import ru.dyatel.inuyama.layout.NyaaWatchItem
import ru.dyatel.inuyama.layout.directorySelector
import ru.dyatel.inuyama.model.Directory
import ru.dyatel.inuyama.model.NyaaTorrent
import ru.dyatel.inuyama.model.NyaaTorrent_
import ru.dyatel.inuyama.model.NyaaWatch
import ru.dyatel.inuyama.screens.InuScreen
import ru.dyatel.inuyama.utilities.buildFastAdapter
import sibwaf.inuyama.app.common.DIM_EXTRA_LARGE
import sibwaf.inuyama.app.common.components.UniformDatePicker
import sibwaf.inuyama.app.common.components.UniformTextInput
import sibwaf.inuyama.app.common.components.showConfirmationDialog
import sibwaf.inuyama.app.common.components.uniformDatePicker
import sibwaf.inuyama.app.common.components.uniformTextInput

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

class NyaaScreen : InuScreen<NyaaView>(), KodeinAware {

    override val titleResource = R.string.module_nyaa

    private val directoryBox by instance<Box<Directory>>()
    private val watchBox by instance<Box<NyaaWatch>>()
    private val torrentBox by instance<Box<NyaaTorrent>>()

    private val adapter = ItemAdapter<NyaaWatchItem>()
    private val fastAdapter = adapter.buildFastAdapter()

    override fun createView(context: Context) = NyaaView(context).apply { bindAdapter(fastAdapter) }

    override fun onShow(context: Context) {
        super.onShow(context)

        reload()
        observeChanges<NyaaWatch>(::reload)
    }

    private fun reload() {
        val watches = watchBox.all.map {
            NyaaWatchItem(it, { showEditDialog(it) }, {
                activity!!.showConfirmationDialog(
                    context!!.getString(R.string.dialog_remove_watch_title),
                    context!!.getString(R.string.dialog_remove_watch_message, it.description),
                    context!!.getString(R.string.action_remove)
                ) {
                    boxStore.runInTx {
                        torrentBox.query()
                            .equal(NyaaTorrent_.watchId, it.id)
                            .build()
                            .remove()
                        watchBox.remove(it)
                    }
                }
            })
        }

        adapter.set(watches)
    }

    private fun showEditDialog(watch: NyaaWatch) {
        lateinit var queryEditor: UniformTextInput
        lateinit var startDateSelector: UniformDatePicker
        lateinit var descriptionEditor: UniformTextInput
        lateinit var collectPathEditor: UniformTextInput
        lateinit var directorySelector: DirectorySelector

        val view = context!!.verticalLayout {
            lparams(width = matchParent, height = wrapContent) {
                padding = DIM_EXTRA_LARGE
            }

            queryEditor = uniformTextInput {
                hintResource = R.string.hint_nyaa_watch_query
            }.apply { text = watch.query }

            startDateSelector = uniformDatePicker {
                hintResource = R.string.hint_nyaa_watch_start_date
            }.apply { date = watch.startDatetime }

            descriptionEditor = uniformTextInput {
                hintResource = R.string.hint_nyaa_watch_description
            }.apply { text = watch.description }

            collectPathEditor = uniformTextInput {
                hintResource = R.string.hint_nyaa_watch_collect_path
            }.apply { text = watch.collectPath }

            directorySelector = directorySelector {
                bindItems(directoryBox.all)
                selected = watch.directory.target
            }
        }

        AlertDialog.Builder(context!!)
            .setTitle(R.string.dialog_add_watch)
            .setView(view)
            .setPositiveButton(R.string.action_save) { _, _ ->
                watch.query = queryEditor.text
                watch.startDatetime = startDateSelector.date!!
                watch.description = descriptionEditor.text
                watch.collectPath = collectPathEditor.text
                watch.directory.target = directorySelector.selected

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
