package ru.dyatel.inuyama.rutracker

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
import ru.dyatel.inuyama.layout.DIM_EXTRA_LARGE
import ru.dyatel.inuyama.layout.RutrackerWatchItem
import ru.dyatel.inuyama.layout.components.DirectorySelector
import ru.dyatel.inuyama.layout.components.UniformTextInput
import ru.dyatel.inuyama.layout.components.directorySelector
import ru.dyatel.inuyama.layout.components.showConfirmationDialog
import ru.dyatel.inuyama.layout.components.uniformTextInput
import ru.dyatel.inuyama.model.Directory
import ru.dyatel.inuyama.model.RutrackerWatch
import ru.dyatel.inuyama.screens.InuScreen
import ru.dyatel.inuyama.utilities.PreferenceHelper
import ru.dyatel.inuyama.utilities.buildFastAdapter

class RutrackerScreenView(context: Context) : BaseScreenView<RutrackerScreen>(context) {

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

class RutrackerScreen : InuScreen<RutrackerScreenView>(), KodeinAware {

    override val titleResource = R.string.module_rutracker

    private val preferenceHelper by instance<PreferenceHelper>()
    private val rutrackerConfiguration by instance<RutrackerConfiguration>()

    private val watchBox by instance<Box<RutrackerWatch>>()
    private val directoryBox by instance<Box<Directory>>()

    private val adapter = ItemAdapter<RutrackerWatchItem>()
    private val fastAdapter = adapter.buildFastAdapter()

    override fun createView(context: Context) = RutrackerScreenView(context).apply { bindAdapter(fastAdapter) }

    override fun onShow(context: Context) {
        super.onShow(context)

        refresh()
        observeChanges<RutrackerWatch>(::refresh)
    }

    private fun refresh() {
        adapter.set(watchBox.all.map {
            RutrackerWatchItem(
                    it,
                    { showEditDialog(it) },
                    {
                        activity!!.showConfirmationDialog(
                                context!!.getString(R.string.dialog_remove_watch_title),
                                context!!.getString(R.string.dialog_remove_watch_message, it.description),
                                context!!.getString(R.string.action_remove)
                        ) { watchBox.remove(it) }
                    })
        })
    }

    private fun showEditDialog(watch: RutrackerWatch) {
        lateinit var linkEditor: UniformTextInput
        lateinit var descriptionEditor: UniformTextInput
        lateinit var directorySelector: DirectorySelector

        val view = context!!.verticalLayout {
            lparams(width = matchParent, height = wrapContent) {
                padding = DIM_EXTRA_LARGE
            }

            linkEditor = uniformTextInput {
                hintResource = R.string.hint_rutracker_watch_link
            }.apply { text = watch.topic.takeIf { it != 0L }?.toString() ?: "" }

            descriptionEditor = uniformTextInput {
                hintResource = R.string.hint_rutracker_watch_description
            }.apply { text = watch.description }

            directorySelector = directorySelector {
                bindItems(directoryBox.all)
                selected = watch.directory.target
            }
        }

        AlertDialog.Builder(context!!)
                .setTitle(R.string.dialog_add_watch)
                .setView(view)
                .setPositiveButton(R.string.action_save) { _, _ ->
                    watch.topic = linkEditor.text.let { it.toLongOrNull() ?: RutrackerApi.extractTopic(it) }
                    watch.description = descriptionEditor.text
                    watch.directory.target = directorySelector.selected

                    watchBox.put(watch)
                }
                .setNegativeButton(R.string.action_cancel) { _, _ -> }
                .show()
    }

    private fun showConfigurationDialog() {
        lateinit var hostEditor: UniformTextInput

        val view = context!!.verticalLayout {
            lparams(width = matchParent, height = wrapContent) {
                padding = DIM_EXTRA_LARGE
            }

            hostEditor = uniformTextInput {
                hintResource = R.string.hint_rutracker_host
            }.apply { text = rutrackerConfiguration.host }
        }

        AlertDialog.Builder(context!!)
                .setTitle(R.string.dialog_settings)
                .setView(view)
                .setPositiveButton(R.string.action_save) { _, _ ->
                    rutrackerConfiguration.host = hostEditor.text
                    preferenceHelper.rutracker = rutrackerConfiguration
                }
                .setNegativeButton(R.string.action_cancel) { _, _ -> }
                .show()
    }

    override fun onUpdateMenu(menu: Menu) {
        menu.findItem(R.id.add).apply {
            isVisible = true
            setOnMenuItemClickListener { showEditDialog(RutrackerWatch()); true }
        }
        menu.findItem(R.id.settings).apply {
            isVisible = true
            setOnMenuItemClickListener { showConfigurationDialog(); true }
        }
    }
}
