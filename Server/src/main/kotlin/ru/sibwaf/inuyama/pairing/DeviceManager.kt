package ru.sibwaf.inuyama.pairing

import java.nio.file.Files
import java.nio.file.Paths
import kotlin.streams.asSequence

class DeviceManager {

    fun listDevices(): Collection<String> {
        // todo: real implementation
        return Paths.get("backups")
            .takeIf { Files.isDirectory(it) }
            ?.let { Files.list(it).asSequence() }
            .orEmpty()
            .map { it.fileName.toString() }
            .toList()
    }
}
