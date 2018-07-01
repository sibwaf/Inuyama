package ru.dyatel.inuyama.layout

import android.support.design.widget.TextInputEditText
import android.support.design.widget.TextInputLayout
import android.view.ViewGroup
import org.jetbrains.anko.design.textInputEditText
import org.jetbrains.anko.design.textInputLayout
import org.jetbrains.anko.leftPadding
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.rightPadding
import org.jetbrains.anko.wrapContent
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