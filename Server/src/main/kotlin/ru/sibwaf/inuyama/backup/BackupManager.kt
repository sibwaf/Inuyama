package ru.sibwaf.inuyama.backup

import org.slf4j.LoggerFactory
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.concurrent.thread
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
                val now = OffsetDateTime.now()

                for (devicePath in Files.list(baseDirectory)) {
                    val modules = Files.list(devicePath).asSequence()
                        .mapNotNull { BackupToken.fromPath(it) }
                        .groupBy { it.module }

                    for ((_, backups) in modules) {
                        val outdatedBackups = backups
                            .sortedBy { it.dateTime }
                            .dropLast(1)
                            .filter { it.dateTime + backupRetention < now }

                        for (backup in outdatedBackups) {
                            val path = backup.toPath(baseDirectory)
                            log.info("Cleaning up an outdated backup ${baseDirectory.relativize(path)}")
                            try {
                                Files.delete(path)
                            } catch (e: Exception) {
                                log.error("Failed to clean up and outdated backup ${baseDirectory.relativize(path)}", e)
                            }
                        }
                    }
                }

                Thread.sleep(cleanupPeriod.toMillis())
            }
        }
    }

    fun prepareBackup(deviceId: String, module: String): Boolean {
        val path = getDirectory(deviceId)
        if (!Files.isDirectory(path)) {
            return true
        }

        val latestVersion = Files.list(path).asSequence()
            .mapNotNull { BackupToken.fromPath(it) }
            .filter { it.deviceId == deviceId && it.module == module }
            .sortedBy { it.dateTime }
            .lastOrNull()
            ?.dateTime

        return latestVersion == null ||
                latestVersion + delayBetweenBackups < OffsetDateTime.now()
    }

    // todo: deduplicate by hash
    fun makeBackup(deviceId: String, module: String, data: InputStream) {
        if (!prepareBackup(deviceId, module)) {
            throw BackupNotReadyException()
        }

        val token = BackupToken(
            deviceId = deviceId,
            module = module,
            dateTime = OffsetDateTime.now()
        )

        val path = token.toPath(baseDirectory)
        Files.createDirectories(path.parent)

        log.debug("Writing a backup: $deviceId / $module")

        Files.newOutputStream(path).use {
            ZipOutputStream(it).use { zip ->
                zip.putNextEntry(ZipEntry("data.bin"))
                data.transferTo(zip)
                zip.closeEntry()
            }
        }

        log.info("Successfully written a backup: $deviceId / $module")
    }

    private fun getDirectory(deviceId: String) = baseDirectory.resolve(deviceId)
}

private data class BackupToken(
    val deviceId: String,
    val module: String,
    val dateTime: OffsetDateTime
) {

    companion object {
        private val dateTimeFormat = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")

        fun fromPath(path: Path): BackupToken? {
            val deviceId = path.parent.fileName.toString()
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
