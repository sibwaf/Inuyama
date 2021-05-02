package ru.dyatel.inuyama.layout

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.core.view.isVisible
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon
import org.jetbrains.anko.alignParentLeft
import org.jetbrains.anko.alignParentRight
import org.jetbrains.anko.centerVertically
import org.jetbrains.anko.find
import org.jetbrains.anko.imageView
import org.jetbrains.anko.leftOf
import org.jetbrains.anko.leftPadding
import org.jetbrains.anko.linearLayout
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.padding
import org.jetbrains.anko.relativeLayout
import org.jetbrains.anko.rightPadding
import org.jetbrains.anko.wrapContent
import ru.dyatel.inuyama.ITEM_TYPE_DIRECTORY
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.model.Directory
import sibwaf.inuyama.app.common.DIM_EXTRA_LARGE
import sibwaf.inuyama.app.common.DIM_LARGE
import sibwaf.inuyama.app.common.components.uniformTextInput
import sibwaf.inuyama.app.common.components.uniformTextView

class DirectoryItem(
    private val directory: Directory,
    private val modeChangeListener: () -> Unit,
    private val saveListener: (String) -> Unit,
    private val removeListener: () -> Unit
) : AbstractItem<DirectoryItem, DirectoryItem.ViewHolder>() {

    private companion object {
        val pathViewId = View.generateViewId()
        val editViewId = View.generateViewId()

        val buttonContainerId = View.generateViewId()
        val editButtonId = View.generateViewId()
        val removeButtonId = View.generateViewId()
        val saveButtonId = View.generateViewId()
    }

    private var editMode = false

    init {
        withIdentifier(directory.id)
    }

    class ViewHolder(view: View) : FastAdapter.ViewHolder<DirectoryItem>(view) {

        private val pathView = view.find<TextView>(pathViewId)
        private val editButton = view.find<View>(editButtonId)
        private val removeButton = view.find<View>(removeButtonId)

        private val editView = view.find<EditText>(editViewId)
        private val saveButton = view.find<View>(saveButtonId)

        override fun unbindView(item: DirectoryItem) {
            editButton.setOnClickListener(null)
            removeButton.setOnClickListener(null)
            saveButton.setOnClickListener(null)
        }

        override fun bindView(item: DirectoryItem, payloads: MutableList<Any>?) {
            pathView.isVisible = !item.editMode
            editButton.isVisible = !item.editMode
            removeButton.isVisible = !item.editMode

            editView.isVisible = item.editMode
            saveButton.isVisible = item.editMode

            editButton.setOnClickListener {
                item.editMode = !item.editMode
                item.modeChangeListener()
            }

            if (item.editMode) {
                editView.setText(item.directory.path)
                saveButton.setOnClickListener {
                    item.editMode = false
                    item.saveListener(editView.text.toString())
                }
            } else {
                pathView.text = item.directory.path
                removeButton.setOnClickListener { item.removeListener() }
            }
        }
    }

    override fun getType() = ITEM_TYPE_DIRECTORY

    override fun getViewHolder(view: View) = ViewHolder(view)

    override fun getLayoutRes() = throw UnsupportedOperationException()

    private fun createIcon(ctx: Context, icon: IIcon) =
        IconicsDrawable(ctx)
            .icon(icon)
            .sizeDp(20)
            .colorRes(R.color.material_drawer_dark_secondary_text)

    override fun createView(ctx: Context, parent: ViewGroup?): View {
        return ctx.relativeLayout {
            lparams(width = matchParent, height = wrapContent) {
                padding = DIM_LARGE
                leftPadding = DIM_EXTRA_LARGE
                rightPadding = DIM_EXTRA_LARGE
            }

            uniformTextView {
                id = pathViewId
            }.lparams {
                centerVertically()
                alignParentLeft()
                leftOf(buttonContainerId)
            }

            uniformTextInput {
                id = editViewId
            }.lparams {
                centerVertically()
                alignParentLeft()
                leftOf(buttonContainerId)
            }

            linearLayout {
                id = buttonContainerId

                imageView(createIcon(ctx, CommunityMaterial.Icon2.cmd_pencil)) {
                    id = editButtonId
                }

                imageView(createIcon(ctx, CommunityMaterial.Icon.cmd_close)) {
                    id = removeButtonId
                }.lparams {
                    leftMargin = DIM_LARGE
                }

                imageView(createIcon(ctx, CommunityMaterial.Icon.cmd_check)) {
                    id = saveButtonId
                }
            }.lparams {
                centerVertically()
                alignParentRight()

                leftMargin = DIM_LARGE
            }
        }
    }
}