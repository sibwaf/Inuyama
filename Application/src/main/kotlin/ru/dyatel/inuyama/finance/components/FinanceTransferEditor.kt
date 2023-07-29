package ru.dyatel.inuyama.finance.components

import android.content.Context
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import org.jetbrains.anko.alignParentLeft
import org.jetbrains.anko.alignParentRight
import org.jetbrains.anko.centerVertically
import org.jetbrains.anko.hintResource
import org.jetbrains.anko.imageView
import org.jetbrains.anko.leftOf
import org.jetbrains.anko.margin
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.relativeLayout
import org.jetbrains.anko.verticalLayout
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.finance.dto.FinanceTransferDto
import ru.dyatel.inuyama.layout.FinanceAccountSelector
import ru.dyatel.inuyama.layout.financeAccountSelector
import ru.dyatel.inuyama.model.FinanceAccount
import ru.dyatel.inuyama.utilities.ListenableEditor
import ru.dyatel.inuyama.utilities.PublishListenerHolderImpl
import ru.dyatel.inuyama.utilities.withAdditionalListener
import ru.dyatel.inuyama.utilities.withBatching
import ru.dyatel.inuyama.utilities.withEditor
import sibwaf.inuyama.app.common.DIM_EXTRA_LARGE
import sibwaf.inuyama.app.common.DIM_LARGE
import sibwaf.inuyama.app.common.DIM_MEDIUM
import sibwaf.inuyama.app.common.components.UniformDoubleInput
import sibwaf.inuyama.app.common.components.uniformDoubleInput

class FinanceTransferEditor(context: Context) : LinearLayout(context), ListenableEditor<FinanceTransferDto?> {

    private lateinit var fromAccountSelector: FinanceAccountSelector
    private lateinit var toAccountSelector: FinanceAccountSelector
    private val dateTimeEditor: DateTimeEditor
    private val amountFromEditor: UniformDoubleInput
    private lateinit var amountToEditor: UniformDoubleInput

    private var accounts = emptyList<FinanceAccount>()

    private val changePublisher = PublishListenerHolderImpl<FinanceTransferDto?>()
        .withAdditionalListener { transfer ->
            transfer ?: return@withAdditionalListener
            amountToEditor.isVisible = transfer.toAccount.currency != transfer.fromAccount.currency
        }
        .withEditor(this)
        .withBatching()

    init {
        orientation = VERTICAL

        dateTimeEditor = DateTimeEditor(context).apply {
            onChange { changePublisher.notifyListener() }
        }
        addView(dateTimeEditor)

        relativeLayout {
            lparams(width = matchParent) {
                topMargin = DIM_MEDIUM
            }

            val accountSelectors = verticalLayout {
                id = generateViewId()

                fromAccountSelector = financeAccountSelector {
                    lparams(width = matchParent) {
                        margin = DIM_LARGE
                    }

                    onItemSelected { selected ->
                        changePublisher.notifyAfterBatch {
                            if (toAccountSelector.selected == selected) {
                                toAccountSelector.selected = accounts.firstOrNull { it != selected }
                            }
                        }
                    }
                }
                toAccountSelector = financeAccountSelector {
                    lparams(width = matchParent) {
                        margin = DIM_LARGE
                    }

                    onItemSelected { selected ->
                        changePublisher.notifyAfterBatch {
                            if (fromAccountSelector.selected == selected) {
                                fromAccountSelector.selected = accounts.firstOrNull { it != selected }
                            }
                        }
                    }
                }
            }

            val icon = IconicsDrawable(context)
                .icon(CommunityMaterial.Icon.cmd_arrow_down)
                .sizeDp(24)
                .colorRes(R.color.md_dark_primary_text)

            val iconView = imageView(icon) {
                id = generateViewId()
            }

            accountSelectors.lparams {
                alignParentLeft()
                leftOf(iconView)
            }
            iconView.lparams {
                margin = DIM_EXTRA_LARGE
                leftMargin = 0

                alignParentRight()
                centerVertically()
            }
        }

        amountFromEditor = uniformDoubleInput {
            hintResource = R.string.hint_finance_amount
            doAfterTextChanged { changePublisher.notifyListener() }
        }

        amountToEditor = uniformDoubleInput {
            hintResource = R.string.hint_finance_amount_converted
            doAfterTextChanged { changePublisher.notifyListener() }
        }
    }

    fun bindAccounts(accounts: List<FinanceAccount>) {
        this.accounts = accounts
        fromAccountSelector.bindItems(accounts)
        toAccountSelector.bindItems(accounts)
    }

    override fun onChange(listener: (FinanceTransferDto?) -> Unit) = changePublisher.onChange(listener)

    override fun fillFrom(data: FinanceTransferDto?) {
        if (data == null) {
            return
        }

        val missingAccounts = listOfNotNull(
            data.fromAccount.takeIf { it !in accounts },
            data.toAccount.takeIf { it !in accounts },
        )
        fromAccountSelector.bindItems(missingAccounts + accounts)
        toAccountSelector.bindItems(missingAccounts + accounts)

        changePublisher.notifyAfterBatch {
            fromAccountSelector.selected = data.fromAccount
            toAccountSelector.selected = data.toAccount
            dateTimeEditor.fillFrom(data.datetime)
            amountFromEditor.value = data.amountFrom
            amountToEditor.value = data.amountTo
        }
    }

    override fun buildValue(): FinanceTransferDto? {
        val fromAccount = fromAccountSelector.selected ?: return null
        val toAccount = toAccountSelector.selected ?: return null

        return FinanceTransferDto(
            fromAccount = fromAccount,
            toAccount = toAccount,
            amountFrom = amountFromEditor.value,
            amountTo = if (fromAccount.currency == toAccount.currency) amountFromEditor.value else amountToEditor.value,
            datetime = dateTimeEditor.buildValue(),
        )
    }
}
