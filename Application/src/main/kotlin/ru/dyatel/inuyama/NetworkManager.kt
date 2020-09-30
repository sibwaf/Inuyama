package ru.dyatel.inuyama

import android.content.Context
import android.net.wifi.SupplicantState
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import io.objectbox.Box
import io.objectbox.kotlin.query
import okhttp3.OkHttpClient
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import org.kodein.di.generic.on
import ru.dyatel.inuyama.model.Network
import ru.dyatel.inuyama.model.Network_
import ru.dyatel.inuyama.model.ProxyBinding
import ru.dyatel.inuyama.model.findByBssid
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Proxy
import java.nio.ByteBuffer

class NetworkManager(override val kodein: Kodein) : KodeinAware {

    private val httpClient = OkHttpClient.Builder().build()

    private val context by instance<Context>()
    private val wifiManager by on(context).instance<WifiManager>()

    private val networkBox by instance<Box<Network>>()
    private val proxyBindingBox by instance<Box<ProxyBinding>>()

    private val currentWifiConnection: WifiInfo?
        get() {
            return wifiManager.connectionInfo
                    ?.takeIf { it.bssid != null && it.supplicantState == SupplicantState.COMPLETED }
        }

    val broadcastAddress: InetAddress?
        get() {
            val dhcp = wifiManager.dhcpInfo ?: return null

            val ipAddress = ByteBuffer.allocate(4)
                    .putInt(dhcp.ipAddress)
                    .array()
                    .apply { reverse() }
                    .let { InetAddress.getByAddress(it) }

            return NetworkInterface.getNetworkInterfaces().asSequence()
                    .filter { it.inetAddresses.asSequence().contains(ipAddress) }
                    .flatMap { it.interfaceAddresses.asSequence() }
                    .mapNotNull { it.broadcast }
                    .singleOrNull()
        }

    val isNetworkTrusted: Boolean
        get() {
            val current = currentWifiConnection ?: return false
            return networkBox.findByBssid(current.bssid)?.trusted == true
        }

    fun refreshNetworkList() {
        val current = currentWifiConnection

        networkBox.query {
            equal(Network_.trusted, false)

            if (current != null) {
                and()
                notEqual(Network_.bssid, current.bssid)
            }
        }.remove()

        if (current != null && networkBox.findByBssid(current.bssid) == null) {
            networkBox.put(Network(name = current.ssid, bssid = current.bssid))
        }
    }

    fun getHttpClient(trustedOnly: Boolean, serviceId: Long? = null): OkHttpClient {
        if (trustedOnly && !isNetworkTrusted) {
            throw UntrustedNetworkException()
        }

        val proxy = serviceId?.let { proxyBindingBox[it] }
                ?.proxy
                ?.target

        return if (proxy == null) {
            httpClient
        } else {
            httpClient.newBuilder()
                    .proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress(proxy.host, proxy.port)))
                    .build()
        }
    }

    @Deprecated("Use getHttpClient() instead")
    fun createJsoupConnection(url: String, trustedOnly: Boolean): Connection {
        if (trustedOnly && !isNetworkTrusted) {
            throw UntrustedNetworkException()
        }

        return Jsoup.connect(url)
    }

    @Deprecated("Use getHttpClient() instead")
    fun createProxiedJsoupConnection(url: String, trustedOnly: Boolean, serviceId: Long): Connection {
        val connection = createJsoupConnection(url, trustedOnly)

        proxyBindingBox[serviceId]
                ?.proxy
                ?.target
                ?.let { connection.proxy(it.host, it.port) }

        return connection
    }

}

class UntrustedNetworkException : IOException {
    constructor() : super()
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(cause: Throwable) : super(cause)
}
