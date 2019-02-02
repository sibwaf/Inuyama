package ru.dyatel.inuyama.layout

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import androidx.annotation.ColorRes
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import hirondelle.date4j.DateTime
import org.jetbrains.anko.alignParentLeft
import org.jetbrains.anko.alignParentRight
import org.jetbrains.anko.backgroundColorResource
import org.jetbrains.anko.cardview.v7.cardView
import org.jetbrains.anko.centerVertically
import org.jetbrains.anko.find
import org.jetbrains.anko.frameLayout
import org.jetbrains.anko.horizontalPadding
import org.jetbrains.anko.leftOf
import org.jetbrains.anko.linearLayout
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.relativeLayout
import org.jetbrains.anko.switch
import org.jetbrains.anko.verticalLayout
import org.jetbrains.anko.verticalMargin
import org.jetbrains.anko.verticalPadding
import org.jetbrains.anko.view
import org.jetbrains.anko.wrapContent
import ru.dyatel.inuyama.ITEM_TYPE_FINANCE_ACCOUNT
import ru.dyatel.inuyama.ITEM_TYPE_FINANCE_CATEGORY
import ru.dyatel.inuyama.ITEM_TYPE_FINANCE_OPERATION
import ru.dyatel.inuyama.ITEM_TYPE_HOME_UPDATE
import ru.dyatel.inuyama.ITEM_TYPE_NETWORK
import ru.dyatel.inuyama.ITEM_TYPE_PROXY
import ru.dyatel.inuyama.ITEM_TYPE_SERVICE_STATE
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.RemoteService
import ru.dyatel.inuyama.layout.components.uniformTextView
import ru.dyatel.inuyama.model.FinanceAccount
import ru.dyatel.inuyama.model.FinanceCategory
import ru.dyatel.inuyama.model.FinanceOperation
import ru.dyatel.inuyama.model.Network
import ru.dyatel.inuyama.model.Proxy
import ru.dyatel.inuyama.model.Update
import ru.dyatel.inuyama.utilities.hideIf
import ru.dyatel.inuyama.utilities.isVisible
import java.util.TimeZone
import kotlin.math.abs

abstract class UniformSimpleItem : AbstractItem<UniformSimpleItem, UniformSimpleItem.ViewHolder>() {

    protected companion object {
        val markerViewId = View.generateViewId()
        val titleViewId = View.generateViewId()
        val subtitleViewId = View.generateViewId()
        val switchViewId = View.generateViewId()
    }

    class ViewHolder(view: View) : FastAdapter.ViewHolder<UniformSimpleItem>(view) {

        private val context = view.context

        private val markerView = view.find<View>(markerViewId)
        private val titleView = view.find<TextView>(titleViewId)
        private val subtitleView = view.find<TextView>(subtitleViewId)
        private val switchView = view.find<Switch>(switchViewId)

        override fun unbindView(item: UniformSimpleItem) {
            switchView.setOnCheckedChangeListener(null)
        }

        override fun bindView(item: UniformSimpleItem, payloads: MutableList<Any>) {
            item.markerColorResource
                    .also { markerView.isVisible = it != null }
                    ?.also { markerView.backgroundColorResource = it }

            titleView.text = item.getTitle(context)
            subtitleView.text = item.getSubtitle(context)

            switchView.isChecked = item.switchState
            switchView.setOnCheckedChangeListener { _, isChecked -> item.onSwitchStateChange?.invoke(isChecked) }

            subtitleView.hideIf { it.text.isBlank() }
            switchView.isVisible = item.onSwitchStateChange != null
        }
    }

    protected open fun getRootView(context: Context): ViewGroup = context.cardView()

    protected open fun getHorizontalPadding(context: Context): Int = context.DIM_EXTRA_LARGE

    @ColorRes
    protected open val markerColorResource: Int? = null

    protected abstract fun getTitle(context: Context): String
    protected open fun getSubtitle(context: Context): String? = null

    protected open val switchState: Boolean = false
    protected open val onSwitchStateChange: ((Boolean) -> Unit)? = null

    override fun getViewHolder(v: View) = ViewHolder(v)
    override fun getLayoutRes() = throw UnsupportedOperationException()

