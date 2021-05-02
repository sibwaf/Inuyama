package sibwaf.inuyama.app.common.components

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.view.isVisible
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import org.jetbrains.anko.alignParentLeft
import org.jetbrains.anko.alignParentRight
import org.jetbrains.anko.backgroundColorResource
import org.jetbrains.anko.cardview.v7.cardView
import org.jetbrains.anko.centerVertically
import org.jetbrains.anko.find
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
import sibwaf.inuyama.app.common.DIM_EXTRA_LARGE
import sibwaf.inuyama.app.common.DIM_LARGE
import sibwaf.inuyama.app.common.DIM_MEDIUM
import sibwaf.inuyama.app.common.SP_SMALL
import sibwaf.inuyama.app.common.utilities.hideIf

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
