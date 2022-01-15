package ru.sibwaf.inuyama.finance

import com.github.benmanes.caffeine.cache.Caffeine
import com.google.gson.Gson
import ru.sibwaf.inuyama.backup.BackupManager
import ru.sibwaf.inuyama.fromJson
import ru.sibwaf.inuyama.systemZoneOffset
import java.nio.file.Files
import java.time.Duration
import java.time.LocalDateTime
import java.util.zip.ZipInputStream

class FinanceBackupDataProvider(
    private val backupManager: BackupManager,
    private val gson: Gson
) {

    private val dataCache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(10))
        .build<String, BackupData>()

    private fun getDeviceData(deviceId: String): BackupData? {
        return dataCache.get(deviceId) { key ->
            val backupPath = backupManager.getLatestBackup(key, "sibwaf.finance") ?: return@get null

            Files.newInputStream(backupPath).use { input ->
                ZipInputStream(input).use { zip ->
                    zip.nextEntry ?: return@get null

                    zip.bufferedReader(charset = Charsets.UTF_8).use {
                        gson.fromJson(it)
                    }
                }
            }
        }
    }

    fun getCategories(deviceId: String): Set<FinanceCategoryDto> {
        return getDeviceData(deviceId)?.categories
            .orEmpty() // todo: crash on no device data?
            .map { FinanceCategoryDto(id = it.id, name = it.name) }
            .toSet()
    }

    fun getOperations(deviceId: String): Sequence<FinanceOperationDto> {
        val data = getDeviceData(deviceId) ?: return emptySequence()

        val rawOperations = data.operations
            .orEmpty()
            .asSequence()
            .mapNotNull {
                val datetime = it.datetime ?: return@mapNotNull null
                FinanceOperationDto(
                    amount = it.amount,
                    categoryId = it.categoryIds.single(),
                    datetime = LocalDateTime.parse(datetime).atOffset(systemZoneOffset)
                )
            }

        val receiptOperations = data.receipts
            .orEmpty()
            .asSequence()
            .flatMap { receipt ->
                receipt.operations.map {
                    FinanceOperationDto(
                        amount = it.amount,
                        categoryId = it.categoryIds.single(),
                        datetime = LocalDateTime.parse(receipt.datetime).atOffset(systemZoneOffset)
                    )
                }
            }

        return rawOperations + receiptOperations
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
    val categoryIds: List<String>,

    val datetime: String?
)

private data class BackupFinanceTransfer(
    val id: String,
    val amount: Double,
    val datetime: String,
    val fromId: String,
    val toId: String
)
