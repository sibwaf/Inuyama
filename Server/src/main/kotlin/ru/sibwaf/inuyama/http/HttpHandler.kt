package ru.sibwaf.inuyama.http

import io.javalin.Javalin

interface HttpHandler {
    fun install(javalin: Javalin)
}
