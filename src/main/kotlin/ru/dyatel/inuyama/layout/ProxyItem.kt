package ru.dyatel.inuyama.layout

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import org.jetbrains.anko.backgroundColorResource
import org.jetbrains.anko.find
import org.jetbrains.anko.frameLayout
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.padding
import org.jetbrains.anko.verticalMargin
import org.jetbrains.anko.wrapContent
import ru.dyatel.inuyama.ITEM_TYPE_PROXY
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.model.Proxy

class ProxyItem(private val proxy: Proxy) : AbstractItem<ProxyItem, ProxyItem.ViewHolder>() {

    private companion object {
        val textViewId = View.generateViewId()
    }

    class ViewHolder(view: View) : FastAdapter.ViewHolder<ProxyItem>(view) {
        private val textView = view.find<TextView>(textViewId)

        override fun unbindView(item: ProxyItem) {
            textView.text = null
        }

        override fun bindView(item: ProxyItem, payloads: MutableList<Any>) {
            textView.text = "${item.proxy.host}:${item.proxy.port}"
        }
    }

    override fun getType() = ITEM_TYPE_PROXY

    override fun getViewHolder(v: View) = ViewHolder(v)

    override fun getLayoutRes() = throw UnsupportedOperationException()

    override fun createView(ctx: Context, parent: ViewGroup?): View {
        return ctx.frameLayout {
            lparams(width = matchParent, height = wrapContent) {
                verticalMargin = DIM_LARGE
                padding = DIM_EXTRA_LARGE
            }

            uniformTextView { id = textViewId }

            backgroundColorResource = R.color.color_primary_dark
        }
    }
}