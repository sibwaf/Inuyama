package ru.dyatel.inuyama.finance

import android.content.Context
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
import ru.dyatel.inuyama.screens.InuScreen
import sibwaf.inuyama.app.common.DIM_LARGE

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

class FinanceTransferScreen : InuScreen<FinanceTransferView>() {

    override val titleResource = R.string.screen_finance_new_transfer

    private val accountManager by instance<FinanceAccountManager>()
    private val operationManager by instance<FinanceOperationManager>()

    override fun createView(context: Context): FinanceTransferView {
        return FinanceTransferView(context).apply {
            editor.bindAccounts(accountManager.getActiveAccounts())
            editor.onChange {
                saveButton.isEnabled = it != null
            }

            saveButton.setOnClickListener {
                saveTransfer(editor.buildValue()!!)
                navigator.goBack()
            }
        }
    }

    private fun saveTransfer(transfer: FinanceTransferDto) {
        operationManager.createTransfer(
            from = transfer.fromAccount,
            to = transfer.toAccount,
            amount = transfer.amount
        )
    }
}
