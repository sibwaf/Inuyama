package sibwaf.inuyama.app.common.components

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import org.jetbrains.anko.frameLayout
import org.jetbrains.anko.verticalLayout
import sibwaf.inuyama.app.common.DIM_LARGE
import kotlin.properties.Delegates

// todo: fix cropped shadow
class NestedFloatingActionButton(context: Context) : LinearLayout(context) {

    private var mainDrawable: Drawable? = null

    private var mainButton: FloatingActionButton? = null
    private val mainButtonContainer: ViewGroup
    private val extraButtonContainer: ViewGroup

    private var mainButtonClickListener: (() -> Unit)? = null

    init {
        orientation = VERTICAL

        extraButtonContainer = verticalLayout {
            gravity = Gravity.CENTER_HORIZONTAL
            isVisible = false
        }
        mainButtonContainer = frameLayout()
    }

    private var extraButtonsVisible by Delegates.observable(false) { _, _, extraButtonsVisible ->
        extraButtonContainer.isVisible = extraButtonsVisible
        if (extraButtonsVisible) {
            mainButton?.withIcon(CommunityMaterial.Icon.cmd_close)
        } else {
            mainButton?.setImageDrawable(mainDrawable)
        }
    }

    fun setMainButton(init: Context.() -> FloatingActionButton): FloatingActionButton {
        val button = context.init()

        mainDrawable = button.drawable
        mainButton = button

        button.setOnClickListener {
            if (extraButtonsVisible) {
                extraButtonsVisible = false
            } else {
                mainButtonClickListener?.invoke()
            }
        }
        button.setOnLongClickListener {
            if (!extraButtonsVisible) {
                extraButtonsVisible = true
            }
            true
        }

        mainButtonContainer.removeAllViews()
        mainButtonContainer.addView(button)

        return button
    }

    fun onMainButtonClick(listener: () -> Unit) {
        mainButtonClickListener = listener
    }

    fun addExtraButton(init: Context.() -> FloatingActionButton): FloatingActionButton {
        val button = context.init().apply {
            size = FloatingActionButton.SIZE_MINI
        }

        // todo: this seems excessive for just a margin
        val buttonWrapper = context.frameLayout {
            lparams {
                bottomMargin = DIM_LARGE
            }

            addView(button)
        }

        extraButtonContainer.addView(buttonWrapper, 0)
        return button
    }
}

fun Context.nestedFloatingActionButton(init: NestedFloatingActionButton.() -> Unit = {}) =
    NestedFloatingActionButton(this).apply(init)

fun ViewGroup.nestedFloatingActionButton(init: NestedFloatingActionButton.() -> Unit = {}) =
    context.nestedFloatingActionButton(init).also { addView(it) }
