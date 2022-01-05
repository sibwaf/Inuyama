package ru.dyatel.inuyama.finance.components

import android.content.Context
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import org.jetbrains.anko.alignParentLeft
import org.jetbrains.anko.alignParentRight
import org.jetbrains.anko.alignParentTop
import org.jetbrains.anko.cardview.v7._CardView
import org.jetbrains.anko.centerVertically
import org.jetbrains.anko.leftOf
import org.jetbrains.anko.margin
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.relativeLayout
import org.jetbrains.anko.verticalLayout
import org.jetbrains.anko.wrapContent
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.finance.dto.FinanceOperationInfo
import ru.dyatel.inuyama.model.FinanceCategory
import ru.dyatel.inuyama.utilities.ListenableEditor
import ru.dyatel.inuyama.utilities.PublishListenerHolderImpl
import ru.dyatel.inuyama.utilities.withAdditionalListener
import ru.dyatel.inuyama.utilities.withBatching
import ru.dyatel.inuyama.utilities.withEditor
import sibwaf.inuyama.app.common.DIM_LARGE
import sibwaf.inuyama.app.common.DIM_MEDIUM
import sibwaf.inuyama.app.common.components.uniformIconButton
import sibwaf.inuyama.app.common.components.uniformTextView

class FinanceOperationListEditor(context: Context) : LinearLayout(context), ListenableEditor<List<FinanceOperationInfo>> {

    private val rowContainer: ViewGroup
    private lateinit var totalSumView: TextView

    private val operationEditors = mutableListOf<FinanceOperationListEditorRow>()
    private var categories: List<FinanceCategory> = emptyList()

    private val changePublisher = PublishListenerHolderImpl<List<FinanceOperationInfo>>()
        .withAdditionalListener { operations ->
            val amount = operations.sumByDouble { it.amount }
            totalSumView.text = context.getString(R.string.label_finance_amount, amount)
        }
        .withEditor(this)
        .withBatching()

    init {
        orientation = VERTICAL

        rowContainer = verticalLayout()

        relativeLayout {
            lparams(height = wrapContent, width = matchParent)

            totalSumView = uniformTextView {
                id = generateViewId()
                gravity = Gravity.CENTER_HORIZONTAL
            }

            val addEditorButton = uniformIconButton(CommunityMaterial.Icon2.cmd_plus) {
                id = generateViewId()
                setOnClickListener { addOperationEditor() }
            }

            totalSumView.lparams {
                margin = DIM_MEDIUM

                centerVertically()
                alignParentLeft()
                leftOf(addEditorButton)
            }
            addEditorButton.lparams {
                centerVertically()
                alignParentRight()
            }
        }

        addOperationEditor()
    }

    private fun addOperationEditor() {
        val editor = FinanceOperationListEditorRow(context).apply {
            bindCategories(categories)
            onRemove { removeOperationEditor(this) }
            onChange { changePublisher.notifyListener() }
        }

        operationEditors += editor
        rowContainer.addView(editor)

        changePublisher.notifyListener()
    }

    private fun removeOperationEditor(editor: FinanceOperationListEditorRow) {
        operationEditors -= editor
        rowContainer.removeView(editor)

        changePublisher.notifyListener()
    }

    fun bindCategories(categories: List<FinanceCategory>) {
        this.categories = categories
        for (editor in operationEditors) {
            editor.bindCategories(categories)
        }
    }

    override fun onChange(listener: (List<FinanceOperationInfo>) -> Unit) = changePublisher.onChange(listener)

    override fun fillFrom(data: List<FinanceOperationInfo>) {
        changePublisher.notifyAfterBatch {
            while (operationEditors.size > data.size) {
                removeOperationEditor(operationEditors.last())
            }
            while (operationEditors.size < data.size) {
                addOperationEditor()
            }

            for ((operation, editor) in data.zip(operationEditors)) {
                editor.fillFrom(operation)
            }
        }
    }

    override fun buildValue(): List<FinanceOperationInfo> {
        return operationEditors.mapNotNull { it.buildValue() }
    }
}

private class FinanceOperationListEditorRow(context: Context) : _CardView(context), ListenableEditor<FinanceOperationInfo?> {

    private val editor: FinanceOperationEditor
    private var removeListener: (() -> Unit)? = null

    init {
        lparams {
            margin = DIM_MEDIUM
        }

        editor = FinanceOperationEditor(context).apply {
            id = LinearLayout.generateViewId()
        }

        relativeLayout {
            lparams {
                margin = DIM_LARGE
            }

            addView(editor)

            // todo: disable if there is only one operation
            val removeButton = uniformIconButton(CommunityMaterial.Icon2.cmd_trash_can_outline) {
                id = generateViewId()
                setOnClickListener { removeListener?.invoke() }
            }

            editor.lparams {
                alignParentTop()
                alignParentLeft()
                leftOf(removeButton)
            }
            removeButton.lparams {
                alignParentTop()
                alignParentRight()
            }
        }
    }

    fun bindCategories(categories: List<FinanceCategory>) = editor.bindCategories(categories)

    fun onRemove(listener: () -> Unit) {
        removeListener = listener
    }

    override fun onChange(listener: (FinanceOperationInfo?) -> Unit) = editor.onChange(listener)

    override fun fillFrom(data: FinanceOperationInfo?) = editor.fillFrom(data)

    override fun buildValue() = editor.buildValue()
}
