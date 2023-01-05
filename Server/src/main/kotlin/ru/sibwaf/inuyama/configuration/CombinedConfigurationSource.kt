package ru.sibwaf.inuyama.configuration

class CombinedConfigurationSource(vararg sources: ConfigurationSource) : ConfigurationSource {

    private val reversedSources = sources.reversed()

    override fun getValue(name: String): String? {
        for (source in reversedSources) {
            val value = source.getValue(name)
            if (value != null) {
                return value
            }
        }

        return null
    }
}
