package sibwaf.inuyama.app.common.utilities

import android.text.InputType
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView

fun <T : View> T.hideIf(predicate: (T) -> Boolean) {
    isVisible = !predicate(this)
}

fun EditText.disableUiExtraction() {
    imeOptions = imeOptions or EditorInfo.IME_FLAG_NO_EXTRACT_UI
}

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
