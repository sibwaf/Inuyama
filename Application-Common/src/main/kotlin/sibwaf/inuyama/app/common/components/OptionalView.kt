package sibwaf.inuyama.app.common.components

import android.view.View
import android.view.ViewGroup
import org.jetbrains.anko.frameLayout

interface OptionalView {
    var isEmpty: Boolean
}

private class OptionalViewImpl(
    private val container: ViewGroup,
    private val regularView: View,
    private val emptyView: View,
    isEmpty: Boolean
) : OptionalView {

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

fun ViewGroup.createOptionalView(
    regularView: View,
    emptyView: View = context.uniformEmptyView(),
    isEmpty: Boolean = true,
    init: View.() -> Unit = {}
): OptionalView {
    return OptionalViewImpl(frameLayout(init), regularView, emptyView, isEmpty)
}
