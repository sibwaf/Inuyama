package ru.dyatel.inuyama.finance

import android.content.Context
import android.view.Menu
import android.widget.Button
import com.wealthfront.magellan.BaseScreenView
import org.jetbrains.anko.alignParentBottom
import org.jetbrains.anko.alignParentLeft
import org.jetbrains.anko.alignParentRight
import org.jetbrains.anko.alignParentTop
import org.jetbrains.anko.appcompat.v7.tintedButton
import org.jetbrains.anko.margin
import org.jetbrains.anko.relativeLayout
import org.jetbrains.anko.support.v4.nestedScrollView
import org.jetbrains.anko.topOf
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.finance.components.FinanceTransferEditor
import ru.dyatel.inuyama.finance.dto.FinanceTransferDto
import ru.dyatel.inuyama.model.FinanceTransfer
import ru.dyatel.inuyama.screens.InuScreen
import sibwaf.inuyama.app.common.DIM_LARGE
import sibwaf.inuyama.app.common.components.showConfirmationDialog

class FinanceTransferView(context: Context) : BaseScreenView<FinanceTransferScreen>(context) {

    val editor = FinanceTransferEditor(context)

    lateinit var saveButton: Button
        private set

    init {
        relativeLayout {
            val editorView = nestedScrollView {
                id = generateViewId()
                addView(editor)
            }

            saveButton = tintedButton(R.string.action_save) {
                id = generateViewId()
            }

            editorView.lparams {
                margin = DIM_LARGE
                bottomMargin = 0

                alignParentLeft()
                alignParentRight()
                alignParentTop()
                topOf(saveButton)
            }
            saveButton.lparams {
                alignParentLeft()
                alignParentRight()
                alignParentBottom()
            }
        }
    }
}

class FinanceTransferScreen : InuScreen<FinanceTransferView> {

    private val accountManager by instance<FinanceAccountManager>()
    private val operationManager by instance<FinanceOperationManager>()

    private val transfer: FinanceTransfer?
    private val transferInfo: FinanceTransferDto

    constructor(transfer: FinanceTransfer) {
        this.transfer = transfer
        transferInfo = FinanceTransferDto(
            fromAccount = transfer.from.target,
            toAccount = transfer.to.target,
            amountFrom = transfer.amount,
            amountTo = transfer.amountTo,
            datetime = transfer.datetime,
        )
    }

    constructor(transferInfo: FinanceTransferDto) {
        transfer = null
        this.transferInfo = transferInfo
    }

    override val titleResource get() = if (transfer == null) R.string.screen_finance_new_transfer else R.string.screen_finance_edit_transfer

    override fun createView(context: Context): FinanceTransferView {
        return FinanceTransferView(context).apply {
            editor.onChange {
                saveButton.isEnabled = it != null
            }

            editor.bindAccounts(accountManager.getActiveAccounts())
            editor.fillFrom(transferInfo)

            saveButton.setOnClickListener {
                saveTransfer(editor.buildValue()!!)
                navigator.goBack()
            }
        }
    }

    private fun saveTransfer(transferInfo: FinanceTransferDto) {
        if (transfer == null) {
            operationManager.createTransfer(transferInfo)
        } else {
            operationManager.update(transfer, transferInfo)
        }
    }

    override fun onUpdateMenu(menu: Menu) {
        if (transfer == null) {
            return
        }

        menu.findItem(R.id.remove).apply {
            isVisible = true
            setOnMenuItemClickListener {
                val context = context ?: return@setOnMenuItemClickListener false

                context.showConfirmationDialog(
                    title = context.getString(R.string.dialog_finance_remove_transfer_title),
                    message = context.getString(R.string.dialog_finance_remove_transfer_message),
                    action = context.getString(R.string.action_remove),
                ) {
                    operationManager.cancel(transfer)
                    navigator.goBack()
                }

                true
            }
        }
    }
}
