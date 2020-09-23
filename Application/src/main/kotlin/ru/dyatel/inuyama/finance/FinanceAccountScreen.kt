package ru.dyatel.inuyama.finance

import android.content.Context
import android.widget.Button
import com.wealthfront.magellan.BaseScreenView
import io.objectbox.Box
import org.jetbrains.anko.appcompat.v7.tintedButton
import org.jetbrains.anko.hintResource
import org.jetbrains.anko.margin
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.support.v4.nestedScrollView
import org.jetbrains.anko.verticalLayout
import org.jetbrains.anko.wrapContent
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.layout.DIM_EXTRA_LARGE
import ru.dyatel.inuyama.layout.components.UniformDoubleInput
import ru.dyatel.inuyama.layout.components.UniformTextInput
import ru.dyatel.inuyama.layout.components.uniformDoubleInput
import ru.dyatel.inuyama.layout.components.uniformTextInput
import ru.dyatel.inuyama.model.FinanceAccount
import ru.dyatel.inuyama.model.FinanceCategory
import ru.dyatel.inuyama.screens.InuScreen
import ru.dyatel.inuyama.utilities.capitalizeSentences
import ru.dyatel.inuyama.utilities.isVisible

class FinanceAccountView(context: Context) : BaseScreenView<FinanceAccountScreen>(context) {

    lateinit var financeOperationEditor: FinanceOperationEditor
        private set

    lateinit var nameEditor: UniformTextInput
        private set
    lateinit var initialBalanceEditor: UniformDoubleInput
        private set
    lateinit var saveButton: Button
        private set

    init {
        nestedScrollView {
            verticalLayout {
                lparams(width = matchParent, height = wrapContent) {
                    margin = DIM_EXTRA_LARGE
                }

                financeOperationEditor = financeOperationEditor {
                    lparams(width = matchParent, height = wrapContent) {
                        bottomMargin = DIM_EXTRA_LARGE
                    }
                }

                nameEditor = uniformTextInput {
                    hintResource = R.string.hint_name
                    capitalizeSentences()
                }

                initialBalanceEditor = uniformDoubleInput {
                    hintResource = R.string.hint_finance_account_balance
                }

                saveButton = tintedButton(R.string.action_save)
            }
        }
    }
}

class FinanceAccountScreen(private val account: FinanceAccount) : InuScreen<FinanceAccountView>() {

    private val existingAccount = account.id != 0L

    private val accountBox by instance<Box<FinanceAccount>>()
    private val categoryBox by instance<Box<FinanceCategory>>()

    private val operationManager by instance<FinanceOperationManager>()

    override fun getTitle(context: Context) = if (existingAccount) account.name else context.getString(R.string.screen_finance_new_account)!!

    override fun createView(context: Context) = FinanceAccountView(context).apply {
        with(financeOperationEditor) {
            if (!existingAccount) {
                isVisible = false
                return@with
            }

            currentAccountSelector.bindItems(listOf(account))
            currentAccountSelector.isEnabled = false

            targetAccountSelector.bindItems(accountBox.all.filter { it.id != account.id })

            val categories = categoryBox.all
            expenseCategorySelector.bindItems(categories)
            incomeCategorySelector.bindItems(categories)

            allowTransfers = accountBox.count(2) > 1

            saveButton.setOnClickListener {
                when (selectedTab) {
                    FinanceOperationEditor.TAB_EXPENSE -> {
                        val amount = amountEditor.value
                        val category = expenseCategorySelector.selected!!
                        operationManager.createExpense(account, category, amount)
                    }
                    FinanceOperationEditor.TAB_TRANSFER -> {
                        val other = targetAccountSelector.selected!!
                        val amount = amountEditor.value
                        operationManager.createTransfer(account, other, amount)
                    }
                    FinanceOperationEditor.TAB_INCOME -> {
                        val amount = amountEditor.value
                        val category = incomeCategorySelector.selected!!
                        operationManager.createIncome(account, category, amount)
                    }
                    else -> throw IllegalArgumentException("Unknown tab is selected")
                }

                navigator.goBack()
            }
        }

        nameEditor.text = account.name
        initialBalanceEditor.value = account.initialBalance
        saveButton.setOnClickListener {
            account.name = nameEditor.text
            account.initialBalance = initialBalanceEditor.value
            accountBox.put(account)

            if (!existingAccount) {
                navigator.goBack()
            }
        }
    }
}