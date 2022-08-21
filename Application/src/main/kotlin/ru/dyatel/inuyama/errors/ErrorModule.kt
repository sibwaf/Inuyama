package ru.dyatel.inuyama.errors

import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton

val errorModule = Kodein.Module("errors") {
    bind<ErrorLogManager>() with singleton {
        ErrorLogManager(
            preferenceHelper = instance(),
        )
    }

    bind<ErrorLogSenderService>() with singleton {
        ErrorLogSenderService(
            pairedApi = instance(),
            errorLogManager = instance(),
        )
    }
}
