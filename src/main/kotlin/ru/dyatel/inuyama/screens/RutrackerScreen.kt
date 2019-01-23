package ru.dyatel.inuyama.screens

import android.app.AlertDialog
import android.content.Context
import android.view.Menu
import android.view.View
import android.widget.EditText
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.wealthfront.magellan.BaseScreenView
import com.wealthfront.magellan.Screen
import io.objectbox.Box
import io.objectbox.android.AndroidScheduler
import io.objectbox.reactive.DataSubscription
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
import ru.dyatel.inuyama.layout.DirectorySelector
import ru.dyatel.inuyama.layout.RutrackerWatchItem
import ru.dyatel.inuyama.layout.directorySelector
import ru.dyatel.inuyama.layout.showConfirmationDialog
import ru.dyatel.inuyama.layout.uniformTextInput
import ru.dyatel.inuyama.model.Directory
import ru.dyatel.inuyama.model.RutrackerWatch
import ru.dyatel.inuyama.rutracker.RutrackerApi
import ru.dyatel.inuyama.rutracker.RutrackerConfiguration
import ru.dyatel.inuyama.utilities.PreferenceHelper
import ru.dyatel.inuyama.utilities.buildFastAdapter
import ru.dyatel.inuyama.utilities.ctx
import ru.dyatel.inuyama.utilities.subscribeFor

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

class RutrackerScreen : Screen<RutrackerScreenView>(), KodeinAware {

    private companion object {
        val linkEditorId = View.generateViewId()
        val descriptionEditorId = View.generateViewId()
        val directorySelectorId = View.generateViewId()

        val hostEditorId = View.generateViewId()
        val proxySelectorId = View.generateViewId()
    }

    override val kodein by closestKodein { activity }

    private val preferenceHelper by instance<PreferenceHelper>()
    private val rutrackerConfiguration by instance<RutrackerConfiguration>()

    private val watchBox by instance<Box<RutrackerWatch>>()
    private val directoryBox by instance<Box<Directory>>()

    private var boxObserver: DataSubscription? = null

    private val adapter = ItemAdapter<RutrackerWatchItem>()
    private val fastAdapter = adapter.buildFastAdapter()

    override fun createView(context: Context) = RutrackerScreenView(context).apply { bindAdapter(fastAdapter) }

    override fun onShow(context: Context?) {
        super.onShow(context)

        reload()
        boxObserver = watchBox.store
                .subscribeFor<RutrackerWatch>()
                .on(AndroidScheduler.mainThread())
                .onlyChanges()
                .observer { reload() }
    }

    override fun onHide(context: Context?) {
        boxObserver?.cancel()
        boxObserver = null

        super.onHide(context)
    }

    private fun reload() {
        val watches = watchBox.all.map {
            RutrackerWatchItem(it, { showEditDialog(it) }, {
                activity.showConfirmationDialog(
                        activity.getString(R.string.dialog_remove_watch_title),
                        activity.getString(R.string.dialog_remove_watch_message, it.description),
                        activity.getString(R.string.action_remove)
                ) { watchBox.remove(it) }
            })
        }

        adapter.set(watches)
    }

    private fun showEditDialog(watch: RutrackerWatch) {
        val view = ctx.verticalLayout {
            lparams(width = matchParent, height = wrapContent) {
                padding = DIM_EXTRA_LARGE
            }

            uniformTextInput {
                id = linkEditorId
                hintResource = R.string.hint_rutracker_watch_link

                setText(watch.topic.takeIf { it != 0L }?.toString())
            }

            uniformTextInput {
                id = descriptionEditorId
                hintResource = R.string.hint_rutracker_watch_description

                setText(watch.description)
            }

            directorySelector {
                id = directorySelectorId

                bindItems(directoryBox.all)
                selected = watch.directory.target
            }
        }

        val linkEditor = view.find<EditText>(linkEditorId)
        val descriptionEditor = view.find<EditText>(descriptionEditorId)
        val directorySelector = view.find<DirectorySelector>(directorySelectorId)

        AlertDialog.Builder(activity)
                .setTitle(R.string.dialog_add_watch)
                .setView(view)
                .setPositiveButton(R.string.action_save) { _, _ ->
                    watch.topic = linkEditor.text.toString()
                            .let { it.toLongOrNull() ?: RutrackerApi.extractTopic(it) }
                    watch.description = descriptionEditor.text.toString()
                    watch.directory.target = directorySelector.selected

                    watchBox.put(watch)
                }
                .setNegativeButton(R.string.action_cancel) { _, _ -> }
                .show()
    }

    private fun showConfigurationDialog() {
        val view = ctx.verticalLayout {
            lparams(width = matchParent, height = wrapContent) {
                padding = DIM_EXTRA_LARGE
            }

            uniformTextInput {
                id = hostEditorId
                hintResource = R.string.hint_rutracker_host

                setText(rutrackerConfiguration.host)
            }
        }

        AlertDialog.Builder(activity)
                .setTitle(R.string.dialog_settings)
                .setView(view)
                .setPositiveButton(R.string.action_save) { _, _ ->
                    rutrackerConfiguration.host = view.find<EditText>(hostEditorId).text.toString()
                    preferenceHelper.rutracker = rutrackerConfiguration
                }
                .setNegativeButton(R.string.action_cancel) { _, _ -> }
                .show()
    }

    override fun getTitle(context: Context) = context.getString(R.string.module_rutracker)!!

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
