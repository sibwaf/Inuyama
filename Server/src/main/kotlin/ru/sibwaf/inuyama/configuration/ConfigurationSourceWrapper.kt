package ru.sibwaf.inuyama.configuration

class ConfigurationSourceWrapper(private val source: ConfigurationSource) {

    fun getString(name: String): String? = source.getValue(name)

    fun requireString(name: String) = getString(name) ?: throw ConfigurationException("Missing string property $name")

    fun getInt(name: String): Int? = source.getValue(name)?.toInt()

    fun requireInt(name: String) = getInt(name) ?: throw ConfigurationException("Missing integer property $name")

    inline fun <reified T : Enum<T>> getEnum(name: String): T? {
        val value = getString(name) ?: return null
        return enumValues<T>()
            .firstOrNull { it.name.equals(value, ignoreCase = true) }
            ?: throw ConfigurationException("Invalid value for property $name, expected one of ${enumValues<T>().asList()}")
    }
}

fun ConfigurationSource.wrapped(): ConfigurationSourceWrapper = ConfigurationSourceWrapper(this)
