package ru.dyatel.inuyama.finance

import hirondelle.date4j.DateTime
import io.objectbox.Box
import io.objectbox.BoxStore
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.model.FinanceAccount
import ru.dyatel.inuyama.model.FinanceCategory
import ru.dyatel.inuyama.model.FinanceOperation
import ru.dyatel.inuyama.model.FinanceTransfer
import java.util.TimeZone

class FinanceOperationManager(override val kodein: Kodein) : KodeinAware {

    private val boxStore by instance<BoxStore>()

    private val accountBox by instance<Box<FinanceAccount>>()
    private val operationBox by instance<Box<FinanceOperation>>()
    private val transferBox by instance<Box<FinanceTransfer>>()

    fun createExpense(account: FinanceAccount, category: FinanceCategory, amount: Double) {
        account.balance -= amount

        val operation = FinanceOperation(amount = -amount, datetime = DateTime.now(TimeZone.getDefault()))
        operation.account.target = account
        operation.categories.add(category)

        boxStore.runInTx {
            accountBox.put(account)
            operationBox.put(operation)
        }
    }

    fun createTransfer(from: FinanceAccount, to: FinanceAccount, amount: Double) {
        from.balance -= amount
        to.balance += amount

        val transfer = FinanceTransfer(amount = amount)
        transfer.from.target = from
        transfer.to.target = to

        boxStore.runInTx {
            accountBox.put(from, to)
            transferBox.put(transfer)
        }
    }

    fun createIncome(account: FinanceAccount, category: FinanceCategory, amount: Double) {
        account.balance += amount

        val operation = FinanceOperation(amount = amount, datetime = DateTime.now(TimeZone.getDefault()))
        operation.account.target = account
        operation.categories.add(category)

        boxStore.runInTx {
            accountBox.put(account)
            operationBox.put(operation)
        }
    }
}