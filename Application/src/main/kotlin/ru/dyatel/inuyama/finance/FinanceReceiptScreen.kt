package ru.dyatel.inuyama.finance

import android.content.Context
import android.view.Menu
import android.widget.Button
import com.wealthfront.magellan.BaseScreenView
import io.objectbox.Box
import org.jetbrains.anko.alignParentBottom
import org.jetbrains.anko.alignParentLeft
import org.jetbrains.anko.alignParentRight
import org.jetbrains.anko.alignParentTop
import org.jetbrains.anko.appcompat.v7.tintedButton
import org.jetbrains.anko.backgroundColorResource
import org.jetbrains.anko.margin
import org.jetbrains.anko.relativeLayout
import org.jetbrains.anko.support.v4.nestedScrollView
import org.jetbrains.anko.topOf
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.finance.components.FinanceReceiptEditor
import ru.dyatel.inuyama.finance.dto.FinanceOperationDirection
import ru.dyatel.inuyama.finance.dto.FinanceOperationInfo
import ru.dyatel.inuyama.finance.dto.FinanceReceiptInfo
import ru.dyatel.inuyama.model.FinanceCategory
import ru.dyatel.inuyama.model.FinanceReceipt
import ru.dyatel.inuyama.screens.InuScreen
import sibwaf.inuyama.app.common.DIM_LARGE
import sibwaf.inuyama.app.common.components.showConfirmationDialog
import kotlin.math.abs

class FinanceReceiptView(context: Context) : BaseScreenView<FinanceReceiptScreen>(context) {

    val editor = FinanceReceiptEditor(context)

    lateinit var saveButton: Button
        private set

    init {
        relativeLayout {
            lparams {
                margin = DIM_LARGE
            }

            val editorView = nestedScrollView {
                id = generateViewId()
                addView(editor)
            }

            saveButton = tintedButton {
                id = generateViewId()

                editor.onChange { receipt ->
                    val amount = receipt.operations.sumOf { it.amount }

                    text = context.getString(R.string.label_finance_amount, abs(amount), receipt.account.currency)
                    backgroundColorResource = when (receipt.direction) {
                        FinanceOperationDirection.EXPENSE -> R.color.color_fail
                        FinanceOperationDirection.INCOME -> R.color.color_ok
                    }
                }
            }

            editorView.lparams {
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

    private val grabFocus: Boolean

    constructor(receipt: FinanceReceipt, grabFocus: Boolean) {
        this.receipt = receipt
        receiptInfo = FinanceReceiptInfo(
            // operationManager is not available here as it is received from kodein
            // and kodein requires an activity which was not set yet
            direction = if (receipt.operations.sumOf { it.amount } > 0) {
                FinanceOperationDirection.INCOME
            } else {
                FinanceOperationDirection.EXPENSE
            },
            account = receipt.account.target,
            operations = receipt.operations.map {
                FinanceOperationInfo(
                    category = it.categories.first(),
                    amount = it.amount,
                    description = it.description
                )
            },
            datetime = receipt.datetime,
        )
        this.grabFocus = grabFocus
    }

    constructor(receiptInfo: FinanceReceiptInfo, grabFocus: Boolean) {
        receipt = null
        this.receiptInfo = receiptInfo
        this.grabFocus = grabFocus
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

    override fun onShow(context: Context?) {
        super.onShow(context)

        if (grabFocus) {
            view.editor.requestFocus()
        }
    }

    private fun saveReceipt(receiptInfo: FinanceReceiptInfo) {
        if (receipt == null) {
            operationManager.createReceipt(receiptInfo)
        } else {
            operationManager.update(receipt, receiptInfo)
        }
    }

    override fun onUpdateMenu(menu: Menu) {
        if (receipt == null) {
            return
        }

        menu.findItem(R.id.remove).apply {
            isVisible = true
            setOnMenuItemClickListener {
                val context = context ?: return@setOnMenuItemClickListener false

                context.showConfirmationDialog(
                    title = context.getString(R.string.dialog_finance_remove_receipt_title),
                    message = context.getString(R.string.dialog_finance_remove_receipt_message),
                    action = context.getString(R.string.action_remove),
                ) {
                    operationManager.cancel(receipt)
                    navigator.goBack()
                }

                true
            }
        }
    }
}
