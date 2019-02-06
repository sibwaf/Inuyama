package ru.sibwaf.inuyama

import io.javalin.Javalin
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton

val kodein = Kodein.lazy {
    bind<PairingManager>() with singleton { PairingManager(kodein) }
}

fun main() {
    Javalin.create().start()

    val pairingManager by kodein.instance<PairingManager>()
    pairingManager.toString()
}