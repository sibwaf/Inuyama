package ru.dyatel.inuyama.layout.components

import android.app.AlertDialog
import android.content.Context
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.jetbrains.anko._LinearLayout
import org.jetbrains.anko.appcompat.v7.tintedButton
import org.jetbrains.anko.cardview.v7.cardView
import org.jetbrains.anko.design._TextInputLayout
import org.jetbrains.anko.design.textInputEditText
import org.jetbrains.anko.leftPadding
import org.jetbrains.anko.linearLayout
import org.jetbrains.anko.margin
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.padding
import org.jetbrains.anko.rightPadding
import org.jetbrains.anko.textResource
import org.jetbrains.anko.textView
import org.jetbrains.anko.verticalLayout
import org.jetbrains.anko.wrapContent
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.layout.DIM_EXTRA_LARGE
import ru.dyatel.inuyama.layout.DIM_LARGE
import ru.dyatel.inuyama.layout.SP_MEDIUM
import ru.dyatel.inuyama.utilities.disableSuggestions
import ru.dyatel.inuyama.utilities.disableUiExtraction

inline fun ViewGroup.uniformTextView(init: TextView.() -> Unit = {}): TextView {
    return textView {
        textSize = SP_MEDIUM
        init()
    }
}

inline fun TextInputLayout.uniformTextInputEditText(init: TextInputEditText.() -> Unit = {}): TextInputEditText {
    return textInputEditText {
        disableUiExtraction()
        disableSuggestions()

        leftPadding = DIM_LARGE
        rightPadding = DIM_LARGE

        init()
    }
}

open class UniformTextInput(context: Context) : _TextInputLayout(context) {

    var text: String
        get() = editText!!.text.toString()
        set(value) {
            editText!!.setText(value)
        }

    init {
        lparams(width = matchParent, height = wrapContent) {
            bottomMargin = DIM_LARGE
        }
    }
}

inline fun ViewGroup.uniformTextInputLayout(init: _TextInputLayout.() -> Unit = {}): UniformTextInput {
    val view = UniformTextInput(context)
    view.init()
    addView(view)
    return view
}

inline fun ViewGroup.uniformTextInput(init: TextInputEditText.() -> Unit = {}): UniformTextInput {
    return uniformTextInputLayout {
        uniformTextInputEditText(init)
    }
}

class UniformDoubleInput(context: Context) : UniformTextInput(context) {

    var value: Double
        get() = editText!!.text.toString()
                .takeIf { it.isNotBlank() }
                ?.let { it.toDouble() }
                ?: 0.0
        set(value) {
            editText!!.setText(value.toString())
        }
}

inline fun ViewGroup.uniformDoubleInput(init: TextInputEditText.() -> Unit = {}): UniformDoubleInput {
    val view = UniformDoubleInput(context)
    view.uniformTextInputEditText {
        inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        init()
    }
    addView(view)
    return view
}

inline fun Context.uniformWatchView(
        descriptionViewId: Int,
        editButtonId: Int, removeButtonId: Int,
        init: _LinearLayout.() -> Unit = {}
): View {
    return cardView {
        lparams(width = matchParent, height = wrapContent) {
            margin = DIM_LARGE
        }

        verticalLayout {
            lparams(width = matchParent, height = wrapContent) {
                padding = DIM_LARGE
            }

            uniformTextView {
                id = descriptionViewId
                gravity = Gravity.CENTER_HORIZONTAL
            }

            verticalLayout {
                lparams(width = matchParent, height = wrapContent) {
                    margin = DIM_EXTRA_LARGE
                }

                init(this)
            }

            linearLayout {
                lparams(width = matchParent, height = matchParent)

                tintedButton {
                    id = editButtonId
                    textResource = R.string.action_edit
                }.lparams(width = 0) {
                    weight = 0.5f
                }

                tintedButton {
                    id = removeButtonId
                    textResource = R.string.action_remove
                }.lparams(width = 0) {
                    weight = 0.5f
                }
            }
        }
    }
}

inline fun Context.showConfirmationDialog(title: String, message: String, action: String, crossinline callback: () -> Unit) {
    AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(action) { _, _ -> callback() }
            .setNegativeButton(R.string.action_cancel) { _, _ -> }
            .show()
}
