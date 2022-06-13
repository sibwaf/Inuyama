package ru.dyatel.inuyama.pairing

import java.security.PublicKey

data class PairedServer(
    val host: String,
    val port: Int,
    val key: PublicKey,
    val wasDiscovered: Boolean,
) {
    val url get() = "http://$host:$port/paired"
}
