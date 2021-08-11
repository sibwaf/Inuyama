package ru.dyatel.inuyama.finance

import com.google.gson.Gson
import io.objectbox.Box
import ru.dyatel.inuyama.model.FinanceAccount
import ru.dyatel.inuyama.model.FinanceCategory
import ru.dyatel.inuyama.model.FinanceOperation
import ru.dyatel.inuyama.model.FinanceTransfer
import sibwaf.inuyama.app.common.backup.BackupProvider

class FinanceBackupProvider(
    private val accounts: Box<FinanceAccount>,
    private val categories: Box<FinanceCategory>,
    private val operations: Box<FinanceOperation>,
    private val transfers: Box<FinanceTransfer>,

    private val gson: Gson
) : BackupProvider("sibwaf.finance") {

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
            operations = operations.all.map {
                BackupFinanceOperation(
                    id = it.id.toString(),
                    amount = it.amount,
                    datetime = it.datetime.format("YYYY-MM-DDThh:mm:ss"),
                    description = it.description,
                    accountId = it.account.targetId.toString(),
                    categoryIds = it.categories.map { it.id.toString() }
                )
            },
            transfers = transfers.all.map {
                BackupFinanceTransfer(
                    id = it.id.toString(),
                    amount = it.amount,
                    datetime = it.datetime.format("YYYY-MM-DDThh:mm:ss"),
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
    val operations: Collection<BackupFinanceOperation>,
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

private data class BackupFinanceOperation(
    val id: String,
    val amount: Double,
    val datetime: String,
    val description: String?,
    val accountId: String,
    val categoryIds: List<String>
)

private data class BackupFinanceTransfer(
    val id: String,
    val amount: Double,
    val datetime: String,
    val fromId: String,
    val toId: String
)
