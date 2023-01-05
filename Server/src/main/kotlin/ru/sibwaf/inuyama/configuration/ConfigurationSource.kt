package ru.sibwaf.inuyama.configuration

interface ConfigurationSource {
    fun getValue(name: String): String?
}
