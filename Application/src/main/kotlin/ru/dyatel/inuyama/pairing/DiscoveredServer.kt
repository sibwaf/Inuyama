package ru.dyatel.inuyama.pairing

import java.net.InetAddress
import java.security.PublicKey

data class DiscoveredServer(val address: InetAddress, val port: Int, val key: PublicKey) {
    val url get() = "http://${address.hostAddress}:$port/paired"
}
