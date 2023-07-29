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
import ru.dyatel.inuyama.finance.dto.FinanceOperationDirection
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

    private lateinit var directionSelector: IconTabSelector<FinanceOperationDirection>
    private lateinit var accountSelector: FinanceAccountSelector
    private lateinit var operationListEditor: FinanceOperationListEditor
    private lateinit var dateTimeEditor: DateTimeEditor

    private var accounts = emptyList<FinanceAccount>()

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
                                FinanceOperationDirection.EXPENSE to CommunityMaterial.Icon.cmd_cart_outline,
                                FinanceOperationDirection.INCOME to CommunityMaterial.Icon.cmd_credit_card_plus,
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
        this.accounts = accounts
        accountSelector.bindItems(accounts)
    }

    fun bindCategories(categories: List<FinanceCategory>) {
        operationListEditor.bindCategories(categories)
    }

    override fun onChange(listener: (FinanceReceiptInfo) -> Unit) = changePublisher.onChange(listener)

    override fun fillFrom(data: FinanceReceiptInfo) {
        val missingAccounts = listOfNotNull(
            data.account.takeIf { it !in accounts },
        )
        accountSelector.bindItems(missingAccounts + accounts)

        changePublisher.notifyAfterBatch {
            directionSelector.selected = data.direction
            accountSelector.selected = data.account
            operationListEditor.fillFrom(
                data.operations.map { it.copy(amount = abs(it.amount)) }
            )
            dateTimeEditor.fillFrom(data.datetime)
        }
    }

    override fun buildValue(): FinanceReceiptInfo {
        return FinanceReceiptInfo(
            direction = directionSelector.selected!!,
            account = accountSelector.selected!!,
            operations = operationListEditor.buildValue(),
            datetime = dateTimeEditor.buildValue()
        )
    }
}
