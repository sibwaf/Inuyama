package ru.sibwaf.inuyama.backup

import io.javalin.Javalin
import ru.sibwaf.inuyama.exception
import ru.sibwaf.inuyama.http.HttpHandler
import ru.sibwaf.inuyama.http.SecurityHttpFilter.Companion.decryptBody
import ru.sibwaf.inuyama.http.SecurityHttpFilter.Companion.requireSession
import ru.sibwaf.inuyama.http.pairedSubroute
import ru.sibwaf.inuyama.http.subroute

class BackupHttpHandler(private val backupManager: BackupManager) : HttpHandler {

    override fun Javalin.install() {
        pairedSubroute {
            subroute("/backup") {
                get("/:module/content") { ctx ->
                    val session = ctx.requireSession()
                    val data = backupManager.useLatestBackup(
                        deviceId = session.deviceId,
                        module = ctx.pathParam("module")
                    ) { it.readText() }

                    if (data == null) {
                        ctx.status(404)
                    } else {
                        ctx.result(data)
                    }
                }

                post("/:module") { ctx ->
                    val session = ctx.requireSession()
                    backupManager.makeBackup(
                        deviceId = session.deviceId,
                        module = ctx.pathParam("module"),
                        data = ctx.decryptBody().byteInputStream()
                    )
                }
            }
        }

        exception<BackupNotReadyException> { _, ctx ->
            ctx.status(425)
        }
    }
}
