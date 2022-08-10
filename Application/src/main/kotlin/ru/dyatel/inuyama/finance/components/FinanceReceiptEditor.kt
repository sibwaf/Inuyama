package ru.dyatel.inuyama.finance.components

import android.content.Context
import android.widget.FrameLayout
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import org.jetbrains.anko.cardview.v7.themedCardView
import org.jetbrains.anko.horizontalMargin
import org.jetbrains.anko.margin
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.verticalLayout
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.finance.dto.FinanceReceiptInfo
import ru.dyatel.inuyama.layout.FinanceAccountSelector
import ru.dyatel.inuyama.layout.financeAccountSelector
import ru.dyatel.inuyama.model.FinanceAccount
import ru.dyatel.inuyama.model.FinanceCategory
import ru.dyatel.inuyama.utilities.ListenableEditor
import ru.dyatel.inuyama.utilities.PublishListenerHolderImpl
import ru.dyatel.inuyama.utilities.withBatching
import ru.dyatel.inuyama.utilities.withEditor
import sibwaf.inuyama.app.common.DIM_EXTRA_LARGE
import sibwaf.inuyama.app.common.DIM_LARGE
import sibwaf.inuyama.app.common.DIM_MEDIUM
import sibwaf.inuyama.app.common.components.IconTabSelector
import sibwaf.inuyama.app.common.components.iconTabSelector
import kotlin.math.abs

class FinanceReceiptEditor(context: Context) : FrameLayout(context), ListenableEditor<FinanceReceiptInfo> {

    private lateinit var directionSelector: IconTabSelector<OperationDirection>
    private lateinit var accountSelector: FinanceAccountSelector
    private lateinit var operationListEditor: FinanceOperationListEditor
    private lateinit var dateTimeEditor: DateTimeEditor

    private val changePublisher = PublishListenerHolderImpl<FinanceReceiptInfo>()
        .withEditor(this)
        .withBatching()

    init {
        verticalLayout {
            themedCardView {
                lparams(width = matchParent) {
                    margin = DIM_MEDIUM
                }

                setCardBackgroundColor(context.getColor(R.color.color_primary))

                verticalLayout {
                    directionSelector = iconTabSelector {
                        bindOptions(
                            listOf(
                                OperationDirection.EXPENSE to CommunityMaterial.Icon.cmd_cart_outline,
                                OperationDirection.INCOME to CommunityMaterial.Icon.cmd_credit_card_plus,
                            )
                        )

                        onOptionSelected { changePublisher.notifyListener() }
                    }

                    accountSelector = financeAccountSelector {
                        lparams(width = matchParent) {
                            topMargin = DIM_EXTRA_LARGE
                            leftMargin = DIM_LARGE
                            rightMargin = DIM_MEDIUM
                        }

                        onItemSelected { changePublisher.notifyListener() }
                    }

                    dateTimeEditor = DateTimeEditor(context).apply {
                        lparams(width = matchParent) {
                            horizontalMargin = DIM_LARGE
                            bottomMargin = DIM_LARGE
                        }

                        onChange { changePublisher.notifyListener() }
                    }
                    addView(dateTimeEditor)
                }
            }

            operationListEditor = FinanceOperationListEditor(context).apply {
                onChange { changePublisher.notifyListener() }
            }
            addView(operationListEditor)
        }
    }

    fun bindAccounts(accounts: List<FinanceAccount>) {
        accountSelector.bindItems(accounts)
    }

    fun bindCategories(categories: List<FinanceCategory>) {
        operationListEditor.bindCategories(categories)
    }

    override fun onChange(listener: (FinanceReceiptInfo) -> Unit) = changePublisher.onChange(listener)

    override fun fillFrom(data: FinanceReceiptInfo) {
        changePublisher.notifyAfterBatch {
            val direction = if (data.operations.sumOf { it.amount } >= 0) {
                OperationDirection.INCOME
            } else {
                OperationDirection.EXPENSE
            }

            directionSelector.selected = direction
            accountSelector.selected = data.account
            operationListEditor.fillFrom(
                data.operations.map { it.copy(amount = abs(it.amount)) }
            )
            dateTimeEditor.fillFrom(data.datetime)
        }
    }

    override fun buildValue(): FinanceReceiptInfo {
        val operations = operationListEditor.buildValue().map {
            it.copy(
                amount = if (directionSelector.selected == OperationDirection.EXPENSE) -it.amount else it.amount
            )
        }

        return FinanceReceiptInfo(
            account = accountSelector.selected!!,
            operations = operations,
            datetime = dateTimeEditor.buildValue()
        )
    }
}

private enum class OperationDirection {
    EXPENSE, INCOME
}
