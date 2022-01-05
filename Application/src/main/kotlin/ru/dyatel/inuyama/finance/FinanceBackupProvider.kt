package ru.dyatel.inuyama.finance

import com.google.gson.Gson
import io.objectbox.Box
import ru.dyatel.inuyama.model.FinanceAccount
import ru.dyatel.inuyama.model.FinanceCategory
import ru.dyatel.inuyama.model.FinanceReceipt
import ru.dyatel.inuyama.model.FinanceTransfer
import sibwaf.inuyama.app.common.backup.BackupProvider

class FinanceBackupProvider(
    private val accounts: Box<FinanceAccount>,
    private val categories: Box<FinanceCategory>,
    private val receipts: Box<FinanceReceipt>,
    private val transfers: Box<FinanceTransfer>,

    private val gson: Gson
) : BackupProvider("sibwaf.finance") {

    private companion object {
        const val DATETIME_FORMAT = "YYYY-MM-DDThh:mm:ss"
    }

    override fun provideData(): String {
        val data = BackupData(
            accounts = accounts.all.map {
                BackupFinanceAccount(
                    id = it.id.toString(),
                    name = it.name,
                    initialBalance = it.initialBalance,
                    balance = it.balance
                )
            },
            categories = categories.all.map {
                BackupFinanceCategory(
                    id = it.id.toString(),
                    name = it.name
                )
            },
            receipts = receipts.all.map { receipt ->
                BackupFinanceReceipt(
                    id = receipt.id.toString(),
                    accountId = receipt.account.targetId.toString(),
                    datetime = receipt.datetime.format(DATETIME_FORMAT),
                    operations = receipt.operations.map { operation ->
                        BackupFinanceOperation(
                            id = operation.id.toString(),
                            amount = operation.amount,
                            description = operation.description,
                            categoryIds = operation.categories.map { it.id.toString() }
                        )
                    }
                )
            },
            transfers = transfers.all.map {
                BackupFinanceTransfer(
                    id = it.id.toString(),
                    amount = it.amount,
                    datetime = it.datetime.format(DATETIME_FORMAT),
                    fromId = it.from.targetId.toString(),
                    toId = it.to.targetId.toString()
                )
            }
        )

        return gson.toJson(data)
    }
}

private data class BackupData(
    val accounts: Collection<BackupFinanceAccount>,
    val categories: Collection<BackupFinanceCategory>,
    val receipts: Collection<BackupFinanceReceipt>,
    val transfers: Collection<BackupFinanceTransfer>
)

private data class BackupFinanceAccount(
    val id: String,
    val name: String,
    val initialBalance: Double,
    val balance: Double
)

private data class BackupFinanceCategory(
    val id: String,
    val name: String
)

private data class BackupFinanceReceipt(
    val id: String,
    val accountId: String,
    val datetime: String,
    val operations: Collection<BackupFinanceOperation>
)

private data class BackupFinanceOperation(
    val id: String,
    val amount: Double,
    val description: String?,
    val categoryIds: List<String>
)

private data class BackupFinanceTransfer(
    val id: String,
    val amount: Double,
    val datetime: String,
    val fromId: String,
    val toId: String
)
