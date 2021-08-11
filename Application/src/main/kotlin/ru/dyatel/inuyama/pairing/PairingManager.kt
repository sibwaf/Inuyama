package ru.dyatel.inuyama.pairing

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.utilities.PreferenceHelper
import ru.sibwaf.inuyama.common.Pairing
import ru.sibwaf.inuyama.common.utilities.Cryptography
import sibwaf.inuyama.app.common.NetworkManager
import java.security.KeyPair
import java.security.PublicKey
import kotlin.coroutines.resume

data class PairedServer(val key: PublicKey)

class PairingManager(override val kodein: Kodein) : KodeinAware {

    private val networkManager by instance<NetworkManager>()
    private val discoveryService by instance<DiscoveryService>()

    private val preferenceHelper by instance<PreferenceHelper>()

    val deviceKeyPair: KeyPair
        get() = preferenceHelper.keyPair ?: regenerateDeviceKeyPair()

    val deviceIdentifier: String
        get() = preferenceHelper.deviceIdentifier ?: regenerateDeviceIdentifier()

    var pairedServer: PairedServer?
        get() = preferenceHelper.pairedServer
        set(value) {
            preferenceHelper.pairedServer = value
        }

    fun regenerateDeviceKeyPair(): KeyPair {
        val keyPair = Cryptography.createRSAKeyPair()
        preferenceHelper.keyPair = keyPair
        return keyPair
    }

    fun regenerateDeviceIdentifier(): String {
        val identifier = Pairing.generateDeviceIdentifier()
        preferenceHelper.deviceIdentifier = identifier
        return identifier
    }

    fun compareWithPaired(server: DiscoveredServer): Boolean {
        return server.key == pairedServer?.key
    }

    suspend fun findPairedServer(): DiscoveredServer? {
        if (pairedServer == null) {
            return null
        }

        // TODO: magic constants
        var listener: ((DiscoveredServer) -> Unit)? = null
        return try {
            withTimeoutOrNull(10000) {
                suspendCancellableCoroutine<DiscoveredServer> { continuation ->
                    listener = { server: DiscoveredServer ->
                        if (compareWithPaired(server) && !continuation.isCompleted) {
                            continuation.resume(server)
                        }
                    }.also { discoveryService.addListener(it) }

                    launch {
                        while (continuation.isActive) {
                            discoveryService.sendDiscoverRequest()
                            delay(1000)
                        }
                    }
                }
            }
        } finally {
            listener?.let { discoveryService.removeListener(it) }
        }
    }

    fun unbind() {
        pairedServer = null
    }

    fun bind(server: DiscoveredServer) {
        pairedServer = PairedServer(server.key)
    }

}
