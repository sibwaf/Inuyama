package ru.dyatel.inuyama.layout

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import org.jetbrains.anko.alignParentLeft
import org.jetbrains.anko.alignParentRight
import org.jetbrains.anko.centerVertically
import org.jetbrains.anko.find
import org.jetbrains.anko.frameLayout
import org.jetbrains.anko.leftOf
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.padding
import org.jetbrains.anko.relativeLayout
import org.jetbrains.anko.switch
import org.jetbrains.anko.textView
import org.jetbrains.anko.wrapContent
import ru.dyatel.inuyama.ITEM_TYPE_NETWORK
import ru.dyatel.inuyama.model.Network

class NetworkItem(
        private val network: Network,
        private val trustChangeListener: (Network) -> Unit
) : AbstractItem<NetworkItem, NetworkItem.ViewHolder>() {

    private companion object {
        val nameViewId = View.generateViewId()
        val switchViewId = View.generateViewId()
    }

    init {
        withIdentifier(network.id)
    }

    class ViewHolder(view: View) : FastAdapter.ViewHolder<NetworkItem>(view) {

        private val nameView = view.find<TextView>(nameViewId)
        private val switchView = view.find<Switch>(switchViewId)

        override fun unbindView(item: NetworkItem) {
            nameView.text = null
            switchView.setOnCheckedChangeListener(null)
            switchView.isChecked = false
        }

        override fun bindView(item: NetworkItem, payloads: MutableList<Any>?) {
            nameView.text = item.network.name
            switchView.isChecked = item.network.trusted
            switchView.setOnCheckedChangeListener { _, trusted ->
                item.network.trusted = trusted
                item.trustChangeListener(item.network)
            }
        }
    }

    override fun getType() = ITEM_TYPE_NETWORK

    override fun getViewHolder(view: View) = ViewHolder(view)

    override fun getLayoutRes() = throw UnsupportedOperationException()

    override fun createView(ctx: Context, parent: ViewGroup?): View {
        return ctx.frameLayout {
            lparams(width = matchParent, height = wrapContent) {
                padding = DIM_LARGE
            }

            relativeLayout {
                textView {
                    id = nameViewId
                }.lparams {
                    centerVertically()
                    alignParentLeft()
                    leftOf(switchViewId)

                    leftMargin = DIM_LARGE
                    rightMargin = DIM_LARGE
                }

                switch {
                    id = switchViewId
                }.lparams {
                    centerVertically()
                    alignParentRight()
                }
            }

        }
    }
}