package ru.sibwaf.inuyama.configuration

class EnvironmentConfigurationSource : ConfigurationSource {

    private val environment = System.getenv()

    private val nonAlnumRegex = Regex("[^A-Za-z0-9]")

    override fun getValue(name: String): String? {
        val normalizedName = name.trim()
        val snakeCaseName = normalizedName.replace(nonAlnumRegex, "_")

        for ((key, value) in environment) {
            if (key.equals(normalizedName, ignoreCase = true) || key.equals(snakeCaseName, ignoreCase = true)) {
                return value
            }
        }

        return null
    }
}
