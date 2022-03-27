package ru.sibwaf.inuyama.backup

import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.time.Duration
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.concurrent.thread
import kotlin.io.path.isDirectory
import kotlin.streams.asSequence

class BackupManager {

    private val log = LoggerFactory.getLogger(javaClass)

    // todo: config
    private val baseDirectory = Paths.get("backups")

    private val delayBetweenBackups = Duration.ofDays(1)
    private val backupRetention = Duration.ofDays(30)
    private val cleanupPeriod = Duration.ofHours(2)

    init {
        thread(name = "Backup cleaner", isDaemon = true) {
            while (true) {
                cleanup()
                Thread.sleep(cleanupPeriod.toMillis())
            }
        }
    }

    private fun cleanup() {
        val now = OffsetDateTime.now()
        for ((_, backups) in listAllBackups().groupBy { it.deviceId to it.module }) {
            val previousBackups = backups
                .sortedBy { it.dateTime }
                .dropLast(1)

            val outdatedBackups = previousBackups.filter { it.dateTime + backupRetention < now }.toSet()
            for (backup in outdatedBackups) {
                val path = backup.toPath(baseDirectory)
                try {
                    Files.delete(path)
                    log.info("Cleaned up an outdated backup ${baseDirectory.relativize(path)}")
                } catch (e: Exception) {
                    log.error("Failed to clean up an outdated backup ${baseDirectory.relativize(path)}", e)
                }
            }

            val remainingBackups = previousBackups - outdatedBackups
            var currentBackup = remainingBackups.firstOrNull() ?: continue
            for (backup in remainingBackups.asSequence().drop(1)) {
                if (currentBackup.dateTime + delayBetweenBackups > backup.dateTime) {
                    val path = backup.toPath(baseDirectory)
                    try {
                        Files.delete(path)
                        log.info("Cleaned up a surplus backup ${baseDirectory.relativize(path)}")
                    } catch (e: Exception) {
                        log.error("Failed to clean up a surplus backup ${baseDirectory.relativize(path)}", e)
                    }
                } else {
                    currentBackup = backup
                }
            }
        }
    }

    // todo: deduplicate by hash
    fun makeBackup(deviceId: String, module: String, data: InputStream) {
        val token = BackupToken(
            deviceId = deviceId,
            module = module,
            dateTime = OffsetDateTime.now()
        )

        val path = token.toPath(baseDirectory)
        Files.createDirectories(path.parent)

        log.debug("Writing a backup: $deviceId / $module")

        Files.newOutputStream(path, StandardOpenOption.CREATE_NEW).use {
            ZipOutputStream(it).use { zip ->
                zip.putNextEntry(ZipEntry("data.bin"))
                data.transferTo(zip)
                zip.closeEntry()
            }
        }

        log.info("Successfully written a backup: $deviceId / $module")
    }

    fun <T> useLatestBackup(deviceId: String, module: String, block: (BufferedReader) -> T): T? {
        val path = listAllBackups()
            .filter { it.deviceId == deviceId && it.module == module }
            .maxByOrNull { it.dateTime }
            ?.toPath(baseDirectory)
            ?: return null

        return Files.newInputStream(path).use { input ->
            ZipInputStream(input).use { zip ->
                zip.nextEntry ?: return@useLatestBackup null
                zip.bufferedReader(charset = Charsets.UTF_8).use(block)
            }
        }
    }

    private fun listAllBackups(): Sequence<BackupToken> {
        return baseDirectory
            .takeIf { it.isDirectory() }
            ?.let { Files.walk(it) }
            ?.asSequence()
            .orEmpty()
            .mapNotNull { BackupToken.fromPath(it) }
    }
}

private data class BackupToken(
    val deviceId: String,
    val module: String,
    val dateTime: OffsetDateTime
) {

    companion object {
        private val dateTimeFormat = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")

        fun fromPath(path: Path): BackupToken? {
            val deviceId = path.toAbsolutePath().parent?.fileName?.toString() ?: return null
            val filename = path.fileName.toString().removeSuffix(".zip")

            val parts = filename.split("_")
            if (parts.size != 3) {
                return null
            }

            val dateTime = try {
                LocalDateTime.parse("${parts[1]}_${parts[2]}", dateTimeFormat)
            } catch (e: DateTimeParseException) {
                return null
            }

            return BackupToken(deviceId, parts[0], dateTime.atOffset(ZoneOffset.UTC))
        }
    }

    fun toPath(baseDirectory: Path): Path =
        baseDirectory
            .resolve(deviceId)
            .resolve("${module}_${dateTime.withOffsetSameInstant(ZoneOffset.UTC).format(dateTimeFormat)}.zip")
}
