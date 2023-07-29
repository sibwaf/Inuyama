package sibwaf.inuyama.app.common.utilities

import android.text.InputType
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.annotation.Px
import androidx.cardview.widget.CardView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import org.jetbrains.anko.inputMethodManager

fun <T : View> T.hideIf(predicate: (T) -> Boolean) {
    isVisible = !predicate(this)
}

fun EditText.disableUiExtraction() {
    // todo: seems useless
    imeOptions = imeOptions or EditorInfo.IME_FLAG_NO_EXTRACT_UI
}

// todo: inputType = inputType or ...
// todo: will make all inputs multiline, needs fixing
fun EditText.disableSuggestions() {
    inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
}

fun EditText.capitalizeSentences() {
    inputType = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
}

fun RecyclerView.propagateTouchEvents() {
    val parent = parent as ViewGroup

    setOnTouchListener { _, event -> parent.onTouchEvent(event) }
    addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
        override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
            parent.onTouchEvent(e)
        }

        override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent) = true
        override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) = Unit
    })
}

fun CardView.setContentPadding(@Px value: Int) = setContentPadding(value, value, value, value)

fun View.showKeyboard() {
    val inputMethodManager = context.inputMethodManager
    inputMethodManager.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
}

fun View.hideKeyboard() {
    val inputMethodManager = context.inputMethodManager
    inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
}
