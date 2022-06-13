package ru.dyatel.inuyama.pairing

import java.security.PublicKey

data class SavedPairedServer(
    val key: PublicKey,
    val address: HostAndPort?,
) {
    data class HostAndPort(
        val host: String,
        val port: Int,
    )
}
