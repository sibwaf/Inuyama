package ru.sibwaf.inuyama.finance

import com.github.benmanes.caffeine.cache.Caffeine
import com.google.gson.Gson
import ru.sibwaf.inuyama.backup.BackupManager
import ru.sibwaf.inuyama.fromJson
import ru.sibwaf.inuyama.systemZoneOffset
import java.time.Duration
import java.time.LocalDateTime
import java.time.OffsetDateTime

class FinanceBackupDataProvider(
    private val backupManager: BackupManager,
    private val gson: Gson
) {

    private val dataCache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(10))
        .build<String, BackupData>()

    private fun getDeviceData(deviceId: String): BackupData? {
        return dataCache.get(deviceId) { key ->
            backupManager.getLatestBackupContent(key, "sibwaf.finance")?.bufferedReader()?.use {
                gson.fromJson(it)
            }
        }
    }

    fun getCategories(deviceId: String): Set<FinanceCategoryDto> {
        return getDeviceData(deviceId)?.categories
            .orEmpty()
            .map { it.toDto() }
            .toSet()
    }

    fun getAccounts(deviceId: String): Set<FinanceAccountDto> {
        return getDeviceData(deviceId)?.accounts
            .orEmpty()
            .map { it.toDto() }
            .toSet()
    }

    fun getOperations(deviceId: String): Sequence<FinanceOperationDto> {
        val accounts = getAccounts(deviceId).associateBy { it.id }
        val categories = getCategories(deviceId).associateBy { it.id }

        return getDeviceData(deviceId)?.receipts
            .orEmpty()
            .asSequence()
            .flatMap { receipt ->
                val account = accounts.getValue(receipt.accountId)
                val datetime = LocalDateTime.parse(receipt.datetime).atOffset(systemZoneOffset)

                receipt.operations.map { operation ->
                    operation.toDto(
                        account = account,
                        categoryProvider = { categories.getValue(it) },
                        datetime = datetime,
                    )
                }
            }
    }

    fun getTransfers(deviceId: String): Sequence<FinanceTransferDto> {
        val accounts = getAccounts(deviceId).associateBy { it.id }

        return getDeviceData(deviceId)?.transfers
            .orEmpty()
            .asSequence()
            .map { transfer ->
                transfer.toDto(
                    accountProvider = { accounts.getValue(it) },
                )
            }
    }
}

private data class BackupData(
    val accounts: Collection<BackupFinanceAccount>,
    val categories: Collection<BackupFinanceCategory>,
    val receipts: Collection<BackupFinanceReceipt>?,
    val operations: Collection<BackupFinanceOperation>?,
    val transfers: Collection<BackupFinanceTransfer>
)

private data class BackupFinanceAccount(
    val id: String,
    val name: String,
    val initialBalance: Double,
    val balance: Double,
    val currency: String,
) {
    fun toDto(): FinanceAccountDto {
        return FinanceAccountDto(
            id = id,
            name = name,
            balance = initialBalance + balance,
            currency = currency,
        )
    }
}

private data class BackupFinanceCategory(
    val id: String,
    val name: String
) {
    fun toDto(): FinanceCategoryDto {
        return FinanceCategoryDto(
            id = id,
            name = name,
        )
    }
}

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
    val categoryIds: List<String>,

    val datetime: String?
) {
    fun toDto(
        account: FinanceAccountDto,
        categoryProvider: (String) -> FinanceCategoryDto,
        datetime: OffsetDateTime,
    ): FinanceOperationDto {
        return FinanceOperationDto(
            account = account,
            amount = amount,
            category = categoryProvider(categoryIds.single()),
            datetime = datetime,
        )
    }
}

private data class BackupFinanceTransfer(
    val id: String,
    val amount: Double?,
    val amountFrom: Double?,
    val amountTo: Double?,
    val datetime: String,
    val fromId: String,
    val toId: String
) {
    fun toDto(
        accountProvider: (String) -> FinanceAccountDto,
    ): FinanceTransferDto {
        return FinanceTransferDto(
            fromAccount = accountProvider(fromId),
            toAccount = accountProvider(toId),
            amountFrom = (amountFrom ?: amount)!!,
            amountTo = (amountTo ?: amount)!!,
            datetime = LocalDateTime.parse(datetime).atOffset(systemZoneOffset),
        )
    }
}
