package ru.dyatel.inuyama.layout

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import org.jetbrains.anko.cardview.v7.cardView
import org.jetbrains.anko.find
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.padding
import org.jetbrains.anko.verticalLayout
import org.jetbrains.anko.verticalMargin
import org.jetbrains.anko.wrapContent
import ru.dyatel.inuyama.ITEM_TYPE_PROXY
import ru.dyatel.inuyama.layout.components.uniformTextView
import ru.dyatel.inuyama.model.Proxy

abstract class UniformSimpleItem : AbstractItem<UniformSimpleItem, UniformSimpleItem.ViewHolder>() {

    protected companion object {
        val titleViewId = View.generateViewId()
    }

    class ViewHolder(view: View) : FastAdapter.ViewHolder<UniformSimpleItem>(view) {

        private val context = view.context

        private val titleView = view.find<TextView>(titleViewId)

        override fun unbindView(item: UniformSimpleItem) {
        }

        override fun bindView(item: UniformSimpleItem, payloads: MutableList<Any>) {
            titleView.text = item.getTitle(context)
        }
    }

    abstract fun getTitle(context: Context): String

    override fun getViewHolder(v: View) = ViewHolder(v)
    override fun getLayoutRes() = throw UnsupportedOperationException()

    override fun createView(ctx: Context, parent: ViewGroup?): View {
        return ctx.cardView {
            lparams(width = matchParent, height = wrapContent) {
                verticalMargin = DIM_MEDIUM
            }

            verticalLayout {
                lparams(width = matchParent, height = wrapContent) {
                    padding = DIM_LARGE
                }

                uniformTextView {
                    id = titleViewId
                }
            }
        }
    }
}

class ProxyItem(val proxy: Proxy) : UniformSimpleItem() {
    override fun getTitle(context: Context) = "${proxy.host}:${proxy.port}"
    override fun getType() = ITEM_TYPE_PROXY
}