package ru.dyatel.inuyama.layout.components

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon
import org.jetbrains.anko.alignParentRight
import org.jetbrains.anko.cardview.v7._CardView
import org.jetbrains.anko.cardview.v7.cardView
import org.jetbrains.anko.centerVertically
import org.jetbrains.anko.find
import org.jetbrains.anko.imageView
import org.jetbrains.anko.leftOf
import org.jetbrains.anko.margin
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.relativeLayout
import org.jetbrains.anko.rightOf
import org.jetbrains.anko.switch
import org.jetbrains.anko.textResource
import org.jetbrains.anko.textView
import org.jetbrains.anko.wrapContent
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.layout.DIM_EXTRA_LARGE
import ru.dyatel.inuyama.layout.SP_MEDIUM
import ru.dyatel.inuyama.utilities.isVisible
import kotlin.properties.Delegates

class StatusBar(context: Context) : _CardView(context) {

    private companion object {
        val iconViewId = View.generateViewId()
        val textViewId = View.generateViewId()
        val switchViewId = View.generateViewId()
    }

    private val iconView: ImageView
    private val textView: TextView
    private val switchView: Switch

    init {
        cardView {
            lparams(width = matchParent, height = wrapContent)

            relativeLayout {
                lparams(width = matchParent, height = wrapContent) {
                    margin = DIM_EXTRA_LARGE
                }

                imageView {
                    id = iconViewId
                }.lparams {
                    rightMargin = DIM_EXTRA_LARGE
                    centerVertically()
                }

                textView {
                    id = textViewId
                    textSize = SP_MEDIUM
                }.lparams {
                    centerVertically()
                    rightOf(iconViewId)
                    leftOf(switchViewId)
                }

                switch {
                    id = switchViewId
                }.lparams {
                    centerVertically()
                    alignParentRight()
                }
            }
        }

        iconView = find(iconViewId)
        textView = find(textViewId)
        switchView = find(switchViewId)
    }

    var icon by Delegates.observable<IIcon?>(null) { _, _, value ->
        val drawable = value?.let {
            IconicsDrawable(context)
                    .icon(value)
                    .sizeDp(24)
                    .colorRes(R.color.material_drawer_dark_secondary_text)
        }

        iconView.setImageDrawable(drawable)
        iconView.isVisible = drawable != null
    }

    var textResource: Int
        get() = throw UnsupportedOperationException()
        set(value) {
            textView.textResource = value
        }

    var text: String?
        get() = textView.text?.toString()
        set(value) {
            textView.text = value
        }

    var switchEnabled by Delegates.observable(false) { _, _, value ->
        switchView.isVisible = value
    }

    var switchState: Boolean
        get() = switchView.isChecked
        set(value) {
            switchView.isChecked = value
        }

    fun onSwitchChange(listener: (Boolean) -> Unit) {
        switchView.setOnCheckedChangeListener { _, checked -> listener(checked) }
    }

}

fun ViewGroup.statusBar(init: StatusBar.() -> Unit): StatusBar {
    val view = StatusBar(context)
    view.init()
    addView(view)
    return view
}

fun Context.statusBar(init: StatusBar.() -> Unit) = StatusBar(this).apply(init)
