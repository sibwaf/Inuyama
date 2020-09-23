package ru.dyatel.inuyama.layout.components

import android.content.Context
import android.view.View
import android.view.ViewGroup
import org.jetbrains.anko.frameLayout

interface OptionalView {
    var isEmpty: Boolean
}

private class OptionalViewImpl(
        context: Context,
        private val container: ViewGroup,
        private val regularView: View,
        isEmpty: Boolean
) : OptionalView {

    private val emptyView: View by lazy { context.uniformEmptyView() }

    override var isEmpty: Boolean = false
        set(value) {
            if (field != value || container.childCount == 0) {
                if (value) {
                    container.removeAllViews()
                    container.addView(emptyView)
                } else {
                    container.removeAllViews()
                    container.addView(regularView)
                }
            }

            field = value
        }

    init {
        this.isEmpty = isEmpty
    }
}

fun ViewGroup.createOptionalView(view: View, isEmpty: Boolean): OptionalView {
    return OptionalViewImpl(context, frameLayout(), view, isEmpty)
}