    override fun createView(ctx: Context, parent: ViewGroup?): View {
        return getRootView(ctx).apply {
            layoutParams = ViewGroup.MarginLayoutParams(matchParent, wrapContent).apply {
                verticalMargin = ctx.DIM_MEDIUM
            }

            linearLayout {
                lparams(width = matchParent, height = wrapContent)

                view {
                    id = markerViewId
                }.lparams(width = DIM_MEDIUM, height = matchParent)

                relativeLayout {
                    lparams(width = matchParent, height = wrapContent) {
                        verticalPadding = DIM_LARGE
                        horizontalPadding = getHorizontalPadding(ctx)
                    }

                    verticalLayout {
                        uniformTextView {
                            id = titleViewId
                        }

                        uniformTextView {
                            id = subtitleViewId
                            textSize = SP_SMALL
                        }
                    }.lparams {
                        centerVertically()
                        alignParentLeft()
                        leftOf(switchViewId)
                    }

                    switch {
                        id = switchViewId
                    }.lparams {
                        centerVertically()
                        alignParentRight()
                        leftMargin = DIM_EXTRA_LARGE
                    }
                }
            }
        }
    }
}

class RemoteServiceStateItem(val service: RemoteService, @ColorRes var markerColor: Int) : UniformSimpleItem() {
    override fun getRootView(context: Context): ViewGroup = context.frameLayout()

    override val markerColorResource: Int
        get() = markerColor

    override fun getTitle(context: Context) = service.getName(context)
    override fun getType() = ITEM_TYPE_SERVICE_STATE
}

class UpdateItem(val update: Update) : UniformSimpleItem() {
    override fun getRootView(context: Context): ViewGroup = context.frameLayout()

    override fun getHorizontalPadding(context: Context) = 0

    override fun getTitle(context: Context) = update.description
    override fun getSubtitle(context: Context) =
            DateTime.forInstant(update.timestamp, TimeZone.getDefault()).format("DD.MM.YYYY, hh:mm")!!

    override fun getType() = ITEM_TYPE_HOME_UPDATE
}

class ProxyItem(val proxy: Proxy) : UniformSimpleItem() {
    override fun getTitle(context: Context) = "${proxy.host}:${proxy.port}"
    override fun getType() = ITEM_TYPE_PROXY
}

class NetworkItem(private val network: Network, trustChangeListener: (Boolean) -> Unit) : UniformSimpleItem() {
    init {
        withIdentifier(network.id)
    }

    override fun getTitle(context: Context) = network.name
    override fun getType() = ITEM_TYPE_NETWORK

    override val switchState = network.trusted
    override val onSwitchStateChange = trustChangeListener
}

class FinanceAccountItem(val account: FinanceAccount) : UniformSimpleItem() {
    override fun getTitle(context: Context) = account.name
    override fun getSubtitle(context: Context) =
            context.getString(R.string.label_finance_amount, account.initialBalance + account.balance)!!

    override fun getType() = ITEM_TYPE_FINANCE_ACCOUNT
}

class FinanceCategoryItem(val category: FinanceCategory) : UniformSimpleItem() {
    override fun getTitle(context: Context) = category.name
    override fun getType() = ITEM_TYPE_FINANCE_CATEGORY
}

class FinanceOperationItem(val operation: FinanceOperation) : UniformSimpleItem() {
    override val markerColorResource = if (operation.amount < 0) R.color.color_fail else R.color.color_ok

    override fun getTitle(context: Context): String {
        val builder = StringBuilder()

        val account = operation.account.target
        val category = operation.category.target

        if (operation.amount > 0) {
            builder.append(category.name, " > ")
        }

        builder.append(account.name)

        if (operation.amount < 0) {
            builder.append(" > ", category.name)
        }

        builder.append(", ", context.getString(R.string.label_finance_amount, abs(operation.amount)))

        return builder.toString()
    }

    override fun getSubtitle(context: Context) = operation.datetime.format("DD.MM.YYYY, hh:mm")!!

    override fun getType() = ITEM_TYPE_FINANCE_OPERATION
}