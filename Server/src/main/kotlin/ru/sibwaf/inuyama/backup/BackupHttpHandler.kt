package ru.sibwaf.inuyama.backup

import io.javalin.Javalin
import ru.sibwaf.inuyama.common.BackupPrepareResponse
import ru.sibwaf.inuyama.exception
import ru.sibwaf.inuyama.http.HttpHandler
import ru.sibwaf.inuyama.http.SecurityHttpFilter.Companion.decryptBody
import ru.sibwaf.inuyama.http.SecurityHttpFilter.Companion.requireSession
import ru.sibwaf.inuyama.http.pairedSubroute
import ru.sibwaf.inuyama.http.subroute

class BackupHttpHandler(private val backupManager: BackupManager) : HttpHandler {

    override fun install(javalin: Javalin) {
        javalin.apply {
            pairedSubroute {
                subroute("/backup") {
                    get("/:module") { ctx ->
                        val session = ctx.requireSession()

                        val isReady = backupManager.prepareBackup(
                            deviceId = session.deviceId,
                            module = ctx.pathParam("module")
                        )

                        ctx.json(BackupPrepareResponse(isReady))
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
}
