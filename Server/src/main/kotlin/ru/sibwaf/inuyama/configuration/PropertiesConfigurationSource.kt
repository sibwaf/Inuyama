package ru.sibwaf.inuyama.configuration

import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.util.Properties
import kotlin.io.path.bufferedReader

class PropertiesConfigurationSource(path: Path) : ConfigurationSource {

    private val properties = Properties().let { properties ->
        try {
            path.bufferedReader().use {
                properties.load(it)
            }
        } catch (e: NoSuchFileException) {
            // do nothing
        }

        properties.stringPropertyNames().associate {
            it.normalizeAsName() to properties.getProperty(it)
        }
    }

    override fun getValue(name: String): String? = properties[name.normalizeAsName()]

    private fun String.normalizeAsName(): String = trim().lowercase()
}
