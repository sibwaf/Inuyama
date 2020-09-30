package ru.dyatel.inuyama.finance

import android.content.Context
import com.wealthfront.magellan.BaseScreenView
import io.objectbox.Box
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.model.FinanceAccount
import ru.dyatel.inuyama.model.FinanceCategory
import ru.dyatel.inuyama.model.FinanceOperation
import ru.dyatel.inuyama.screens.InuScreen
import kotlin.math.abs

class FinanceOperationView(context: Context) : BaseScreenView<FinanceOperationScreen>(context) {
    val financeOperationEditor = financeOperationEditor()
}

class FinanceOperationScreen(private val operation: FinanceOperation) : InuScreen<FinanceOperationView>() {

    private val accountBox by instance<Box<FinanceAccount>>()
    private val categoryBox by instance<Box<FinanceCategory>>()

    private val operationManager by instance<FinanceOperationManager>()

    override fun createView(context: Context): FinanceOperationView {
        return FinanceOperationView(context).apply {
            with(financeOperationEditor) {
                allowTransfers = false

                currentAccountSelector.bindItems(accountBox.all)
                currentAccountSelector.selected = operation.account.target

                val categories = categoryBox.all
                expenseCategorySelector.bindItems(categories)
                expenseCategorySelector.selected = operation.categories.firstOrNull()
                incomeCategorySelector.bindItems(categories)
                incomeCategorySelector.selected = operation.categories.firstOrNull()

                selectedTab = if (operation.amount > 0) {
                    FinanceOperationEditor.TAB_INCOME
                } else {
                    FinanceOperationEditor.TAB_EXPENSE
                }

                amountEditor.value = abs(operation.amount)
                descriptionEditor.text = operation.description ?: ""

                saveButton.setOnClickListener {
                    var amount = amountEditor.value
                    // TODO: check > 0

                    when (selectedTab) {
                        FinanceOperationEditor.TAB_INCOME -> {
                        }
                        FinanceOperationEditor.TAB_EXPENSE -> amount = -amount
                        else -> throw IllegalArgumentException("Unknown tab is selected")
                    }

                    operationManager.update(
                            operation,
                            account = currentAccountSelector.selected!!,
                            category = expenseCategorySelector.selected!!,
                            amount = amount,
                            description = descriptionEditor.text
                    )

                    navigator.goBack()
                }
            }
        }
    }
}