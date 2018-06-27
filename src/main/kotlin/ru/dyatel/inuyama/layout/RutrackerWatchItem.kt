package ru.dyatel.inuyama.layout

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import org.jetbrains.anko.appcompat.v7.tintedButton
import org.jetbrains.anko.cardview.v7.cardView
import org.jetbrains.anko.find
import org.jetbrains.anko.linearLayout
import org.jetbrains.anko.margin
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.padding
import org.jetbrains.anko.textResource
import org.jetbrains.anko.textView
import org.jetbrains.anko.verticalLayout
import org.jetbrains.anko.wrapContent
import org.ocpsoft.prettytime.PrettyTime
import ru.dyatel.inuyama.ITEM_TYPE_RUTRACKER_WATCH
import ru.dyatel.inuyama.LOCALE_RU
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.model.RutrackerWatch
import java.util.Date

class RutrackerWatchItem(
        private val watch: RutrackerWatch,
        private val editListener: () -> Unit,
        private val removeListener: () -> Unit
) : AbstractItem<RutrackerWatchItem, RutrackerWatchItem.ViewHolder>() {

    private companion object {
        val descriptionViewId = View.generateViewId()
        val directoryViewId = View.generateViewId()
        val lastUpdateViewId = View.generateViewId()

        val editButtonId = View.generateViewId()
        val removeButtonId = View.generateViewId()

        val prettyTime by lazy { PrettyTime(LOCALE_RU) }
    }

    init {
        withIdentifier(watch.id)
    }

    class ViewHolder(view: View) : FastAdapter.ViewHolder<RutrackerWatchItem>(view) {

        private val context = view.context

        private val descriptionView = view.find<TextView>(descriptionViewId)
        private val directoryView = view.find<TextView>(directoryViewId)
        private val lastUpdateView = view.find<TextView>(lastUpdateViewId)

        private val editButton = view.find<Button>(editButtonId)
        private val removeButton = view.find<Button>(removeButtonId)

        override fun unbindView(item: RutrackerWatchItem) {
            descriptionView.text = null
            directoryView.text = null
            lastUpdateView.text = null

            editButton.setOnClickListener(null)
            removeButton.setOnClickListener(null)
        }

        override fun bindView(item: RutrackerWatchItem, payloads: MutableList<Any>?) {
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

    override fun getType() = ITEM_TYPE_RUTRACKER_WATCH

    override fun getViewHolder(view: View) = ViewHolder(view)

    override fun getLayoutRes() = throw UnsupportedOperationException()

    override fun createView(ctx: Context, parent: ViewGroup?): View {
        return ctx.cardView {
            lparams(width = matchParent, height = wrapContent) {
                margin = DIM_LARGE
            }

            verticalLayout {
                lparams(width = matchParent, height = wrapContent) {
                    padding = DIM_LARGE
                }

                textView {
                    id = descriptionViewId
                    gravity = Gravity.CENTER_HORIZONTAL
                    textSize = SP_MEDIUM
                }

                verticalLayout {
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
                }.lparams(width = matchParent, height = wrapContent) {
                    margin = DIM_EXTRA_LARGE
                }

                linearLayout {
                    lparams(width = matchParent, height = wrapContent)

                    tintedButton {
                        id = editButtonId
                        textResource = R.string.action_edit
                    }.lparams(width = 0) {
                        weight = 0.5f
                    }

                    tintedButton {
                        id = removeButtonId
                        textResource = R.string.action_remove
                    }.lparams(width = 0) {
                        weight = 0.5f
                    }
                }
            }
        }
    }
}