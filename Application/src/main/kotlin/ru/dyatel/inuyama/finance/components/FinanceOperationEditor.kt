package ru.dyatel.inuyama.finance.components

import android.content.Context
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import androidx.core.widget.doAfterTextChanged
import org.jetbrains.anko.hintResource
import org.jetbrains.anko.linearLayout
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.verticalLayout
import org.jetbrains.anko.verticalMargin
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.finance.dto.FinanceOperationInfo
import ru.dyatel.inuyama.layout.FinanceCategorySelector
import ru.dyatel.inuyama.layout.financeCategorySelector
import ru.dyatel.inuyama.model.FinanceCategory
import ru.dyatel.inuyama.utilities.ListenableEditor
import ru.dyatel.inuyama.utilities.PublishListenerHolderImpl
import ru.dyatel.inuyama.utilities.withBatching
import ru.dyatel.inuyama.utilities.withEditor
import sibwaf.inuyama.app.common.DIM_LARGE
import sibwaf.inuyama.app.common.components.UniformDoubleInput
import sibwaf.inuyama.app.common.components.UniformTextInput
import sibwaf.inuyama.app.common.components.uniformDoubleInput
import sibwaf.inuyama.app.common.components.uniformTextInput

class FinanceOperationEditor(context: Context) : FrameLayout(context), ListenableEditor<FinanceOperationInfo?> {

    private lateinit var categorySelector: FinanceCategorySelector
    private lateinit var amountEditor: UniformDoubleInput
    private lateinit var descriptionEditor: UniformTextInput

    private val changePublisher = PublishListenerHolderImpl<FinanceOperationInfo?>()
        .withEditor(this)
        .withBatching()

    private var forwardAction: (() -> Unit)? = null

    init {
        verticalLayout {
            categorySelector = financeCategorySelector {
                lparams(width = matchParent) {
                    verticalMargin = DIM_LARGE
                }

                onItemSelected { changePublisher.notifyListener() }
            }
            linearLayout {
                weightSum = 1.0f

                descriptionEditor = uniformTextInput {
                    id = generateViewId()
                    hintResource = R.string.hint_description
                    imeOptions = imeOptions.or(EditorInfo.IME_ACTION_NEXT)

                    doAfterTextChanged { changePublisher.notifyListener() }
                }.apply {
                    lparams {
                        width = 0
                        weight = 0.7f
                    }
                }

                amountEditor = uniformDoubleInput {
                    id = generateViewId()
                    hintResource = R.string.hint_finance_amount

                    doAfterTextChanged { changePublisher.notifyListener() }

                    setOnEditorActionListener { _, actionId, _ ->
                        val forwardAction = forwardAction
                        if (forwardAction != null && actionId == EditorInfo.IME_ACTION_NEXT) {
                            forwardAction()
                        }

                        false
                    }
                }.apply {
                    lparams {
                        width = 0
                        weight = 0.3f
                    }
                }
            }
        }
    }

    fun bindCategories(categories: List<FinanceCategory>) {
        categorySelector.bindItems(categories)
    }

    fun withForwardAction(listener: (() -> Unit)? = null) {
        forwardAction = listener
        amountEditor.editText!!.apply { imeOptions = imeOptions.or(EditorInfo.IME_ACTION_NEXT) }
    }

    override fun onChange(listener: (FinanceOperationInfo?) -> Unit) = changePublisher.onChange(listener)

    override fun fillFrom(data: FinanceOperationInfo?) {
        if (data == null) {
            // todo: fill with empty values?
            return
        }

        changePublisher.notifyAfterBatch {
            categorySelector.selected = data.category
            if (data.amount == 0.0) {
                amountEditor.text = ""
            } else {
                amountEditor.value = data.amount
            }
            descriptionEditor.text = data.description ?: ""
        }
    }

    override fun buildValue(): FinanceOperationInfo? {
        val category = categorySelector.selected ?: return null

        return FinanceOperationInfo(
            category = category,
            amount = amountEditor.value,
            description = descriptionEditor.text.takeUnless { it.isBlank() }
        )
    }
}
