package ru.dyatel.inuyama.layout

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.jetbrains.anko._LinearLayout
import org.jetbrains.anko.backgroundColorResource
import org.jetbrains.anko.margin
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.view
import org.jetbrains.anko.wrapContent
import ru.dyatel.inuyama.R
import kotlin.properties.Delegates

enum class State {
    FAIL, PENDING, OK
}

class Marker(context: Context) : _LinearLayout(context) {

    private val markerView: View
    private val textView: TextView

    init {
        lparams(width = matchParent, height = wrapContent)

        markerView = view {
        }.lparams(width = DIM_MEDIUM, height = matchParent)

        textView = uniformTextView().lparams {
            margin = DIM_LARGE
            rightMargin = 0
        }
    }

    var state by Delegates.observable(State.PENDING) { _, _, value ->
        markerView.backgroundColorResource = when (value) {
            State.FAIL -> R.color.color_fail
            State.PENDING -> R.color.color_pending
            State.OK -> R.color.color_ok
        }
    }

    var text: String
        get() = textView.text.toString()
        set(value) {
            textView.text = value
        }

}

fun ViewGroup.marker(init: Marker.() -> Unit): Marker {
    val view = Marker(context)
    view.init()
    addView(view)
    return view
}

fun Context.marker(init: Marker.() -> Unit) = Marker(this).apply(init)
