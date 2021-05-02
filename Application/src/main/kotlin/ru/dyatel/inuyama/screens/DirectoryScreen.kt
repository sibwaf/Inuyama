package ru.dyatel.inuyama.screens

import android.app.AlertDialog
import android.content.Context
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.wealthfront.magellan.BaseScreenView
import io.objectbox.Box
import org.jetbrains.anko.appcompat.v7.tintedButton
import org.jetbrains.anko.frameLayout
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.padding
import org.jetbrains.anko.recyclerview.v7.recyclerView
import org.jetbrains.anko.verticalLayout
import org.jetbrains.anko.wrapContent
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.layout.DirectoryItem
import ru.dyatel.inuyama.model.Directory
import ru.dyatel.inuyama.utilities.buildFastAdapter
import ru.dyatel.inuyama.utilities.hideKeyboard
import sibwaf.inuyama.app.common.DIM_EXTRA_LARGE
import sibwaf.inuyama.app.common.DIM_LARGE
import sibwaf.inuyama.app.common.components.UniformTextInput
import sibwaf.inuyama.app.common.components.uniformTextInput

class DirectoryView(context: Context) : BaseScreenView<DirectoryScreen>(context) {

    private lateinit var recyclerView: RecyclerView

    init {
        verticalLayout {
            lparams(width = matchParent, height = matchParent) {
                padding = DIM_LARGE
            }

            tintedButton(R.string.action_add) {
                setOnClickListener { screen.createDirectory() }
            }

            recyclerView = recyclerView {
                lparams(width = matchParent, height = matchParent)

                layoutManager = LinearLayoutManager(context)
                addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            }
        }
    }

    fun bindAdapter(adapter: RecyclerView.Adapter<*>) {
        recyclerView.adapter = adapter
    }

}

class DirectoryScreen : InuScreen<DirectoryView>(), KodeinAware {

    override val titleResource = R.string.screen_directories

    private val directoryBox by instance<Box<Directory>>()

    private val adapter = ItemAdapter<DirectoryItem>()
    private val fastAdapter = adapter.buildFastAdapter()

    override fun createView(context: Context) = DirectoryView(context).apply { bindAdapter(fastAdapter) }

    override fun onShow(context: Context) {
        super.onShow(context)

        refresh()
        observeChanges<Directory>(::refresh)
    }

    private fun refresh() {
        adapter.set(directoryBox.all.map { directory ->
            DirectoryItem(
                directory,
                { fastAdapter.notifyAdapterItemChanged(adapter.getAdapterPosition(directory.id)) },
                {
                    activity!!.hideKeyboard()

                    directory.path = it
                    directoryBox.put(directory)
                },
                { directoryBox.remove(directory) }
            )
        })
    }

    fun createDirectory() {
        lateinit var editor: UniformTextInput

        val view = context!!.frameLayout {
            lparams(width = matchParent, height = wrapContent) {
                padding = DIM_EXTRA_LARGE
            }

            editor = uniformTextInput()
        }

        AlertDialog.Builder(context!!)
            .setTitle(R.string.dialog_add_directory)
            .setView(view)
            .setPositiveButton(R.string.action_save) { _, _ -> directoryBox.put(Directory(path = editor.text)) }
            .setNegativeButton(R.string.action_cancel) { _, _ -> }
            .show()
    }

}
