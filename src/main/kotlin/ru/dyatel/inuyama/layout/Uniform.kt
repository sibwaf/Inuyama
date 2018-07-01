package ru.dyatel.inuyama.layout

import android.app.AlertDialog
import android.content.Context
import android.support.design.widget.TextInputEditText
import android.support.design.widget.TextInputLayout
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import org.jetbrains.anko._LinearLayout
import org.jetbrains.anko.appcompat.v7.tintedButton
import org.jetbrains.anko.cardview.v7.cardView
import org.jetbrains.anko.design.textInputEditText
import org.jetbrains.anko.design.textInputLayout
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
import ru.dyatel.inuyama.utilities.disableSuggestions
import ru.dyatel.inuyama.utilities.disableUiExtraction

inline fun ViewGroup.uniformTextInput(init: TextInputEditText.() -> Unit): TextInputLayout {
    return textInputLayout {
        lparams(width = matchParent, height = wrapContent) {
            bottomMargin = DIM_LARGE
        }

        textInputEditText {
            disableUiExtraction()
            disableSuggestions()

            leftPadding = DIM_LARGE
            rightPadding = DIM_LARGE

            init()
        }
    }
}

inline fun Context.uniformWatchView(
        descriptionViewId: Int,
        editButtonId: Int, removeButtonId: Int,
        init: _LinearLayout.() -> Unit
): View {
    return cardView {
        lparams(width = matchParent, height = wrapContent) {
            margin = DIM_LARGE
        }

        verticalLayout {
            lparams(width = matchParent, height = wrapContent) {
                padding = DIM_LARGE
            }

            textView {
                id = descriptionViewId
                gravity = Gravity.CENTER_HORIZONTAL
                textSize = SP_MEDIUM
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
