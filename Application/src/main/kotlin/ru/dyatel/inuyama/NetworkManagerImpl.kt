package ru.dyatel.inuyama

import android.net.wifi.SupplicantState
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import io.objectbox.Box
import io.objectbox.kotlin.query
import io.objectbox.query.QueryBuilder.StringOrder.CASE_INSENSITIVE
import okhttp3.OkHttpClient
import org.jsoup.Connection
import org.jsoup.Jsoup
import ru.dyatel.inuyama.model.Network
import ru.dyatel.inuyama.model.Network_
import ru.dyatel.inuyama.model.ProxyBinding
import ru.dyatel.inuyama.model.findByBssid
import sibwaf.inuyama.app.common.NetworkManager
import sibwaf.inuyama.app.common.UntrustedNetworkException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Proxy
import java.nio.ByteBuffer

class NetworkManagerImpl(
    private val wifiManager: WifiManager,
    private val networkBox: Box<Network>,
    private val proxyBindingBox: Box<ProxyBinding>,
) : NetworkManager {

    private val httpClient = OkHttpClient.Builder().build()

    private val currentWifiConnection: WifiInfo?
        get() {
            return wifiManager.connectionInfo
                ?.takeIf { it.bssid != null && it.supplicantState == SupplicantState.COMPLETED }
        }

    override val broadcastAddress: InetAddress?
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

    override val isNetworkTrusted = true

    override fun refreshNetworkList() {
        val current = currentWifiConnection

        networkBox.query {
            equal(Network_.trusted, false)

            if (current != null) {
                and()
                notEqual(Network_.bssid, current.bssid, CASE_INSENSITIVE)
            }
        }.remove()

        if (current != null && networkBox.findByBssid(current.bssid) == null) {
            networkBox.put(Network(name = current.ssid, bssid = current.bssid))
        }
    }

    override fun getHttpClient(trustedOnly: Boolean, serviceId: Long?): OkHttpClient {
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
    override fun createJsoupConnection(url: String, trustedOnly: Boolean): Connection {
        if (trustedOnly && !isNetworkTrusted) {
            throw UntrustedNetworkException()
        }

        return Jsoup.connect(url)
    }

    @Deprecated("Use getHttpClient() instead")
    override fun createProxiedJsoupConnection(url: String, trustedOnly: Boolean, serviceId: Long): Connection {
        val connection = createJsoupConnection(url, trustedOnly)

        proxyBindingBox[serviceId]
            ?.proxy
            ?.target
            ?.let { connection.proxy(it.host, it.port) }

        return connection
    }
}
