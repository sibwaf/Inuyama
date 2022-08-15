package ru.dyatel.inuyama.finance

import hirondelle.date4j.DateTime
import io.objectbox.Box
import io.objectbox.BoxStore
import io.objectbox.kotlin.query
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.finance.dto.FinanceOperationInfo
import ru.dyatel.inuyama.finance.dto.FinanceTransferDto
import ru.dyatel.inuyama.finance.dto.TransactionHistoryCursor
import ru.dyatel.inuyama.model.FinanceAccount
import ru.dyatel.inuyama.model.FinanceCategory
import ru.dyatel.inuyama.model.FinanceOperation
import ru.dyatel.inuyama.model.FinanceReceipt
import ru.dyatel.inuyama.model.FinanceReceipt_
import ru.dyatel.inuyama.model.FinanceTransaction
import ru.dyatel.inuyama.model.FinanceTransfer
import ru.dyatel.inuyama.model.FinanceTransfer_
import ru.dyatel.inuyama.utilities.lessOrEqual

class FinanceOperationManager(override val kodein: Kodein) : KodeinAware {

    private val boxStore by instance<BoxStore>()

    private val accountBox by instance<Box<FinanceAccount>>()
    private val receiptBox by instance<Box<FinanceReceipt>>()
    private val operationBox by instance<Box<FinanceOperation>>()
    private val transferBox by instance<Box<FinanceTransfer>>()

//    fun getCurrentBalance(account: FinanceAccount): Double {
//        return boxStore.callInReadTx {
//            val operationQuery = operationBox.query {
//                link(FinanceOperation_.account).equal(FinanceAccount_.id, account.id)
//            }.property(FinanceOperation_.amount)
//
//            val transferFromQuery = transferBox.query {
//                link(FinanceTransfer_.from).equal(FinanceAccount_.id, account.id)
//            }.property(FinanceTransfer_.amount)
//
//            val transferToQuery = transferBox.query {
//                link(FinanceTransfer_.to).equal(FinanceAccount_.id, account.id)
//            }.property(FinanceTransfer_.amount)
//
//            0.0 +
//                    +account.initialBalance +
//                    +operationQuery.sumDouble() +
//                    -transferFromQuery.sumDouble() +
//                    +transferToQuery.sumDouble()
//        }
//    }

    fun getTransactions(count: Int, cursor: TransactionHistoryCursor? = null): Pair<List<FinanceTransaction>, TransactionHistoryCursor> {
        if (cursor?.finished == true) {
            return emptyList<FinanceTransaction>() to cursor
        }

        val receipts = receiptBox
            .query {
                cursor?.lastReceiptDatetime?.let { lessOrEqual(FinanceReceipt_.datetime, it) }
                cursor?.lastReceiptId?.let { less(FinanceReceipt_.id, it) }

                orderDesc(FinanceReceipt_.datetime)
                orderDesc(FinanceReceipt_.id)
            }
            .find(0L, count.toLong() + 1)

        val transfers = transferBox
            .query {
                cursor?.lastTransferDatetime?.let { lessOrEqual(FinanceTransfer_.datetime, it) }
                cursor?.lastTransferId?.let { less(FinanceTransfer_.id, it) }

                orderDesc(FinanceTransfer_.datetime)
                orderDesc(FinanceTransfer_.id)
            }
            .find(0L, count.toLong() + 1)

        val sortedTransactions = (receipts + transfers)
            .sortedByDescending { it.datetime }
            .take(count)

        val lastReceipt = sortedTransactions.lastOrNull { it is FinanceReceipt } as FinanceReceipt?
        val lastTransfer = sortedTransactions.lastOrNull { it is FinanceTransfer } as FinanceTransfer?

        return sortedTransactions to TransactionHistoryCursor(
            lastReceiptId = lastReceipt?.id ?: cursor?.lastReceiptId,
            lastReceiptDatetime = lastReceipt?.datetime ?: cursor?.lastReceiptDatetime,
            lastTransferId = lastTransfer?.id ?: cursor?.lastTransferId,
            lastTransferDatetime = lastTransfer?.datetime ?: cursor?.lastTransferDatetime,

            finished = receipts.size + transfers.size <= count,
        )
    }

    fun getCurrentBalance(account: FinanceAccount): Double {
        return account.initialBalance + account.balance
    }

    fun getAmount(receipt: FinanceReceipt): Double {
        return receipt.operations.sumOf { it.amount }
    }

