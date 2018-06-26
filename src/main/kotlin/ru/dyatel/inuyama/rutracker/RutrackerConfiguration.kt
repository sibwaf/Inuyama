package ru.dyatel.inuyama.rutracker

import ru.dyatel.inuyama.model.Proxy

data class RutrackerConfiguration(
        val host: String = "https://rutracker.org",
        val proxy: Proxy? = null
)