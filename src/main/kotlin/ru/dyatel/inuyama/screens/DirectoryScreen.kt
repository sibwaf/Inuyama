package ru.dyatel.inuyama.screens

import android.app.AlertDialog
import android.content.Context
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.Menu
import android.widget.EditText
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.wealthfront.magellan.BaseScreenView
import com.wealthfront.magellan.Screen
import io.objectbox.Box
import org.jetbrains.anko.editText
import org.jetbrains.anko.frameLayout
import org.jetbrains.anko.leftPadding
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.padding
import org.jetbrains.anko.recyclerview.v7.recyclerView
import org.jetbrains.anko.rightPadding
import org.jetbrains.anko.wrapContent
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.buildFastAdapter
import ru.dyatel.inuyama.ctx
import ru.dyatel.inuyama.disableSuggestions
import ru.dyatel.inuyama.disableUiExtraction
import ru.dyatel.inuyama.hideKeyboard
import ru.dyatel.inuyama.layout.DIM_EXTRA_LARGE
import ru.dyatel.inuyama.layout.DIM_LARGE
import ru.dyatel.inuyama.layout.DirectoryItem
import ru.dyatel.inuyama.model.Directory

class DirectoryView(context: Context) : BaseScreenView<DirectoryScreen>(context) {

    private val recyclerView: RecyclerView

    init {
        recyclerView = recyclerView {
            lparams(width = matchParent, height = matchParent)

            layoutManager = LinearLayoutManager(context)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
    }

    fun bindAdapter(adapter: RecyclerView.Adapter<*>) {
        recyclerView.adapter = adapter
    }

}

class DirectoryScreen : Screen<DirectoryView>(), KodeinAware {

    override val kodein by closestKodein { activity }

    private val directoryBox by instance<Box<Directory>>()

    private val adapter = ItemAdapter<DirectoryItem>()
    private val fastAdapter = adapter.buildFastAdapter()

    override fun createView(context: Context) = DirectoryView(context).apply { bindAdapter(fastAdapter) }

    override fun onShow(context: Context) {
        super.onShow(context)

        val directories = directoryBox.all.map { createItem(it) }
        adapter.set(directories)
    }

    private fun createItem(directory: Directory): DirectoryItem {
        return DirectoryItem(
                directory,
                {
                    fastAdapter.notifyAdapterItemChanged(findPosition(directory))
                },
                {
                    activity.hideKeyboard()

                    directory.path = it
                    directoryBox.put(directory)
                    fastAdapter.notifyAdapterItemChanged(findPosition(directory))
                },
                {
                    directoryBox.remove(directory)
                    adapter.removeByIdentifier(directory.id)
                }
        )
    }

    private fun findPosition(directory: Directory) = adapter.getAdapterPosition(directory.id)

    override fun onUpdateMenu(menu: Menu) {
        menu.findItem(R.id.add).apply {
            isVisible = true
            setOnMenuItemClickListener {
                val editor: EditText
                val view = ctx.frameLayout {
                    lparams(width = matchParent, height = wrapContent) {
                        padding = DIM_EXTRA_LARGE
                    }

                    editText {
                        disableUiExtraction()
                        disableSuggestions()

                        leftPadding = DIM_LARGE
                        rightPadding = DIM_LARGE
                    }
                }
                editor = view.getChildAt(0) as EditText

                AlertDialog.Builder(ctx)
                        .setTitle(R.string.dialog_add_directory)
                        .setView(view)
                        .setPositiveButton(R.string.action_save) { _, _ ->
                            val directory = Directory(path = editor.text.toString())
                            directoryBox.put(directory)
                            adapter.add(createItem(directory))
                        }
                        .setNegativeButton(R.string.action_cancel) { _, _ -> }
                        .show()

                true
            }
        }
    }

    override fun getTitle(context: Context) = context.getString(R.string.screen_directories)!!

}
