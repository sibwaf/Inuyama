package ru.dyatel.inuyama.pairing

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import ru.dyatel.inuyama.utilities.PreferenceHelper
import ru.sibwaf.inuyama.common.Pairing
import ru.sibwaf.inuyama.common.utilities.Cryptography
import java.security.KeyPair
import kotlin.coroutines.resume

class PairingManager(
    private val discoveryService: DiscoveryService,
    private val preferenceHelper: PreferenceHelper,
) {

    val deviceKeyPair: KeyPair
        get() = preferenceHelper.keyPair ?: regenerateDeviceKeyPair()

    val deviceIdentifier: String
        get() = preferenceHelper.deviceIdentifier ?: regenerateDeviceIdentifier()

    val isPaired: Boolean
        get() = preferenceHelper.pairedServer != null

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

    fun compareWithPaired(server: PairedServer): Boolean {
        val pairedServer = preferenceHelper.pairedServer ?: return false
        val pairedWasDiscovered = pairedServer.address == null
        return server.key == pairedServer.key && server.wasDiscovered == pairedWasDiscovered
    }

    suspend fun findPairedServer(): PairedServer? {
        val pairedServer = preferenceHelper.pairedServer ?: return null

        if (pairedServer.address != null) {
            return PairedServer(
                host = pairedServer.address.host,
                port = pairedServer.address.port,
                key = pairedServer.key,
                wasDiscovered = false,
            )
        }

        // TODO: magic constants
        var listener: ((PairedServer) -> Unit)? = null
        return try {
            withTimeoutOrNull(10000) {
                suspendCancellableCoroutine<PairedServer> { continuation ->
                    listener = { server: PairedServer ->
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
        preferenceHelper.pairedServer = null
    }

    fun bind(server: PairedServer) {
        preferenceHelper.pairedServer = SavedPairedServer(
            key = server.key,
            address = SavedPairedServer.HostAndPort(
                host = server.host,
                port = server.port,
            ).takeUnless { server.wasDiscovered }
        )
    }
}
