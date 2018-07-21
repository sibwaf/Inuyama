package ru.dyatel.inuyama.layout

import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import org.jetbrains.anko.find
import ru.dyatel.inuyama.ITEM_TYPE_MODULE_STATE

class ModuleStateItem(
        private val name: String, var state: State
) : AbstractItem<ModuleStateItem, ModuleStateItem.ViewHolder>() {

    private companion object {
        val markerViewId = View.generateViewId()
    }

    class ViewHolder(view: View) : FastAdapter.ViewHolder<ModuleStateItem>(view) {

        private val markerView = view.find<Marker>(markerViewId)

        override fun unbindView(item: ModuleStateItem) {
            markerView.text = ""
            markerView.state = State.PENDING
        }

        override fun bindView(item: ModuleStateItem, payloads: MutableList<Any>?) {
            markerView.text = item.name
            markerView.state = item.state
        }

    }

    override fun getType() = ITEM_TYPE_MODULE_STATE

    override fun getViewHolder(view: View) = ViewHolder(view)

    override fun getLayoutRes() = throw UnsupportedOperationException()

    override fun createView(ctx: Context, parent: ViewGroup?): View {
        return ctx.marker {
            lparams {
                topMargin = DIM_MEDIUM
                bottomMargin = DIM_MEDIUM
            }

            id = markerViewId
        }
    }
}