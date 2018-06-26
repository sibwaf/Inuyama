package ru.dyatel.inuyama.screens

import android.app.AlertDialog
import android.content.Context
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.Menu
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.wealthfront.magellan.BaseScreenView
import com.wealthfront.magellan.Screen
import io.objectbox.Box
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
import ru.dyatel.inuyama.buildFastAdapter
import ru.dyatel.inuyama.ctx
import ru.dyatel.inuyama.layout.DIM_EXTRA_LARGE
import ru.dyatel.inuyama.layout.RutrackerWatchItem
import ru.dyatel.inuyama.layout.uniformTextInput
import ru.dyatel.inuyama.model.Directory
import ru.dyatel.inuyama.model.RutrackerWatch
import ru.dyatel.inuyama.rutracker.RutrackerApi

class RutrackerView(context: Context) : BaseScreenView<RutrackerScreen>(context) {

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

class RutrackerScreen : Screen<RutrackerView>(), KodeinAware {

    private companion object {
        val linkEditorId = View.generateViewId()
        val descriptionEditorId = View.generateViewId()
        val directorySelectorId = View.generateViewId()
    }

    override val kodein by closestKodein { activity }

    private val watchBox by instance<Box<RutrackerWatch>>()
    private val directoryBox by instance<Box<Directory>>()

    private var boxObserver: DataSubscription? = null

    private val adapter = ItemAdapter<RutrackerWatchItem>()
    private val fastAdapter = adapter.buildFastAdapter()

    override fun createView(context: Context) = RutrackerView(context).apply { bindAdapter(fastAdapter) }

    override fun onShow(context: Context?) {
        super.onShow(context)

        reload()
        boxObserver = watchBox.store
                .subscribe(RutrackerWatch::class.java)
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
            RutrackerWatchItem(
                    it,
                    {
                        showEditDialog(it)
                    },
                    {
                        watchBox.remove(it)
                    }
            )
        }

        adapter.set(watches)
    }

    private fun showEditDialog(watch: RutrackerWatch) {
        val directories = directoryBox.all

        val view = ctx.verticalLayout {
            lparams(width = matchParent, height = wrapContent) {
                padding = DIM_EXTRA_LARGE
            }

            uniformTextInput {
                id = linkEditorId
                hintResource = R.string.hint_rutracker_watch_link

                setText(watch.id.takeIf { it != 0L }?.toString())
            }

            uniformTextInput {
                id = descriptionEditorId
                hintResource = R.string.hint_rutracker_watch_description

                setText(watch.description)
            }

            tintedSpinner {
                id = directorySelectorId

                adapter = ArrayAdapter<String>(context, android.R.layout.simple_spinner_item).apply {
                    setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

                    add(context.getString(R.string.const_directory_default))
                    addAll(directories.map { it.path })
                }

                val position = directories.indexOfFirst { it.id == watch.directory.targetId }
                setSelection(position + 1)
            }
        }

        val linkEditor = view.find<EditText>(linkEditorId)
        val descriptionEditor = view.find<EditText>(descriptionEditorId)
        val directorySelector = view.find<Spinner>(directorySelectorId)

        AlertDialog.Builder(activity)
                .setTitle(R.string.dialog_add_watch)
                .setView(view)
                .setPositiveButton(R.string.action_save) { _, _ ->
                    watch.id = linkEditor.text.toString()
                            .let { it.toLongOrNull() ?: RutrackerApi.extractTopic(it) }
                    watch.description = descriptionEditor.text.toString()
                    watch.directory.target = directorySelector.selectedItemPosition
                            .takeIf { it != 0 }
                            ?.let { directories[it - 1] }

                    watchBox.put(watch)
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
    }
}
