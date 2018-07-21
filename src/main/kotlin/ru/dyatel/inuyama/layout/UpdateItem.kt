package ru.dyatel.inuyama.layout

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import hirondelle.date4j.DateTime
import org.jetbrains.anko.find
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.textView
import org.jetbrains.anko.verticalLayout
import org.jetbrains.anko.wrapContent
import ru.dyatel.inuyama.ITEM_TYPE_DASHBOARD_UPDATE
import ru.dyatel.inuyama.model.Update
import java.util.TimeZone

class UpdateItem(private val update: Update) : AbstractItem<UpdateItem, UpdateItem.ViewHolder>() {

    private companion object {
        val timestampViewId = View.generateViewId()
        val descriptionViewId = View.generateViewId()
    }

    private val timestampText = DateTime.forInstant(update.timestamp, TimeZone.getDefault())
            .format("DD.MM.YYYY")

    class ViewHolder(view: View) : FastAdapter.ViewHolder<UpdateItem>(view) {

        private val timestampView = view.find<TextView>(timestampViewId)
        private val descriptionView = view.find<TextView>(descriptionViewId)

        override fun unbindView(item: UpdateItem) {
            timestampView.text = null
            descriptionView.text = null
        }

        override fun bindView(item: UpdateItem, payloads: MutableList<Any>?) {
            timestampView.text = item.timestampText
            descriptionView.text = item.update.description
        }
    }

    override fun getType() = ITEM_TYPE_DASHBOARD_UPDATE

    override fun getViewHolder(view: View) = ViewHolder(view)

    override fun getLayoutRes() = throw UnsupportedOperationException()

    override fun createView(ctx: Context, parent: ViewGroup?): View {
        return ctx.verticalLayout {
            lparams(width = matchParent, height = wrapContent) {
                topMargin = DIM_MEDIUM
                bottomMargin = DIM_MEDIUM
            }

            textView {
                id = timestampViewId
                textSize = SP_MEDIUM
            }

            textView {
                id = descriptionViewId
                textSize = SP_MEDIUM
            }
        }
    }

}