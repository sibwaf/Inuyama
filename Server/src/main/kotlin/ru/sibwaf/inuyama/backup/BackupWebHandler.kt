package ru.sibwaf.inuyama.backup

import io.javalin.Javalin
import ru.sibwaf.inuyama.common.BackupPrepareResponse
import ru.sibwaf.inuyama.exception
import ru.sibwaf.inuyama.web.SecurityFilter.Companion.decryptedBody
import ru.sibwaf.inuyama.web.SecurityFilter.Companion.requireSession
import ru.sibwaf.inuyama.web.WebHandler

class BackupWebHandler(private val backupManager: BackupManager) : WebHandler {

    override fun install(javalin: Javalin) {
        javalin.apply {
            get("/backup/:module") { ctx ->
                val session = ctx.requireSession()

                val isReady = backupManager.prepareBackup(
                    deviceId = session.deviceId,
                    module = ctx.pathParam("module")
                )

                ctx.json(BackupPrepareResponse(isReady))
            }

            post("/backup/:module") { ctx ->
                val session = ctx.requireSession()
                backupManager.makeBackup(
                    deviceId = session.deviceId,
                    module = ctx.pathParam("module"),
                    data = ctx.decryptedBody().byteInputStream()
                )
            }

            exception<BackupNotReadyException> { _, ctx ->
                ctx.status(425)
            }
        }
    }
}
