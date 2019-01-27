package ru.dyatel.inuyama.layout

import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import org.jetbrains.anko.find
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.wrapContent
import ru.dyatel.inuyama.ITEM_TYPE_MODULE_STATE
import ru.dyatel.inuyama.RemoteService
import ru.dyatel.inuyama.layout.components.Marker
import ru.dyatel.inuyama.layout.components.State
import ru.dyatel.inuyama.layout.components.marker

class RemoteServiceStateItem(
        val service: RemoteService, var state: State
) : AbstractItem<RemoteServiceStateItem, RemoteServiceStateItem.ViewHolder>() {

    private companion object {
        val markerViewId = View.generateViewId()
    }

    class ViewHolder(view: View) : FastAdapter.ViewHolder<RemoteServiceStateItem>(view) {

        private val context = view.context
        private val markerView = view.find<Marker>(markerViewId)

        override fun unbindView(item: RemoteServiceStateItem) {
            markerView.text = ""
            markerView.state = State.PENDING
        }

        override fun bindView(item: RemoteServiceStateItem, payloads: MutableList<Any>?) {
            markerView.text = item.service.getName(context)
            markerView.state = item.state
        }

    }

    override fun getType() = ITEM_TYPE_MODULE_STATE

    override fun getViewHolder(view: View) = ViewHolder(view)

    override fun getLayoutRes() = throw UnsupportedOperationException()

    override fun createView(ctx: Context, parent: ViewGroup?): View {
        return ctx.marker {
            lparams(width = matchParent, height = wrapContent) {
                topMargin = DIM_MEDIUM
                bottomMargin = DIM_MEDIUM
            }

            id = markerViewId
        }
    }
}