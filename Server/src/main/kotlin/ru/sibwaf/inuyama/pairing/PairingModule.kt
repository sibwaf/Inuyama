package ru.sibwaf.inuyama.pairing

import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton
import ru.sibwaf.inuyama.Module

val pairingModule = Kodein.Module("pairing") {
    bind<PairingManager>() with singleton { PairingManager(kodein) }
    bind<DeviceManager>() with singleton { DeviceManager() }

    bind<PairingHttpHandler>() with singleton { PairingHttpHandler(instance()) }

    bind<PairingModule>() with singleton { PairingModule(instance()) }
}

class PairingModule(private val pairingManager: PairingManager) : Module {
    override fun install() {
        pairingManager.start()
    }
}
