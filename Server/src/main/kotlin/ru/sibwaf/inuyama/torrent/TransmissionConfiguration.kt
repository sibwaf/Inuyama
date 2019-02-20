package ru.sibwaf.inuyama.torrent

data class TransmissionConfiguration(
        var host: String = "localhost",
        var port: Int = 9091,
        var path: String = "/transmission/rpc",
        var username: String = "",
        var password: String = ""
)