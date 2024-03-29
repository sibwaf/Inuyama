package ru.dyatel.inuyama.finance

import com.google.gson.Gson
import hirondelle.date4j.DateTime
import io.objectbox.Box
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.apache.commons.io.input.ReaderInputStream
import ru.dyatel.inuyama.model.FinanceAccount
import ru.dyatel.inuyama.model.FinanceCategory
import ru.dyatel.inuyama.model.FinanceOperation
import ru.dyatel.inuyama.model.FinanceReceipt
import ru.dyatel.inuyama.model.FinanceTransfer
import ru.sibwaf.inuyama.common.utilities.Encoding
import ru.sibwaf.inuyama.common.utilities.gson.fromJson
import ru.sibwaf.inuyama.common.utilities.gson.toJsonReader
import sibwaf.inuyama.app.common.backup.ModuleBackupHandler
import java.io.InputStream
import java.util.UUID

class FinanceBackupHandler(
    private val accountRepository: Box<FinanceAccount>,
    private val categoryRepository: Box<FinanceCategory>,
    private val operationRepository: Box<FinanceOperation>,
    private val receiptRepository: Box<FinanceReceipt>,
    private val transferRepository: Box<FinanceTransfer>,

    private val gson: Gson
) : ModuleBackupHandler("sibwaf.finance") {

    private companion object {
        const val DATETIME_FORMAT = "YYYY-MM-DDThh:mm:ss"
    }

    override fun provideData(): InputStream {
        val data = BackupData(
            accounts = accountRepository.all.map {
                BackupFinanceAccount(
                    id = it.id.toString(),
                    name = it.name,
                    initialBalance = it.initialBalance,
                    balance = it.balance,
                    currency = it.currency,
                    quickAccess = it.quickAccess,
                    disabled = it.disabled,
                )
            },
            categories = categoryRepository.all.map {
                BackupFinanceCategory(
                    id = it.id.toString(),
                    name = it.name
                )
            },
            receipts = receiptRepository.all.map { receipt ->
                BackupFinanceReceipt(
                    id = receipt.id.toString(),
                    accountId = receipt.account.targetId.toString(),
                    datetime = receipt.datetime.format(DATETIME_FORMAT),
                    operations = receipt.operations.map { operation ->
                        BackupFinanceOperation(
                            id = operation.id.toString(),
                            amount = operation.amount,
                            description = operation.description,
                            guid = operation.guid,
                            categoryIds = operation.categories.map { it.id.toString() }
                        )
                    }
                )
            },
            transfers = transferRepository.all.map {
                BackupFinanceTransfer(
                    id = it.id.toString(),
                    amountFrom = it.amount,
                    amountTo = it.amountTo,
                    datetime = it.datetime.format(DATETIME_FORMAT),
                    fromId = it.from.targetId.toString(),
                    toId = it.to.targetId.toString()
                )
            }
        )

        return ReaderInputStream.builder()
            .setCharset(Encoding.CHARSET)
            .setReader(gson.toJsonReader(data, CoroutineScope(Dispatchers.Default)))
            .get()
    }

    override fun restoreData(data: InputStream) {
        // todo: support older backup formats
        val backup = data.reader().use { gson.fromJson<BackupData>(it) }

        accountRepository.store.runInTx {
            accountRepository.removeAll()
            val accountIdMapping = mutableMapOf<String, Long>() // i fucking hate objectbox, there is no way to reset id counter
            for (account in backup.accounts) {
                accountIdMapping[account.id] = accountRepository.put(
                    FinanceAccount(
                        name = account.name,
                        initialBalance = account.initialBalance,
                        balance = account.balance,
                        currency = account.currency ?: "",
                    ).also { savedAccount ->
                        account.quickAccess?.let { savedAccount.quickAccess = it }
                        account.disabled?.let { savedAccount.disabled = it }
                    }
                )
            }

            categoryRepository.removeAll()
            val categoryIdMapping = mutableMapOf<String, Long>()
            for (category in backup.categories) {
                categoryIdMapping[category.id] = categoryRepository.put(
                    FinanceCategory(
                        name = category.name
                    )
                )
            }

            operationRepository.removeAll()
            receiptRepository.removeAll()
            for (receipt in backup.receipts) {
                val datetime = DateTime(receipt.datetime)

                val operations = receipt.operations.map { operation ->
                    FinanceOperation(
                        amount = operation.amount,
                        datetime = datetime,
                        guid = operation.guid ?: UUID.randomUUID(),
                        description = operation.description
                    ).also {
                        operationRepository.attach(it)
                        it.categories.addAll(
                            categoryRepository.get(operation.categoryIds.map { categoryId -> categoryIdMapping.getValue(categoryId) })
                        )
                    }
                }

                receiptRepository.put(
                    FinanceReceipt(
                        datetime = datetime
                    ).also {
                        receiptRepository.attach(it)
                        it.account.targetId = accountIdMapping.getValue(receipt.accountId)
                        it.operations.addAll(operations)
                    }
                )
            }

            transferRepository.removeAll()
            for (transfer in backup.transfers) {
                transferRepository.put(
                    FinanceTransfer(
                        amount = (transfer.amountFrom ?: transfer.amount)!!,
                        amountTo = (transfer.amountTo ?: transfer.amount)!!,
                        datetime = DateTime(transfer.datetime)
                    ).also {
                        transferRepository.attach(it)
                        it.from.targetId = accountIdMapping.getValue(transfer.fromId)
                        it.to.targetId = accountIdMapping.getValue(transfer.toId)
                    }
                )
            }
        }
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
    val balance: Double,
    val currency: String?,
    val quickAccess: Boolean?,
    val disabled: Boolean?,
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
    val guid: UUID?,
    val categoryIds: List<String>
)

private data class BackupFinanceTransfer(
    val id: String,
    val amount: Double? = null,
    val amountFrom: Double?,
    val amountTo: Double?,
    val datetime: String,
    val fromId: String,
    val toId: String
)