    fun createReceipt(
        account: FinanceAccount,
        operations: List<FinanceOperationInfo>,
        datetime: DateTime
    ) {
        val receipt = FinanceReceipt()
        receipt.account.target = account

        for (operationInfo in operations) {
            receipt.operations.add(operationInfo.asFinanceOperation(account, receipt.datetime))
        }

        account.balance += getAmount(receipt)

        receipt.datetime = datetime

        boxStore.runInTx {
            accountBox.put(account)
            receiptBox.put(receipt)
        }
    }

    fun createTransfer(data: FinanceTransferDto) {
        data.fromAccount.balance -= data.amountFrom
        data.toAccount.balance += data.amountTo

        val transfer = FinanceTransfer(amount = data.amountFrom, amountTo = data.amountTo, datetime = data.datetime)
        transfer.from.target = data.fromAccount
        transfer.to.target = data.toAccount

        boxStore.runInTx {
            accountBox.put(data.fromAccount, data.toAccount)
            transferBox.put(transfer)
        }
    }

    fun cancel(receipt: FinanceReceipt) {
        boxStore.runInTx {
            val account = receipt.account.target
            account.balance -= getAmount(receipt)
            accountBox.put(account)

            operationBox.remove(receipt.operations)
            receiptBox.remove(receipt)
        }
    }

    fun cancel(transfer: FinanceTransfer) {
        boxStore.runInTx {
            val fromAccount = transfer.from.target
            fromAccount.balance += transfer.amount
            accountBox.put(fromAccount)

            val toAccount = transfer.to.target
            toAccount.balance -= transfer.amountTo
            accountBox.put(toAccount)

            transferBox.remove(transfer)
        }
    }

    fun cancel(operation: FinanceOperation) {
        boxStore.runInTx {
            val account = operation.receipt.target.account.target
            account.balance -= operation.amount
            accountBox.put(account)

            operationBox.remove(operation)
        }
    }

    fun update(
        receipt: FinanceReceipt,
        account: FinanceAccount,
        operations: List<FinanceOperationInfo>,
        datetime: DateTime,
    ) {
        boxStore.runInTx {
            receipt.datetime = datetime

            val oldAccount = accountBox[receipt.account.targetId]
            oldAccount.balance -= getAmount(receipt)
            accountBox.put(oldAccount)

            val oldOperations = receipt.operations.toList()
            receipt.operations.clear()

            for (operationInfo in operations) {
                receipt.operations.add(operationInfo.asFinanceOperation(account, receipt.datetime))
            }

            receipt.account.targetId = account.id

            val newAccount = accountBox[account.id]
            newAccount.balance += getAmount(receipt)
            accountBox.put(newAccount)

            receipt.datetime = datetime

            receiptBox.put(receipt)
            operationBox.remove(oldOperations)
        }
    }

    fun update(transfer: FinanceTransfer, data: FinanceTransferDto) {
        boxStore.runInTx {
            val oldFrom = accountBox[transfer.from.targetId]
            oldFrom.balance += transfer.amount
            accountBox.put(oldFrom)

            val oldTo = accountBox[transfer.to.targetId]
            oldTo.balance -= transfer.amountTo
            accountBox.put(oldTo)

            val newFrom = accountBox[data.fromAccount.id]
            newFrom.balance -= data.amountFrom
            accountBox.put(newFrom)

            val newTo = accountBox[data.toAccount.id]
            newTo.balance += data.amountTo
            accountBox.put(newTo)

            transfer.from.target = newFrom
            transfer.to.target = newTo
            transfer.amount = data.amountFrom
            transfer.amountTo = data.amountTo
            transfer.datetime = data.datetime
            transferBox.put(transfer)
        }
    }

    fun update(
        operation: FinanceOperation,
        category: FinanceCategory,
        amount: Double,
        description: String?
    ) {
        boxStore.runInTx {
            val account = operation.receipt.target.account.target
            account.balance -= operation.amount
            account.balance += amount
            accountBox.put(account)

            operation.categories.clear()
            operation.categories.add(category)
            operation.amount = amount
            operation.description = description
            operationBox.put(operation)
        }
    }

    private fun FinanceOperationInfo.asFinanceOperation(account: FinanceAccount, datetime: DateTime): FinanceOperation {
        val operation = FinanceOperation(
            amount = amount,
            datetime = datetime,
            description = description
        )

        @Suppress("DEPRECATION")
        operation.account.target = account
        operation.categories.add(category)

        return operation
    }
}
