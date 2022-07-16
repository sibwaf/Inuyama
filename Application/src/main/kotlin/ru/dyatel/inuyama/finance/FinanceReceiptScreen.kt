package ru.dyatel.inuyama.finance

import android.content.Context
import android.widget.Button
import com.wealthfront.magellan.BaseScreenView
import io.objectbox.Box
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
import ru.dyatel.inuyama.finance.components.FinanceReceiptEditor
import ru.dyatel.inuyama.finance.dto.FinanceOperationInfo
import ru.dyatel.inuyama.finance.dto.FinanceReceiptInfo
import ru.dyatel.inuyama.model.FinanceAccount
import ru.dyatel.inuyama.model.FinanceCategory
import ru.dyatel.inuyama.model.FinanceReceipt
import ru.dyatel.inuyama.screens.InuScreen
import sibwaf.inuyama.app.common.DIM_LARGE

class FinanceReceiptView(context: Context) : BaseScreenView<FinanceReceiptScreen>(context) {

    val editor = FinanceReceiptEditor(context)

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

class FinanceReceiptScreen : InuScreen<FinanceReceiptView> {

    private val accountManager by instance<FinanceAccountManager>()
    private val operationManager by instance<FinanceOperationManager>()

    private val categoryBox by instance<Box<FinanceCategory>>()

    private val receipt: FinanceReceipt?
    private val receiptInfo: FinanceReceiptInfo?

    constructor() {
        receipt = null
        receiptInfo = null
    }

    constructor(receipt: FinanceReceipt) {
        this.receipt = receipt
        receiptInfo = FinanceReceiptInfo(
            account = receipt.account.target,
            operations = receipt.operations.map {
                FinanceOperationInfo(
                    category = it.categories.first(),
                    amount = it.amount,
                    description = it.description
                )
            }
        )
    }

    constructor(receiptInfo: FinanceReceiptInfo) {
        receipt = null
        this.receiptInfo = receiptInfo
    }

    constructor(account: FinanceAccount) {
        receipt = null
        receiptInfo = FinanceReceiptInfo(
            account = account,
            operations = emptyList()
        )
    }

    override val titleResource get() = if (receipt == null) R.string.screen_finance_new_receipt else R.string.screen_finance_edit_receipt

    override fun createView(context: Context): FinanceReceiptView {
        return FinanceReceiptView(context).apply {
            editor.bindAccounts(accountManager.getActiveAccounts())
            editor.bindCategories(categoryBox.all)

            if (receiptInfo != null) {
                editor.fillFrom(receiptInfo)
            }

            saveButton.setOnClickListener {
                saveReceipt(editor.buildValue())
                navigator.goBack()
            }
        }
    }

    private fun saveReceipt(receiptInfo: FinanceReceiptInfo) {
        if (receipt == null) {
            operationManager.createReceipt(
                account = receiptInfo.account,
                operations = receiptInfo.operations
            )
        } else {
            operationManager.update(
                receipt = receipt,
                account = receiptInfo.account,
                operations = receiptInfo.operations
            )
        }
    }
}
