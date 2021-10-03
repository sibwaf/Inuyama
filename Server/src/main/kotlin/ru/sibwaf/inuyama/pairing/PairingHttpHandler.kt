package ru.sibwaf.inuyama.pairing

import io.javalin.Javalin
import ru.sibwaf.inuyama.http.HttpHandler
import ru.sibwaf.inuyama.http.subroute
import ru.sibwaf.inuyama.http.webSubroute

class PairingHttpHandler(private val deviceManager: DeviceManager) : HttpHandler {

    override fun Javalin.install() {
        webSubroute {
            subroute("/devices") {
                get("/") {
                    it.json(deviceManager.listDevices())
                }
            }
        }
    }
}
