package ru.sibwaf.inuyama.pairing

import com.github.benmanes.caffeine.cache.Caffeine
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Duration
import kotlin.streams.asSequence

class DeviceManager {

    private val cache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(10))
        .build<Unit, Collection<String>>()

    fun listDevices(): Collection<String> {
        // todo: real implementation
        return cache.get(Unit) {
            Paths.get("backups")
                .takeIf { Files.isDirectory(it) }
                ?.let { Files.list(it).asSequence() }
                .orEmpty()
                .map { it.fileName.toString() }
                .filter { it != "cache" }
                .toList()
        }
    }
}
