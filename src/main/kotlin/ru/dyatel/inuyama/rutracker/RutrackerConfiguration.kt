package ru.dyatel.inuyama.rutracker

import ru.dyatel.inuyama.model.Proxy

data class RutrackerConfiguration(
        var host: String = "https://rutracker.org",
        var proxy: Proxy? = null
)