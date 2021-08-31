package ru.sibwaf.inuyama.http

import io.javalin.Javalin

interface HttpHandler {

    val insecurePaths: Set<String> get() = emptySet()

    fun install(javalin: Javalin)
}
