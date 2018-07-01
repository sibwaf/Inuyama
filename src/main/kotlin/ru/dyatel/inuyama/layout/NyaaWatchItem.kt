package ru.dyatel.inuyama.layout

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import org.jetbrains.anko.find
import org.jetbrains.anko.textView
import ru.dyatel.inuyama.ITEM_TYPE_NYAA_WATCH
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.model.NyaaWatch
import ru.dyatel.inuyama.utilities.prettyTime
import java.util.Date

class NyaaWatchItem(
        private val watch: NyaaWatch,
        private val editListener: () -> Unit,
        private val removeListener: () -> Unit
) : AbstractItem<NyaaWatchItem, NyaaWatchItem.ViewHolder>() {

    private companion object {
        val descriptionViewId = View.generateViewId()
        val directoryViewId = View.generateViewId()
        val lastUpdateViewId = View.generateViewId()

        val editButtonId = View.generateViewId()
        val removeButtonId = View.generateViewId()
    }

    init {
        withIdentifier(watch.id)
    }

    class ViewHolder(view: View) : FastAdapter.ViewHolder<NyaaWatchItem>(view) {

        private val context = view.context

        private val descriptionView = view.find<TextView>(descriptionViewId)
        private val directoryView = view.find<TextView>(directoryViewId)
        private val lastUpdateView = view.find<TextView>(lastUpdateViewId)

        private val editButton = view.find<Button>(editButtonId)
        private val removeButton = view.find<Button>(removeButtonId)

        override fun unbindView(item: NyaaWatchItem) {
            descriptionView.text = null
            directoryView.text = null
            lastUpdateView.text = null

            editButton.setOnClickListener(null)
            removeButton.setOnClickListener(null)
        }

        override fun bindView(item: NyaaWatchItem, payloads: MutableList<Any>?) {
            descriptionView.text = item.watch.description

            val directoryText = item.watch.directory.target?.path
                    ?: context.getString(R.string.const_directory_default)
            directoryView.text = context.getString(R.string.label_path, directoryText)

            val lastUpdateText = item.watch.lastUpdate?.let { prettyTime.format(Date(it)) }
                    ?: context.getString(R.string.const_never)
            lastUpdateView.text = context.getString(R.string.label_last_update, lastUpdateText)

            editButton.setOnClickListener { item.editListener() }
            removeButton.setOnClickListener { item.removeListener() }
        }
    }

    override fun getType() = ITEM_TYPE_NYAA_WATCH

    override fun getViewHolder(view: View) = ViewHolder(view)

    override fun getLayoutRes() = throw UnsupportedOperationException()

    override fun createView(ctx: Context, parent: ViewGroup?): View {
        return ctx.uniformWatchView(descriptionViewId, editButtonId, removeButtonId) {
            textView {
                id = directoryViewId
                textSize = SP_MEDIUM
            }.lparams {
                bottomMargin = DIM_MEDIUM
            }

            textView {
                id = lastUpdateViewId
                textSize = SP_MEDIUM
            }
        }
    }
}