package ru.sibwaf.inuyama.backup

import io.javalin.Javalin
import ru.sibwaf.inuyama.exception
import ru.sibwaf.inuyama.http.HttpHandler
import ru.sibwaf.inuyama.http.SecurityHttpFilter.Companion.decryptedBody
import ru.sibwaf.inuyama.http.SecurityHttpFilter.Companion.encryptableResponseBody
import ru.sibwaf.inuyama.http.SecurityHttpFilter.Companion.requireSession
import ru.sibwaf.inuyama.http.pairedSubroute
import ru.sibwaf.inuyama.http.subroute

class BackupHttpHandler(private val backupManager: BackupManager) : HttpHandler {

    override fun Javalin.install() {
        pairedSubroute {
            subroute("/backup") {
                get("/{module}/content") { ctx ->
                    val session = ctx.requireSession()
                    val data = backupManager.getLatestBackupContent(
                        deviceId = session.deviceId,
                        module = ctx.pathParam("module")
                    )

                    if (data == null) {
                        ctx.status(404)
                    } else {
                        ctx.encryptableResponseBody = data
                    }
                }

                post("/{module}") { ctx ->
                    val session = ctx.requireSession()
                    ctx.decryptedBody().use {
                        backupManager.makeBackup(
                            deviceId = session.deviceId,
                            module = ctx.pathParam("module"),
                            data = it
                        )
                    }
                }
            }
        }

        exception<BackupNotReadyException> { _, ctx ->
            ctx.status(425)
        }
    }
}
