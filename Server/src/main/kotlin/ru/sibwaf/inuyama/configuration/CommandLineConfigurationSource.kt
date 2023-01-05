package ru.sibwaf.inuyama.configuration

class CommandLineConfigurationSource(args: List<String>) : ConfigurationSource {

    private val parsedArgs = args
        .mapNotNull {
            if (it.startsWith("--") && "=" in it) {
                val name = it.substringBefore("=").removePrefix("--").normalizeAsName()
                val value = it.substringAfter("=").trim()
                name to value
            } else {
                null
            }
        }
        .toMap()

    override fun getValue(name: String): String? = parsedArgs[name.normalizeAsName()]

    private fun String.normalizeAsName(): String = trim().lowercase()
}
