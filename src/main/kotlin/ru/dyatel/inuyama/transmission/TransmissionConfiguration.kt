package ru.dyatel.inuyama.transmission

data class TransmissionConfiguration(
        val host: String = "localhost",
        val port: Int = 9091,
        val path: String = "/transmission/rpc",
        val username: String = "",
        val password: String = ""
)