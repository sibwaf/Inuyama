package ru.sibwaf.inuyama

import ru.sibwaf.inuyama.common.Pairing

data class InuyamaConfiguration(
        val discoveryPort: Int = Pairing.DEFAULT_DISCOVER_SERVER_PORT,
        val serverPort: Int = Pairing.DEFAULT_DISCOVER_SERVER_PORT + 1
)