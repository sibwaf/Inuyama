package ru.sibwaf.inuyama.http

import io.javalin.Javalin

fun interface HttpHandler {
    fun Javalin.install()
}
