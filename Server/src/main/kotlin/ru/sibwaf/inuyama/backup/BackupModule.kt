package ru.sibwaf.inuyama.backup

import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton

val backupModule = Kodein.Module("backup") {
    bind<BackupManager>() with singleton {
        BackupManager()
    }

    bind<BackupHttpHandler>() with singleton {
        BackupHttpHandler(instance())
    }
}
