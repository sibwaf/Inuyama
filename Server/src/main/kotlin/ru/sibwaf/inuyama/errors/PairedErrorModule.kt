package ru.sibwaf.inuyama.errors

import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.singleton

val pairedErrorModule = Kodein.Module("errors") {
    bind<PairedErrorHttpHandler>() with singleton {
        PairedErrorHttpHandler()
    }
}
